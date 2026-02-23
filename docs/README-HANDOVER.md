# Technical Handover Package — Payroll System

This folder contains everything the client needs to **own and operate** the tailor-made payroll solution.

---

## Contents

| Document | Audience | Description |
|----------|----------|-------------|
| **USER-MANUAL.md** | End users (non-technical) | Step-by-step guides for login, loading CSV, adding/editing employees, paid leave, search, receipts, and export. Insert your own screenshots where indicated. |
| **ADMIN-DOCUMENTATION.md** | IT / technical staff | Runtime requirements (Java 21, JavaFX), CSV input formats (attendance vs legacy), encryption and audit log behaviour, and Philippine deduction rules. |
| **templates/attendance_template.csv** | Anyone preparing data | **Golden template** for **attendance / biometric export** format. Use this as the single source of truth for how the CSV must look (header rows, column order, date range, working hours as HH:MM, absences in column 13). |
| **templates/legacy_payroll_template.csv** | Anyone preparing data | **Golden template** for the **simple payroll** format (employeeId, name, hoursWorked, hourlyRate, optional client). |

---

## Quick Start for the Client

1. **End users:** Open **USER-MANUAL.md** and follow the steps; replace each *[Screenshot: ...]* with a real screenshot from your environment.
2. **IT / admins:** Read **ADMIN-DOCUMENTATION.md** for install requirements, CSV specs, and security notes.
3. **Data preparation:** Export or build your CSV to match either **attendance_template.csv** or **legacy_payroll_template.csv** exactly (column order and types). Do not rely on other exports without checking against these files.

---

*This handover package is part of the delivered solution. The client owns the solution; support or hosting can be agreed separately.*
