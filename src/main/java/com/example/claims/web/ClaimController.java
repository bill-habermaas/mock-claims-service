package com.example.claims.web;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimStatus;
import com.example.claims.claim.ClaimStore;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private static final String VALID_CODES =
            Arrays.stream(ClaimStatus.values()).map(Enum::name).collect(Collectors.joining(", "));

    private final ClaimStore store;

    public ClaimController(ClaimStore store) {
        this.store = store;
    }

    @GetMapping("/{claimId}")
    public ClaimResponse getClaim(@PathVariable String claimId) {
        return store.findById(claimId)
                .map(ClaimResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No claim with ID " + claimId + "."));
    }

    /** Lists claims, optionally filtered by status code. */
    @GetMapping
    public List<ClaimResponse> listClaims(@RequestParam(required = false) String status) {
        List<Claim> claims;
        if (status == null) {
            claims = store.findAll();
        } else {
            ClaimStatus code = ClaimStatus.fromCode(status)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Status '" + status + "' is not a valid code. Use one of: " + VALID_CODES + "."));
            claims = store.findByStatus(code);
        }
        return claims.stream().map(ClaimResponse::from).toList();
    }
}
