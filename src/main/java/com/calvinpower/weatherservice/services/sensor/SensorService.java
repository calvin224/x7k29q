package com.calvinpower.weatherservice.services.sensor;

import com.calvinpower.weatherservice.model.Sensor;

import java.util.Collection;
import java.util.List;

public interface SensorService {

    Sensor createSensor(String name);

    List<Sensor> getSensors();

    Sensor getSensorByName(String name);

    List<Long> resolveSensorIds(
            Collection<Long> sensorIds,
            Collection<String> sensorNames
    );
}
