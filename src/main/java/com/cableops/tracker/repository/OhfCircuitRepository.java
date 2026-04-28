package com.cableops.tracker.repository;

import com.cableops.tracker.entity.OhfCircuit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OhfCircuitRepository extends JpaRepository<OhfCircuit, String> {
    List<OhfCircuit> findByAccountId(String accountId);
    List<OhfCircuit> findByCircuitStatus(String circuitStatus);
    Optional<OhfCircuit> findByCircuitId(String circuitId);   // lookup by serial e.g. "20260002"
}