package com.sportcourt.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración CORS global para SportCourt 2.0
 *
 * Se expone como CorsConfigurationSource (no como WebMvcConfigurer) para que
 * SecurityConfig la conecte con .cors(...): así Spring Security reconoce las
 * peticiones preflight (OPTIONS) y las resuelve ANTES de pasar por la cadena
 * de autorización/sesión. Sin esa conexión, cada preflight se trata como una
 * petición anónima más: HttpSessionSecurityContextRepository le crea una
 * sesión nueva (Set-Cookie: JSESSIONID) que pisa la cookie de sesión
 * autenticada del navegador justo antes de la petición real, tumbando el
 * login con 403 en cualquier llamada que dispare preflight (por llevar
 * X-XSRF-TOKEN u otro header no "simple").
 *
 * IMPORTANTE: En producción, cambiar origins a tu dominio real.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5500",
                "http://127.0.0.1:5500"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
