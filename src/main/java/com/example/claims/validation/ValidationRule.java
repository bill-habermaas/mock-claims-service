package com.example.claims.validation;

import java.util.Optional;

import com.example.claims.layout.DetailLine;

/**
 * One check on a detail line.
 *
 * @see ClaimValidator for the list of rules in force
 */
@FunctionalInterface
public interface ValidationRule {

    /** Returns a plain-language reason if the line breaks this rule, or empty if it passes. */
    Optional<String> check(DetailLine line);
}
