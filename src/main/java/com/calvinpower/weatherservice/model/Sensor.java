package com.calvinpower.weatherservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sensors")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected Sensor() {
    }

    public Sensor(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}