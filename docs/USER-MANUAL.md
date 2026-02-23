# Payroll System — User Manual

Step-by-step guide for non-technical users. Replace each *[Screenshot: description]* with an actual screenshot from your environment.

---

## 1. Logging In

1. Start the application (double-click the program or run it as instructed by your IT team).
2. You will see the **Login** window.
   - *[Screenshot: Login window showing Username and Password fields.]*
3. Enter your **Username** (e.g. `admin`).
4. Enter your **Password**.
5. Click **Login**.
6. If the details are correct, the **Payroll** window opens. If not, you will see an error message. After 5 failed attempts, login is blocked until the application is restarted.

---

## 2. Loading Attendance Data (CSV)

1. In the Payroll window, click **Load CSV** (blue button at the top).
   - *[Screenshot: Toolbar with Load CSV button highlighted.]*
2. A file browser opens. Go to the folder where your **attendance export** or **biometric report** CSV is saved.
3. Select the CSV file and click **Open**.
4. The table will fill with employees. Each row shows: Employee ID, Name, Client/Dept., Effective Hours, Rate, Gross Pay, SSS, PhilHealth, Pag-IBIG, Tax, and Net Pay.
   - *[Screenshot: Table filled with payroll rows after loading CSV.]*
5. **Optional:** Before loading, you can type a **Default hourly rate** (e.g. `100`) in the second row of the toolbar. This is used when the CSV does not contain hourly rates (e.g. attendance-only exports).

---

## 3. Adding a New Employee

1. Click **Add Employee** (green button).
   - *[Screenshot: Add Employee button in toolbar.]*
2. In the dialog, enter:
   - **Employee ID** (required)
   - **Name** (required)
   - **Department / Client** (optional)
   - **Hours worked** (e.g. `160`; leave blank for 0)
   - **Hourly rate (Php)** (e.g. `100`; leave blank to use the default rate)
3. Click **OK**. The new employee appears in the table and totals update.

---

## 4. Editing an Employee

1. Click a row in the table to **select** the employee.
2. Click **Edit Selected**.
3. In the dialog, type a **field** and **value** in this form: `field=value`
   - Examples:
     - `hours=160`
     - `rate=120`
     - `name=Juan Dela Cruz`
     - `department=IT`
     - `sss=500` (manual override for SSS deduction)
4. Click **OK**. Pay is recalculated and the table refreshes. All such changes are recorded in the audit log.

---

## 5. Paid Leave

**Adding paid leave**

1. Select the employee in the table.
2. Click **Add Paid Leave**.
3. Enter:
   - **Start date** (YYYY-MM-DD, e.g. `2025-02-01`)
   - **End date** (optional; leave blank to use start date)
   - **Total paid-leave hours** (e.g. `8`)
4. Click **OK**. The employee’s effective hours and gross pay will include these hours; they are not treated as absence.

**Removing paid leave**

1. Select the employee.
2. Click **Remove Paid Leave**.
3. The dialog lists existing paid-leave entries by number. Type the **entry number** to remove (e.g. `1`) and click **OK**.

---

## 6. Search and Filter

- **Search:** Type in the **Search** box to filter by employee name, ID, or client. The table and bottom totals update as you type.
- **Department:** Type in the **Department** box to show only employees in that department.

---

## 7. Generating Receipts

1. Optionally enter a **Company name** in the toolbar (used on the receipt header).
2. Click **Generate Receipts**.
3. Choose the **folder** where you want to save the HTML receipts.
4. Click **OK**. One receipt per employee is created. Open the HTML files in a browser to view or print.

---

## 8. Exporting Payroll (Encrypted)

1. Click **Export**.
2. Choose a location and name the file (default extension `.enc`).
3. Click **Save**. The payroll data is exported in **encrypted** form. Only someone with the same login password can decrypt it (via a separate decryption process if your IT team has set one up).

---

## 9. Summary at the Bottom

The bar at the bottom always shows:

- **Total Employees** (after filters)
- **Total Gross**
- **Total Deductions**
- **Total Net**

These update automatically when you load data, add or edit employees, or change search/filter.

---

## Getting Help

- For **forgotten password** or **access issues**, contact your administrator.
- For **CSV format** requirements, see the **Golden CSV Template** and **Admin Documentation** provided with this handover.
