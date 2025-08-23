module com.system.chattalkdesktop {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.system.chattalkdesktop to javafx.fxml;
    exports com.system.chattalkdesktop;
}