package com.paranietharan.tripgrid.provider.config;

import com.paranietharan.tripgrid.provider.entity.*;
import com.paranietharan.tripgrid.provider.repository.*;
import com.paranietharan.tripgrid.provider.service.BusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class ProviderDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProviderDataSeeder.class);

    private final ProviderRepository providerRepository;
    private final ProviderUserRepository providerUserRepository;
    private final BusRepository busRepository;
    private final BusService busService;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final boolean seedEnabled;

    public ProviderDataSeeder(
            ProviderRepository providerRepository,
            ProviderUserRepository providerUserRepository,
            BusRepository busRepository,
            BusService busService,
            RouteRepository routeRepository,
            TripRepository tripRepository,
            @Value("${tripgrid.seed.enabled:true}") boolean seedEnabled) {
        this.providerRepository = providerRepository;
        this.providerUserRepository = providerUserRepository;
        this.busRepository = busRepository;
        this.busService = busService;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Provider data seeding is disabled (tripgrid.seed.enabled=false)");
            return;
        }

        log.info("Checking sample data seeds for TripGrid Provider Service...");

        String sampleEmail = "provider@tripgrid.com";
        Provider provider = providerRepository.findByEmailIgnoreCase(sampleEmail)
                .orElseGet(() -> {
                    Provider p = new Provider();
                    p.setName("Express Lines Lanka");
                    p.setEmail(sampleEmail);
                    p.setPhoneNumber("+94771234567");
                    p.setAddress("45 Galle Road, Colombo 03, Sri Lanka");
                    p.setStatus(ProviderStatus.ACTIVE);
                    Provider saved = providerRepository.save(p);
                    log.info("Seeded sample provider: {} ({})", saved.getName(), saved.getId());
                    return saved;
                });

        // Seed sample Bus if not exists
        String regNumber = "WP-ND-4521";
        Bus bus = busRepository.findByProviderId(provider.getId()).stream()
                .filter(b -> b.getRegistrationNumber().equalsIgnoreCase(regNumber))
                .findFirst()
                .orElseGet(() -> {
                    Bus b = new Bus();
                    b.setProviderId(provider.getId());
                    b.setRegistrationNumber(regNumber);
                    b.setName("Super Express 01");
                    b.setBusType(BusType.LUXURY);
                    b.setSeatCount(40);
                    b.setStatus(BusStatus.ACTIVE);
                    Bus saved = busRepository.save(b);
                    busService.generateSeatsForBus(saved.getId(), 40);
                    log.info("Seeded sample bus: {} with 40 seats", saved.getRegistrationNumber());
                    return saved;
                });

        // Seed sample Route if not exists
        Route route = routeRepository.findByProviderId(provider.getId()).stream()
                .filter(r -> r.getOrigin().equalsIgnoreCase("Colombo") && r.getDestination().equalsIgnoreCase("Kandy"))
                .findFirst()
                .orElseGet(() -> {
                    Route r = new Route();
                    r.setProviderId(provider.getId());
                    r.setOrigin("Colombo");
                    r.setDestination("Kandy");
                    r.setDistance(115.5);
                    r.setEstimatedDurationMinutes(210);
                    r.setStatus(RouteStatus.ACTIVE);
                    Route saved = routeRepository.save(r);
                    log.info("Seeded sample route: {} -> {}", saved.getOrigin(), saved.getDestination());
                    return saved;
                });

        // Seed sample scheduled Trip if not exists
        if (tripRepository.findByProviderId(provider.getId()).isEmpty()) {
            Instant departure = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS).plus(8, ChronoUnit.HOURS);
            Instant arrival = departure.plus(3, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);

            Trip trip = new Trip();
            trip.setProviderId(provider.getId());
            trip.setBusId(bus.getId());
            trip.setRouteId(route.getId());
            trip.setDepartureTime(departure);
            trip.setArrivalTime(arrival);
            trip.setPrice(new BigDecimal("2500.00"));
            trip.setCurrency("LKR");
            trip.setStatus(TripStatus.SCHEDULED);
            tripRepository.save(trip);
            log.info("Seeded sample trip: Colombo -> Kandy at {}", departure);
        }

        log.info("Provider data seeding check complete.");
    }
}
