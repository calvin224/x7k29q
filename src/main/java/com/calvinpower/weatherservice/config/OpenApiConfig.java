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
                        .description("REST API for ingesting and querying weather sensor measurements.")
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
