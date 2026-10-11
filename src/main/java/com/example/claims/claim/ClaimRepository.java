package com.example.claims.claim;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClaimRepository extends MongoRepository<Claim, String> {

    List<Claim> findByStatus(ClaimStatus status, Sort sort);
}
