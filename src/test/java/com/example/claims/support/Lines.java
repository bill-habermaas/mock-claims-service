package com.example.claims.support;

/** Builds fixed-width test lines so tests read as field values, not column counting. */
public final class Lines {

    private Lines() {
    }

    public static String detail(String claimId, String personId, String last, String first,
                                String dob, String filed, String status, String cents) {
        String line = "D" + claimId + personId + pad(last, 20) + pad(first, 15) + dob + filed + status + cents;
        if (line.length() != 80) {
            throw new IllegalArgumentException("Test line is " + line.length() + " characters: " + line);
        }
        return line;
    }

    /** A detail line that passes every rule. */
    public static String validDetail(String claimId) {
        return detail(claimId, "P12345678", "DOE", "JANE", "19800101", "20250115", "OP", "0033400");
    }

    public static String header(String exportDate) {
        return pad("H" + exportDate + "STATE-CLAIMS-LEGACY", 80);
    }

    public static String trailer(int count) {
        return pad("T" + String.format("%09d", count), 80);
    }

    private static String pad(String s, int width) {
        return String.format("%-" + width + "s", s);
    }
}
