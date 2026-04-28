package com.cableops.tracker.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.cableops.tracker.entity.Account;

public interface AccountRepository extends JpaRepository<Account, String> {
	boolean existsByNameIgnoreCase(String name);
}