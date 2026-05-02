package com.cableops.tracker.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.cableops.tracker.dto.AccountRequest;
import com.cableops.tracker.dto.AccountResponse;
import com.cableops.tracker.entity.Account;
import com.cableops.tracker.repository.AccountRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repo;

    public AccountResponse create(AccountRequest req) {

        if (repo.existsByNameIgnoreCase(req.getName())) {
            throw new RuntimeException("The record you are creating might already exist");
        }

        Account a = new Account();
        a.setId(req.getId() != null ? req.getId() : UUID.randomUUID().toString());

        map(a, req);

        a.setCreatedAt(LocalDateTime.now());
        a.setModifiedAt(LocalDateTime.now());

        return toResponse(repo.save(a));
    }

    public AccountResponse update(String id, AccountRequest req) {

        Account a = repo.findById(id).orElseThrow();

        map(a, req);
        a.setModifiedAt(LocalDateTime.now());

        return toResponse(repo.save(a));
    }

    public AccountResponse get(String id) {
        return toResponse(repo.findById(id).orElseThrow());
    }

    public List<AccountResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    private void map(Account a, AccountRequest r) {
        a.setName(r.getName());
        a.setEmailAddress(r.getEmailAddress());
        a.setPhoneNumber(r.getPhoneNumber());
        a.setType(r.getType());
        a.setIndustry(r.getIndustry());

        a.setBillingAddressStreet(r.getBillingAddressStreet());
        a.setBillingAddressCity(r.getBillingAddressCity());
        a.setBillingAddressState(r.getBillingAddressState());
        a.setBillingAddressCountry(r.getBillingAddressCountry());
        a.setBillingAddressPostalCode(r.getBillingAddressPostalCode());

        a.setShippingAddressStreet(r.getShippingAddressStreet());
        a.setShippingAddressCity(r.getShippingAddressCity());
        a.setShippingAddressState(r.getShippingAddressState());
        a.setShippingAddressCountry(r.getShippingAddressCountry());
        a.setShippingAddressPostalCode(r.getShippingAddressPostalCode());

        a.setDescription(r.getDescription());
        a.setCTelegramgroupchatid(r.getCTelegramgroupchatid());
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getName(),
                a.getEmailAddress(),
                a.getPhoneNumber(),
                a.getType(),
                a.getIndustry(),

                a.getBillingAddressStreet(),
                a.getBillingAddressCity(),
                a.getBillingAddressState(),
                a.getBillingAddressCountry(),
                a.getBillingAddressPostalCode(),

                a.getShippingAddressStreet(),
                a.getShippingAddressCity(),
                a.getShippingAddressState(),
                a.getShippingAddressCountry(),
                a.getShippingAddressPostalCode(),

                a.getDescription(),
                a.getWebsite(), 
                a.getCTelegramgroupchatid(),

                a.getCreatedAt(),
                a.getModifiedAt()
        );
    }
}