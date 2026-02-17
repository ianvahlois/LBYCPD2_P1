package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import ph.edu.dlsu.lbycpei.lbycpd2_p1.security.SessionContext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Appends human‑readable audit entries whenever a manual override or
 * paid‑leave change is made.
 *
 * The goal is transparency rather than tamper‑proof logging, so a simple
 * line‑based text file is sufficient.
 */
public final class AuditLogger {

    private static final Path LOG_FILE = Paths.get("payroll_audit.log");
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLogger() { }

    public static void logFieldChange(String employeeId,
                                      String fieldName,
                                      Object oldValue,
                                      Object newValue) {
        String user = SessionContext.getCurrentUsername();
        String msg = String.format("FIELD_CHANGE employeeId=%s field=%s old=%s new=%s",
                safe(employeeId), safe(fieldName), safe(oldValue), safe(newValue));
        writeLine(user, msg);
    }

    public static void logPaidLeave(String employeeId,
                                    String action,
                                    String details) {
        String user = SessionContext.getCurrentUsername();
        String msg = String.format("PAID_LEAVE employeeId=%s action=%s details=%s",
                safe(employeeId), safe(action), safe(details));
        writeLine(user, msg);
    }

    private static String safe(Object v) {
        return v == null ? "" : v.toString().replace('\n', ' ').replace('\r', ' ');
    }

    private static synchronized void writeLine(String username, String message) {
        String user = username != null ? username : "unknown";
        String ts = LocalDateTime.now().format(TS_FMT);
        String line = String.format("%s [%s] %s", ts, user, message);
        try {
            Files.createDirectories(LOG_FILE.getParent() != null
                    ? LOG_FILE.getParent()
                    : Paths.get("."));
            try (BufferedWriter w = Files.newBufferedWriter(
                    LOG_FILE,
                    Files.exists(LOG_FILE)
                            ? java.nio.file.StandardOpenOption.APPEND
                            : java.nio.file.StandardOpenOption.CREATE)) {
                w.write(line);
                w.newLine();
            }
        } catch (IOException e) {
            // For this small desktop tool we simply print to stderr; we don't
            // want logging failures to break payroll use.
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}

