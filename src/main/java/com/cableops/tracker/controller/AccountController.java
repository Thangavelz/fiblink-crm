package com.cableops.tracker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.cableops.tracker.service.AccountService;
import com.cableops.tracker.dto.AccountRequest;
import com.cableops.tracker.dto.AccountResponse;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService service;

	@PostMapping
	public AccountResponse create(@RequestBody AccountRequest req) {
		return service.create(req);
	}

	@GetMapping("/{id}")
	public AccountResponse get(@PathVariable("id") String id) {
		return service.get(id);
	}

	@PutMapping("/{id}")
	public AccountResponse update(@PathVariable("id") String id, @RequestBody AccountRequest req) {
		return service.update(id, req);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable("id") String id) {
		service.delete(id);
	}

	@GetMapping
	public List<AccountResponse> list() {
		return service.list();
	}

}