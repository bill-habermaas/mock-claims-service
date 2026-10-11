package com.example.claims.claim;

import java.util.Arrays;
import java.util.Optional;

/** The four status codes in the legacy code list. */
public enum ClaimStatus {
    OP("Open"),
    AP("Approved"),
    DN("Denied"),
    CL("Closed");

    private final String description;

    ClaimStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public static Optional<ClaimStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(s -> s.name().equals(code)).findFirst();
    }
}
