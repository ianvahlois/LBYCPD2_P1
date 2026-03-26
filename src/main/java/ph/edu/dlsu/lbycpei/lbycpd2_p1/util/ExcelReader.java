package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PayrollRecord;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Imports Excel/biometric per-day attendance exports.
 *
 * Expected (case-insensitive) headers in the sheet:
 * - Employee ID (or ID)
 * - Name (optional but used when available)
 * - Date (optional; required to compute pay period range)
 * - Time In
 * - Time Out
 *
 * Day entries with missing/blank Time In or Time Out are ignored for hours
 * computation and are counted for accounting.
 */
public final class ExcelReader {

    private ExcelReader() {}

    public static ExcelImportResult readExcel(String excelPath, Double defaultHourlyRate) throws IOException {
        requireNonNull(excelPath, "excelPath");
        double rate = defaultHourlyRate != null ? defaultHourlyRate : CSVReader.DEFAULT_HOURLY_RATE;

        Path inputPath = Paths.get(excelPath);
        String baseName = stripExtension(inputPath.getFileName().toString());
        Path dumpCsv = inputPath.resolveSibling(baseName + "_full_data_dump.csv");

        ExcelImportStats stats = new ExcelImportStats();
        Map<String, EmployeeAgg> employees = new HashMap<>();

        DataFormatter formatter = new DataFormatter();

        boolean parsedAny = false;
        int maxHeaderSearchRows = 40;

        try (Workbook workbook = WorkbookFactory.create(new File(excelPath))) {
            // Always write the raw dump first for audit/debug.
            ExcelRawDump.dumpWorkbookToCsv(workbook, dumpCsv);

            for (Sheet sheet : iterableSheets(workbook)) {
                HeaderMapping mapping = findHeaderMapping(sheet, formatter, maxHeaderSearchRows);
                if (mapping == null) continue;

                parsedAny = true;
                parseSheet(sheet, mapping, formatter, rate, employees, stats);
            }
        } catch (Exception e) {
            throw new IOException("Failed to read Excel file: " + excelPath, e);
        }

        List<PayrollRecord> records = new ArrayList<>();
        for (EmployeeAgg agg : employees.values()) {
            PayrollRecord rec = new PayrollRecord(
                    agg.employeeId,
                    agg.name,
                    agg.dept,
                    agg.totalHours,
                    rate,
                    agg.periodStart,
                    agg.periodEnd
            );
            rec.setDepartment(agg.dept);
            rec.setMissingDataFlag(agg.ignoredDaysDueToMissingTimeInOrTimeOut > 0);
            rec.setIgnoredDaysDueToMissingTimeInOut(agg.ignoredDaysDueToMissingTimeInOrTimeOut);
            records.add(rec);
        }

        records.sort(Comparator.comparing(PayrollRecord::getEmployeeId, Comparator.nullsLast(String::compareTo)));
        stats.setEmployeeCount(records.size());

        if (!parsedAny) {
            // Keep stats empty but still return parsedAny=false indicator via employee count=0
        }
        return new ExcelImportResult(records, stats, dumpCsv);
    }

    private static void parseSheet(Sheet sheet,
                                    HeaderMapping mapping,
                                    DataFormatter formatter,
                                    double rate,
                                    Map<String, EmployeeAgg> employees,
                                    ExcelImportStats stats) {

        int startRow = mapping.headerRowIndex + 1;
        int lastRow = sheet.getLastRowNum();

        for (int r = startRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // Quick scan: if the row is basically empty, skip.
            boolean rowHasAnyCell = false;
            for (int c = 0; c <= Math.max(mapping.timeOutCol, mapping.timeInCol); c++) {
                Cell cell = row.getCell(c);
                if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                    rowHasAnyCell = true;
                    break;
                }
            }
            if (!rowHasAnyCell) continue;

            stats.setTotalRowsSeen(stats.getTotalRowsSeen() + 1);

            String employeeId = getCellString(row, mapping.employeeIdCol, formatter);
            if (employeeId.isBlank()) continue;

            employeeId = normalizeEmployeeId(employeeId);

            String name = getCellString(row, mapping.nameCol, formatter);
            String dept = getCellString(row, mapping.deptCol, formatter);

            LocalDate date = mapping.dateCol >= 0 ? parseDateCell(row.getCell(mapping.dateCol), formatter) : null;

            LocalTime timeIn = parseTimeCell(row.getCell(mapping.timeInCol), formatter);
            LocalTime timeOut = parseTimeCell(row.getCell(mapping.timeOutCol), formatter);

            EmployeeAgg agg = employees.computeIfAbsent(employeeId, id -> new EmployeeAgg(id));
            agg.name = (name != null && !name.isBlank()) ? name : agg.name;
            agg.dept = (dept != null && !dept.isBlank()) ? dept : agg.dept;

            if (date != null) {
                agg.periodStart = agg.periodStart == null ? date : min(agg.periodStart, date);
                agg.periodEnd = agg.periodEnd == null ? date : max(agg.periodEnd, date);
            }

            if (timeIn == null || timeOut == null) {
                // Accounting requested: ignored due to missing Time In/Time Out
                agg.ignoredDaysDueToMissingTimeInOrTimeOut++;
                stats.setIgnoredDueToMissingTimeInOrTimeOut(stats.getIgnoredDueToMissingTimeInOrTimeOut() + 1);
                continue;
            }

            if (date == null) {
                stats.setIgnoredDueToMissingOrInvalidDate(stats.getIgnoredDueToMissingOrInvalidDate() + 1);
                continue;
            }

            double hours = computeHours(timeIn, timeOut);
            if (hours <= 0) continue;

            agg.totalHours += hours;
            stats.setDayRowsParsed(stats.getDayRowsParsed() + 1);
        }
    }

    private static double computeHours(LocalTime timeIn, LocalTime timeOut) {
        long minutes = Duration.between(timeIn, timeOut).toMinutes();
        if (minutes < 0) {
            // Handle overnight shifts: add 24 hours.
            minutes += 24 * 60;
        }
        return minutes / 60.0;
    }

    private static HeaderMapping findHeaderMapping(Sheet sheet,
                                                   DataFormatter formatter,
                                                   int maxHeaderSearchRows) {
        int lastRow = Math.min(sheet.getLastRowNum(), maxHeaderSearchRows);
        for (int r = sheet.getFirstRowNum(); r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Integer employeeIdCol = null;
            Integer nameCol = null;
            Integer deptCol = null;
            Integer dateCol = null;
            Integer timeInCol = null;
            Integer timeOutCol = null;

            short lastCellNum = row.getLastCellNum();
            int maxCol = lastCellNum >= 0 ? lastCellNum : 0;
            for (int c = 0; c < maxCol; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;
                String header = formatter.formatCellValue(cell).trim().toLowerCase();
                if (header.isBlank()) continue;

                if (header.contains("time in")) timeInCol = c;
                else if (header.contains("time out")) timeOutCol = c;
                else if (header.contains("time-in")) timeInCol = c;
                else if (header.contains("time-out")) timeOutCol = c;
                else if (header.equals("id") || header.contains("employee id")) employeeIdCol = c;
                else if (header.contains("name")) nameCol = c;
                else if (header.contains("dept")) deptCol = c;
                else if (header.contains("department")) deptCol = c;
                else if (header.contains("date")) dateCol = c;
            }

            if (employeeIdCol != null && timeInCol != null && timeOutCol != null) {
                return new HeaderMapping(r,
                        employeeIdCol,
                        nameCol != null ? nameCol : -1,
                        deptCol != null ? deptCol : -1,
                        dateCol != null ? dateCol : -1,
                        timeInCol,
                        timeOutCol);
            }
        }
        return null;
    }

    private static String getCellString(Row row, int colIndex, DataFormatter formatter) {
        if (row == null) return "";
        if (colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        String s = formatter.formatCellValue(cell);
        return s != null ? s.trim() : "";
    }

    private static LocalDate parseDateCell(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String s = formatter.formatCellValue(cell);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        // Try ISO and common alternatives.
        LocalDate direct = ParseUtils.parseDate(s);
        if (direct != null) return direct;

        List<DateTimeFormatter> patterns = List.of(
                DateTimeFormatter.ofPattern("M/d/uuuu"),
                DateTimeFormatter.ofPattern("MM/dd/uuuu"),
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("uuuu-M-d"),
                DateTimeFormatter.ofPattern("uuuu/M/d")
        );
        for (DateTimeFormatter p : patterns) {
            try {
                return LocalDate.parse(s, p);
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        return null;
    }

    private static LocalTime parseTimeCell(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        }

        String s = formatter.formatCellValue(cell);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        // Normalize: sometimes Excel outputs "8:30:00" etc.
        s = s.replaceAll("\\s+", " ");

        List<DateTimeFormatter> patterns = List.of(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("h:mm a"),
                DateTimeFormatter.ofPattern("hh:mm a")
        );

        for (DateTimeFormatter p : patterns) {
            try {
                return LocalTime.parse(s, p);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }

        // Fall back to ISO LocalTime parsing if it matches.
        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String stripExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    private static String normalizeEmployeeId(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        // Excel may store IDs as numbers; DataFormatter sometimes returns "101.0".
        if (s.matches("^\\d+\\.0+$")) {
            s = s.replaceAll("\\.0+$", "");
        }
        return s;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static Iterable<Sheet> iterableSheets(Workbook wb) {
        return () -> wb.sheetIterator();
    }

    private static final class HeaderMapping {
        private final int headerRowIndex;
        private final int employeeIdCol;
        private final int nameCol;
        private final int deptCol;
        private final int dateCol;
        private final int timeInCol;
        private final int timeOutCol;

        private HeaderMapping(int headerRowIndex,
                               int employeeIdCol,
                               int nameCol,
                               int deptCol,
                               int dateCol,
                               int timeInCol,
                               int timeOutCol) {
            this.headerRowIndex = headerRowIndex;
            this.employeeIdCol = employeeIdCol;
            this.nameCol = nameCol;
            this.deptCol = deptCol;
            this.dateCol = dateCol;
            this.timeInCol = timeInCol;
            this.timeOutCol = timeOutCol;
        }
    }

    private static final class EmployeeAgg {
        private final String employeeId;
        private String name = "";
        private String dept = "";
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private double totalHours;
        private int ignoredDaysDueToMissingTimeInOrTimeOut;

        private EmployeeAgg(String employeeId) {
            this.employeeId = employeeId;
        }
    }
}

