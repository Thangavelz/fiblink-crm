package com.cableops.tracker.service;

import com.cableops.tracker.dto.EbbMllRequest;
import com.cableops.tracker.dto.EbbMllResponse;
import com.cableops.tracker.entity.EbbMll;
import com.cableops.tracker.repository.AccountRepository;
import com.cableops.tracker.repository.EbbMllRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EbbMllService {

    private final EbbMllRepository repo;
    private final AccountRepository accountRepo;

    // ── CREATE ────────────────────────────────────────────────────────────────
    public EbbMllResponse create(EbbMllRequest req) {
        EbbMll e = new EbbMll();
        e.setId(UUID.randomUUID().toString());
        e.setCreatedAt(LocalDateTime.now());
        e.setModifiedAt(LocalDateTime.now());
        map(e, req);
        return toResponse(repo.save(e));
    }

    // ── GET ───────────────────────────────────────────────────────────────────
    public EbbMllResponse get(String id) {
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new RuntimeException("EBB/MLL not found: " + id)));
    }

    // ── LIST ──────────────────────────────────────────────────────────────────
    public List<EbbMllResponse> list(String productType, String accountId, String assignedUserId) {
        List<EbbMll> items;
        if (productType != null)       items = repo.findByProductType(productType);
        else if (accountId != null)    items = repo.findByAccountId(accountId);
        else if (assignedUserId != null) items = repo.findByAssignedUserId(assignedUserId);
        else                           items = repo.findAll();
        return items.stream().map(this::toResponse).toList();
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public EbbMllResponse update(String id, EbbMllRequest req) {
        EbbMll e = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("EBB/MLL not found: " + id));
        map(e, req);
        e.setModifiedAt(LocalDateTime.now());
        return toResponse(repo.save(e));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public void delete(String id) {
        if (!repo.existsById(id)) throw new RuntimeException("EBB/MLL not found: " + id);
        repo.deleteById(id);
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private void map(EbbMll e, EbbMllRequest r) {
        if (r.getName()             != null) e.setName(r.getName());
        if (r.getProductType()      != null) e.setProductType(r.getProductType());

        // Account — resolve name from DB if not supplied
        if (r.getAccountId() != null) {
            e.setAccountId(r.getAccountId());
            if (r.getAccountName() != null) {
                e.setAccountName(r.getAccountName());
            } else {
                accountRepo.findById(r.getAccountId())
                        .ifPresent(a -> e.setAccountName(a.getName()));
            }
        }

        // Address
        if (r.getAddressStreet()     != null) e.setAddressStreet(r.getAddressStreet());
        if (r.getAddressCity()       != null) e.setAddressCity(r.getAddressCity());
        if (r.getAddressState()      != null) e.setAddressState(r.getAddressState());
        if (r.getAddressPostalCode() != null) e.setAddressPostalCode(r.getAddressPostalCode());
        if (r.getAddressCountry()    != null) e.setAddressCountry(r.getAddressCountry());

        // Customer details
        if (r.getCustomerLocation()  != null) e.setCustomerLocation(r.getCustomerLocation());
        if (r.getCustomerLatLong()   != null) e.setCustomerLatLong(r.getCustomerLatLong());
        if (r.getSpocLcDetails()     != null) e.setSpocLcDetails(r.getSpocLcDetails());
        if (r.getDescription()       != null) e.setDescription(r.getDescription());

        // Assigned user
        if (r.getAssignedUserId()   != null) e.setAssignedUserId(r.getAssignedUserId());
        if (r.getAssignedUserName() != null) e.setAssignedUserName(r.getAssignedUserName());

        // Teams — stored as comma-separated
        if (r.getTeamsIds() != null) {
            e.setTeamIds(String.join(",", r.getTeamsIds()));
        }
        if (r.getTeamsNames() != null && !r.getTeamsNames().isEmpty()) {
            // preserve order from teamsIds
            if (r.getTeamsIds() != null) {
                String names = r.getTeamsIds().stream()
                        .map(id -> r.getTeamsNames().getOrDefault(id, ""))
                        .collect(Collectors.joining(","));
                e.setTeamNames(names);
            }
        }
    }

    private EbbMllResponse toResponse(EbbMll e) {
        EbbMllResponse res = new EbbMllResponse();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setProductType(e.getProductType());
        res.setAccountId(e.getAccountId());
        res.setAccountName(e.getAccountName());
        res.setAddressStreet(e.getAddressStreet());
        res.setAddressCity(e.getAddressCity());
        res.setAddressState(e.getAddressState());
        res.setAddressPostalCode(e.getAddressPostalCode());
        res.setAddressCountry(e.getAddressCountry());
        res.setCustomerLocation(e.getCustomerLocation());
        res.setCustomerLatLong(e.getCustomerLatLong());
        res.setSpocLcDetails(e.getSpocLcDetails());
        res.setDescription(e.getDescription());
        res.setAssignedUserId(e.getAssignedUserId());
        res.setAssignedUserName(e.getAssignedUserName());
        res.setCreatedAt(e.getCreatedAt());
        res.setModifiedAt(e.getModifiedAt());
        res.setCreatedById(e.getCreatedById());
        res.setCreatedByName(e.getCreatedByName());

        // Reconstruct teams lists from comma-separated storage
        if (e.getTeamIds() != null && !e.getTeamIds().isBlank()) {
            List<String> ids = Arrays.asList(e.getTeamIds().split(","));
            res.setTeamsIds(ids);
            if (e.getTeamNames() != null) {
                String[] names = e.getTeamNames().split(",", -1);
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < ids.size(); i++) {
                    map.put(ids.get(i), i < names.length ? names[i] : ids.get(i));
                }
                res.setTeamsNames(map);
            } else {
                res.setTeamsNames(Map.of());
            }
        } else {
            res.setTeamsIds(List.of());
            res.setTeamsNames(Map.of());
        }

        return res;
    }
}