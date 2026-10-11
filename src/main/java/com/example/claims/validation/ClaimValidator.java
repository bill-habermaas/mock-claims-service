package com.example.claims.validation;

import static com.example.claims.layout.RecordLayout.CLAIM_ID;
import static com.example.claims.layout.RecordLayout.DATE_FILED;
import static com.example.claims.layout.RecordLayout.DATE_OF_BIRTH;
import static com.example.claims.layout.RecordLayout.FIRST_NAME;
import static com.example.claims.layout.RecordLayout.LAST_NAME;
import static com.example.claims.layout.RecordLayout.LINE_LENGTH;
import static com.example.claims.layout.RecordLayout.PERSON_ID;
import static com.example.claims.layout.RecordLayout.STATUS;
import static com.example.claims.layout.RecordLayout.UNKNOWN_DATE;
import static com.example.claims.layout.RecordLayout.WEEKLY_BENEFIT_CENTS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.example.claims.claim.ClaimStatus;
import com.example.claims.layout.DetailLine;

/**
 * Applies the agency's validation rules to one detail line.
 *
 * <p><b>To add a rule, add an entry to {@link #FIELD_RULES}.</b> Each rule returns a
 * reason in words a non-engineer can read, because reasons go straight into the
 * rejection report.
 */
@Component
public class ClaimValidator {

    private static final Pattern CLAIM_ID_FORMAT = Pattern.compile("CLM\\d{7}");
    private static final Pattern PERSON_ID_FORMAT = Pattern.compile("P\\d{8}");
    private static final Pattern SEVEN_DIGITS = Pattern.compile("\\d{7}");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    /** Checked first. If the length is wrong, field positions are meaningless, so no other rule runs. */
    private static final ValidationRule LINE_LENGTH_RULE = line ->
            line.text().length() == LINE_LENGTH
                    ? Optional.empty()
                    : Optional.of("Line is " + line.text().length() + " characters long; expected " + LINE_LENGTH + ".");

    /** Field-level rules, applied in this order to every line of the right length. */
    private static final List<ValidationRule> FIELD_RULES = List.of(
            line -> CLAIM_ID_FORMAT.matcher(line.raw(CLAIM_ID)).matches()
                    ? Optional.empty()
                    : Optional.of("Claim ID '" + line.trimmed(CLAIM_ID) + "' is not CLM followed by seven digits."),

            line -> PERSON_ID_FORMAT.matcher(line.raw(PERSON_ID)).matches()
                    ? Optional.empty()
                    : Optional.of("Person ID '" + line.trimmed(PERSON_ID) + "' is not P followed by eight digits."),

            line -> line.trimmed(LAST_NAME).isEmpty()
                    ? Optional.of("Last name is blank.")
                    : Optional.empty(),

            line -> line.trimmed(FIRST_NAME).isEmpty()
                    ? Optional.of("First name is blank.")
                    : Optional.empty(),

            // Decision 1: 00000000 means "unknown" and is allowed; anything else must be a real date.
            line -> UNKNOWN_DATE.equals(line.raw(DATE_OF_BIRTH)) || parseDate(line.raw(DATE_OF_BIRTH)).isPresent()
                    ? Optional.empty()
                    : Optional.of("Date of birth '" + line.raw(DATE_OF_BIRTH) + "' is not a valid calendar date."),

            line -> parseDate(line.raw(DATE_FILED)).isPresent()
                    ? Optional.empty()
                    : Optional.of("Date filed '" + line.raw(DATE_FILED) + "' is not a valid calendar date."),

            ClaimValidator::filedNotBeforeBirth,

            line -> ClaimStatus.fromCode(line.raw(STATUS)).isPresent()
                    ? Optional.empty()
                    : Optional.of("Status code '" + line.raw(STATUS) + "' is not one of OP, AP, DN, CL."),

            line -> SEVEN_DIGITS.matcher(line.raw(WEEKLY_BENEFIT_CENTS)).matches()
                    ? Optional.empty()
                    : Optional.of("Weekly benefit amount '" + line.raw(WEEKLY_BENEFIT_CENTS) + "' must be seven digits of whole cents.")
    );

    /** Returns every rule the line breaks. An empty list means the line is valid. */
    public List<String> validate(DetailLine line) {
        Optional<String> lengthProblem = LINE_LENGTH_RULE.check(line);
        if (lengthProblem.isPresent()) {
            return List.of(lengthProblem.get());
        }
        List<String> reasons = new ArrayList<>();
        for (ValidationRule rule : FIELD_RULES) {
            rule.check(line).ifPresent(reasons::add);
        }
        return reasons;
    }

    private static Optional<String> filedNotBeforeBirth(DetailLine line) {
        Optional<LocalDate> born = parseDate(line.raw(DATE_OF_BIRTH));
        Optional<LocalDate> filed = parseDate(line.raw(DATE_FILED));
        // Skipped when either date is unknown or invalid; those cases are reported by their own rules.
        if (born.isPresent() && filed.isPresent() && filed.get().isBefore(born.get())) {
            return Optional.of("Date filed " + filed.get() + " is earlier than date of birth " + born.get() + ".");
        }
        return Optional.empty();
    }

    static Optional<LocalDate> parseDate(String raw) {
        if (raw == null || raw.length() != 8) {
            return Optional.empty();
        }
        try {
            // BASIC_ISO_DATE resolves strictly, so 20250231 fails instead of rolling over to March.
            return Optional.of(LocalDate.parse(raw, YYYYMMDD));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
