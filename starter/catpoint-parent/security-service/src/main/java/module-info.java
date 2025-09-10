module com.udacity.catpoint.security {
    requires java.desktop;
    requires org.slf4j;
    requires com.miglayout.core;
    requires com.google.gson;
    requires com.google.common;
    requires com.miglayout.swing;
    requires java.prefs;
    requires com.udacity.catpoint.image;
    opens com.udacity.catpoint.security.data to com.google.gson;
}