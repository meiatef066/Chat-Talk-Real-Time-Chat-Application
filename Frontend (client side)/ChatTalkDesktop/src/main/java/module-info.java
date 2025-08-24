module com.system.chattalkdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.desktop;
    requires org.slf4j;
    requires org.slf4j.simple;
    requires java.net.http;
    requires com.google.gson;
    requires spring.web;
    requires spring.websocket;
    requires spring.messaging;
    requires spring.core;
    requires spring.beans;
    requires spring.context;

    opens com.system.chattalkdesktop.Dto to com.google.gson, com.fasterxml.jackson.databind;
    opens com.system.chattalkdesktop.AuthService to javafx.fxml;
    opens com.system.chattalkdesktop to javafx.fxml, com.fasterxml.jackson.databind;
    opens com.system.chattalkdesktop.NotificationService to javafx.fxml;
//    opens com.system.chattalkdesktop.MainChat to com.fasterxml.jackson.databind, javafx.fxml;
//    opens com.system.chattalkdesktop.SearchService to com.fasterxml.jackson.databind, javafx.fxml;
    opens com.system.chattalkdesktop.Profile to javafx.fxml;

    exports com.system.chattalkdesktop;
    exports com.system.chattalkdesktop.AuthService;
    exports com.system.chattalkdesktop.Dto;
    exports com.system.chattalkdesktop.NotificationService;
//    exports com.system.chattalkdesktop.MainChat;
//    exports com.system.chattalkdesktop.SearchService;
//    exports com.system.chattalkdesktop.Dto.AuthDto;
//    exports com.system.chattalkdesktop.Dto.ChatDto;
//    exports com.system.chattalkdesktop.Dto.entity;
//    exports com.system.chattalkdesktop.MainChat.APIService;
//    exports com.system.chattalkdesktop.service;

    opens com.system.chattalkdesktop.common to javafx.fxml;
    opens com.system.chattalkdesktop.Dto.AuthDto to com.fasterxml.jackson.databind, com.google.gson;
    opens com.system.chattalkdesktop.Dto.ChatDto to com.fasterxml.jackson.databind, com.google.gson;
    opens com.system.chattalkdesktop.Dto.entity to com.fasterxml.jackson.databind, com.google.gson;
//    opens com.system.chattalkdesktop.MainChat.APIService to com.fasterxml.jackson.databind, javafx.fxml;
//    opens com.system.chattalkdesktop.service to com.fasterxml.jackson.databind, javafx.fxml;
}