package com.example.claims.claim;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A migrated claim.
 *
 * <p>The claim ID is the MongoDB document ID, so loading the same claim again replaces
 * the stored document instead of adding a second one.
 *
 * @param dateOfBirth          {@code null} when the legacy system recorded the date as unknown
 * @param weeklyBenefitCents   weekly benefit in whole cents (0033400 is $334.00)
 * @param sourceLineNumber     line in the export this claim was loaded from, for tracing
 */
@Document(collection = "claims")
public record Claim(
        @Id String claimId,
        String personId,
        String lastName,
        String firstName,
        LocalDate dateOfBirth,
        boolean dateOfBirthUnknown,
        LocalDate dateFiled,
        @Indexed ClaimStatus status,
        long weeklyBenefitCents,
        int sourceLineNumber) {
}
