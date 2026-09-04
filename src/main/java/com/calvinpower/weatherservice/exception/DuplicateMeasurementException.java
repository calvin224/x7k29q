package com.calvinpower.weatherservice.exception;

import com.calvinpower.weatherservice.model.Metric;

public class DuplicateMeasurementException extends RuntimeException {

    public DuplicateMeasurementException(Metric metric) {
        super("A " + metric
                + " measurement already exists for this sensor at the supplied timestamp");
    }
}
