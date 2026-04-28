package com.cableops.tracker.service;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import com.cableops.tracker.entity.Attachment;
import com.cableops.tracker.repository.AttachmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Path;
import java.nio.file.Paths;
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository repo;

    private static final String UPLOAD_DIR = "uploads";

    public Map<String, Object> upload(MultipartFile file,
                                      String relatedType,
                                      String relatedId,
                                      String field) throws Exception {

        // 🔴 1. Validate file
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // 🔴 2. Create uploads folder if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 🔴 3. Generate unique file name
        String id = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename();
        String safeName = originalName != null ? originalName.replaceAll("\\s+", "_") : "file";

        String fileName = id + "_" + safeName;
        Path filePath = uploadPath.resolve(fileName);

        // 🔴 4. Save file (replace if exists)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 🔴 5. Save DB
        Attachment a = new Attachment();
        a.setId(id);
        a.setName(originalName);
        a.setType(file.getContentType());
        a.setSize(file.getSize());
        a.setPath(filePath.toString());
        a.setRelatedType(relatedType);
        a.setRelatedId(relatedId);
        a.setField(field);
        a.setCreatedAt(LocalDateTime.now());

        repo.save(a);

        // 🔴 6. Response (Espo-style minimal)
        return Map.of(
                "id", id,
                "name", a.getName(),
                "type", a.getType(),
                "size", a.getSize(),
                "path", a.getPath()
        );
    }

    public List<Attachment> getByUser(String userId) {
        return repo.findByRelatedIdAndRelatedType(userId, "User");
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) throws Exception {

        Attachment att = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Path path = Paths.get(att.getPath());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File missing on disk");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(att.getType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + att.getName() + "\"")
                .body(resource);
    }
}