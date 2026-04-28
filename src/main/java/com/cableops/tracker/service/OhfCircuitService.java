package com.cableops.tracker.service;

import com.cableops.tracker.dto.OhfCircuitRequest;
import com.cableops.tracker.dto.OhfCircuitResponse;
import com.cableops.tracker.entity.OhfCircuit;
import com.cableops.tracker.repository.AccountRepository;
import com.cableops.tracker.repository.OhfCircuitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OhfCircuitService {

    // ── Allowed enums ─────────────────────────────────────────────────────────
    private static final List<String> ALLOWED_CIRCUIT_TYPES =
            List.of("Infra", "Ent Customer");

    private static final List<String> ALLOWED_STATUSES =
            List.of("Under Provisioning", "Active", "Pending/Hold",
                    "Suspended", "Acceptance Pending");

    private static final List<String> ALLOWED_FIBER_TYPES =
            List.of("Single Core", "Multi Core");

    /** Circuit types that use Tower A / Tower B address blocks. */
    private static final List<String> TOWER_TYPES = List.of("Infra", "OHF", "UG", "MW");

    private final OhfCircuitRepository repo;
    private final AccountRepository    accountRepo;   // ← added to resolve accountName

    // ── CREATE ────────────────────────────────────────────────────────────────
    public OhfCircuitResponse create(OhfCircuitRequest req) {
        validate(req);

        OhfCircuit c = new OhfCircuit();
        c.setId(UUID.randomUUID().toString());
        c.setCircuitId(generateCircuitId());

        map(c, req);
        c.setCreatedAt(LocalDateTime.now());
        c.setModifiedAt(LocalDateTime.now());

        return toResponse(repo.save(c), req);
    }

    // ── GET by UUID ───────────────────────────────────────────────────────────
    public OhfCircuitResponse get(String id) {
        OhfCircuit c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Circuit not found: " + id));
        return toResponse(c, null);
    }

    // ── GET by circuitId serial (e.g. "20260002") ─────────────────────────────
    public OhfCircuitResponse getByCircuitId(String circuitId) {
        OhfCircuit c = repo.findByCircuitId(circuitId)
                .orElseThrow(() -> new RuntimeException("Circuit not found with circuitId: " + circuitId));
        return toResponse(c, null);
    }

    // ── LIST ──────────────────────────────────────────────────────────────────
    public List<OhfCircuitResponse> list() {
        return repo.findAll().stream().map(c -> toResponse(c, null)).toList();
    }

    // ── LIST BY ACCOUNT ───────────────────────────────────────────────────────
    public List<OhfCircuitResponse> listByAccount(String accountId) {
        return repo.findByAccountId(accountId).stream()
                .map(c -> toResponse(c, null)).toList();
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public OhfCircuitResponse update(String id, OhfCircuitRequest req) {
        OhfCircuit c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Circuit not found: " + id));

        validate(req);
        map(c, req);
        c.setModifiedAt(LocalDateTime.now());

        return toResponse(repo.save(c), req);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Circuit not found: " + id);
        }
        repo.deleteById(id);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private void validate(OhfCircuitRequest req) {
        if (req.getCircuitType() != null && !ALLOWED_CIRCUIT_TYPES.contains(req.getCircuitType())) {
            throw new RuntimeException("Invalid circuitType '" + req.getCircuitType()
                    + "'. Allowed: " + ALLOWED_CIRCUIT_TYPES);
        }
        if (req.getCircuitStatus() != null && !ALLOWED_STATUSES.contains(req.getCircuitStatus())) {
            throw new RuntimeException("Invalid circuitStatus '" + req.getCircuitStatus()
                    + "'. Allowed: " + ALLOWED_STATUSES);
        }
        if (req.getFiberType() != null && !ALLOWED_FIBER_TYPES.contains(req.getFiberType())) {
            throw new RuntimeException("Invalid fiberType '" + req.getFiberType()
                    + "'. Allowed: " + ALLOWED_FIBER_TYPES);
        }
    }

    private String generateCircuitId() {
        long count = repo.count() + 1;
        return Year.now().getValue() + String.format("%04d", count);
    }

    private void map(OhfCircuit c, OhfCircuitRequest r) {

        // ── Overview ──────────────────────────────────────────────────────────
        if (r.getName()               != null) c.setName(r.getName());
        if (r.getCircuitType()        != null) c.setCircuitType(r.getCircuitType());
        if (r.getCircuitStatus()      != null) c.setCircuitStatus(r.getCircuitStatus());
        if (r.getFiberType()          != null) c.setFiberType(r.getFiberType());
        if (r.getActualLinkDistance() != null) c.setActualLinkDistance(r.getActualLinkDistance());
        if (r.getDescription()        != null) c.setDescription(r.getDescription());

        // ── Account — resolve name from DB if not supplied in request ─────────
        if (r.getAccountId() != null) {
            c.setAccountId(r.getAccountId());
            // Use the name from the request if provided; otherwise look it up
            if (r.getAccountName() != null) {
                c.setAccountName(r.getAccountName());
            } else {
                accountRepo.findById(r.getAccountId()).ifPresent(acc ->
                        c.setAccountName(acc.getName()));
            }
        }

        // ── Contact ───────────────────────────────────────────────────────────
        if (r.getContactId()   != null) c.setContactId(r.getContactId());
        if (r.getContactName() != null) c.setContactName(r.getContactName());

        // ── Address blocks — use the SAVED circuit type as fallback ───────────
        // This prevents overwriting address fields when circuitType is null in the request
        String effectiveType = r.getCircuitType() != null ? r.getCircuitType() : c.getCircuitType();
        boolean isTowerType  = effectiveType == null || TOWER_TYPES.contains(effectiveType);

        if (isTowerType) {
            if (r.getTowerAAddressStreet()      != null) c.setTowerAAddressStreet(r.getTowerAAddressStreet());
            if (r.getTowerAAddressCity()        != null) c.setTowerAAddressCity(r.getTowerAAddressCity());
            if (r.getTowerAAddressState()       != null) c.setTowerAAddressState(r.getTowerAAddressState());
            if (r.getTowerAAddressCountry()     != null) c.setTowerAAddressCountry(r.getTowerAAddressCountry());
            if (r.getTowerAAddressPostalCode()  != null) c.setTowerAAddressPostalCode(r.getTowerAAddressPostalCode());
            if (r.getTowerALocation()           != null) c.setTowerALocation(r.getTowerALocation());
            if (r.getTowerALatLong()            != null) c.setTowerALatLong(r.getTowerALatLong());

            if (r.getTowerBAddressStreet()      != null) c.setTowerBAddressStreet(r.getTowerBAddressStreet());
            if (r.getTowerBAddressCity()        != null) c.setTowerBAddressCity(r.getTowerBAddressCity());
            if (r.getTowerBAddressState()       != null) c.setTowerBAddressState(r.getTowerBAddressState());
            if (r.getTowerBAddressCountry()     != null) c.setTowerBAddressCountry(r.getTowerBAddressCountry());
            if (r.getTowerBAddressPostalCode()  != null) c.setTowerBAddressPostalCode(r.getTowerBAddressPostalCode());
            if (r.getTowerBLocation()           != null) c.setTowerBLocation(r.getTowerBLocation());
            if (r.getTowerBLatLong()            != null) c.setTowerBLatLong(r.getTowerBLatLong());
        } else {
            if (r.getCustomerAddressStreet()     != null) c.setCustomerAddressStreet(r.getCustomerAddressStreet());
            if (r.getCustomerAddressCity()       != null) c.setCustomerAddressCity(r.getCustomerAddressCity());
            if (r.getCustomerAddressState()      != null) c.setCustomerAddressState(r.getCustomerAddressState());
            if (r.getCustomerAddressCountry()    != null) c.setCustomerAddressCountry(r.getCustomerAddressCountry());
            if (r.getCustomerAddressPostalCode() != null) c.setCustomerAddressPostalCode(r.getCustomerAddressPostalCode());
            if (r.getCustomerlocation()          != null) c.setCustomerlocation(r.getCustomerlocation());
            if (r.getCustometLatLong()           != null) c.setCustometLatLong(r.getCustometLatLong());

            if (r.getPOPAddressStreet()          != null) c.setPOPAddressStreet(r.getPOPAddressStreet());
            if (r.getPOPAddressCity()            != null) c.setPOPAddressCity(r.getPOPAddressCity());
            if (r.getPOPAddressState()           != null) c.setPOPAddressState(r.getPOPAddressState());
            if (r.getPOPAddressCountry()         != null) c.setPOPAddressCountry(r.getPOPAddressCountry());
            if (r.getPOPAddressPostalCode()      != null) c.setPOPAddressPostalCode(r.getPOPAddressPostalCode());
            if (r.getPOPLocation()               != null) c.setPOPLocation(r.getPOPLocation());
            if (r.getPOPLatLong()                != null) c.setPOPLatLong(r.getPOPLatLong());
        }

        // ── Commercial (all types) ────────────────────────────────────────────
        if (r.getPONumber()                      != null) c.setPONumber(r.getPONumber());
        if (r.getPODate()                        != null) c.setPODate(r.getPODate());
        if (r.getPodistanceinkm()                != null) c.setPodistanceinkm(r.getPodistanceinkm());
        if (r.getAcceptanceDate()                != null) c.setAcceptanceDate(r.getAcceptanceDate());
        if (r.getOneTimeCharges()                != null) c.setOneTimeCharges(r.getOneTimeCharges());
        if (r.getOneTimeChargesCurrency()        != null) c.setOneTimeChargesCurrency(r.getOneTimeChargesCurrency());
        if (r.getArcAmount()                     != null) c.setArcAmount(r.getArcAmount());
        if (r.getArcasperRatecardperKm()         != null) c.setArcasperRatecardperKm(r.getArcasperRatecardperKm());
        if (r.getArcasperRatecardperKmCurrency() != null) c.setArcasperRatecardperKmCurrency(r.getArcasperRatecardperKmCurrency());
        if (r.getAmountPerKmPerMonth()           != null) c.setAmountPerKmPerMonth(r.getAmountPerKmPerMonth());
    }

    private OhfCircuitResponse toResponse(OhfCircuit c, OhfCircuitRequest req) {
        OhfCircuitResponse res = new OhfCircuitResponse();

        res.setId(c.getId());
        res.setName(c.getName());
        res.setCircuitType(c.getCircuitType());
        res.setCircuitId(c.getCircuitId());
        res.setCircuitStatus(c.getCircuitStatus());
        res.setFiberType(c.getFiberType());
        res.setActualLinkDistance(c.getActualLinkDistance());
        res.setDescription(c.getDescription());

        res.setAccountId(c.getAccountId());
        res.setAccountName(c.getAccountName());
        res.setContactId(c.getContactId());
        res.setContactName(c.getContactName());

        res.setTowerAAddressStreet(c.getTowerAAddressStreet());
        res.setTowerAAddressCity(c.getTowerAAddressCity());
        res.setTowerAAddressState(c.getTowerAAddressState());
        res.setTowerAAddressCountry(c.getTowerAAddressCountry());
        res.setTowerAAddressPostalCode(c.getTowerAAddressPostalCode());
        res.setTowerALocation(c.getTowerALocation());
        res.setTowerALatLong(c.getTowerALatLong());

        res.setTowerBAddressStreet(c.getTowerBAddressStreet());
        res.setTowerBAddressCity(c.getTowerBAddressCity());
        res.setTowerBAddressState(c.getTowerBAddressState());
        res.setTowerBAddressCountry(c.getTowerBAddressCountry());
        res.setTowerBAddressPostalCode(c.getTowerBAddressPostalCode());
        res.setTowerBLocation(c.getTowerBLocation());
        res.setTowerBLatLong(c.getTowerBLatLong());

        res.setCustomerAddressStreet(c.getCustomerAddressStreet());
        res.setCustomerAddressCity(c.getCustomerAddressCity());
        res.setCustomerAddressState(c.getCustomerAddressState());
        res.setCustomerAddressCountry(c.getCustomerAddressCountry());
        res.setCustomerAddressPostalCode(c.getCustomerAddressPostalCode());
        res.setCustomerlocation(c.getCustomerlocation());
        res.setCustometLatLong(c.getCustometLatLong());

        res.setPOPAddressStreet(c.getPOPAddressStreet());
        res.setPOPAddressCity(c.getPOPAddressCity());
        res.setPOPAddressState(c.getPOPAddressState());
        res.setPOPAddressCountry(c.getPOPAddressCountry());
        res.setPOPAddressPostalCode(c.getPOPAddressPostalCode());
        res.setPOPLocation(c.getPOPLocation());
        res.setPOPLatLong(c.getPOPLatLong());

        res.setPONumber(c.getPONumber());
        res.setPODate(c.getPODate());
        res.setPodistanceinkm(c.getPodistanceinkm());
        res.setAcceptanceDate(c.getAcceptanceDate());
        res.setOneTimeCharges(c.getOneTimeCharges());
        res.setOneTimeChargesCurrency(c.getOneTimeChargesCurrency());
        res.setArcAmount(c.getArcAmount());
        res.setArcasperRatecardperKm(c.getArcasperRatecardperKm());
        res.setArcasperRatecardperKmCurrency(c.getArcasperRatecardperKmCurrency());
        res.setAmountPerKmPerMonth(c.getAmountPerKmPerMonth());

        res.setCreatedAt(c.getCreatedAt());
        res.setModifiedAt(c.getModifiedAt());

        if (req != null) {
            res.setTeamsIds(req.getTeamsIds());
            res.setTeamsNames(req.getTeamsNames());
            res.setAssignedUsersIds(req.getAssignedUsersIds());
            res.setAssignedUsersNames(req.getAssignedUsersNames());
            res.setCollaboratorsIds(req.getCollaboratorsIds());
            res.setCollaboratorsNames(req.getCollaboratorsNames());
        }

        return res;
    }
}