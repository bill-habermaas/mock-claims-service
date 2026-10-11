package com.example.claims.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.claims.claim.Claim;
import com.example.claims.claim.ClaimStatus;
import com.example.claims.layout.DetailLine;
import com.example.claims.support.Lines;

class ClaimValidatorTest {

    private final ClaimValidator validator = new ClaimValidator();

    private List<String> validate(String text) {
        return validator.validate(new DetailLine(2, text));
    }

    private static String line(String claimId, String personId, String last, String first,
                               String dob, String filed, String status, String cents) {
        return Lines.detail(claimId, personId, last, first, dob, filed, status, cents);
    }

    @Test
    void acceptsAValidLine() {
        assertThat(validate(Lines.validDetail("CLM0000001"))).isEmpty();
    }

    @Test
    void rejectsWrongLengthWithoutCheckingFields() {
        List<String> reasons = validate(Lines.validDetail("CLM0000001").substring(0, 52));
        assertThat(reasons).containsExactly("Line is 52 characters long; expected 80.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLX0000001", "CLM000000A", "clm0000001"})
    void rejectsMalformedClaimId(String claimId) {
        String text = line(claimId, "P12345678", "DOE", "JANE", "19800101", "20250115", "OP", "0033400");
        assertThat(validate(text)).singleElement().asString().contains("Claim ID");
    }

    @Test
    void rejectsMalformedPersonId() {
        String text = line("CLM0000001", "Q12345678", "DOE", "JANE", "19800101", "20250115", "OP", "0033400");
        assertThat(validate(text)).singleElement().asString().contains("Person ID");
    }

    @Test
    void rejectsBlankNames() {
        assertThat(validate(line("CLM0000001", "P12345678", "", "JANE", "19800101", "20250115", "OP", "0033400")))
                .containsExactly("Last name is blank.");
        assertThat(validate(line("CLM0000001", "P12345678", "DOE", "", "19800101", "20250115", "OP", "0033400")))
                .containsExactly("First name is blank.");
    }

    @Test
    void rejectsImpossibleFiledDate() {
        String text = line("CLM0000001", "P12345678", "DOE", "JANE", "19800101", "20250231", "OP", "0033400");
        assertThat(validate(text)).singleElement().asString().contains("Date filed '20250231' is not a valid");
    }

    @Test
    void rejectsFiledBeforeBirth() {
        String text = line("CLM0000001", "P12345678", "DOE", "JANE", "19900515", "19890301", "OP", "0033400");
        assertThat(validate(text)).singleElement().asString().contains("earlier than date of birth");
    }

    @Test
    void acceptsFiledOnBirthDate() {
        String text = line("CLM0000001", "P12345678", "DOE", "JANE", "19900515", "19900515", "OP", "0033400");
        assertThat(validate(text)).isEmpty();
    }

    @Test
    void decision1_unknownDateOfBirthIsAcceptedAndMappedToNull() {
        String text = line("CLM0000001", "P12345678", "DOE", "JANE", "00000000", "20250115", "OP", "0033400");
        assertThat(validate(text)).isEmpty();

        Claim claim = new ClaimMapper().toClaim(new DetailLine(2, text));
        assertThat(claim.dateOfBirth()).isNull();
        assertThat(claim.dateOfBirthUnknown()).isTrue();
    }

    @Test
    void rejectsInvalidDateOfBirthOtherThanTheUnknownPlaceholder() {
        String text = line("CLM0000001", "P12345678", "DOE", "JANE", "19801301", "20250115", "OP", "0033400");
        assertThat(validate(text)).singleElement().asString().contains("Date of birth");
    }

    @Test
    void rejectsUnknownStatus() {
        String text = line("CLM0000001", "P12345678", "DOE", "JANE", "19800101", "20250115", "XX", "0033400");
        assertThat(validate(text)).containsExactly("Status code 'XX' is not one of OP, AP, DN, CL.");
    }

    @Test
    void rejectsNonNumericAmount() {
        String text = line("CLM0000001", "P12345678", "DOE", "JANE", "19800101", "20250115", "OP", "00A3400");
        assertThat(validate(text)).singleElement().asString().contains("Weekly benefit amount");
    }

    @Test
    void reportsEveryBrokenRuleOnOneLine() {
        String text = line("CLM0000001", "P12345678", "", "", "19800101", "20250115", "XX", "0033400");
        assertThat(validate(text)).hasSize(3);
    }

    @Test
    void mapsAValidLineToAClaim() {
        Claim claim = new ClaimMapper().toClaim(new DetailLine(7, Lines.validDetail("CLM0000001")));
        assertThat(claim.claimId()).isEqualTo("CLM0000001");
        assertThat(claim.lastName()).isEqualTo("DOE");
        assertThat(claim.status()).isEqualTo(ClaimStatus.OP);
        assertThat(claim.weeklyBenefitCents()).isEqualTo(33400L);
        assertThat(claim.sourceLineNumber()).isEqualTo(7);
    }
}
