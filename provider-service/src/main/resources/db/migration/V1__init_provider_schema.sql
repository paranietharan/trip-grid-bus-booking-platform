-- ============================================================
-- TripGrid Provider Service Database Schema
-- Version: V1
-- ============================================================

CREATE TABLE IF NOT EXISTS providers (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(30) NOT NULL,
    address VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_providers_status ON providers (status);

CREATE TABLE IF NOT EXISTS provider_users (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_provider_user UNIQUE (provider_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_provider_users_provider_id ON provider_users (provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_users_user_id ON provider_users (user_id);

CREATE TABLE IF NOT EXISTS buses (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    registration_number VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    bus_type VARCHAR(50) NOT NULL,
    seat_count INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_bus_provider_reg UNIQUE (provider_id, registration_number)
);

CREATE INDEX IF NOT EXISTS idx_buses_provider_id ON buses (provider_id);
CREATE INDEX IF NOT EXISTS idx_buses_registration ON buses (registration_number);

CREATE TABLE IF NOT EXISTS seats (
    id UUID PRIMARY KEY,
    bus_id UUID NOT NULL REFERENCES buses(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    row_number INT NOT NULL,
    column_number INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_seat_bus_num UNIQUE (bus_id, seat_number)
);

CREATE INDEX IF NOT EXISTS idx_seats_bus_id ON seats (bus_id);

CREATE TABLE IF NOT EXISTS routes (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    distance DOUBLE PRECISION NOT NULL,
    estimated_duration_minutes INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_routes_provider_id ON routes (provider_id);
CREATE INDEX IF NOT EXISTS idx_routes_origin_dest ON routes (origin, destination);

CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    bus_id UUID NOT NULL REFERENCES buses(id) ON DELETE RESTRICT,
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE RESTRICT,
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_trips_provider_id ON trips (provider_id);
CREATE INDEX IF NOT EXISTS idx_trips_bus_id ON trips (bus_id);
CREATE INDEX IF NOT EXISTS idx_trips_route_id ON trips (route_id);
CREATE INDEX IF NOT EXISTS idx_trips_departure_time ON trips (departure_time);
CREATE INDEX IF NOT EXISTS idx_trips_schedule ON trips (bus_id, departure_time, arrival_time);
