package ph.edu.dlsu.lbycpei.lbycpd2_p1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(
                FXMLLoader.load(
                        getClass().getResource("/ph/edu/dlsu/lbycpei/lbycpd2_p1/payroll-view.fxml")
                )
        );

        stage.setTitle("Simple Payroll System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
