package com.calvinpower.weatherservice.test_util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class FixtureLoader {

    private FixtureLoader() {
    }

    public static String load(String path) throws IOException {
        try (InputStream inputStream =
                     FixtureLoader.class
                             .getClassLoader()
                             .getResourceAsStream(path)) {

            if (inputStream == null) {
                throw new IllegalArgumentException(
                        "Fixture not found: " + path
                );
            }

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
