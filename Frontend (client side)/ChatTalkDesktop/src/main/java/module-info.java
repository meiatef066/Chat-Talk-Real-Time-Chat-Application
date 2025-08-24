module com.system.chattalkdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires javafx.media;
    requires org.slf4j;
    requires java.net.http;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;

    opens com.system.chattalkdesktop.Dto to com.google.gson, com.fasterxml.jackson.databind;


    exports com.system.chattalkdesktop;
    exports com.system.chattalkdesktop.AuthService;
    exports com.system.chattalkdesktop.Dto;

    // Root package (if you have FXML controllers there)
    // AuthService package where LoginController is
    opens com.system.chattalkdesktop.AuthService to javafx.fxml;
    opens com.system.chattalkdesktop.Dto.AuthDto to com.fasterxml.jackson.databind, com.google.gson;
    opens com.system.chattalkdesktop.Dto.ChatDto to com.fasterxml.jackson.databind, com.google.gson;
    opens com.system.chattalkdesktop.Dto.entity to com.fasterxml.jackson.databind, com.google.gson;
    opens com.system.chattalkdesktop.NotificationService to javafx.fxml;
//    opens com.system.chattalkdesktop.MainChat.APIService to com.fasterxml.jackson.databind, javafx.fxml;
//    opens com.system.chattalkdesktop.service to com.fasterxml.jackson.databind, javafx.fxml;

}