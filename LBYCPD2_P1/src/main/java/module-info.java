module ph.edu.dlsu.lbycpei.lbycpd2_p1 {
    requires javafx.controls;
    requires javafx.fxml;

    opens ph.edu.dlsu.lbycpei.lbycpd2_p1.controller to javafx.fxml;
    opens ph.edu.dlsu.lbycpei.lbycpd2_p1.model to javafx.base;

    exports ph.edu.dlsu.lbycpei.lbycpd2_p1;
}
