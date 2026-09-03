package com.calvinpower.weatherservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weatherServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weather Service API")
                        .description("""
                                Register weather sensors, record their measurements, query raw readings,
                                and calculate statistics over a selected time range.

                                Query endpoints accept sensor IDs, exact sensor names, and metric filters
                                in a JSON request body. Set `allSensors` to `true` with empty `sensorIds`
                                and `sensorNames` arrays to include every sensor.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Calvin Power")
                                .email("calvinpower44@gmail.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"))
                );
    }
}
