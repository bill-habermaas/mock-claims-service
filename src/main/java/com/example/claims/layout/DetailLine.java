package com.example.claims.layout;

/**
 * One detail (type D) line exactly as it appeared in the export, with its 1-based line number.
 */
public record DetailLine(int lineNumber, String text) {

    public String raw(RecordLayout.Field field) {
        return field.rawFrom(text);
    }

    public String trimmed(RecordLayout.Field field) {
        return field.trimmedFrom(text);
    }

    /** Best-effort claim ID for reports, even when the line is otherwise invalid. */
    public String claimIdForReport() {
        String id = trimmed(RecordLayout.CLAIM_ID);
        if (id != null) {
            return id;
        }
        // Line too short to hold the full field: report whatever is there.
        return text.length() > 1 ? text.substring(1).strip() : "";
    }
}
