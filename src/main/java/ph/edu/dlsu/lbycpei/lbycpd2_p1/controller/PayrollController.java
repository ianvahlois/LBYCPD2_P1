package ph.edu.dlsu.lbycpei.lbycpd2_p1.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PaidLeaveEntry;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PayrollRecord;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.security.SessionContext;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.AuditLogger;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.CSVReader;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.CryptoUtils;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.ParseUtils;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.PayrollExport;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.ReceiptGenerator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PayrollController {

    @FXML private TableView<PayrollRecord> table;
    @FXML private TableColumn<PayrollRecord, String> idCol;
    @FXML private TableColumn<PayrollRecord, String> nameCol;
    @FXML private TableColumn<PayrollRecord, String> clientCol;
    @FXML private TableColumn<PayrollRecord, Double> hoursCol;
    @FXML private TableColumn<PayrollRecord, Double> rateCol;
    @FXML private TableColumn<PayrollRecord, Double> grossCol;
    @FXML private TableColumn<PayrollRecord, Double> sssCol;
    @FXML private TableColumn<PayrollRecord, Double> philhealthCol;
    @FXML private TableColumn<PayrollRecord, Double> pagibigCol;
    @FXML private TableColumn<PayrollRecord, Double> taxCol;
    @FXML private TableColumn<PayrollRecord, Double> netCol;

    @FXML private Label summaryLabel;
    @FXML private TextField companyNameField;
    @FXML private TextField defaultRateField;

    // Dashboard + search / filter controls
    @FXML private Label totalEmployeesLabel;
    @FXML private Label totalGrossLabel;
    @FXML private Label totalDeductionsLabel;
    @FXML private Label totalNetLabel;
    @FXML private TextField searchField;
    @FXML private TextField departmentFilterField;

    // Backing list with filtering support
    private final ObservableList<PayrollRecord> backingList = FXCollections.observableArrayList();
    private FilteredList<PayrollRecord> filteredList;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getEmployeeId()));
        nameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        clientCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getClient()));
        hoursCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getEffectiveHours()).asObject());
        rateCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getHourlyRate()).asObject());
        grossCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getGrossPay()).asObject());
        sssCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getSssDeduction()).asObject());
        philhealthCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPhilhealthDeduction()).asObject());
        pagibigCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPagibigDeduction()).asObject());
        taxCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getTaxDeduction()).asObject());
        netCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getNetPay()).asObject());

        // Tooltips so long names are fully visible on hover
        setColumnTooltip(nameCol, PayrollRecord::getName);
        setColumnTooltip(clientCol, PayrollRecord::getClient);
        setColumnTooltip(idCol, PayrollRecord::getEmployeeId);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        filteredList = new FilteredList<>(backingList, r -> true);
        table.setItems(filteredList);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        }
        if (departmentFilterField != null) {
            departmentFilterField.textProperty().addListener((obs, old, val) -> applyFilters());
        }

        updateSummary();
        backingList.addListener((javafx.collections.ListChangeListener.Change<? extends PayrollRecord> c) -> updateSummary());
    }

    private void setColumnTooltip(TableColumn<PayrollRecord, String> col,
                                  java.util.function.Function<PayrollRecord, String> getter) {
        col.setCellFactory(tc -> {
            TableCell<PayrollRecord, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    PayrollRecord r = empty ? null : getTableRow().getItem();
                    if (r != null) {
                        String full = getter.apply(r);
                        setTooltip(full == null || full.isEmpty() ? null : new Tooltip(full));
                    } else {
                        setTooltip(null);
                    }
                }
            };
            return cell;
        });
    }

    private void applyFilters() {
        String text = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase()
                : "";
        String deptFilter = departmentFilterField != null && departmentFilterField.getText() != null
                ? departmentFilterField.getText().trim().toLowerCase()
                : "";
        filteredList.setPredicate(rec -> {
            if (rec == null) return false;
            boolean matchesSearch = text.isEmpty()
                    || (rec.getName() != null && rec.getName().toLowerCase().contains(text))
                    || (rec.getEmployeeId() != null && rec.getEmployeeId().toLowerCase().contains(text))
                    || (rec.getClient() != null && rec.getClient().toLowerCase().contains(text));
            boolean matchesDept = deptFilter.isEmpty()
                    || (rec.getDepartment() != null && rec.getDepartment().toLowerCase().contains(deptFilter));
            return matchesSearch && matchesDept;
        });
        updateSummary();
    }

    private void updateSummary() {
        List<PayrollRecord> items = filteredList != null ? filteredList : table.getItems();
        if (items == null || items.isEmpty()) {
            if (summaryLabel != null) {
                summaryLabel.setText("No payroll data loaded.");
            }
            if (totalEmployeesLabel != null) totalEmployeesLabel.setText("0");
            if (totalGrossLabel != null) totalGrossLabel.setText("Php 0.00");
            if (totalDeductionsLabel != null) totalDeductionsLabel.setText("Php 0.00");
            if (totalNetLabel != null) totalNetLabel.setText("Php 0.00");
            return;
        }
        double totalGross = 0, totalDeductions = 0, totalNet = 0;
        for (PayrollRecord r : items) {
            totalGross += r.getGrossPay();
            totalDeductions += r.getTotalDeductions();
            totalNet += r.getNetPay();
        }
        if (summaryLabel != null) {
            summaryLabel.setText(String.format("Employees: %d  |  Total Gross: Php %,.2f  |  Total Deductions: Php %,.2f  |  Total Net: Php %,.2f",
                    items.size(), totalGross, totalDeductions, totalNet));
        }
        if (totalEmployeesLabel != null) totalEmployeesLabel.setText(String.valueOf(items.size()));
        if (totalGrossLabel != null) totalGrossLabel.setText(String.format("Php %,.2f", totalGross));
        if (totalDeductionsLabel != null) totalDeductionsLabel.setText(String.format("Php %,.2f", totalDeductions));
        if (totalNetLabel != null) totalNetLabel.setText(String.format("Php %,.2f", totalNet));
    }

    /** Adds a new employee via dialog; validates and audit-logs. */
    @FXML
    private void addEmployee() {
        ensureAuthenticated();
        Dialog<PayrollRecord> dialog = new Dialog<>();
        dialog.setTitle("Add Employee");
        dialog.setHeaderText("Enter new employee details");

        TextField idField = new TextField();
        idField.setPromptText("Employee ID");
        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        TextField deptField = new TextField();
        deptField.setPromptText("Department / Client");
        TextField hoursField = new TextField();
        hoursField.setPromptText("Hours worked (e.g. 160)");
        TextField rateField = new TextField();
        rateField.setPromptText("Hourly rate (e.g. 100)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Employee ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Department / Client:"), 0, 2);
        grid.add(deptField, 1, 2);
        grid.add(new Label("Hours worked:"), 0, 3);
        grid.add(hoursField, 1, 3);
        grid.add(new Label("Hourly rate (Php):"), 0, 4);
        grid.add(rateField, 1, 4);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        double defaultRate = 100.0;
        if (defaultRateField != null && defaultRateField.getText() != null && !defaultRateField.getText().isBlank()) {
            double r = ParseUtils.parseDouble(defaultRateField.getText().trim(), -1);
            if (r >= 0) defaultRate = r;
        }
        final double rateDefault = defaultRate;

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String id = idField.getText() != null ? idField.getText().trim() : "";
            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            if (id.isEmpty() || name.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Invalid input", "Employee ID and Name are required.");
                return null;
            }
            double hours = ParseUtils.parseDouble(hoursField.getText(), -1);
            double rate = ParseUtils.parseDouble(rateField.getText(), -1);
            if (hours < 0) hours = 0;
            if (rate < 0) rate = rateDefault;
            String client = deptField.getText() != null ? deptField.getText().trim() : "";
            PayrollRecord rec = new PayrollRecord(id, name, client, hours, rate, null, null);
            rec.setDepartment(client);
            return rec;
        });

        Optional<PayrollRecord> result = dialog.showAndWait();
        result.ifPresent(rec -> {
            backingList.add(rec);
            AuditLogger.logFieldChange(rec.getEmployeeId(), "ADD_EMPLOYEE", null, rec.getName());
            table.refresh();
            updateSummary();
        });
    }

    @FXML
    private void loadCSV() {
        ensureAuthenticated();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Payroll CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv")
        );
        File selectedFile = fileChooser.showOpenDialog(table.getScene().getWindow());
        if (selectedFile != null) {
            Double rate = null;
            if (defaultRateField != null && defaultRateField.getText() != null && !defaultRateField.getText().isBlank()) {
                double r = ParseUtils.parseDouble(defaultRateField.getText().trim(), -1);
                if (r >= 0) rate = r;
            }
            List<PayrollRecord> data = CSVReader.readCSV(selectedFile.getAbsolutePath(), rate);
            backingList.setAll(data);
            updateSummary();
        }
    }

    @FXML
    private void generateReceipts() {
        ensureAuthenticated();
        if (backingList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "Load payroll data first.");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Folder for Receipts");
        File dir = chooser.showDialog(table.getScene().getWindow());
        if (dir == null) return;
        try {
            String company = companyNameField != null ? companyNameField.getText() : null;
            LocalDate end = LocalDate.now();
            int count = ReceiptGenerator.generateReceipts(backingList, dir.toPath(), company, end);
            showAlert(Alert.AlertType.INFORMATION, "Receipts Generated",
                    String.format("%d receipt(s) saved to %s", count, dir.getAbsolutePath()));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate receipts: " + e.getMessage());
        }
    }

    @FXML
    private void exportPayroll() {
        ensureAuthenticated();
        if (backingList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "Load payroll data first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Encrypted Payroll CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Encrypted files (*.enc)", "*.enc"));
        chooser.setInitialFileName("payroll_export.enc");
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        try {
            // Export clear CSV into a string (never saved unencrypted to disk)
            Path temp = Files.createTempFile("payroll_export_", ".csv");
            try {
                PayrollExport.exportToCsv(backingList, temp);
                String csvContent = Files.readString(temp, StandardCharsets.UTF_8);
                char[] encPassword = SessionContext.getEncryptionPassword();
                byte[] encrypted = CryptoUtils.encryptToBytes(csvContent, encPassword);
                Files.write(file.toPath(), encrypted);
            } finally {
                Files.deleteIfExists(temp);
            }
            showAlert(Alert.AlertType.INFORMATION, "Export Done", "Encrypted payroll saved to " + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Export failed: " + e.getMessage());
        }
    }

    /**
     * Opens a small dialog that allows an authorized user to manually edit key
     * fields (hours, rate, deductions, basic identity fields). All changes are
     * validated and audit‑logged, and net pay is recalculated automatically.
     */
    @FXML
    private void editSelectedEmployee() {
        ensureAuthenticated();
        PayrollRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "No selection", "Select an employee first.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Edit Employee");
        dialog.setHeaderText("Manual override for " + selected.getName());
        dialog.setContentText("Enter field=value (e.g., hours=160, rate=120, sss=500):");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> applyManualEdit(selected, input));
    }

    private void applyManualEdit(PayrollRecord rec, String input) {
        String trimmed = input != null ? input.trim() : "";
        if (trimmed.isEmpty()) return;

        String[] parts = trimmed.split("=", 2);
        if (parts.length != 2) {
            showAlert(Alert.AlertType.ERROR, "Invalid format",
                    "Use: field=value (examples: hours=160, rate=120, name=Juan Dela Cruz)");
            return;
        }
        String field = parts[0].trim().toLowerCase();
        String value = parts[1].trim();
        try {
            switch (field) {
                case "hours" -> {
                    double old = rec.getHoursWorked();
                    double v = ParseUtils.parseDouble(value, -1);
                    if (v < 0) throw new IllegalArgumentException("Hours must not be negative.");
                    rec.setHoursWorked(v);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "hoursWorked", old, v);
                }
                case "rate" -> {
                    double old = rec.getHourlyRate();
                    double v = ParseUtils.parseDouble(value, -1);
                    if (v < 0) throw new IllegalArgumentException("Rate must not be negative.");
                    rec.setHourlyRate(v);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "hourlyRate", old, v);
                }
                case "name" -> {
                    String old = rec.getName();
                    rec.setName(value);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "name", old, value);
                }
                case "client" -> {
                    String old = rec.getClient();
                    rec.setClient(value);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "client", old, value);
                }
                case "department" -> {
                    String old = rec.getDepartment();
                    rec.setDepartment(value);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "department", old, value);
                }
                case "sss" -> {
                    double old = rec.getSssDeduction();
                    double v = ParseUtils.parseDouble(value, -1);
                    if (v < 0) throw new IllegalArgumentException("SSS must not be negative.");
                    rec.overrideSssDeduction(v);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "sssDeduction", old, v);
                }
                case "philhealth" -> {
                    double old = rec.getPhilhealthDeduction();
                    double v = ParseUtils.parseDouble(value, -1);
                    if (v < 0) throw new IllegalArgumentException("PhilHealth must not be negative.");
                    rec.overridePhilhealthDeduction(v);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "philhealthDeduction", old, v);
                }
                case "pagibig" -> {
                    double old = rec.getPagibigDeduction();
                    double v = ParseUtils.parseDouble(value, -1);
                    if (v < 0) throw new IllegalArgumentException("Pag-IBIG must not be negative.");
                    rec.overridePagibigDeduction(v);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "pagibigDeduction", old, v);
                }
                case "tax" -> {
                    double old = rec.getTaxDeduction();
                    double v = ParseUtils.parseDouble(value, -1);
                    if (v < 0) throw new IllegalArgumentException("Tax must not be negative.");
                    rec.overrideTaxDeduction(v);
                    AuditLogger.logFieldChange(rec.getEmployeeId(), "taxDeduction", old, v);
                }
                default -> {
                    showAlert(Alert.AlertType.ERROR, "Unknown field",
                            "Field must be one of: hours, rate, name, client, department, sss, philhealth, pagibig, tax.");
                    return;
                }
            }
            rec.computeDeductions();
            table.refresh();
            updateSummary();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid value", ex.getMessage());
        }
    }

    /**
     * Dialog to add a paid‑leave entry to the selected employee.
     */
    @FXML
    private void addPaidLeave() {
        ensureAuthenticated();
        PayrollRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "No selection", "Select an employee first.");
            return;
        }

        Dialog<PaidLeaveEntry> dialog = new Dialog<>();
        dialog.setTitle("Add Paid Leave");
        dialog.setHeaderText("Add paid leave for " + selected.getName());

        Label startLabel = new Label("Start date (YYYY-MM-DD):");
        TextField startField = new TextField();
        Label endLabel = new Label("End date (YYYY-MM-DD, optional):");
        TextField endField = new TextField();
        Label hoursLabel = new Label("Total paid-leave hours:");
        TextField hoursField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, startLabel, startField);
        grid.addRow(1, endLabel, endField);
        grid.addRow(2, hoursLabel, hoursField);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                LocalDate start = ParseUtils.parseDate(startField.getText());
                LocalDate end = endField.getText() == null || endField.getText().isBlank()
                        ? start
                        : ParseUtils.parseDate(endField.getText());
                double hours = ParseUtils.parseDouble(hoursField.getText(), -1);
                if (hours < 0) {
                    throw new IllegalArgumentException("Paid-leave hours must not be negative.");
                }
                return new PaidLeaveEntry(start, end, hours);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Invalid input", e.getMessage());
                return null;
            }
        });

        Optional<PaidLeaveEntry> res = dialog.showAndWait();
        res.ifPresent(entry -> {
            selected.addPaidLeaveEntry(entry);
            AuditLogger.logPaidLeave(selected.getEmployeeId(), "ADD", entry.toString());
            table.refresh();
            updateSummary();
        });
    }

    /**
     * Simple removal of a paid‑leave entry: asks for index number and removes
     * it from the selected employee.
     */
    @FXML
    private void removePaidLeave() {
        ensureAuthenticated();
        PayrollRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "No selection", "Select an employee first.");
            return;
        }
        List<PaidLeaveEntry> entries = selected.getPaidLeaveEntries();
        if (entries.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No paid leave",
                    "Selected employee has no paid-leave entries.");
            return;
        }

        StringBuilder sb = new StringBuilder("Existing paid-leave entries:\n");
        for (int i = 0; i < entries.size(); i++) {
            sb.append(i + 1).append(". ").append(entries.get(i)).append("\n");
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Remove Paid Leave");
        dialog.setHeaderText("Remove paid leave for " + selected.getName());
        dialog.setContentText(sb + "\nEnter entry number to remove:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(s -> {
            int idx = (int) ParseUtils.parseDouble(s, -1) - 1;
            if (idx < 0 || idx >= entries.size()) {
                showAlert(Alert.AlertType.ERROR, "Invalid index", "No entry with that number.");
                return;
            }
            PaidLeaveEntry removed = entries.get(idx);
            selected.removePaidLeaveEntry(removed);
            AuditLogger.logPaidLeave(selected.getEmployeeId(), "REMOVE", removed.toString());
            table.refresh();
            updateSummary();
        });
    }

    private void ensureAuthenticated() {
        if (!SessionContext.isAuthenticated()) {
            showAlert(Alert.AlertType.ERROR, "Access denied",
                    "You must log in before using payroll features.");
            throw new IllegalStateException("Unauthenticated access");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
