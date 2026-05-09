package com.example.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Single source of truth for CORS in the gateway.
 * NOTE: Do NOT add globalcors in application.properties — that creates duplicate headers.
 * NOTE: Do NOT add @CrossOrigin on any controller behind this gateway.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // Specific origins only — wildcard + allowCredentials=true is rejected by browsers
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://192.168.56.*:*");
        config.addAllowedOriginPattern("http://192.168.40.*:*");

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
