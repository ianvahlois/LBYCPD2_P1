package ph.edu.dlsu.lbycpei.lbycpd2_p1.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.security.AuthService;
import ph.edu.dlsu.lbycpei.lbycpd2_p1.security.SessionContext;

import java.io.IOException;

/**
 * Controller for the login screen. Only authenticated users can open the
 * payroll dashboard window.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    private int failedAttempts = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @FXML
    private void handleLogin(ActionEvent event) {
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            showAlert(Alert.AlertType.ERROR, "Access blocked",
                    "Too many failed attempts. Please restart the application.");
            loginButton.setDisable(true);
            return;
        }

        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        char[] password = passwordField.getText() != null
                ? passwordField.getText().toCharArray()
                : new char[0];

        boolean ok = AuthService.authenticate(username, password);
        if (!ok) {
            failedAttempts++;
            passwordField.clear();
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                loginButton.setDisable(true);
                showAlert(Alert.AlertType.ERROR, "Access blocked",
                        "Too many failed attempts. Application will remain locked.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid credentials",
                        "Username or password is incorrect.");
            }
            return;
        }

        // Successful login: start session and open the payroll window.
        SessionContext.startSession(username, password);
        openPayrollWindow();

        // Clear sensitive data in the login window and close it.
        passwordField.clear();
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.close();
    }

    private void openPayrollWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/ph/edu/dlsu/lbycpei/lbycpd2_p1/payroll-view.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Simple Payroll System");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to open payroll window: " + e.getMessage());
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

