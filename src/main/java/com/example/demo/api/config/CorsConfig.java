// src/main/java/com/example/demo/api/config/CorsConfig.java
package com.example.demo.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins (React development server)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",  // React dev server
                "http://localhost:3001",  // Alternative React port
                "http://127.0.0.1:3000",
                "https://your-domain.com" // Production domain
        ));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow credentials (for JWT tokens)
        configuration.setAllowCredentials(true);

        // How long the browser can cache preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}