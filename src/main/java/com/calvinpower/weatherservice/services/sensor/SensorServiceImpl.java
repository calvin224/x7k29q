package com.calvinpower.weatherservice.services.sensor;

import com.calvinpower.weatherservice.exception.DuplicateSensorNameException;
import com.calvinpower.weatherservice.exception.SensorNotFoundException;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.repository.SensorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class SensorServiceImpl implements SensorService {

    private final SensorRepository sensorRepository;

    public SensorServiceImpl(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    @Override
    public Sensor createSensor(String name) {
        if (name != null && sensorRepository.existsByName(name)) {
            throw new DuplicateSensorNameException(name);
        }

        Sensor sensor = name == null
                ? Sensor.create()
                : Sensor.create(name);

        try {
            return sensorRepository.saveAndFlush(sensor);
        } catch (DataIntegrityViolationException exception) {
            if (name != null) {
                throw new DuplicateSensorNameException(name);
            }

            throw exception;
        }
    }

    @Override
    public List<Sensor> getSensors() {
        return sensorRepository.findAll();
    }

    @Override
    public Sensor getSensorByName(String name) {
        return sensorRepository.findByName(name)
                .orElseThrow(() -> new SensorNotFoundException(name));
    }

    @Override
    public List<Long> resolveSensorIds(
            Collection<Long> sensorIds,
            Collection<String> sensorNames
    ) {
        LinkedHashSet<Long> resolvedSensorIds =
                new LinkedHashSet<>(sensorIds);

        if (sensorNames.isEmpty()) {
            return List.copyOf(resolvedSensorIds);
        }

        Map<String, Long> sensorIdsByName = new LinkedHashMap<>();
        sensorRepository.findAllByNameIn(sensorNames)
                .forEach(sensor -> sensorIdsByName.put(
                        sensor.getName(),
                        sensor.getId()
                ));

        for (String sensorName : sensorNames) {
            Long sensorId = sensorIdsByName.get(sensorName);

            if (sensorId == null) {
                throw new SensorNotFoundException(sensorName);
            }

            resolvedSensorIds.add(sensorId);
        }

        return List.copyOf(resolvedSensorIds);
    }
}
