package com.calvinpower.weatherservice.unit.controller;

import com.calvinpower.weatherservice.controller.SensorController;
import com.calvinpower.weatherservice.exception.DuplicateSensorNameException;
import com.calvinpower.weatherservice.model.Sensor;
import com.calvinpower.weatherservice.services.sensor.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.calvinpower.weatherservice.test_util.FixtureLoader.load;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorController.class)
class SensorControllerTest {

    private static final String FIXTURE_ROOT = "fixtures/sensors/create_sensor/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorService sensorService;

    @Test
    void given_registered_sensors_when_getting_sensors_then_returns_ok() throws Exception {
        when(sensorService.getSensors()).thenReturn(List.of(
                new Sensor(1L, "Dublin City Sensor"),
                new Sensor(2L)
        ));

        mockMvc.perform(get("/api/v1/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Dublin City Sensor"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").doesNotExist());

        verify(sensorService).getSensors();
    }

    @Test
    void given_no_registered_sensors_when_getting_sensors_then_returns_empty_result()
            throws Exception {
        when(sensorService.getSensors()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void given_sensor_name_when_creating_sensor_then_returns_created_sensor()
            throws Exception {

        Sensor sensor =
                new Sensor(1L, "Dublin City Sensor");

        when(sensorService.createSensor("Dublin City Sensor"))
                .thenReturn(sensor);

        mockMvc.perform(
                        post("/api/v1/sensors")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(load(FIXTURE_ROOT + "named-sensor.json"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Dublin City Sensor"));

        verify(sensorService)
                .createSensor("Dublin City Sensor");
    }

    @Test
    void given_no_sensor_name_when_creating_sensor_then_returns_created_sensor()
            throws Exception {

        Sensor sensor =
                new Sensor(1L);

        when(sensorService.createSensor(null))
                .thenReturn(sensor);

        mockMvc.perform(
                        post("/api/v1/sensors")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(load(FIXTURE_ROOT + "unnamed-sensor.json"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").doesNotExist());

        verify(sensorService)
                .createSensor(null);
    }

    @Test
    void given_duplicate_sensor_name_when_creating_sensor_then_returns_conflict()
            throws Exception {
        when(sensorService.createSensor("Dublin City Sensor"))
                .thenThrow(new DuplicateSensorNameException(
                        "Dublin City Sensor"
                ));

        mockMvc.perform(
                        post("/api/v1/sensors")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(load(FIXTURE_ROOT + "named-sensor.json"))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Sensor already exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "A sensor named 'Dublin City Sensor' already exists"
                ))
                .andExpect(jsonPath("$.instance")
                        .value("/api/v1/sensors"))
                .andExpect(jsonPath("$.code")
                        .value("DUPLICATE_SENSOR_NAME"));
    }

    @Test
    void given_blank_sensor_name_when_creating_sensor_then_returns_validation_problem()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/sensors")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(load(FIXTURE_ROOT + "blank-sensor.json"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("name must not be blank"))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }


}
