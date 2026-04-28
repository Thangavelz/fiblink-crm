package com.cableops.tracker.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cableops.tracker.entity.Attachment;
import com.cableops.tracker.repository.AttachmentRepository;
import com.cableops.tracker.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

@RestController
@RequestMapping("/api/v1/Attachment")
@RequiredArgsConstructor
public class AttachmentController {

	private final AttachmentService service;
    private final AttachmentRepository repo;   // ✅ ADD THIS
	@PostMapping
	public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
			@RequestParam("relatedType") String relatedType, @RequestParam("relatedId") String relatedId,
			@RequestParam("field") String field) throws Exception {
		return service.upload(file, relatedType, relatedId, field);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Resource> download(@PathVariable("id") String id) {

		try {
			// 🔹 1. Fetch from DB
			Attachment att = repo.findById(id).orElseThrow(() -> new RuntimeException("File not found in DB"));

			// 🔹 2. Resolve file path (important: handle uploads folder properly)
			Path filePath = Paths.get("").toAbsolutePath().resolve(att.getPath()).normalize();

			// 🔹 3. Load file
			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				throw new RuntimeException("File not found on disk");
			}

			// 🔹 4. Return response (inline = open in browser)
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(att.getType()))
					.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + att.getName() + "\"")
					.body(resource);

		} catch (Exception e) {
			throw new RuntimeException("Error while downloading file: " + e.getMessage());
		}
	}
}