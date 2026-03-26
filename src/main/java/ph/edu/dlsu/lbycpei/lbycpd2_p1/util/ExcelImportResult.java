package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PayrollRecord;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Return type for Excel imports: parsed payroll records + import quality stats + raw dump location.
 */
public final class ExcelImportResult {
    private final List<PayrollRecord> records;
    private final ExcelImportStats stats;
    private final Path rawDumpCsvPath;

    public ExcelImportResult(List<PayrollRecord> records, ExcelImportStats stats, Path rawDumpCsvPath) {
        this.records = records != null ? records : new ArrayList<>();
        this.stats = stats != null ? stats : new ExcelImportStats();
        this.rawDumpCsvPath = rawDumpCsvPath;
    }

    public List<PayrollRecord> getRecords() {
        return records;
    }

    public ExcelImportStats getStats() {
        return stats;
    }

    public Path getRawDumpCsvPath() {
        return rawDumpCsvPath;
    }
}

