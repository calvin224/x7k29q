package com.calvinpower.weatherservice.repository;

import com.calvinpower.weatherservice.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
}