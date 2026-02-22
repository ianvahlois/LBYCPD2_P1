package ph.edu.dlsu.lbycpei.lbycpd2_p1.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single payroll record with Philippine statutory deductions.
 *
 * This class also keeps track of manual overrides (hours, rate, individual
 * deduction components) and paid leave entries. All calculations are derived
 * from the current state of the record so the original CSV source stays
 * untouched.
 */
public class PayrollRecord {

    private String employeeId;
    private String name;
    private String client;  // Department or client this payment is attributed to
    private double hoursWorked;
    private double hourlyRate;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;

    // Additional attributes parsed from CSV where available
    private String department;

    // Paid leave entries (each contains its own hours)
    private final List<PaidLeaveEntry> paidLeaveEntries = new ArrayList<>();

    // Deductions (computed, but may be overridden manually)
    private double sssDeduction;
    private double philhealthDeduction;
    private double pagibigDeduction;
    private double taxDeduction;

    // Manual override flags -- if true, the corresponding deduction value
    // is treated as fixed and will not be recomputed automatically.
    private boolean sssOverridden;
    private boolean philhealthOverridden;
    private boolean pagibigOverridden;
    private boolean taxOverridden;

    public PayrollRecord(String employeeId, String name, double hoursWorked, double hourlyRate) {
        this(employeeId, name, null, hoursWorked, hourlyRate, null, null);
    }

    public PayrollRecord(String employeeId, String name, String client,
                         double hoursWorked, double hourlyRate,
                         LocalDate payPeriodStart, LocalDate payPeriodEnd) {
        this.employeeId = employeeId;
        this.name = name;
        this.client = client != null ? client.trim() : "";
        this.hoursWorked = Math.max(0, hoursWorked);
        this.hourlyRate = Math.max(0, hourlyRate);
        this.payPeriodStart = payPeriodStart;
        this.payPeriodEnd = payPeriodEnd;
        computeDeductions();
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getClient() { return client != null ? client : ""; }
    public void setClient(String client) { this.client = client; }

    public String getDepartment() { return department != null ? department : ""; }
    public void setDepartment(String department) { this.department = department; }

    public double getHoursWorked() { return hoursWorked; }

    /**
     * Sets the base worked hours (excluding paid leave). Negative values are
     * rejected to avoid unrealistic input.
     */
    public void setHoursWorked(double hoursWorked) {
        if (hoursWorked < 0) {
            throw new IllegalArgumentException("Hours worked must not be negative");
        }
        this.hoursWorked = hoursWorked;
        computeDeductions();
    }

    public double getHourlyRate() { return hourlyRate; }

    /**
     * Sets the hourly rate. Negative values are rejected.
     */
    public void setHourlyRate(double hourlyRate) {
        if (hourlyRate < 0) {
            throw new IllegalArgumentException("Hourly rate must not be negative");
        }
        this.hourlyRate = hourlyRate;
        computeDeductions();
    }

    public LocalDate getPayPeriodStart() { return payPeriodStart; }
    public void setPayPeriodStart(LocalDate payPeriodStart) {
        this.payPeriodStart = payPeriodStart;
        computeDeductions();
    }

    public LocalDate getPayPeriodEnd() { return payPeriodEnd; }
    public void setPayPeriodEnd(LocalDate payPeriodEnd) {
        this.payPeriodEnd = payPeriodEnd;
        computeDeductions();
    }

    /**
     * Total paid-leave hours added to base worked hours when computing gross
     * pay. Paid leave is never treated as an absence.
     */
    public double getPaidLeaveHours() {
        return paidLeaveEntries.stream().mapToDouble(PaidLeaveEntry::getHours).sum();
    }

    /**
     * Returns an unmodifiable view of the paid-leave entries for display.
     */
    public List<PaidLeaveEntry> getPaidLeaveEntries() {
        return Collections.unmodifiableList(paidLeaveEntries);
    }

    public void addPaidLeaveEntry(PaidLeaveEntry entry) {
        if (entry == null) return;
        paidLeaveEntries.add(entry);
        computeDeductions();
    }

    public void removePaidLeaveEntry(PaidLeaveEntry entry) {
        if (entry == null) return;
        paidLeaveEntries.remove(entry);
        computeDeductions();
    }

    /**
     * Effective hours used for gross pay computation, including paid leave.
     */
    public double getEffectiveHours() {
        return hoursWorked + getPaidLeaveHours();
    }

    public double getGrossPay() {
        return Math.round(getEffectiveHours() * hourlyRate * 100.0) / 100.0;
    }

    public double getSssDeduction() { return sssDeduction; }
    public double getPhilhealthDeduction() { return philhealthDeduction; }
    public double getPagibigDeduction() { return pagibigDeduction; }
    public double getTaxDeduction() { return taxDeduction; }

    public void overrideSssDeduction(double value) {
        if (value < 0) throw new IllegalArgumentException("SSS deduction must not be negative");
        this.sssDeduction = value;
        this.sssOverridden = true;
    }

    public void overridePhilhealthDeduction(double value) {
        if (value < 0) throw new IllegalArgumentException("PhilHealth deduction must not be negative");
        this.philhealthDeduction = value;
        this.philhealthOverridden = true;
    }

    public void overridePagibigDeduction(double value) {
        if (value < 0) throw new IllegalArgumentException("Pag-IBIG deduction must not be negative");
        this.pagibigDeduction = value;
        this.pagibigOverridden = true;
    }

    public void overrideTaxDeduction(double value) {
        if (value < 0) throw new IllegalArgumentException("Tax deduction must not be negative");
        this.taxDeduction = value;
        this.taxOverridden = true;
    }

    public double getTotalDeductions() {
        return Math.round((sssDeduction + philhealthDeduction + pagibigDeduction + taxDeduction) * 100.0) / 100.0;
    }

    public double getNetPay() {
        return Math.round((getGrossPay() - getTotalDeductions()) * 100.0) / 100.0;
    }

    /**
     * Recomputes statutory deductions based on the current effective hours and
     * hourly rate, unless a particular component has been manually overridden.
     */
    public void computeDeductions() {
        double gross = getGrossPay();
        // Use monthly equivalent for contribution tables (assume 2 pays per month if not set)
        double monthlyGross;
        if (payPeriodStart != null && payPeriodEnd != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(payPeriodStart, payPeriodEnd) + 1;
            monthlyGross = days > 0 ? gross * (30.0 / days) : gross * 2;
        } else {
            monthlyGross = gross * 2;
        }

        if (monthlyGross <= 0) {
            if (!sssOverridden) sssDeduction = 0;
            if (!philhealthOverridden) philhealthDeduction = 0;
            if (!pagibigOverridden) pagibigDeduction = 0;
            if (!taxOverridden) taxDeduction = 0;
        } else {
            if (!sssOverridden) {
                sssDeduction = computeSSS(monthlyGross, gross);
            }
            if (!philhealthOverridden) {
                philhealthDeduction = computePhilHealth(monthlyGross, gross);
            }
            if (!pagibigOverridden) {
                pagibigDeduction = computePagIbig(monthlyGross, gross);
            }
            if (!taxOverridden) {
                taxDeduction = computeWithholdingTax(monthlyGross, gross);
            }
        }

        sssDeduction = Math.round(sssDeduction * 100.0) / 100.0;
        philhealthDeduction = Math.round(philhealthDeduction * 100.0) / 100.0;
        pagibigDeduction = Math.round(pagibigDeduction * 100.0) / 100.0;
        taxDeduction = Math.round(taxDeduction * 100.0) / 100.0;
    }

    /** SSS: employee share per MSC brackets (SSS Circulars). Simplified: 4.5% of MSC, MSC clamped to 4,250–99,750. */
    private double computeSSS(double monthlyGross, double grossThisPay) {
        double msc = Math.min(Math.max(monthlyGross, 4_250), 99_750);
        double monthlyContribution = msc * 0.045;
        return (grossThisPay / monthlyGross) * monthlyContribution;
    }

    /** PhilHealth: 4% of basic salary, min ₱200 / max ₱5,000 monthly premium (UHC Law). Prorated to pay period. */
    private double computePhilHealth(double monthlyGross, double grossThisPay) {
        double monthlyPremium = monthlyGross * 0.04;
        monthlyPremium = Math.max(200, Math.min(5_000, monthlyPremium));
        return (grossThisPay / monthlyGross) * monthlyPremium;
    }

    /** Pag-IBIG: 1% if monthly compensation ≤ ₱1,500, else 2%; max contributable salary ₱5,000 (Pag-IBIG rules). */
    private double computePagIbig(double monthlyGross, double grossThisPay) {
        double rate = monthlyGross <= 1_500 ? 0.01 : 0.02;
        double contributable = Math.min(monthlyGross, 5_000);
        double monthlyContribution = contributable * rate;
        return (grossThisPay / monthlyGross) * monthlyContribution;
    }

    /**
     * Withholding tax on compensation per TRAIN Law (RA 10963), BIR tables.
     * Annual brackets: ≤250k exempt; 250k–400k 15%; 400k–800k 22.5k+20%;
     * 800k–2M 102.5k+25%; 2M–8M 402.5k+30%; above 8M 2,202.5k+35%.
     */
    private double computeWithholdingTax(double monthlyGross, double grossThisPay) {
        double annual = monthlyGross * 12;
        double annualTax;
        if (annual <= 250_000) {
            annualTax = 0;
        } else if (annual <= 400_000) {
            annualTax = (annual - 250_000) * 0.15;
        } else if (annual <= 800_000) {
            annualTax = 22_500 + (annual - 400_000) * 0.20;
        } else if (annual <= 2_000_000) {
            annualTax = 102_500 + (annual - 800_000) * 0.25;
        } else if (annual <= 8_000_000) {
            annualTax = 402_500 + (annual - 2_000_000) * 0.30;
        } else {
            annualTax = 2_202_500 + (annual - 8_000_000) * 0.35;
        }
        double monthlyTax = annualTax / 12;
        return (grossThisPay / monthlyGross) * monthlyTax;
    }
}

