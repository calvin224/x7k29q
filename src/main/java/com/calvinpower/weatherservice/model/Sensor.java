package com.calvinpower.weatherservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sensors")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String name;

    protected Sensor() {
    }

    private Sensor(String name) {
        this.name = name;
    }

    public Sensor(Long id) {
        this.id = id;
    }

    public Sensor(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Sensor create() {
        return new Sensor();
    }

    public static Sensor create(String name) {
        return new Sensor(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}