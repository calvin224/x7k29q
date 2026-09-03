ALTER TABLE sensors
    ADD COLUMN name VARCHAR(100);

ALTER TABLE sensors
    ADD CONSTRAINT uq_sensors_name UNIQUE (name);