package com.cableops.tracker.repository;

import com.cableops.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean  existsByUserName(String userName);
    Optional<User> findByUserName(String userName);
}
