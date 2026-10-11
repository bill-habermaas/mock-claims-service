package com.example.claims.importing;

import java.time.LocalDate;
import java.util.List;

/**
 * Result of one import.
 *
 * @param recordsRead     detail records found in the file
 * @param recordsAccepted detail records stored
 * @param recordsRejected detail records not stored; each one has an entry in {@code rejections}
 * @param rejections      why each rejected record was not stored
 * @param warnings        records that were stored but need a person's attention, and file-level notes
 */
public record ImportSummary(
        String source,
        LocalDate exportDate,
        String sourceSystem,
        int recordsRead,
        int recordsAccepted,
        int recordsRejected,
        Integer trailerCount,
        boolean trailerCountMatches,
        List<Issue> rejections,
        List<Issue> warnings) {

    /**
     * One reported problem.
     *
     * @param lineNumber 1-based line in the file, counting the header; {@code null} for file-level notes
     * @param claimId    claim ID as read from the line, possibly malformed; {@code null} for file-level notes
     */
    public record Issue(Integer lineNumber, String claimId, String reason) {
    }
}
