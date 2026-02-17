package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PayrollRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates printable payroll receipts (HTML) for each employee.
 */
public class ReceiptGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    /**
     * Generates one HTML receipt per employee in the given directory.
     *
     * @param records   list of payroll records
     * @param outputDir directory to write receipt files
     * @param companyName optional company name for header
     * @param payPeriodEnd optional pay period end date
     * @return number of receipts written
     */
    public static int generateReceipts(List<PayrollRecord> records, Path outputDir,
                                       String companyName, LocalDate payPeriodEnd) {
        if (records == null || records.isEmpty()) return 0;
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create output directory: " + outputDir, e);
        }
        LocalDate end = payPeriodEnd != null ? payPeriodEnd : LocalDate.now();
        String company = companyName != null && !companyName.isBlank() ? companyName : "Company";
        int count = 0;
        for (PayrollRecord r : records) {
            Path file = outputDir.resolve(sanitizeFileName(r.getEmployeeId() + "_" + r.getName()) + "_receipt.html");
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
                writeReceiptHtml(out, r, company, end);
                count++;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write receipt: " + file, e);
            }
        }
        return count;
    }

    private static String sanitizeFileName(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private static void writeReceiptHtml(PrintWriter out, PayrollRecord r, String companyName, LocalDate payDate) {
        String periodStart = r.getPayPeriodStart() != null ? r.getPayPeriodStart().format(DATE_FMT) : "—";
        String periodEnd = r.getPayPeriodEnd() != null ? r.getPayPeriodEnd().format(DATE_FMT) : payDate.format(DATE_FMT);

        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("<title>Payroll Receipt - " + escape(r.getName()) + "</title>");
        out.println("<style>");
        out.println("body { font-family: 'Segoe UI', sans-serif; max-width: 600px; margin: 2em auto; padding: 1em; }");
        out.println("h1 { font-size: 1.25em; border-bottom: 2px solid #333; padding-bottom: 0.25em; }");
        out.println("table { width: 100%; border-collapse: collapse; margin: 1em 0; }");
        out.println("th, td { text-align: left; padding: 0.4em 0.5em; border-bottom: 1px solid #ddd; }");
        out.println("th { background: #f5f5f5; }");
        out.println(".amount { text-align: right; }");
        out.println(".total { font-weight: bold; font-size: 1.1em; }");
        out.println(".footer { margin-top: 2em; font-size: 0.9em; color: #666; }");
        out.println("@media print { body { margin: 0; } }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>PAYROLL RECEIPT</h1>");
        out.println("<p><strong>" + escape(companyName) + "</strong></p>");
        out.println("<p>Pay Period: " + periodStart + " – " + periodEnd + "</p>");
        out.println("<table>");
        out.println("<tr><th>Employee ID</th><td>" + escape(r.getEmployeeId()) + "</td></tr>");
        out.println("<tr><th>Employee Name</th><td>" + escape(r.getName()) + "</td></tr>");
        if (r.getClient() != null && !r.getClient().isBlank()) {
            out.println("<tr><th>Client / Department</th><td>" + escape(r.getClient()) + "</td></tr>");
        }
        out.println("<tr><th>Hours Worked</th><td class=\"amount\">" + String.format("%.2f", r.getHoursWorked()) + "</td></tr>");
        out.println("<tr><th>Hourly Rate</th><td class=\"amount\">Php " + String.format("%,.2f", r.getHourlyRate()) + "</td></tr>");
        out.println("<tr><th>Gross Pay</th><td class=\"amount\">Php " + String.format("%,.2f", r.getGrossPay()) + "</td></tr>");
        out.println("</table>");
        out.println("<table>");
        out.println("<caption>Deductions</caption>");
        out.println("<tr><th>SSS</th><td class=\"amount\">(Php " + String.format("%,.2f", r.getSssDeduction()) + ")</td></tr>");
        out.println("<tr><th>PhilHealth</th><td class=\"amount\">(Php " + String.format("%,.2f", r.getPhilhealthDeduction()) + ")</td></tr>");
        out.println("<tr><th>Pag-IBIG</th><td class=\"amount\">(Php " + String.format("%,.2f", r.getPagibigDeduction()) + ")</td></tr>");
        out.println("<tr><th>Withholding Tax</th><td class=\"amount\">(Php " + String.format("%,.2f", r.getTaxDeduction()) + ")</td></tr>");
        out.println("<tr><th>Total Deductions</th><td class=\"amount\">(Php " + String.format("%,.2f", r.getTotalDeductions()) + ")</td></tr>");
        out.println("</table>");
        out.println("<table>");
        out.println("<tr><th>Net Pay</th><td class=\"amount total\">Php " + String.format("%,.2f", r.getNetPay()) + "</td></tr>");
        out.println("</table>");
        out.println("<p class=\"footer\">Generated on " + LocalDate.now().format(DATE_FMT) + ". This is a computer-generated receipt.</p>");
        out.println("</body>");
        out.println("</html>");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
