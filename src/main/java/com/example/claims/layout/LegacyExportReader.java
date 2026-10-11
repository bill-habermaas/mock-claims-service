package com.example.claims.layout;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Splits raw export lines into header, detail lines and trailer.
 *
 * <p>This class only sorts lines by record type. It does not judge whether a detail
 * line is valid; that is the validator's job.
 */
@Component
public class LegacyExportReader {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    public LegacyExport read(List<String> lines) {
        LocalDate exportDate = null;
        String sourceSystem = null;
        Integer trailerCount = null;
        boolean headerSeen = false;
        boolean trailerSeen = false;
        List<DetailLine> details = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = stripLineEnding(lines.get(i));
            if (line.isBlank()) {
                continue;
            }
            char type = line.charAt(0);
            switch (type) {
                case RecordLayout.HEADER -> {
                    headerSeen = true;
                    sourceSystem = RecordLayout.HEADER_SOURCE_SYSTEM.trimmedFrom(line);
                    exportDate = parseDate(RecordLayout.HEADER_EXPORT_DATE.rawFrom(line));
                    if (exportDate == null) {
                        notes.add("Line " + lineNumber + ": header export date is missing or not a valid date.");
                    }
                }
                case RecordLayout.DETAIL -> details.add(new DetailLine(lineNumber, line));
                case RecordLayout.TRAILER -> {
                    trailerSeen = true;
                    trailerCount = parseCount(RecordLayout.TRAILER_DETAIL_COUNT.rawFrom(line));
                    if (trailerCount == null) {
                        notes.add("Line " + lineNumber + ": trailer detail count is missing or not numeric.");
                    }
                }
                default -> notes.add("Line " + lineNumber + ": unknown record type '" + type + "'; line ignored.");
            }
        }
        if (!headerSeen) {
            notes.add("File has no header record.");
        }
        if (!trailerSeen) {
            notes.add("File has no trailer record.");
        }
        return new LegacyExport(exportDate, sourceSystem, List.copyOf(details), trailerCount, List.copyOf(notes));
    }

    private static String stripLineEnding(String line) {
        // Tolerate Windows line endings; never trim trailing spaces, which are part of the layout.
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw, YYYYMMDD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Integer parseCount(String raw) {
        if (raw == null || !raw.matches("\\d+")) {
            return null;
        }
        return Integer.parseInt(raw);
    }
}
