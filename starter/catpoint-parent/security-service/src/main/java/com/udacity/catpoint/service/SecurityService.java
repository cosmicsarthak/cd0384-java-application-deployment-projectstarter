package com.udacity.catpoint.service;

import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private final ImageService imageService;
    private final SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private Boolean catDetect = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        if (securityRepository == null) {
            throw new IllegalArgumentException("securityRepository of SecurityRepository should not be null.");
        }

        if (imageService == null) {
            throw new IllegalArgumentException("imageService of ImageService should not be null.");
        }

        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if (armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        securityRepository.setArmingStatus(armingStatus);

        if (armingStatus == ArmingStatus.ARMED_HOME || armingStatus == ArmingStatus.ARMED_AWAY) {
            getSensors().forEach(sensor -> {
                sensor.setActive(false);
                securityRepository.updateSensor(sensor);
            });
        }

        if (armingStatus == ArmingStatus.ARMED_HOME && catDetect) {
            setAlarmStatus(AlarmStatus.ALARM);
        }

        statusListeners.forEach(statusListener -> statusListener.sensorStatusChanged());
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        catDetect = cat;
        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && noSensorActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
            case ALARM -> {}
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        if (securityRepository.getAlarmStatus() == AlarmStatus.ALARM) {
            return;
        }

        if (securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM && noSensorActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    private boolean noSensorActive() {
        return getSensors().stream().noneMatch(Sensor::getActive);
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        AlarmStatus currentAlarmStatus = securityRepository.getAlarmStatus();
        if (currentAlarmStatus == AlarmStatus.ALARM) {
            sensor.setActive(active);
            securityRepository.updateSensor(sensor);
            return;
        }

        if (Objects.equals(sensor.getActive(), active)) {
            if (active && currentAlarmStatus == AlarmStatus.PENDING_ALARM) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
            securityRepository.updateSensor(sensor);
            return;
        }

        boolean wasActive = sensor.getActive();
        sensor.setActive(active);

        if(!wasActive && active) {
            handleSensorActivated();
        } else if (wasActive && !active) {
            handleSensorDeactivated();
        }
        securityRepository.updateSensor(sensor);
        statusListeners.forEach(statusListener -> statusListener.sensorStatusChanged());
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
