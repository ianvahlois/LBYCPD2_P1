package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import org.apache.poi.ss.usermodel.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Writes a full raw dump of all sheets/cells in an Excel file into a CSV.
 * This is used for auditing/debugging the import mapping.
 */
public final class ExcelRawDump {

    private ExcelRawDump() {}

    public static void dumpWorkbookToCsv(Workbook workbook, Path outputCsv) throws IOException {
        if (workbook == null) return;
        if (outputCsv == null) return;
        Path parent = outputCsv.getParent();
        if (parent != null) Files.createDirectories(parent);

        try (BufferedWriter w = Files.newBufferedWriter(outputCsv, StandardCharsets.UTF_8)) {
            w.write("Sheet,Row,Column,Value");
            w.newLine();

            DataFormatter formatter = new DataFormatter();
            Iterator<Sheet> sheetIt = workbook.sheetIterator();
            while (sheetIt.hasNext()) {
                Sheet sheet = sheetIt.next();
                String sheetName = sheet.getSheetName();
                int maxRow = sheet.getLastRowNum();

                for (int r = 0; r <= maxRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    short lastCellNum = row.getLastCellNum();
                    int maxCol = lastCellNum >= 0 ? lastCellNum : 0;
                    for (int c = 0; c < maxCol; c++) {
                        Cell cell = row.getCell(c);
                        String value = cell == null ? "" : formatter.formatCellValue(cell);
                        w.write(csvEscape(sheetName));
                        w.write(",");
                        w.write(String.valueOf(r));
                        w.write(",");
                        w.write(String.valueOf(c));
                        w.write(",");
                        w.write(csvEscape(value));
                        w.newLine();
                    }
                }
            }
        }
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

