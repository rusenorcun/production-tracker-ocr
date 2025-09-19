// src/main/java/com/example/config/WebClientConfig.java
package com.example.uretimveri.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Configuration
@ConfigurationProperties(prefix = "python.api")
public class WebClientConfig {

    @Bean
    public WebClient pythonClient(
            @Value("${python.api.base:http://127.0.0.1:8000}") String baseUrl,
            @Value("${python.api.token:}") String token
    ) {
        var builder = WebClient.builder()
            .baseUrl(baseUrl)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer for large responses
            .filter((request, next) -> next.exchange(request)
                .doOnError(error -> {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Error communicating with Python API: " + error.getMessage(),
                        error
                    );
                }));

        if (!token.isBlank()) {
            builder = builder.defaultHeader("Authorization", "Bearer " + token);
        }
        
        return builder.build();
    }
}
