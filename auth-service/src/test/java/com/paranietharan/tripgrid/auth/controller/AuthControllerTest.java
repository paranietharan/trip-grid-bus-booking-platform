package com.paranietharan.tripgrid.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paranietharan.tripgrid.auth.dto.*;
import com.paranietharan.tripgrid.auth.entity.Role;
import com.paranietharan.tripgrid.auth.entity.UserStatus;
import com.paranietharan.tripgrid.auth.security.CustomUserDetailsService;
import com.paranietharan.tripgrid.auth.security.JwtAuthenticationEntryPoint;
import com.paranietharan.tripgrid.auth.security.JwtService;
import com.paranietharan.tripgrid.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // focus on MVC routing & validation in web slice
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint entryPoint;

    @Test
    @DisplayName("POST /api/auth/register - Should return 201 when registration request is valid")
    void shouldReturn201OnValidRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Kasun",
                "Perera",
                "kasun@example.com",
                "+94771234567",
                "Password123!"
        );

        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(),
                "Kasun",
                "Perera",
                "kasun@example.com",
                "+94771234567",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                false,
                null,
                Instant.now()
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("kasun@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Kasun"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when validation fails")
    void shouldReturn400OnInvalidRegistration() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "", // empty first name
                "",
                "invalid-email",
                "123", // invalid phone
                "short" // invalid weak password
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 200 and AuthResponse on valid credentials")
    void shouldReturn200OnSuccessfulLogin() throws Exception {
        LoginRequest request = new LoginRequest("kasun@example.com", "Password123!");
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(),
                "Kasun",
                "Perera",
                "kasun@example.com",
                "+94771234567",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                true,
                null,
                Instant.now()
        );
        AuthResponse authResponse = new AuthResponse("mock_access_token", "mock_refresh_token", 900000, userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock_refresh_token"));
    }

    @Test
    @DisplayName("POST /api/auth/verify-email - Should return 200 on successful verification")
    void shouldReturn200OnVerifyEmail() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("kasun@example.com", "123456");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
