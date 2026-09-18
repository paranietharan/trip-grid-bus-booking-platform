package com.paranietharan.tripgrid.provider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paranietharan.tripgrid.provider.dto.ErrorResponse;
import com.paranietharan.tripgrid.provider.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = new ErrorResponse(
                    Instant.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "UNAUTHORIZED",
                    "Full authentication is required to access this resource",
                    request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = new ErrorResponse(
                    Instant.now(),
                    HttpStatus.FORBIDDEN.value(),
                    "FORBIDDEN",
                    "Access is denied: You do not have permission to access this resource",
                    request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trips/search").permitAll()

                        // Provider Administration (SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/providers").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/providers").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/providers/*/status").hasRole("SUPER_ADMIN")

                        // Provider Self Management
                        .requestMatchers(HttpMethod.GET, "/api/providers/me").hasAnyRole("PROVIDER_ADMIN", "PROVIDER_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/providers/me").hasRole("PROVIDER_ADMIN")
                        .requestMatchers("/api/providers/me/users/**").hasRole("PROVIDER_ADMIN")

                        // Buses, Seats, Routes, Trips
                        .requestMatchers("/api/buses/**").hasAnyRole("SUPER_ADMIN", "PROVIDER_ADMIN", "PROVIDER_STAFF")
                        .requestMatchers("/api/routes/**").hasAnyRole("SUPER_ADMIN", "PROVIDER_ADMIN", "PROVIDER_STAFF")
                        .requestMatchers("/api/trips/**").hasAnyRole("SUPER_ADMIN", "PROVIDER_ADMIN", "PROVIDER_STAFF")

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
