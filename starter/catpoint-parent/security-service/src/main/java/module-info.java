module com.udacity.catpoint.security {
    requires java.desktop;
    requires org.slf4j;
    requires com.miglayout.core;
    requires com.google.gson;
    requires com.google.common;
    requires com.miglayout.swing;
    requires java.prefs;
    requires com.udacity.catpoint.image;
    exports com.udacity.catpoint.security.application;
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.service;

    opens com.udacity.catpoint.security.data to com.google.gson;
}