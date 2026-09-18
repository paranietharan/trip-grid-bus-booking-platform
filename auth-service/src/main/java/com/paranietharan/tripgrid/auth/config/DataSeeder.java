package com.paranietharan.tripgrid.auth.config;

import com.paranietharan.tripgrid.auth.entity.Role;
import com.paranietharan.tripgrid.auth.entity.User;
import com.paranietharan.tripgrid.auth.entity.UserStatus;
import com.paranietharan.tripgrid.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final UUID SEED_SUPER_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID SEED_PROVIDER_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID SEED_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID SEED_PROVIDER_STAFF_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final String SEED_PROVIDER_UUID = "11111111-1111-1111-1111-111111111111";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;

    public DataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${tripgrid.seed.enabled:true}") boolean seedEnabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Data seeding is disabled (tripgrid.seed.enabled=false)");
            return;
        }

        log.info("Checking sample data seeds for TripGrid Auth Service...");

        // 1. Super Admin
        seedUserIfNotExists(
                SEED_SUPER_ADMIN_ID,
                "superadmin@tripgrid.com",
                "SuperAdminPassword123!",
                "Super",
                "Admin",
                "+94770000001",
                Role.SUPER_ADMIN,
                null
        );

        // 2. Provider Admin
        seedUserIfNotExists(
                SEED_PROVIDER_ADMIN_ID,
                "provider@tripgrid.com",
                "ProviderPassword123!",
                "Provider",
                "Admin",
                "+94770000002",
                Role.PROVIDER_ADMIN,
                SEED_PROVIDER_UUID
        );

        // 3. Provider Staff
        seedUserIfNotExists(
                SEED_PROVIDER_STAFF_ID,
                "staff@tripgrid.com",
                "StaffPassword123!",
                "Provider",
                "Staff",
                "+94770000004",
                Role.PROVIDER_STAFF,
                SEED_PROVIDER_UUID
        );

        // 4. Customer
        seedUserIfNotExists(
                SEED_CUSTOMER_ID,
                "customer@tripgrid.com",
                "CustomerPassword123!",
                "Sample",
                "Customer",
                "+94770000003",
                Role.CUSTOMER,
                null
        );

        log.info("Data seeding check complete.");
    }

    private void seedUserIfNotExists(
            UUID id,
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            String phoneNumber,
            Role role,
            String tenantId) {

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.debug("Seed user already exists with email: {}", normalizedEmail);
            return;
        }

        User user = new User();
        user.setId(id);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setTenantId(tenantId);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.info("Seeded sample user: {} (Role: {}, Tenant: {})", normalizedEmail, role, tenantId != null ? tenantId : "N/A");
    }
}
