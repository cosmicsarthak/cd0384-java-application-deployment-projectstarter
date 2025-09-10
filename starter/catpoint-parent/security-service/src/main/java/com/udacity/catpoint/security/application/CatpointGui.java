package com.udacity.catpoint.security.application;

import com.udacity.catpoint.security.data.PretendDatabaseSecurityRepositoryImpl;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.security.service.SecurityService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * This is the primary JFrame for the application that contains all the top-level JPanels.
 *
 * We're not using any dependency injection framework, so this class also handles constructing
 * all our dependencies and providing them to other classes as necessary.
 */
public class CatpointGui extends JFrame {
    private transient SecurityRepository securityRepository;
    private transient FakeImageService imageService;
    private transient SecurityService securityService;
    private transient DisplayPanel displayPanel;
    private transient ControlPanel controlPanel;
    private transient SensorPanel sensorPanel;
    private transient ImagePanel imagePanel;

    public CatpointGui() {
        securityRepository = new PretendDatabaseSecurityRepositoryImpl();
        imageService = new FakeImageService();
        securityService = new SecurityService(securityRepository, imageService);
        displayPanel = new DisplayPanel(securityService);
        controlPanel = new ControlPanel(securityService);
        sensorPanel = new SensorPanel(securityService);
        imagePanel = new ImagePanel(securityService);

        setLocation(100, 100);
        setSize(600, 850);
        setTitle("Very Secure App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout());
        mainPanel.add(displayPanel, "wrap");
        mainPanel.add(imagePanel, "wrap");
        mainPanel.add(controlPanel, "wrap");
        mainPanel.add(sensorPanel);

        setContentPane(mainPanel);

    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();

//        for spotsbug bug of low priority
        securityRepository = new PretendDatabaseSecurityRepositoryImpl();
        imageService = new FakeImageService();
        securityService = new SecurityService(securityRepository, imageService);
        displayPanel = new DisplayPanel(securityService);
        controlPanel = new ControlPanel(securityService);
        sensorPanel = new SensorPanel(securityService);
        imagePanel = new ImagePanel(securityService);
    }

}
