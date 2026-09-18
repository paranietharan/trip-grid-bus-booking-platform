package com.paranietharan.tripgrid.auth.config;

import com.paranietharan.tripgrid.auth.entity.Role;
import com.paranietharan.tripgrid.auth.entity.User;
import com.paranietharan.tripgrid.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPasswordHash");
    }

    @Test
    @DisplayName("Should seed default users when database does not have them")
    void shouldSeedDefaultUsers() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        dataSeeder = new DataSeeder(userRepository, passwordEncoder, true);
        dataSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(userCaptor.capture());

        var seededUsers = userCaptor.getAllValues();
        assertThat(seededUsers).hasSize(4);

        assertThat(seededUsers).anyMatch(u -> u.getEmail().equals("superadmin@tripgrid.com") && u.getRole() == Role.SUPER_ADMIN && u.isEmailVerified());
        assertThat(seededUsers).anyMatch(u -> u.getEmail().equals("provider@tripgrid.com") && u.getRole() == Role.PROVIDER_ADMIN && DataSeeder.SEED_PROVIDER_UUID.equals(u.getTenantId()));
        assertThat(seededUsers).anyMatch(u -> u.getEmail().equals("staff@tripgrid.com") && u.getRole() == Role.PROVIDER_STAFF && DataSeeder.SEED_PROVIDER_UUID.equals(u.getTenantId()));
        assertThat(seededUsers).anyMatch(u -> u.getEmail().equals("customer@tripgrid.com") && u.getRole() == Role.CUSTOMER && u.isEmailVerified());
    }

    @Test
    @DisplayName("Should skip seeding if disabled")
    void shouldSkipSeedingIfDisabled() {
        dataSeeder = new DataSeeder(userRepository, passwordEncoder, false);
        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("Should not duplicate existing users")
    void shouldNotDuplicateExistingUsers() {
        when(userRepository.existsByEmailIgnoreCase("superadmin@tripgrid.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("provider@tripgrid.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("staff@tripgrid.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("customer@tripgrid.com")).thenReturn(true);

        dataSeeder = new DataSeeder(userRepository, passwordEncoder, true);
        dataSeeder.run();

        verify(userRepository, never()).save(any());
    }
}
