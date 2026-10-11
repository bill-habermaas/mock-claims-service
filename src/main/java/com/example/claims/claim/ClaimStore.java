package com.example.claims.claim;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Where claims are kept. The service depends on this interface rather than on MongoDB
 * directly, so the import logic can be tested without a database.
 */
public interface ClaimStore {

    /** Inserts or replaces each claim by claim ID. */
    void saveAll(Collection<Claim> claims);

    Optional<Claim> findById(String claimId);

    List<Claim> findByStatus(ClaimStatus status);

    List<Claim> findAll();
}
