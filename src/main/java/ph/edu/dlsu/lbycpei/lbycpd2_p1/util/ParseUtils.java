package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Safe parsing utilities for CSV input: avoids crashes on bad or missing data.
 */
public final class ParseUtils {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private ParseUtils() {}

    /**
     * Parses a string to double. Returns defaultValue if the string is null, blank, or not a valid number.
     */
    public static double parseDouble(String s, double defaultValue) {
        if (s == null) return defaultValue;
        s = s.trim();
        if (s.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses a time string in "H:MM" or "HH:MM" or "HHH:MM" format (hours:minutes) into decimal hours.
     * Examples: "128:00" -> 128.0, "1:30" -> 1.5. Returns 0.0 on parse failure.
     */
    public static double parseHoursMinutesToDecimalHours(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 0.0;
        String s = timeStr.trim();
        int colon = s.indexOf(':');
        if (colon < 0) {
            return parseDouble(s, 0.0);
        }
        double hours = parseDouble(s.substring(0, colon), 0.0);
        double minutes = parseDouble(s.substring(colon + 1), 0.0);
        return hours + (minutes / 60.0);
    }

    /**
     * Parses a date string (e.g. "2025-01-26"). Returns null on failure.
     */
    public static LocalDate parseDate(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s, ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parses a date range from a string like "2025-01-26 ~ 2025-02-10" or "2025-01-26,2025-02-10".
     * Returns array of two LocalDates { start, end }, or nulls if not found.
     */
    public static LocalDate[] parseDateRange(String s) {
        if (s == null || s.trim().isEmpty()) return new LocalDate[] { null, null };
        s = s.trim();
        String[] parts = s.split("[~,]+");
        if (parts.length >= 2) {
            LocalDate start = parseDate(parts[0].trim());
            LocalDate end = parseDate(parts[1].trim());
            return new LocalDate[] { start, end };
        }
        LocalDate single = parseDate(s);
        return new LocalDate[] { single, single };
    }
}
