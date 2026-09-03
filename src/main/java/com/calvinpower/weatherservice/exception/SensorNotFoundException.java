package com.calvinpower.weatherservice.exception;

public class SensorNotFoundException extends RuntimeException {

    public SensorNotFoundException(Long sensorId) {
        super("Sensor not found: " + sensorId);
    }

    public SensorNotFoundException(String sensorName) {
        super("Sensor not found: " + sensorName);
    }
}
