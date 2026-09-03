package com.calvinpower.weatherservice.exception;

public class SensorNotFoundException extends RuntimeException {

    public SensorNotFoundException(Long sensorId) {
        super("Sensor " + sensorId + " does not exist");
    }

    public SensorNotFoundException(String sensorName) {
        super("Sensor named '" + sensorName + "' does not exist");
    }
}
