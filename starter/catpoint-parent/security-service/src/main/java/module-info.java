module com.udacity.catpoint.security {
    requires java.desktop;
    requires com.miglayout.core;
    requires com.google.gson;
    requires com.google.common;
    requires org.slf4j;
    requires com.miglayout.swing;
    requires java.prefs;
    requires com.udacity.catpoint.image;
    exports com.udacity.catpoint.application;
    exports com.udacity.catpoint.data;
    exports com.udacity.catpoint.service;
}