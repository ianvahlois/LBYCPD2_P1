package ph.edu.dlsu.lbycpei.lbycpd2_p1.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.model.PayrollRecord;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.CSVReader;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.ParseUtils;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.PayrollExport;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.util.ReceiptGenerator;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

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

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getEmployeeId()));
        nameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        clientCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getClient()));
        hoursCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getHoursWorked()).asObject());
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
        setColumnTooltip(nameCol, r -> r.getName());
        setColumnTooltip(clientCol, r -> r.getClient());
        setColumnTooltip(idCol, r -> r.getEmployeeId());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        updateSummary();
        table.getItems().addListener((javafx.collections.ListChangeListener.Change<?> c) -> updateSummary());
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

    private void updateSummary() {
        if (summaryLabel == null) return;
        List<PayrollRecord> items = table.getItems();
        if (items == null || items.isEmpty()) {
            summaryLabel.setText("No payroll data loaded.");
            return;
        }
        double totalGross = 0, totalDeductions = 0, totalNet = 0;
        for (PayrollRecord r : items) {
            totalGross += r.getGrossPay();
            totalDeductions += r.getTotalDeductions();
            totalNet += r.getNetPay();
        }
        summaryLabel.setText(String.format("Employees: %d  |  Total Gross: Php %,.2f  |  Total Deductions: Php %,.2f  |  Total Net: Php %,.2f",
                items.size(), totalGross, totalDeductions, totalNet));
    }

    @FXML
    private void loadCSV() {
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
            table.setItems(FXCollections.observableArrayList(data));
        }
    }

    @FXML
    private void generateReceipts() {
        if (table.getItems().isEmpty()) {
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
            int count = ReceiptGenerator.generateReceipts(table.getItems(), dir.toPath(), company, end);
            showAlert(Alert.AlertType.INFORMATION, "Receipts Generated",
                    String.format("%d receipt(s) saved to %s", count, dir.getAbsolutePath()));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate receipts: " + e.getMessage());
        }
    }

    @FXML
    private void exportPayroll() {
        if (table.getItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "Load payroll data first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Payroll CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        chooser.setInitialFileName("payroll_export.csv");
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        try {
            PayrollExport.exportToCsv(table.getItems(), file.toPath());
            showAlert(Alert.AlertType.INFORMATION, "Export Done", "Saved to " + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Export failed: " + e.getMessage());
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
