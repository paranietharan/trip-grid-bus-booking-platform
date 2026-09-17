# TripGrid — Project Context

## 1. Project Overview

TripGrid is a multi-tenant bus booking platform built with Java and Spring Boot.
The platform allows multiple independent bus providers to operate their own buses, routes, trips, and bookings through isolated provider dashboards.
Customers can register, log in, search available trips, select seats, make payments, and receive booking notifications.
Bus Providers can register, login add their bus details with weekly availabilities and get the reservation details.
A platform-level Super Admin can manage and monitor all bus providers.
The primary goal of this project is to demonstrate real-world backend engineering concepts including:

- Multi-tenant architecture
- Microservices
- Authentication and authorization
- JWT-based security
- Role-based access control
- Seat reservation and concurrency control
- Payment processing
- Event-driven architecture
- RabbitMQ
- Redis
- PostgreSQL
- Docker
- API design
- Observability


## 2. Project Name

Project name: TripGrid
Repository: trip-grid-bus-booking-platform


## 3. Core Users
The platform has three primary roles.

### SUPER_ADMIN

Platform administrator.

Responsibilities:

- Manage bus providers
- Approve providers
- Suspend providers
- View all providers
- View all buses
- View all trips
- View all bookings
- View platform statistics

SUPER_ADMIN has access across all tenants.


### PROVIDER_ADMIN

Administrator of a bus provider.

Responsibilities:

- Manage provider profile
- Manage buses
- Manage seats
- Manage routes
- Create and manage trips
- View bookings belonging to their provider
- View provider statistics

A PROVIDER_ADMIN must only access data belonging to their tenant.


### CUSTOMER

Normal platform user.

Responsibilities:

- Register
- Login
- Search trips
- View available seats
- Book seats
- Make payments
- View booking history
- Cancel eligible bookings
- Receive notifications

Customers are not tenant administrators.


## 4. Multi-Tenant Architecture

Each bus provider is a tenant.

Example:

    Tenant A
    ├── Buses
    ├── Routes
    ├── Trips
    └── Bookings

    Tenant B
    ├── Buses
    ├── Routes
    ├── Trips
    └── Bookings

Tenant A must never be able to access Tenant B's data.
Tenant information must be derived from the authenticated user's security context.
Do NOT trust a tenant_id supplied by the client.
Bad:
    GET /buses?tenant_id=tenant-b

Good:
    GET /buses

The backend determines the tenant from the authenticated user's context.
Tenant isolation must be enforced at the service/repository/data-access layer.


## 5. Authentication

Initial authentication method:

Email + Password
Google OAuth/OIDC is NOT part of the initial implementation.
Google authentication may be added later.
Authentication is handled by auth-service.
Authentication requirements:

- Password hashing
- JWT access tokens
- Refresh tokens
- Token expiration
- Logout/revocation strategy
- Role-based authorization

Passwords must NEVER be stored in plaintext.
Recommended password hashing:
BCrypt or Argon2.


## 6. JWT

The access token should contain information required for authorization.

Example:

    {
      "sub": "user-123",
      "role": "PROVIDER_ADMIN",
      "tenant_id": "tenant-456"
    }

The exact JWT structure can evolve during implementation.

The application must not trust arbitrary tenant information supplied through HTTP parameters when the authenticated token already provides the tenant context.


## 7. Services

The initial architecture contains the following services:

    api-gateway
    auth-service
    provider-service
    booking-service
    payment-service
    notification-service


### 7.1 auth-service

Responsibilities:

- User registration
- Login
- Password hashing
- JWT generation
- Refresh tokens
- Authentication
- Role management

Initial endpoints:

    POST /auth/register
    POST /auth/login
    POST /auth/refresh
    POST /auth/logout


### 7.2 provider-service

Responsibilities:

- Tenant/provider management
- Bus management
- Seat configuration
- Route management
- Trip management
- Provider-level statistics

Main entities:

    Tenant
    Bus
    Seat
    Route
    Trip


### 7.3 booking-service

Responsibilities:

- Trip search
- Seat availability
- Temporary seat reservation
- Booking creation
- Booking cancellation
- Booking history
- Booking state management

Main entities:

    Booking
    BookingSeat

This service is responsible for preventing double booking.

Concurrent requests for the same seat must be handled safely.


### 7.4 payment-service

Responsibilities:

- Payment creation
- Payment status
- Payment confirmation
- Payment failure
- Payment webhooks
- Payment idempotency

The booking service must not directly contain payment-provider-specific logic.

Example flow:

    Booking
       ↓
    Payment Service
       ↓
    Payment Provider
       ↓
    Payment Webhook
       ↓
    Payment Service
       ↓
    PaymentCompleted event


### 7.5 notification-service

Responsibilities:

- Email notifications
- Booking confirmation
- Payment confirmation
- Booking cancellation
- Other system notifications

Notification processing should be asynchronous.

Example:

    BookingConfirmed
          ↓
       RabbitMQ
          ↓
    Notification Service
          ↓
        Email


### 7.6 api-gateway

Responsibilities:

- API routing
- Authentication filtering
- Rate limiting
- Request correlation
- Central entry point for clients

The gateway should not contain business logic.


## 8. Technology Stack

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

### Database

- PostgreSQL

### Messaging

- RabbitMQ

### Cache / Temporary State

- Redis

### Infrastructure

- Docker
- Docker Compose

### Testing

- JUnit
- Mockito
- Testcontainers

### API Documentation

- OpenAPI
- Swagger UI

### Future Infrastructure

- Kubernetes
- Prometheus
- Grafana
- OpenTelemetry


## 9. Database Architecture

Each service owns its own data.

Logical databases:

    auth_db
    provider_db
    booking_db
    payment_db

Services must not directly access another service's database tables.

Example:

    auth-service       → auth_db
    provider-service   → provider_db
    booking-service    → booking_db
    payment-service    → payment_db

For local development, these databases may initially run inside the same PostgreSQL instance.

Database ownership is still separated logically.


## 10. Core Domain Model

### Tenant

    id
    name
    email
    status
    created_at
    updated_at


### User

    id
    email
    password_hash
    name
    role
    status
    created_at
    updated_at


### Bus

    id
    tenant_id
    registration_number
    name
    bus_type
    seat_count
    status


### Seat

    id
    bus_id
    seat_number
    row_number
    column_number


### Route

    id
    tenant_id
    origin
    destination
    distance
    estimated_duration


### Trip

    id
    tenant_id
    bus_id
    route_id
    departure_time
    arrival_time
    price
    status


### Booking

    id
    customer_id
    tenant_id
    trip_id
    status
    total_amount
    created_at
    expires_at


### BookingSeat

    id
    booking_id
    trip_id
    seat_id
    price


### Payment

    id
    booking_id
    customer_id
    amount
    currency
    status
    provider_reference
    created_at
    updated_at


## 11. Important Booking States

Booking states:

    PENDING_PAYMENT
    CONFIRMED
    CANCELLED
    EXPIRED
    PAYMENT_FAILED

Example flow:

    PENDING_PAYMENT
          │
          ├── Payment success → CONFIRMED
          │
          ├── Payment failure → PAYMENT_FAILED
          │
          └── Timeout → EXPIRED

    CONFIRMED
          │
          └── Cancellation → CANCELLED


## 12. Seat Reservation

Seat availability belongs to a specific trip.

A bus has a fixed seat layout, but seat availability changes for every trip.

Example:

    Bus: NB-1234

    Seats:
    A1
    A2
    A3
    ...

Trip 100:

    A1 → BOOKED
    A2 → AVAILABLE

Trip 101:

    A1 → AVAILABLE
    A2 → BOOKED

Do not store permanent availability directly on the Bus entity.


## 13. Concurrent Booking

The system must prevent two customers from successfully booking the same seat for the same trip.

Example:

    Customer A ──┐
                 ├──→ Seat A12
    Customer B ──┘

Only one request may successfully reserve the seat.

Possible implementation techniques:

- PostgreSQL transactions
- Unique constraints
- Pessimistic locking
- Optimistic locking
- Redis temporary reservation

The initial implementation should prefer database-level correctness.

Redis should be introduced for temporary seat holds after the basic booking flow works.


## 14. Temporary Seat Reservation

A selected seat may be temporarily reserved while the customer completes payment.

Example:

    Trip: 100
    Seat: A12
    User: user-123
    TTL: 5 minutes

Redis can store the temporary reservation.

Conceptually:

    trip:100:seat:A12 → user-123

After expiration, the seat becomes available again.

Redis must not be the only source of truth for permanent bookings.


## 15. Payment Flow

Recommended flow:

    Customer
        ↓
    Booking Service
        ↓
    Create PENDING_PAYMENT booking
        ↓
    Payment Service
        ↓
    Payment Provider
        ↓
    Payment Webhook
        ↓
    Payment Service
        ↓
    Publish PaymentCompleted
        ↓
    RabbitMQ
        ↓
    Booking Service
        ↓
    CONFIRMED


Payment webhooks must be idempotent.

The system must handle duplicate webhook deliveries safely.


## 16. Messaging

RabbitMQ will be used for asynchronous communication.

Possible events:

    BookingCreated
    BookingConfirmed
    BookingCancelled
    BookingExpired

    PaymentCompleted
    PaymentFailed

    NotificationRequested


Example:

    Booking Service
          ↓
    BookingConfirmed
          ↓
       RabbitMQ
          ↓
    Notification Service
          ↓
    Email Provider


## 17. Notification

Notifications should not block the booking request.

Bad:

    Create booking
       ↓
    Send email
       ↓
    Return API response

Preferred:

    Create booking
       ↓
    Publish event
       ↓
    Return API response

    RabbitMQ
       ↓
    Notification Service
       ↓
    Send email


## 18. Provider Dashboard

Provider administrators should be able to manage:

- Provider profile
- Buses
- Seats
- Routes
- Trips
- Bookings

Provider dashboard statistics may include:

- Total buses
- Upcoming trips
- Today's bookings
- Occupancy
- Revenue


## 19. Super Admin Dashboard

Super Admin should be able to:

- View providers
- Approve providers
- Suspend providers
- View buses
- View trips
- View bookings
- View platform statistics

Super Admin has cross-tenant access.


## 20. Customer Features

Customers can:

- Register
- Login
- Search trips
- Filter trips
- View trip details
- View available seats
- Select seats
- Reserve seats
- Make payment
- View booking
- View booking history
- Cancel eligible bookings
- Receive email notifications


## 21. API Design Principles

Use RESTful APIs.

Use appropriate HTTP methods:

    GET
    POST
    PUT/PATCH
    DELETE

Use consistent response structures.

Validate all incoming requests.

Use DTOs rather than exposing JPA entities directly through controllers.

Use pagination for large collections.

Example:

    GET /trips?page=0&size=20

Do not expose internal database IDs unnecessarily if a public identifier is more appropriate.


## 22. Security Requirements

- Hash passwords
- Validate JWTs
- Enforce role-based authorization
- Enforce tenant isolation
- Validate request ownership
- Never trust tenant_id from clients
- Never expose passwords
- Never log passwords or tokens
- Validate webhook signatures
- Protect sensitive endpoints
- Apply rate limiting where appropriate


## 23. Error Handling

Use centralized exception handling.

Spring's:

    @ControllerAdvice

can be used to produce consistent error responses.

Example:

    {
      "timestamp": "...",
      "status": 404,
      "error": "NOT_FOUND",
      "message": "Trip not found",
      "path": "/trips/123"
    }


## 24. Testing Strategy

### Unit Tests

Test:

- Services
- Business rules
- Validation
- Security logic

### Integration Tests

Test:

- PostgreSQL
- Redis
- RabbitMQ
- REST APIs

Testcontainers should be used where appropriate.

### Important scenarios

Test concurrent booking attempts.

Example:

    10 users
       ↓
    Same trip
       ↓
    Same seat

Expected:

    Exactly one successful reservation.


## 25. Development Order

Build the project in this order:

### Phase 1 — Authentication

    auth-service
    ├── Registration
    ├── Login
    ├── Password hashing
    ├── JWT
    ├── Refresh tokens
    └── Roles

### Phase 2 — Multi-Tenancy

    provider-service
    ├── Providers
    ├── Provider users
    ├── Tenant isolation
    └── Authorization

### Phase 3 — Bus Management

    Buses
    Seats
    Routes
    Trips

### Phase 4 — Booking

    Trip search
    Seat availability
    Seat reservation
    Booking
    Cancellation

### Phase 5 — Payment

    Payment creation
    Payment status
    Webhooks
    Idempotency

### Phase 6 — Messaging

    RabbitMQ
    Booking events
    Payment events

### Phase 7 — Notifications

    Notification service
    Email delivery

### Phase 8 — Infrastructure

    Docker
    Docker Compose
    API Gateway
    Redis
    Monitoring

### Phase 9 — Optional

    Kubernetes
    Prometheus
    Grafana
    OpenTelemetry
    Google OAuth/OIDC


## 26. Repository Structure

    trip-grid/
    │
    ├── services/
    │   ├── api-gateway/
    │   ├── auth-service/
    │   ├── provider-service/
    │   ├── booking-service/
    │   ├── payment-service/
    │   └── notification-service/
    │
    ├── infrastructure/
    │   ├── docker/
    │   ├── postgres/
    │   ├── rabbitmq/
    │   └── redis/
    │
    ├── docs/
    │   ├── architecture/
    │   ├── api/
    │   └── database/
    │
    ├── .github/
    │   └── workflows/
    │
    ├── context.md
    ├── docker-compose.yml
    ├── pom.xml
    ├── README.md
    └── .gitignore


## 27. Service Internal Structure

Each Spring Boot service should generally follow:

    src/
    ├── main/
    │   ├── java/com/tripgrid/<service>/
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   ├── service/
    │   │   ├── messaging/
    │   │   └── <Service>Application.java
    │   │
    │   └── resources/
    │       └── application.yml
    │
    └── test/


Not every service needs every package.


## 28. Architectural Rules

1. Services own their databases.

2. Services must not directly access another service's database.

3. Controllers must not contain business logic.

4. JPA entities must not be exposed directly as API responses.

5. Use DTOs for API boundaries.

6. Tenant ID must come from authenticated context.

7. Never trust client-provided tenant IDs.

8. Payment-provider-specific logic belongs in payment-service.

9. Notifications should be asynchronous.

10. Booking operations must be transactionally safe.

11. Payment webhooks must be idempotent.

12. Redis must not be the source of truth for permanent bookings.

13. RabbitMQ events should contain only the information consumers need.

14. Do not create microservices without a clear business or technical reason.

15. Prefer simple solutions before introducing distributed complexity.


## 29. Initial Scope

The first working version should NOT include:

- Google OAuth
- Kubernetes
- SMS
- Complex analytics
- Multiple payment providers
- Multiple currencies
- Dynamic pricing
- Route optimization

The first goal is:

    Register
      ↓
    Login
      ↓
    Provider
      ↓
    Bus
      ↓
    Route
      ↓
    Trip
      ↓
    Seat selection
      ↓
    Booking


## 30. Future Features

Potential future additions:

- Google OAuth/OIDC
- Multiple payment providers
- SMS notifications
- Multiple currencies
- Coupon/discount system
- Refunds
- Provider subscription plans
- Advanced analytics
- Custom seat layouts
- Multi-stop routes
- Recurring trips
- Kubernetes deployment
- Distributed tracing
- Prometheus/Grafana monitoring


## 31. Engineering Goal

TripGrid should demonstrate production-oriented backend engineering rather than simply CRUD functionality.

Priority should be given to:

1. Correctness
2. Security
3. Tenant isolation
4. Transactional integrity
5. Concurrency handling
6. Reliable event processing
7. Testability
8. Observability
9. Maintainability
10. Performance

Avoid adding technologies merely to make the technology stack look impressive.