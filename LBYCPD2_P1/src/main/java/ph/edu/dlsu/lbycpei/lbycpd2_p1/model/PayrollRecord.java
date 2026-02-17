package ph.edu.dlsu.lbycpei.lbycpd2_p1.model;

import java.time.LocalDate;

/**
 * Represents a single payroll record with Philippine statutory deductions.
 * Deductions follow SSS, PhilHealth, Pag-IBIG, and BIR withholding tax guidelines.
 */
public class PayrollRecord {

    private String employeeId;
    private String name;
    private String client;  // Department or client this payment is attributed to
    private double hoursWorked;
    private double hourlyRate;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;

    // Deductions (computed)
    private double sssDeduction;
    private double philhealthDeduction;
    private double pagibigDeduction;
    private double taxDeduction;

    public PayrollRecord(String employeeId, String name, double hoursWorked, double hourlyRate) {
        this(employeeId, name, null, hoursWorked, hourlyRate, null, null);
    }

    public PayrollRecord(String employeeId, String name, String client,
                         double hoursWorked, double hourlyRate,
                         LocalDate payPeriodStart, LocalDate payPeriodEnd) {
        this.employeeId = employeeId;
        this.name = name;
        this.client = client != null ? client.trim() : "";
        this.hoursWorked = hoursWorked;
        this.hourlyRate = hourlyRate;
        this.payPeriodStart = payPeriodStart;
        this.payPeriodEnd = payPeriodEnd;
        computeDeductions();
    }

    public String getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public String getClient() { return client != null ? client : ""; }
    public double getHoursWorked() { return hoursWorked; }
    public double getHourlyRate() { return hourlyRate; }
    public LocalDate getPayPeriodStart() { return payPeriodStart; }
    public LocalDate getPayPeriodEnd() { return payPeriodEnd; }

    public double getGrossPay() {
        return Math.round(hoursWorked * hourlyRate * 100.0) / 100.0;
    }

    public double getSssDeduction() { return sssDeduction; }
    public double getPhilhealthDeduction() { return philhealthDeduction; }
    public double getPagibigDeduction() { return pagibigDeduction; }
    public double getTaxDeduction() { return taxDeduction; }

    public double getTotalDeductions() {
        return Math.round((sssDeduction + philhealthDeduction + pagibigDeduction + taxDeduction) * 100.0) / 100.0;
    }

    public double getNetPay() {
        return Math.round((getGrossPay() - getTotalDeductions()) * 100.0) / 100.0;
    }

    /**
     * Philippine statutory deductions (simplified tiered/contribution rules).
     * SSS: based on salary brackets; PhilHealth: 4% with floor/ceiling; Pag-IBIG: 1–2% with cap; Tax: graduated.
     */
    private void computeDeductions() {
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
            sssDeduction = philhealthDeduction = pagibigDeduction = taxDeduction = 0;
        } else {
            sssDeduction = computeSSS(monthlyGross, gross);
            philhealthDeduction = computePhilHealth(monthlyGross, gross);
            pagibigDeduction = computePagIbig(monthlyGross, gross);
            taxDeduction = computeWithholdingTax(monthlyGross, gross);
        }

        sssDeduction = Math.round(sssDeduction * 100.0) / 100.0;
        philhealthDeduction = Math.round(philhealthDeduction * 100.0) / 100.0;
        pagibigDeduction = Math.round(pagibigDeduction * 100.0) / 100.0;
        taxDeduction = Math.round(taxDeduction * 100.0) / 100.0;
    }

    /** SSS employee share: tiered by monthly salary credit (simplified brackets). */
    private double computeSSS(double monthlyGross, double grossThisPay) {
        double msc = Math.min(Math.max(monthlyGross, 4_250), 99_750);
        // Employee share is approximately 4.5% of MSC, prorated to this pay period
        double monthlyContribution = msc * 0.045;
        return (grossThisPay / monthlyGross) * monthlyContribution;
    }

    /** PhilHealth: 4% of basic salary, min 200 / max 5,000 per month (prorated). */
    private double computePhilHealth(double monthlyGross, double grossThisPay) {
        double monthlyPremium = monthlyGross * 0.04;
        monthlyPremium = Math.max(200, Math.min(5_000, monthlyPremium));
        return (grossThisPay / monthlyGross) * monthlyPremium;
    }

    /** Pag-IBIG: 1% if monthly <= 1,500, else 2%, capped at 5,000 monthly salary. */
    private double computePagIbig(double monthlyGross, double grossThisPay) {
        double rate = monthlyGross <= 1_500 ? 0.01 : 0.02;
        double contributable = Math.min(monthlyGross, 5_000);
        double monthlyContribution = contributable * rate;
        return (grossThisPay / monthlyGross) * monthlyContribution;
    }

    /** Withholding tax on compensation (graduated TRAIN rates, simplified). */
    private double computeWithholdingTax(double monthlyGross, double grossThisPay) {
        double annual = monthlyGross * 12;
        double monthlyTax;
        if (annual <= 250_000) {
            monthlyTax = 0;
        } else if (annual <= 400_000) {
            monthlyTax = ((annual - 250_000) * 0.20) / 12;
        } else if (annual <= 800_000) {
            monthlyTax = (30_000 + (annual - 400_000) * 0.25) / 12;
        } else if (annual <= 2_000_000) {
            monthlyTax = (130_000 + (annual - 800_000) * 0.30) / 12;
        } else if (annual <= 8_000_000) {
            monthlyTax = (490_000 + (annual - 2_000_000) * 0.32) / 12;
        } else {
            monthlyTax = (2_410_000 + (annual - 8_000_000) * 0.35) / 12;
        }
        return (grossThisPay / monthlyGross) * monthlyTax;
    }
}
