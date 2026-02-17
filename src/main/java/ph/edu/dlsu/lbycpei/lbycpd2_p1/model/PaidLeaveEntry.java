package ph.edu.dlsu.lbycpei.lbycpd2_p1.model;

import java.time.LocalDate;

/**
 * Represents a single paid-leave entry for an employee.
 * The total paid-leave hours are rolled into gross pay computation in
 * {@link PayrollRecord}.
 */
public class PaidLeaveEntry {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double hours;

    public PaidLeaveEntry(LocalDate startDate, LocalDate endDate, double hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Paid leave hours must not be negative");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.hours = hours;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getHours() {
        return hours;
    }

    @Override
    public String toString() {
        String start = startDate != null ? startDate.toString() : "?";
        String end = endDate != null ? endDate.toString() : start;
        return String.format("%s to %s (%,.2f h)", start, end, hours);
    }
}

