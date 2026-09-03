package com.calvinpower.weatherservice.repository;

import com.calvinpower.weatherservice.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    boolean existsByName(String name);

    List<Sensor> findAllByNameIn(Collection<String> names);

    Optional<Sensor> findByName(String name);
}
