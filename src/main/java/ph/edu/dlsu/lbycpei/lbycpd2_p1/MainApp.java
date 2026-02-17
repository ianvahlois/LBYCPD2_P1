package ph.edu.dlsu.lbycpei.lbycpd2_p1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Start the application at the secure login screen. The actual payroll
        // dashboard window is only opened by the LoginController once the user
        // is authenticated.
        Scene scene = new Scene(
                FXMLLoader.load(
                        getClass().getResource("/ph/edu/dlsu/lbycpei/lbycpd2_p1/login-view.fxml")
                )
        );

        stage.setTitle("Payroll System - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
