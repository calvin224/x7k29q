package com.calvinpower.weatherservice.unit.services.sensor;

import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.repository.SensorRepository;
import com.calvinpower.weatherservice.services.sensor.SensorServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SensorServiceImplTest {

    private final SensorRepository sensorRepository =
            mock(SensorRepository.class);

    private final SensorServiceImpl sensorService =
            new SensorServiceImpl(sensorRepository);

    @Test
    void given_registered_sensors_when_getting_sensors_then_returns_repository_results() {
        List<Sensor> sensors = List.of(
                new Sensor(1L, "Dublin City Sensor"),
                new Sensor(2L)
        );
        when(sensorRepository.findAll()).thenReturn(sensors);

        List<Sensor> result = sensorService.getSensors();

        assertEquals(sensors, result);
        verify(sensorRepository).findAll();
    }

    @Test
    void given_sensor_name_when_creating_sensor_then_saves_sensor() {
        Sensor sensor =
                new Sensor(1L, "Dublin City Sensor");

        when(sensorRepository.save(any(Sensor.class)))
                .thenReturn(sensor);

        Sensor result =
                sensorService.createSensor("Dublin City Sensor");

        assertEquals(sensor, result);

        verify(sensorRepository)
                .save(any(Sensor.class));
    }

    @Test
    void given_no_sensor_name_when_creating_sensor_then_saves_sensor() {
        Sensor sensor =
                new Sensor(1L);

        when(sensorRepository.save(any(Sensor.class)))
                .thenReturn(sensor);

        Sensor result =
                sensorService.createSensor(null);

        assertEquals(sensor, result);

        verify(sensorRepository)
                .save(any(Sensor.class));
    }

    @Test
    void given_existing_sensor_name_when_getting_sensor_then_returns_sensor() {
        Sensor sensor = new Sensor(1L, "Dublin City Sensor");
        when(sensorRepository.findByName("Dublin City Sensor"))
                .thenReturn(Optional.of(sensor));

        Sensor result = sensorService.getSensorByName("Dublin City Sensor");

        assertEquals(sensor, result);
    }

    @Test
    void given_sensor_names_when_resolving_sensor_ids_then_combines_and_deduplicates_ids() {
        when(sensorRepository.findAllByNameIn(List.of(
                "Dublin City Sensor",
                "Cork Sensor"
        ))).thenReturn(List.of(
                new Sensor(1L, "Dublin City Sensor"),
                new Sensor(2L, "Cork Sensor")
        ));

        List<Long> result = sensorService.resolveSensorIds(
                List.of(1L, 3L),
                List.of("Dublin City Sensor", "Cork Sensor")
        );

        assertEquals(List.of(1L, 3L, 2L), result);
    }

    @Test
    void given_unknown_sensor_name_when_resolving_sensor_ids_then_throws_sensor_not_found_exception() {
        when(sensorRepository.findAllByNameIn(List.of("Unknown Sensor")))
                .thenReturn(List.of());

        assertThrows(
                com.calvinpower.weatherservice.exception.SensorNotFoundException.class,
                () -> sensorService.resolveSensorIds(
                        List.of(),
                        List.of("Unknown Sensor")
                )
        );
    }
}
