package com.udacity.catpoint.security;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    private Sensor sensor1;
    private Sensor sensor2;
    private SecurityService securityService;

    @BeforeEach
    void setup() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor1 = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
        sensor2 = new Sensor(UUID.randomUUID().toString(), SensorType.WINDOW);
    }


    // 1 - alarm armed,sensor activated,putting to pending_alarm status
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorActivated_whenSystemArmedAndNoAlarm_shouldSetPendingAlarmStatus(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor1.setActive(false);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    // 2. alarm armed,sensor activated,already pending alarm,setting to alarm
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorActivated_whenSystemArmedAndPendingAlarm_shouldSetAlarmStatusAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor1.setActive(false);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3. if pending alarm,all sensors are inactive,to no_alarm status
    @Test
    void allSensorInactive_whenPendingAlarm_shouldSetAlarmStatusNoAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor1.setActive(true);
        Set<Sensor> sensors = Set.of(sensor1);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. if alarm active, change in sensor should not affect alarm
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sensorsStateChanged_whenAlarmActive_shouldNotChangeAlarmStatus(boolean sensorActive) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor1.setActive(!sensorActive);
        securityService.changeSensorActivationStatus(sensor1, sensorActive);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 5. if sensor activated while already active, in pending state,to alarm status
    @Test
    void sensorActivated_whenAlreadyActiveAndPending_setAlarmStatusAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor1.setActive(true);
        securityService.changeSensorActivationStatus(sensor1, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. if sensor deactivated while already inactive,no change to alarm state
    @Test
    void sensorDeactivated_whenAlreadyInactive_shouldNotChangeAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        sensor1.setActive(false);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }


    // 7. if image service identify image of cat while armed-home,then to alarm status
    @Test
    void imageProcessed_whenCatDetectedAndArmedHome_shouldSetAlarmStatusAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. if image service identify image no having cat, change status to no alarm as long as sensors not active
    @Test
    void imageProcessed_whenNoCatDetectedAndNoActiveSensors_shouldSetNoAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        sensor1.setActive(false);
        sensor2.setActive(false);
        Set<Sensor> inactiveSensors = Set.of(sensor1, sensor2);
        when(securityRepository.getSensors()).thenReturn(inactiveSensors);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    // Edge for 8. if image service identify no cat but sensors are active, not set no_alarm
    @Test
    void imageProcessed_whenNoCatButActiveSensors_shouldNotSetNoAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        sensor1.setActive(true);
        Set<Sensor> sensorsWithActive = Set.of(sensor1);
        when(securityRepository.getSensors()).thenReturn(sensorsWithActive);
        securityService.processImage(image);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9. if system disarmed,set no_alarm
    @Test
    void armingStatus_whenDisarmed_shouldSetNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository).setArmingStatus(ArmingStatus.DISARMED);
    }

    // 10. if system armed, reset all sensor to inactive
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void armingStatus_whenArmed_shouldResetAllToInactive(ArmingStatus armingStatus) {
        sensor1.setActive(true);
        sensor2.setActive(true);
        Set<Sensor> activeSensors = Set.of(sensor1, sensor2);
        when(securityRepository.getSensors()).thenReturn(activeSensors);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.setArmingStatus(armingStatus);
        verify(securityRepository, times(2)).updateSensor(any(Sensor.class));
        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());

    }

    // 11. if system armed_home while camera has cat,set alarm status
    @Test
    void armingStatus_whenArmedAndCatDetected_shouldSetAlarmStatusAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(image);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    // Edge1. process image,cat detected,system not armed,should not set alarm
    @Test
    void processImage_whenCatDetectButNotArmed_shouldNotSetAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(image);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Edge2.sensor deactivated
    @Test
    void sensorDeactivated_whenNoLastActiveSensorAndPending_shouldNotChangeToNoAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor1.setActive(true);
        sensor2.setActive(true);
        Set<Sensor> sensors = Set.of(sensor1, sensor2);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.changeSensorActivationStatus(sensor1, false);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Edge3. arming after disarmed, with active sensors,reset sensors
    @Test
    void armingAfterDisarmedWithActiveSensors_shouldResetSensors() {
        sensor1.setActive(true);
        sensor2.setActive(true);
        Set<Sensor> sensors = Set.of(sensor1, sensor2);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertFalse(sensor1.getActive(), "sensor should be inactive after arming");
        assertFalse(sensor2.getActive(), "sensor should be inactive after arming");
        verify(securityRepository, times(2)).updateSensor(any(Sensor.class));
        verify(securityRepository).setArmingStatus(ArmingStatus.ARMED_HOME);
    }

    // ListenerStatus1
    @Test
    void addStatusListener_ShouldAddListener() {
        securityService.addStatusListener(statusListener);
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
    }

    // ListenerStatus2
    @Test
    void removeStatusListener_ShouldRemoveListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener, never()).notify(any(AlarmStatus.class));

    }

    // getter1
    @Test
    void getAlarmStatus_shouldReturnRepoAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        AlarmStatus result  = securityService.getAlarmStatus();
        assertEquals(AlarmStatus.PENDING_ALARM, result);
    }

    // getter2
    @Test
    void getArmingStatus_shouldReturnRepoArmingStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        ArmingStatus result  = securityService.getArmingStatus();
        assertEquals(ArmingStatus.ARMED_HOME, result);
    }

    // getter3
    @Test
    void getSensors_shouldReturnRepoSensors() {
        Set<Sensor> expectedSensors = Set.of(sensor1, sensor2);
        when(securityRepository.getSensors()).thenReturn(expectedSensors);
        Set<Sensor> result = securityService.getSensors();
        assertEquals(expectedSensors, result);
    }

    // add sensor
    @Test
    void addSensor_shouldDelegateToRepo() {
        securityService.addSensor(sensor1);
        verify(securityRepository).addSensor(sensor1);
    }

    // remove sensor
    @Test
    void removeSensor_shouldDelegateToRepo() {
        securityService.removeSensor(sensor1);
        verify(securityRepository).removeSensor(sensor1);
    }


}
