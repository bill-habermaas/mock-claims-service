package com.example.claims.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimStatus;
import com.example.claims.claim.ClaimStore;

/** Test double with the same keyed-by-claim-ID behaviour as the MongoDB store. */
public class InMemoryClaimStore implements ClaimStore {

    private final Map<String, Claim> claims = new ConcurrentSkipListMap<>();

    @Override
    public void saveAll(Collection<Claim> toSave) {
        toSave.forEach(c -> claims.put(c.claimId(), c));
    }

    @Override
    public Optional<Claim> findById(String claimId) {
        return Optional.ofNullable(claims.get(claimId));
    }

    @Override
    public List<Claim> findByStatus(ClaimStatus status) {
        return claims.values().stream().filter(c -> c.status() == status)
                .sorted(Comparator.comparing(Claim::claimId)).toList();
    }

    @Override
    public List<Claim> findAll() {
        return new ArrayList<>(claims.values());
    }

    public int size() {
        return claims.size();
    }

    public void clear() {
        claims.clear();
    }
}
