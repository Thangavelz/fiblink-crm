package com.cableops.tracker.service;

import com.cableops.tracker.dto.*;
import com.cableops.tracker.entity.*;
import com.cableops.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository        itemRepo;
    private final InventoryStockRepository       stockRepo;
    private final InventoryTransactionRepository txnRepo;
    private final UserRepository                 userRepo;   // ← to resolve name from userId

    // ─── Inventory Items (catalogue) ────────────────────────────────────────

    @Transactional
    public InventoryItemResponse createItem(InventoryItemRequest req) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID().toString());
        mapItem(item, req);
        return toItemResponse(itemRepo.save(item));
    }

    public InventoryItemResponse getItem(String id) {
        return toItemResponse(itemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id)));
    }

    public List<InventoryItemResponse> listItems(String itemType, Boolean activeOnly) {
        List<InventoryItem> all;
        if (itemType != null && activeOnly != null && activeOnly) {
            all = itemRepo.findByItemTypeAndIsActive(itemType, true);
        } else if (activeOnly != null && activeOnly) {
            all = itemRepo.findByIsActive(true);
        } else {
            all = itemRepo.findAll();
        }
        return all.stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    @Transactional
    public InventoryItemResponse updateItem(String id, InventoryItemRequest req) {
        InventoryItem item = itemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));
        mapItem(item, req);
        return toItemResponse(itemRepo.save(item));
    }

    // ─── Inward Entry ────────────────────────────────────────────────────────

    @Transactional
    public InventoryTransactionResponse recordInward(InwardRequest req) {
        if (req.getQuantity() == null || req.getQuantity() <= 0)
            throw new RuntimeException("Quantity must be positive");

        // Item may be a fixed catalogue id (e.g. "ofc-4f") — tolerate missing
        String itemName = req.getItemName() != null ? req.getItemName() : req.getItemId();
        String itemType = resolveItemType(req.getItemId());

        InventoryStock stock = stockRepo
                .findByItemIdAndStoreAreaCode(req.getItemId(), req.getStoreAreaCode())
                .orElseGet(() -> {
                    InventoryStock s = new InventoryStock();
                    s.setId(UUID.randomUUID().toString());
                    s.setItemId(req.getItemId());
                    s.setStoreAreaCode(req.getStoreAreaCode());
                    s.setStoreName(req.getStoreName());
                    s.setQuantityOnHand(0.0);
                    return s;
                });

        // Update store name if provided
        if (req.getStoreName() != null) stock.setStoreName(req.getStoreName());

        double after = stock.getQuantityOnHand() + req.getQuantity();
        stock.setQuantityOnHand(after);
        stockRepo.save(stock);

        return saveTransaction("INWARD", req.getItemId(), itemName,
                stock.getStoreAreaCode(), stock.getStoreName(),
                req.getQuantity(), after,
                null, null, req.getNotes());
    }

    // ─── Usage Entry ─────────────────────────────────────────────────────────

    @Transactional
    public InventoryTransactionResponse recordUsage(UsageRequest req) {
        if (req.getQuantity() == null || req.getQuantity() <= 0)
            throw new RuntimeException("Quantity must be positive");

        String itemName = req.getItemId() != null ? req.getItemId() : req.getItemId();

        InventoryStock stock = stockRepo
                .findByItemIdAndStoreAreaCode(req.getItemId(), req.getStoreAreaCode())
                .orElseThrow(() -> new RuntimeException(
                        "No stock for item " + req.getItemId()
                        + " in store " + req.getStoreAreaCode()));

        if (stock.getQuantityOnHand() < req.getQuantity())
            throw new RuntimeException("Insufficient stock: available "
                    + stock.getQuantityOnHand());

        double after = stock.getQuantityOnHand() - req.getQuantity();
        stock.setQuantityOnHand(after);
        stockRepo.save(stock);

        return saveTransaction("USAGE", req.getItemId(), itemName,
                stock.getStoreAreaCode(), stock.getStoreName(),
                -req.getQuantity(), after,
                req.getTaskId(), req.getTaskName(), req.getNotes());
    }

    // ─── Stock Query ──────────────────────────────────────────────────────────

    public List<InventoryStockResponse> listStock(String storeAreaCode, String itemType) {
        List<InventoryStock> stocks = storeAreaCode != null
                ? stockRepo.findByStoreAreaCode(storeAreaCode)
                : stockRepo.findAll();

        return stocks.stream()
                .filter(s -> {
                    if (itemType == null) return true;
                    InventoryItem it = itemRepo.findById(s.getItemId()).orElse(null);
                    return it != null && itemType.equals(it.getItemType());
                })
                .map(this::toStockResponse)
                .collect(Collectors.toList());
    }

    // ─── Audit Log ────────────────────────────────────────────────────────────

    public List<InventoryTransactionResponse> listTransactions(
            String storeAreaCode, String itemId, String txnType) {
        List<InventoryTransaction> txns;
        if (storeAreaCode != null) {
            txns = txnRepo.findByStoreAreaCodeOrderByCreatedAtDesc(storeAreaCode);
        } else if (itemId != null) {
            txns = txnRepo.findByItemIdOrderByCreatedAtDesc(itemId);
        } else if (txnType != null) {
            txns = txnRepo.findByTxnTypeOrderByCreatedAtDesc(txnType);
        } else {
            txns = txnRepo.findAllByOrderByCreatedAtDesc();
        }
        return txns.stream().map(this::toTxnResponse).collect(Collectors.toList());
    }

    // ─── Summary ──────────────────────────────────────────────────────────────

    public java.util.Map<String, Object> summary(String storeAreaCode) {
        List<InventoryStock> stocks = storeAreaCode != null
                ? stockRepo.findByStoreAreaCode(storeAreaCode)
                : stockRepo.findAll();

        long totalItems    = stocks.size();
        long lowStockItems = stocks.stream()
                .filter(s -> s.getReorderLevel() != null
                          && s.getQuantityOnHand() <= s.getReorderLevel())
                .count();
        long ofcStocks = stocks.stream()
                .filter(s -> "OFC_CABLE".equals(resolveItemType(s.getItemId())))
                .count();
        long jcvStocks = stocks.stream()
                .filter(s -> "JCV_BOX".equals(resolveItemType(s.getItemId())))
                .count();

        return java.util.Map.of(
                "totalItems",    totalItems,
                "lowStockItems", lowStockItems,
                "ofcStocks",     ofcStocks,
                "jcvStocks",     jcvStocks
        );
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    /**
     * Saves an audit transaction row.
     * Reads the current authenticated user from Spring Security context.
     * JwtAuthFilter sets principal = userId (String), so we look up the name.
     */
    private InventoryTransactionResponse saveTransaction(
            String txnType,
            String itemId, String itemName,
            String storeAreaCode, String storeName,
            double change, double after,
            String taskId, String taskName,
            String notes) {

        // ── Resolve current user from Security context ──
        // JwtAuthFilter sets principal = userId String (not a User entity)
        String performedById   = null;
        String performedByName = null;
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String uid && !uid.isBlank()) {
                performedById = uid;
                // Look up full name from DB
                performedByName = userRepo.findById(uid)
                        .map(u -> u.getName() != null ? u.getName() : u.getUserName())
                        .orElse(uid);
            }
        } catch (Exception ignored) {}

        InventoryTransaction txn = new InventoryTransaction();
        txn.setId(UUID.randomUUID().toString());
        txn.setTxnType(txnType);
        txn.setItemId(itemId);
        txn.setItemName(itemName);
        txn.setStoreAreaCode(storeAreaCode);
        txn.setStoreName(storeName);
        txn.setQuantityChange(change);
        txn.setQuantityAfter(after);
        txn.setTaskId(taskId);
        txn.setTaskName(taskName);
        txn.setPerformedById(performedById);
        txn.setPerformedByName(performedByName);
        txn.setNotes(notes);

        return toTxnResponse(txnRepo.save(txn));
    }

    /** Resolve item type from DB, or fall back to id prefix for fixed catalogue */
    private String resolveItemType(String itemId) {
        if (itemId == null) return null;
        return itemRepo.findById(itemId)
                .map(InventoryItem::getItemType)
                .orElseGet(() -> {
                    if (itemId.startsWith("ofc-")) return "OFC_CABLE";
                    if (itemId.startsWith("jcv-")) return "JCV_BOX";
                    return null;
                });
    }

    private void mapItem(InventoryItem item, InventoryItemRequest req) {
        if (req.getItemType()    != null) item.setItemType(req.getItemType());
        if (req.getItemName()    != null) item.setItemName(req.getItemName());
        if (req.getCableType()   != null) item.setCableType(req.getCableType());
        if (req.getCoreCount()   != null) item.setCoreCount(req.getCoreCount());
        if (req.getBoxSize()     != null) item.setBoxSize(req.getBoxSize());
        if (req.getUnit()        != null) item.setUnit(req.getUnit());
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getIsActive()    != null) item.setIsActive(req.getIsActive());
        else if (item.getIsActive() == null) item.setIsActive(true);
        if (item.getUnit() == null)
            item.setUnit("OFC_CABLE".equals(item.getItemType()) ? "MTR" : "PCS");
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private InventoryItemResponse toItemResponse(InventoryItem i) {
        InventoryItemResponse r = new InventoryItemResponse();
        r.setId(i.getId());           r.setItemType(i.getItemType());
        r.setItemName(i.getItemName()); r.setCableType(i.getCableType());
        r.setCoreCount(i.getCoreCount()); r.setBoxSize(i.getBoxSize());
        r.setUnit(i.getUnit());       r.setDescription(i.getDescription());
        r.setIsActive(i.getIsActive());
        r.setCreatedAt(i.getCreatedAt()); r.setUpdatedAt(i.getUpdatedAt());
        return r;
    }

    private InventoryStockResponse toStockResponse(InventoryStock s) {
        InventoryItem item = itemRepo.findById(s.getItemId()).orElse(null);
        InventoryStockResponse r = new InventoryStockResponse();
        r.setId(s.getId());           r.setItemId(s.getItemId());
        r.setStoreAreaCode(s.getStoreAreaCode()); r.setStoreName(s.getStoreName());
        r.setQuantityOnHand(s.getQuantityOnHand()); r.setReorderLevel(s.getReorderLevel());
        r.setUpdatedAt(s.getUpdatedAt());
        if (item != null) {
            r.setItemName(item.getItemName()); r.setItemType(item.getItemType());
            r.setCableType(item.getCableType()); r.setCoreCount(item.getCoreCount());
            r.setBoxSize(item.getBoxSize());   r.setUnit(item.getUnit());
        } else {
            // Fixed catalogue fallback — item may not be in inventory_items table
            r.setItemName(s.getItemId());
            r.setItemType(resolveItemType(s.getItemId()));
            r.setUnit(s.getItemId().startsWith("ofc-") ? "MTR" : "PCS");
        }
        r.setLowStock(s.getReorderLevel() != null
                && s.getQuantityOnHand() <= s.getReorderLevel());
        return r;
    }

    private InventoryTransactionResponse toTxnResponse(InventoryTransaction t) {
        InventoryTransactionResponse r = new InventoryTransactionResponse();
        r.setId(t.getId());             r.setTxnType(t.getTxnType());
        r.setItemId(t.getItemId());     r.setItemName(t.getItemName());
        r.setStoreAreaCode(t.getStoreAreaCode()); r.setStoreName(t.getStoreName());
        r.setQuantityChange(t.getQuantityChange()); r.setQuantityAfter(t.getQuantityAfter());
        r.setTaskId(t.getTaskId());     r.setTaskName(t.getTaskName());
        r.setPerformedById(t.getPerformedById());
        r.setPerformedByName(t.getPerformedByName());
        r.setNotes(t.getNotes());       r.setCreatedAt(t.getCreatedAt());
        return r;
    }
}