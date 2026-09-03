package com.calvinpower.weatherservice.exception;

public class DuplicateSensorNameException extends RuntimeException {

    public DuplicateSensorNameException(String sensorName) {
        super("A sensor named '" + sensorName + "' already exists");
    }
}
