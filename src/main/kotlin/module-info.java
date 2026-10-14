module com.scanner.bidder {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires htmlunit;
    requires org.seleniumhq.selenium.api;
    requires org.seleniumhq.selenium.chrome_driver;
    requires dev.failsafe.core;
    requires commons.logging;
    requires okhttp3;
    requires simple.xml;
    requires org.jsoup;
    requires java.desktop;
    requires com.google.gson;
    requires org.json;
    requires org.apache.commons.io;
    requires com.google.auth.oauth2;
    requires google.cloud.firestore;
    requires firebase.admin;
    requires com.google.api.apicommon;
    requires opencv;

    exports com.linked.model to simple.xml;
    opens com.linked.bidder to javafx.fxml;
    exports com.linked.bidder;
}