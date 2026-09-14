package com.example.MyFirstApp.config;

import com.example.MyFirstApp.security.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =====================================================
                // CSRF
                // =====================================================

                .csrf(
                        AbstractHttpConfigurer::disable
                )

                // =====================================================
                // CORS
                // =====================================================

                .cors(
                        Customizer.withDefaults()
                )

                // =====================================================
                // SESSION
                // =====================================================

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                // =====================================================
                // AUTHORIZATION
                // =====================================================

                .authorizeHttpRequests(
                        auth -> auth

                                // =============================================
                                // PUBLIC AUTH APIs
                                // =============================================

                                .requestMatchers(
                                        "/auth/register",
                                        "/auth/login"
                                )
                                .permitAll()

                                // =============================================
                                // PUBLIC AUDIO
                                //
                                // Important:
                                //
                                // Actual request:
                                //
                                // /api/v1.0/audio/file.mp3
                                //
                                // Because /api/v1.0 is context-path,
                                // Spring Security matcher sees:
                                //
                                // /audio/file.mp3
                                // =============================================

                                .requestMatchers(
                                        "/uploads/conversation/audio/**",
                                        "/audio/**"
                                )
                                .permitAll()

                                // =============================================
                                // PUBLIC WEBSOCKET HANDSHAKE
                                // =============================================

                                .requestMatchers(
                                        "/ws/live-interview",
                                        "/ws/live-interview/**",
                                        "/ws/resume-live-interview",
                                        "/ws/resume-live-interview/**"
                                        ,
                                        "/ws/conversation",
                                        "/ws/conversation/**"
                                )
                                .permitAll()

                                // =============================================
                                // STATIC / ERROR
                                // =============================================

                                .requestMatchers(
                                        "/error"
                                )
                                .permitAll()

                                // =============================================
                                // EVERYTHING ELSE
                                // =============================================

                                .anyRequest()
                                .authenticated()
                )

                // =====================================================
                // JWT FILTER
                // =====================================================

                .addFilterBefore(
                        jwtRequestFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =========================================================
    // CORS
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // ---------------------------------------------------------
        // DEVELOPMENT
        // ---------------------------------------------------------
        //
        // React/Vite
        //
        // React Native native requests generally do not depend
        // on browser CORS, but keeping the backend permissive for
        // development is useful.
        //

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173"
                )
        );

        // ---------------------------------------------------------
        // METHODS
        // ---------------------------------------------------------

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // ---------------------------------------------------------
        // HEADERS
        // ---------------------------------------------------------

        configuration.setAllowedHeaders(
                List.of("*")
        );

        // ---------------------------------------------------------
        // CREDENTIALS
        // ---------------------------------------------------------

        configuration.setAllowCredentials(
                true
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}