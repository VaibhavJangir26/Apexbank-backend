package com.bluewave.apexbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all headers for custom idempotency tokens, auth, etc.
        configuration.setAllowedHeaders(List.of("*"));

        // Allow all essential HTTP methods including preflight OPTIONS
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Allowed origins & origin patterns for local dev and live Vercel/Render deployments
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://apexbank-ui.vercel.app",
                "https://*.vercel.app",
                "https://apexbank-backend-j74s.onrender.com",
                "https://*.onrender.com"
        ));

        // Expose critical response headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // Cache preflight CORS response for 1 hour to reduce preflight roundtrips
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
