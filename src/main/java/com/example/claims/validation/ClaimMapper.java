package com.example.claims.validation;

import static com.example.claims.layout.RecordLayout.CLAIM_ID;
import static com.example.claims.layout.RecordLayout.DATE_FILED;
import static com.example.claims.layout.RecordLayout.DATE_OF_BIRTH;
import static com.example.claims.layout.RecordLayout.FIRST_NAME;
import static com.example.claims.layout.RecordLayout.LAST_NAME;
import static com.example.claims.layout.RecordLayout.PERSON_ID;
import static com.example.claims.layout.RecordLayout.STATUS;
import static com.example.claims.layout.RecordLayout.UNKNOWN_DATE;
import static com.example.claims.layout.RecordLayout.WEEKLY_BENEFIT_CENTS;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimStatus;
import com.example.claims.layout.DetailLine;

/** Converts a detail line that has already passed validation into a {@link Claim}. */
@Component
public class ClaimMapper {

    public Claim toClaim(DetailLine line) {
        boolean dobUnknown = UNKNOWN_DATE.equals(line.raw(DATE_OF_BIRTH));
        LocalDate dob = dobUnknown ? null : ClaimValidator.parseDate(line.raw(DATE_OF_BIRTH)).orElseThrow();
        return new Claim(
                line.trimmed(CLAIM_ID),
                line.trimmed(PERSON_ID),
                line.trimmed(LAST_NAME),
                line.trimmed(FIRST_NAME),
                dob,
                dobUnknown,
                ClaimValidator.parseDate(line.raw(DATE_FILED)).orElseThrow(),
                ClaimStatus.fromCode(line.raw(STATUS)).orElseThrow(),
                Long.parseLong(line.raw(WEEKLY_BENEFIT_CENTS)),
                line.lineNumber());
    }
}
