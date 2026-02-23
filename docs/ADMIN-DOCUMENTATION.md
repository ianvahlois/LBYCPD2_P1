# Payroll System — Admin & Technical Documentation

Technical reference for IT staff: runtime requirements, CSV formats, and file behaviour.

---

## 1. Application Overview

- **Type:** Desktop Java (JavaFX) application.
- **Data:** In-memory during a session. No built-in database; payroll is loaded from CSV and can be exported (encrypted).
- **Authentication:** Single built-in account; password verified via SHA-256 hash. No external directory (LDAP/AD).

---

## 2. Server / Runtime Requirements

| Requirement | Details |
|-------------|--------|
| **Java** | JDK **21** (or as specified in project `pom.xml`). |
| **JavaFX** | Version **21** (OpenJFX), included via Maven dependencies. |
| **OS** | Windows, macOS, or Linux with a supported JDK. |
| **Memory** | Minimal; 512 MB heap is typically sufficient for hundreds of employees. |
| **Disk** | Only for: application JAR/files, CSV input, exported `.enc` file, receipts folder, and audit log. |
| **Network** | Not required for normal operation. |

**Build (for developers)**

- Maven 3.6+
- `mvn clean package` (or `mvn javafx:run` to run from IDE/Maven).

**Main class:** `ph.edu.dlsu.lbycpei.lbycpd2_p1.MainApp`

---

## 3. CSV Input Formats

The system accepts **two** CSV formats; format is auto-detected.

### 3.1 Attendance Report Format (Biometric / Attendance Export)

Used when the source is an **attendance summary** or **biometric export** (e.g. “Attendance summary” report).

**Structure**

| Line index | Content | Notes |
|------------|--------|------|
| 0 | `Attendance summary` (or line must contain the word “attendance”) | Title row. |
| 1 | `Date,YYYY-MM-DD ~ YYYY-MM-DD` | Pay period; comma-separated, second cell is the date range. |
| 2 | Main header row | Must contain “ID”, “Dept.” or “Working hours” or “Name”. |
| 3 | Sub-header row | Can be empty or labels for columns. |
| 4+ | Data rows | At least **13 columns** per row. |

**Required data columns (by index)**

| Index | Header (typical) | Type | Notes |
|-------|------------------|------|--------|
| 0 | ID | Text | Employee ID (numeric string). Rows with blank or non-numeric ID are skipped. |
| 1 | Name | Text | Employee full name. |
| 2 | Dept. | Text | Department or client; mapped to both Client and Department in the app. |
| 3 | Working hours (Standard) | HH:MM | e.g. `128:00` → 128 hours. Parsed as hours:minutes. |
| 12 | Absences | Number | Absence days. **8 hours per day** are deducted from working hours for pay. |

**Encoding:** UTF-8 recommended. Commas inside fields must be quoted (standard CSV).

**Hourly rate:** Not in this CSV. The app uses the **Default hourly rate** from the UI (or 100.0 if blank).

---

### 3.2 Legacy / Simple Payroll CSV

Used when the file does **not** start with an “attendance” title.

**Structure**

| Line index | Content |
|------------|--------|
| 0 | Header: `employeeId,name,hoursWorked,hourlyRate[,client]` |
| 1+ | Data rows: same column order, comma-separated. |

**Columns**

| Index | Name | Type | Notes |
|-------|------|------|--------|
| 0 | employeeId | Text | |
| 1 | name | Text | |
| 2 | hoursWorked | Number | Decimal allowed. |
| 3 | hourlyRate | Number | Decimal allowed. |
| 4 | client | Text | Optional. |

Missing or invalid numbers are treated as 0. Negative hours/rate are clamped to 0.

---

## 4. Golden CSV Template

Two reference files are provided so the client knows exactly what input should look like:

1. **`attendance_template.csv`** — Attendance report / biometric-style format (see Section 3.1).
2. **`legacy_payroll_template.csv`** — Simple payroll format (see Section 3.2).

Location: `docs/templates/` (or as supplied in the handover package). Use these as the **only** input format reference; do not rely on other exports without checking against these templates.

---

## 5. Exported Files and Local Files

| File / output | Location | Description |
|----------------|----------|-------------|
| **Encrypted export** | User-chosen path (e.g. `payroll_export.enc`) | AES-GCM encrypted CSV. Key derived from login password (PBKDF2). Format: first line `PAYROLL_ENC_V1`, then base64 lines for salt, IV, ciphertext. |
| **Audit log** | `payroll_audit.log` in the **current working directory** when the app is run | Plain-text log: timestamp, username, action (e.g. FIELD_CHANGE, PAID_LEAVE, ADD_EMPLOYEE). Append-only. |
| **Receipts** | User-chosen folder | One HTML file per employee; filenames derived from ID and name. |

**Important:** The application does **not** modify the original CSV. All edits and paid leave are in memory and reflected only in exports/receipts and the audit log.

---

## 6. Security Notes (for IT)

- **Password:** Stored only as a SHA-256 hash (e.g. in `AuthService`). Default credentials should be changed for production (change hash and/or username in code or config as per your policy).
- **Lockout:** After 5 failed login attempts, the login button is disabled until the application is restarted.
- **Encryption:** Export uses AES-256-GCM; key from login password via PBKDF2WithHmacSHA256 (65,536 iterations). Decryption is not implemented in the app; if the client needs to decrypt, a small utility or script must be provided separately.

---

## 7. Deduction Rules (Philippine Law)

- **SSS:** Employee share ~4.5% of Monthly Salary Credit (MSC); MSC clamped to 4,250–99,750.
- **PhilHealth:** 4% of basic salary; monthly premium clamped to ₱200–₱5,000; prorated to pay period.
- **Pag-IBIG:** 1% if monthly compensation ≤ ₱1,500, else 2%; contributable salary capped at ₱5,000.
- **Withholding tax:** TRAIN Law (RA 10963) graduated brackets on annualized income; prorated to pay period.

Details and formulas are in the source code (`PayrollRecord`); this document does not replace official BIR/SSS/PhilHealth/Pag-IBIG guidelines.

---

## 8. Support and Customization

- **Changing default credentials:** Update `AuthService` (username constant and password hash). Use a proper hash of the new password.
- **Changing default hourly rate:** See `CSVReader.DEFAULT_HOURLY_RATE` (used when no rate is in CSV and UI default is empty).
- **Adding columns or formats:** Requires changes in `CSVReader` and possibly `PayrollRecord`; ensure any new CSV format is documented and a new golden template is provided if needed.
