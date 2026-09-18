package com.paranietharan.tripgrid.provider.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paranietharan.tripgrid.provider.config.SecurityConfig;
import com.paranietharan.tripgrid.provider.dto.CreateProviderRequest;
import com.paranietharan.tripgrid.provider.dto.ProviderResponse;
import com.paranietharan.tripgrid.provider.entity.ProviderStatus;
import com.paranietharan.tripgrid.provider.entity.Role;
import com.paranietharan.tripgrid.provider.security.JwtAuthenticationFilter;
import com.paranietharan.tripgrid.provider.security.JwtService;
import com.paranietharan.tripgrid.provider.security.UserPrincipal;
import com.paranietharan.tripgrid.provider.service.ProviderService;
import com.paranietharan.tripgrid.provider.service.ProviderUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProviderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProviderService providerService;

    @MockitoBean
    private ProviderUserService providerUserService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/providers should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        CreateProviderRequest request = new CreateProviderRequest("Express", "contact@express.com", "+94771234567", "Colombo");

        mockMvc.perform(post("/api/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/providers should return 403 when authenticated as CUSTOMER")
    void shouldReturn403WhenCustomer() throws Exception {
        CreateProviderRequest request = new CreateProviderRequest("Express", "contact@express.com", "+94771234567", "Colombo");
        UserPrincipal customer = new UserPrincipal(UUID.randomUUID(), "customer@tripgrid.com", Role.CUSTOMER, null);

        mockMvc.perform(post("/api/providers")
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/providers should return 201 when authenticated as SUPER_ADMIN")
    void shouldReturn201WhenSuperAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        CreateProviderRequest request = new CreateProviderRequest("Express", "contact@express.com", "+94771234567", "Colombo");
        ProviderResponse response = new ProviderResponse(id, "Express", "contact@express.com", "+94771234567", "Colombo", ProviderStatus.ACTIVE, Instant.now(), Instant.now());
        UserPrincipal superAdmin = new UserPrincipal(UUID.randomUUID(), "admin@tripgrid.com", Role.SUPER_ADMIN, null);

        when(providerService.createProvider(any(CreateProviderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/providers")
                        .with(user(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Express"));
    }

    @Test
    @DisplayName("GET /api/providers/me should return current provider for PROVIDER_ADMIN")
    void shouldReturnProviderForProviderAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        ProviderResponse response = new ProviderResponse(id, "Express", "contact@express.com", "+94771234567", "Colombo", ProviderStatus.ACTIVE, Instant.now(), Instant.now());
        UserPrincipal providerAdmin = new UserPrincipal(UUID.randomUUID(), "provider@tripgrid.com", Role.PROVIDER_ADMIN, id.toString());

        when(providerService.getCurrentProvider()).thenReturn(response);

        mockMvc.perform(get("/api/providers/me")
                        .with(user(providerAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
