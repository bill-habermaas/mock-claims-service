package com.example.claims.layout;

import java.time.LocalDate;
import java.util.List;

/**
 * A legacy export split into its header, detail lines and trailer.
 *
 * @param exportDate     export date from the header, or {@code null} if missing or unreadable
 * @param sourceSystem   source system name from the header, or {@code null}
 * @param details        every detail line, valid or not, in file order
 * @param trailerCount   detail count stated by the trailer, or {@code null} if missing or unreadable
 * @param structureNotes problems with the file's structure (missing header, unknown record types)
 */
public record LegacyExport(
        LocalDate exportDate,
        String sourceSystem,
        List<DetailLine> details,
        Integer trailerCount,
        List<String> structureNotes) {
}
