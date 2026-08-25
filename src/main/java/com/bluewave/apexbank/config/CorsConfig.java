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
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration=new CorsConfiguration();
        configuration.setAllowedHeaders(Arrays.asList("Authorization","Content-Type"));
        configuration.setAllowedMethods(Arrays.asList("PUT","GET","DELETE","PATCH","POST"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of("http://localhost:5500","http://127.0.0.1:5500","https://apexbank-ui.vercel.app"));

        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",configuration);
        return  source;
    }

}
