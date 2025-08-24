module com.system.chattalkdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires javafx.media;


    opens com.system.chattalkdesktop to javafx.fxml;
    exports com.system.chattalkdesktop;
}