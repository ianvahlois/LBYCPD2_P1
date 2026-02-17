package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PayrollRecord;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Reads payroll CSV files. Supports two formats:
 * <ol>
 *   <li><b>Attendance report format (professor's format)</b>: First line contains "Attendance summary",
 *       headers include "Dept." and "Working hours". Columns: ID (0), Name (1), Dept. (2), Working hours Standard (3).
 *       Working hours are in "HH:MM" form. No hourly rate in file — a default is used (see {@link #DEFAULT_HOURLY_RATE}).
 *       Pay period is read from the second line (Date, start ~ end).</li>
 *   <li><b>Legacy format</b>: First line is header: employeeId, name, hoursWorked, hourlyRate [, client].
 *       Data starts on second line.</li>
 * </ol>
 * Missing or extra columns are handled gracefully. Numeric parsing is safe (invalid values default to 0).
 */
public class CSVReader {

    /** Default hourly rate when the CSV does not contain one (e.g. professor's attendance report). */
    public static final double DEFAULT_HOURLY_RATE = 100.0;

    private static final int ATTENDANCE_HEADER_ROWS = 4; // title, date, main header, sub-header

    /**
     * Reads CSV from the given path. Format is auto-detected. For attendance format, uses {@link #DEFAULT_HOURLY_RATE}.
     */
    public static List<PayrollRecord> readCSV(String path) {
        return readCSV(path, null);
    }

    /**
     * Reads CSV from the given path. Format is auto-detected.
     * @param defaultHourlyRate hourly rate for attendance-report format when CSV has no rate; null uses {@link #DEFAULT_HOURLY_RATE}.
     */
    public static List<PayrollRecord> readCSV(String path, Double defaultHourlyRate) {
        List<String> lines = readAllLines(path);
        if (lines.isEmpty()) return new ArrayList<>();

        String firstLine = lines.get(0);
        if (isAttendanceReportFormat(firstLine, lines.size() > 2 ? lines.get(2) : "")) {
            return parseAttendanceReport(lines, defaultHourlyRate != null ? defaultHourlyRate : DEFAULT_HOURLY_RATE);
        }
        return parseLegacyFormat(lines);
    }

    private static List<String> readAllLines(String path) {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Professor's format: first line contains "Attendance summary" and row 3 has "ID" and "Dept." / "Working hours". */
    private static boolean isAttendanceReportFormat(String firstLine, String headerLine) {
        if (firstLine == null) return false;
        if (!firstLine.trim().toLowerCase().contains("attendance")) return false;
        String h = headerLine != null ? headerLine : "";
        return h.contains("ID") && (h.contains("Dept") || h.contains("Working hours") || h.contains("Name"));
    }

    /**
     * Parses the professor's attendance report CSV.
     * Structure: line 0 = title, line 1 = Date,start~end, line 2 = main header, line 3 = sub-header, line 4+ = data.
     * Data columns: 0=ID, 1=Name, 2=Dept., 3=Working hours (Standard) as "HH:MM".
     */
    private static List<PayrollRecord> parseAttendanceReport(List<String> lines, double defaultHourlyRate) {
        List<PayrollRecord> records = new ArrayList<>();
        LocalDate periodStart = null;
        LocalDate periodEnd = null;

        if (lines.size() > 1) {
            String dateLine = lines.get(1);
            String[] dateParts = parseCsvLine(dateLine);
            if (dateParts.length >= 2) {
                LocalDate[] range = ParseUtils.parseDateRange(dateParts[1].trim());
                periodStart = range[0];
                periodEnd = range[1];
            }
        }

        for (int i = ATTENDANCE_HEADER_ROWS; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] cols = parseCsvLine(line);
            if (cols.length < 4) continue;

            String id = safeTrim(cols, 0);
            String name = safeTrim(cols, 1);
            String dept = safeTrim(cols, 2);
            String workingHoursStr = safeTrim(cols, 3);

            if (id.isEmpty() && name.isEmpty()) continue;

            double hours = ParseUtils.parseHoursMinutesToDecimalHours(workingHoursStr);
            if (hours < 0) hours = 0;

            records.add(new PayrollRecord(id, name, dept, hours, defaultHourlyRate, periodStart, periodEnd));
        }

        return records;
    }

    /** Legacy format: first line header, then one record per line. Columns: id, name, hours, rate [, client]. */
    private static List<PayrollRecord> parseLegacyFormat(List<String> lines) {
        List<PayrollRecord> records = new ArrayList<>();
        if (lines.size() < 2) return records;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] data = parseCsvLine(line);
            if (data.length < 4) continue;

            String employeeId = safeTrim(data, 0);
            String name = safeTrim(data, 1);
            double hours = ParseUtils.parseDouble(safeTrim(data, 2), 0.0);
            double rate = ParseUtils.parseDouble(safeTrim(data, 3), 0.0);
            String client = data.length > 4 ? safeTrim(data, 4) : "";

            if (employeeId.isEmpty() && name.isEmpty()) continue;
            if (hours < 0) hours = 0;
            if (rate < 0) rate = 0;

            records.add(new PayrollRecord(employeeId, name, client, hours, rate, null, null));
        }

        return records;
    }

    private static String safeTrim(String[] arr, int index) {
        if (arr == null || index < 0 || index >= arr.length) return "";
        String s = arr[index];
        return s != null ? s.trim() : "";
    }

    /** Simple CSV line parse: handles quoted fields with commas. */
    private static String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
