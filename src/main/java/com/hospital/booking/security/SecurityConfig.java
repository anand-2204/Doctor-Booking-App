
package com.hospital.booking.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // Swagger documentation: public access
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Authentication endpoints: public access
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/logout"
                ).permitAll()

                .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                
                // All authenticated roles may view doctors
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/doctors/**"
                ).hasAnyRole("ADMIN", "DOCTOR", "PATIENT")

                // Only ADMIN and DOCTOR may manage doctor records
                .requestMatchers("/api/doctors/**")
                    .hasAnyRole("ADMIN", "DOCTOR")

                // Patients can view their own profile
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/patients/me"
                ).hasAnyRole("ADMIN", "PATIENT")

                // Patients can update their own profile
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/patients/me"
                ).hasAnyRole("ADMIN", "PATIENT")

                // All other patient CRUD endpoints are ADMIN-only
                .requestMatchers("/api/patients/**")
                    .hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/appointments/my")
                .hasAnyRole("ADMIN", "DOCTOR", "PATIENT")    
                    
                .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/cancel")
                .hasAnyRole("ADMIN", "DOCTOR", "PATIENT")
                
                .requestMatchers(HttpMethod.POST, "/api/appointments")
                .hasAnyRole("ADMIN", "PATIENT")

                .requestMatchers("/api/appointments/**")
                .hasRole("ADMIN")

                // All remaining endpoints require authentication
                .anyRequest().authenticated()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // CORS configuration for a frontend running on localhost:3000
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of("http://localhost:3000")
        );

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

        configuration.setAllowedHeaders(
            List.of("Authorization", "Content-Type")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
