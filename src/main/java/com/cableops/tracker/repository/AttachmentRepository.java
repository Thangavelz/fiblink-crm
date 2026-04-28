package com.cableops.tracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cableops.tracker.entity.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, String> {
    List<Attachment> findByRelatedIdAndRelatedType(String relatedId, String relatedType);
}
