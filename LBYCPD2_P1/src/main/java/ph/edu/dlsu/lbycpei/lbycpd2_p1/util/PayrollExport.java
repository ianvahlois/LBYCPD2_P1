package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PayrollRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports payroll data to CSV (with client and all deduction columns).
 */
public class PayrollExport {

    public static void exportToCsv(List<PayrollRecord> records, Path file) throws IOException {
        if (records == null) return;
        Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("Employee ID,Name,Client,Hours Worked,Hourly Rate,Gross Pay,SSS,PhilHealth,Pag-IBIG,Withholding Tax,Total Deductions,Net Pay");
            w.newLine();
            for (PayrollRecord r : records) {
                w.write(csvEscape(r.getEmployeeId()) + ",");
                w.write(csvEscape(r.getName()) + ",");
                w.write(csvEscape(r.getClient()) + ",");
                w.write(String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                        r.getHoursWorked(), r.getHourlyRate(), r.getGrossPay(),
                        r.getSssDeduction(), r.getPhilhealthDeduction(), r.getPagibigDeduction(),
                        r.getTaxDeduction(), r.getTotalDeductions(), r.getNetPay()));
                w.newLine();
            }
        }
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
