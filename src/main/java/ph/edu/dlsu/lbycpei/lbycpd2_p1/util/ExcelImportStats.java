package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

/**
 * Summarizes import quality and ignored-entry accounting for Excel/biometric exports.
 */
public final class ExcelImportStats {
    private int totalRowsSeen;
    private int dayRowsParsed;
    private int ignoredDueToMissingTimeInOrTimeOut;
    private int ignoredDueToMissingOrInvalidDate;
    private int employeeCount;

    public int getTotalRowsSeen() {
        return totalRowsSeen;
    }

    public void setTotalRowsSeen(int totalRowsSeen) {
        this.totalRowsSeen = Math.max(0, totalRowsSeen);
    }

    public int getDayRowsParsed() {
        return dayRowsParsed;
    }

    public void setDayRowsParsed(int dayRowsParsed) {
        this.dayRowsParsed = Math.max(0, dayRowsParsed);
    }

    public int getIgnoredDueToMissingTimeInOrTimeOut() {
        return ignoredDueToMissingTimeInOrTimeOut;
    }

    public void setIgnoredDueToMissingTimeInOrTimeOut(int ignoredDueToMissingTimeInOrTimeOut) {
        this.ignoredDueToMissingTimeInOrTimeOut = Math.max(0, ignoredDueToMissingTimeInOrTimeOut);
    }

    public int getIgnoredDueToMissingOrInvalidDate() {
        return ignoredDueToMissingOrInvalidDate;
    }

    public void setIgnoredDueToMissingOrInvalidDate(int ignoredDueToMissingOrInvalidDate) {
        this.ignoredDueToMissingOrInvalidDate = Math.max(0, ignoredDueToMissingOrInvalidDate);
    }

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = Math.max(0, employeeCount);
    }
}
