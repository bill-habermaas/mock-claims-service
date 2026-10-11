package com.example.claims.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.claims.claim.Claim;

/** API view of a claim. Money is shown in dollars; status includes its meaning. */
public record ClaimResponse(
        String claimId,
        String personId,
        String lastName,
        String firstName,
        LocalDate dateOfBirth,
        boolean dateOfBirthUnknown,
        LocalDate dateFiled,
        String status,
        String statusDescription,
        BigDecimal weeklyBenefitAmount) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(
                claim.claimId(),
                claim.personId(),
                claim.lastName(),
                claim.firstName(),
                claim.dateOfBirth(),
                claim.dateOfBirthUnknown(),
                claim.dateFiled(),
                claim.status().name(),
                claim.status().description(),
                BigDecimal.valueOf(claim.weeklyBenefitCents(), 2));
    }
}
