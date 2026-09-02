CREATE TABLE sensors
(
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE sensor_measurements
(
    id          BIGSERIAL PRIMARY KEY,
    sensor_id   BIGINT                   NOT NULL,
    metric      VARCHAR(50)              NOT NULL
        CHECK (metric IN (
                          'TEMPERATURE',
                          'HUMIDITY',
                          'WIND_SPEED',
                          'PRESSURE',
                          'RAINFALL'
            )),
    value       DOUBLE PRECISION         NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_sensor_measurements_sensor
        FOREIGN KEY (sensor_id)
            REFERENCES sensors (id)
);

CREATE INDEX idx_sensor_measurements_sensor_metric_time
    ON sensor_measurements (sensor_id, metric, recorded_at);