package com.example.claims.layout;

/**
 * The legacy export's fixed-width record layout, defined in one place.
 *
 * <p>Positions are 1-based, as in the agency's layout document, so each
 * constant can be checked against the spec by eye.
 */
public final class RecordLayout {

    public static final int LINE_LENGTH = 80;

    public static final char HEADER = 'H';
    public static final char DETAIL = 'D';
    public static final char TRAILER = 'T';

    /** Position 1 on every record. */
    public static final Field RECORD_TYPE = new Field("Record type", 1, 1);

    // Detail record (type D)
    public static final Field CLAIM_ID = new Field("Claim ID", 2, 10);
    public static final Field PERSON_ID = new Field("Person ID", 12, 9);
    public static final Field LAST_NAME = new Field("Last name", 21, 20);
    public static final Field FIRST_NAME = new Field("First name", 41, 15);
    public static final Field DATE_OF_BIRTH = new Field("Date of birth", 56, 8);
    public static final Field DATE_FILED = new Field("Date filed", 64, 8);
    public static final Field STATUS = new Field("Status", 72, 2);
    public static final Field WEEKLY_BENEFIT_CENTS = new Field("Weekly benefit amount", 74, 7);

    // Header record (type H)
    public static final Field HEADER_EXPORT_DATE = new Field("Export date", 2, 8);
    public static final Field HEADER_SOURCE_SYSTEM = new Field("Source system name", 10, 20);

    // Trailer record (type T)
    public static final Field TRAILER_DETAIL_COUNT = new Field("Detail record count", 2, 9);

    /** The value the mainframe wrote in date of birth when the date was unknown. */
    public static final String UNKNOWN_DATE = "00000000";

    private RecordLayout() {
    }

    /**
     * One fixed-width field.
     *
     * @param name   the field name used in error messages
     * @param start  1-based start position
     * @param length width in characters
     */
    public record Field(String name, int start, int length) {

        public int end() {
            return start + length - 1;
        }

        /**
         * Returns the raw field text, or {@code null} if the line is too short to hold it.
         */
        public String rawFrom(String line) {
            if (line == null || line.length() < end()) {
                return null;
            }
            return line.substring(start - 1, end());
        }

        /** Returns the field text with padding spaces removed, or {@code null} if absent. */
        public String trimmedFrom(String line) {
            String raw = rawFrom(line);
            return raw == null ? null : raw.strip();
        }
    }
}
