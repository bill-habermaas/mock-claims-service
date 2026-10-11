package com.example.claims.claim;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class MongoClaimStore implements ClaimStore {

    private static final Sort BY_CLAIM_ID = Sort.by("claimId");

    private final ClaimRepository repository;

    public MongoClaimStore(ClaimRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveAll(Collection<Claim> claims) {
        // save() with an existing _id replaces the document, which makes re-imports idempotent.
        repository.saveAll(claims);
    }

    @Override
    public Optional<Claim> findById(String claimId) {
        return repository.findById(claimId);
    }

    @Override
    public List<Claim> findByStatus(ClaimStatus status) {
        return repository.findByStatus(status, BY_CLAIM_ID);
    }

    @Override
    public List<Claim> findAll() {
        return repository.findAll(BY_CLAIM_ID);
    }
}
