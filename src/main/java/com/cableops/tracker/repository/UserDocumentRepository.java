package com.cableops.tracker.repository;

import com.cableops.tracker.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDocumentRepository extends JpaRepository<UserDocument, String> {
    List<UserDocument> findByUserId(String userId);
}