# TripGrid — Provider Service Endpoints & cURL Testing Guide

This document contains cURL commands to test all endpoints in the **Provider Service** (`provider-service`).

Requests can be made directly to the **Provider Service** on port `8083` or through the **API Gateway** on port `8080`.

---

## 1. Pre-seeded Users & Credentials

| Role | Email | Password | Tenant ID | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`SUPER_ADMIN`** | `superadmin@tripgrid.com` | `SuperAdminPassword123!` | *N/A (Global)* | Platform super administrator |
| **`PROVIDER_ADMIN`** | `provider@tripgrid.com` | `ProviderPassword123!` | `tenant-express-lines` | Provider administrator (Express Lines) |
| **`PROVIDER_STAFF`** | `staff@tripgrid.com` | `StaffPassword123!` | `tenant-express-lines` | Provider staff member |
| **`CUSTOMER`** | `customer@tripgrid.com` | `CustomerPassword123!` | *N/A* | Standard platform customer |

---

## 2. Authentication & Token Extraction

Login via `auth-service` / API Gateway to retrieve JWT tokens:

### A. Login as SUPER_ADMIN
```bash
SUPER_ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "superadmin@tripgrid.com",
    "password": "SuperAdminPassword123!"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "SUPER_ADMIN_TOKEN: $SUPER_ADMIN_TOKEN"
```

### B. Login as PROVIDER_ADMIN
```bash
PROVIDER_ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "provider@tripgrid.com",
    "password": "ProviderPassword123!"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "PROVIDER_ADMIN_TOKEN: $PROVIDER_ADMIN_TOKEN"
```

### C. Login as PROVIDER_STAFF
```bash
PROVIDER_STAFF_TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "staff@tripgrid.com",
    "password": "StaffPassword123!"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "PROVIDER_STAFF_TOKEN: $PROVIDER_STAFF_TOKEN"
```

### D. Login as CUSTOMER
```bash
CUSTOMER_TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@tripgrid.com",
    "password": "CustomerPassword123!"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "CUSTOMER_TOKEN: $CUSTOMER_TOKEN"
```

---

## 3. Provider Self-Registration & Onboarding

### 3.1 Self-Service Provider Registration (Onboard New Provider & Link Admin)
Any authenticated user can register a new bus provider company. The user is automatically linked as `PROVIDER_ADMIN` for this provider.
```bash
curl -X POST "http://localhost:8080/api/providers/register" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Southern Express Transport",
    "email": "contact@southernexpress.lk",
    "phoneNumber": "+94778899000",
    "address": "Matara Bus Terminal, Sri Lanka"
  }'
```

---

## 4. Provider Management (SUPER_ADMIN Endpoints)

### 4.1 Create a Provider (Admin Creation)
```bash
curl -X POST "http://localhost:8080/api/providers" \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Super Express Lanka",
    "email": "info@superexpress.lk",
    "phoneNumber": "+94771234567",
    "address": "45 Galle Road, Colombo 03, Sri Lanka"
  }'
```

### 4.2 List All Providers
```bash
curl -X GET "http://localhost:8080/api/providers?page=0&size=10" \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN"
```

### 4.3 Get Provider by ID
```bash
PROVIDER_ID="<REPLACE_WITH_PROVIDER_UUID>"

curl -X GET "http://localhost:8080/api/providers/$PROVIDER_ID" \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN"
```

### 4.4 Update Provider Status (ACTIVE / SUSPENDED / INACTIVE)
```bash
PROVIDER_ID="<REPLACE_WITH_PROVIDER_UUID>"

curl -X PATCH "http://localhost:8080/api/providers/$PROVIDER_ID/status" \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SUSPENDED"
  }'
```

### 4.5 Link User to Provider (SUPER_ADMIN)
```bash
PROVIDER_ID="<REPLACE_WITH_PROVIDER_UUID>"
USER_ID="<REPLACE_WITH_USER_UUID>"

curl -X POST "http://localhost:8080/api/providers/$PROVIDER_ID/users" \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'"$USER_ID"'",
    "role": "PROVIDER_ADMIN"
  }'
```

---

## 5. Provider Self-Management (PROVIDER_ADMIN / STAFF)

### 5.1 Get Current Provider Profile
```bash
curl -X GET "http://localhost:8080/api/providers/me" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 5.2 Update Current Provider Profile (PROVIDER_ADMIN only)
```bash
curl -X PUT "http://localhost:8080/api/providers/me" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Express Lines Lanka Updated",
    "phoneNumber": "+94779876543",
    "address": "120 High Level Road, Maharagama, Sri Lanka"
  }'
```

---

## 6. Provider User & Staff Management (PROVIDER_ADMIN)

### 6.1 List All Staff & Admins for Provider
```bash
curl -X GET "http://localhost:8080/api/providers/me/users" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 6.2 Assign Staff User to Provider
```bash
USER_ID="<REPLACE_WITH_USER_UUID>"

curl -X POST "http://localhost:8080/api/providers/me/users" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'"$USER_ID"'",
    "role": "PROVIDER_STAFF"
  }'
```

### 6.3 Update Provider User Role
```bash
USER_ID="<REPLACE_WITH_USER_UUID>"

curl -X PATCH "http://localhost:8080/api/providers/me/users/$USER_ID/role" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "PROVIDER_ADMIN"
  }'
```

### 6.4 Remove User from Provider
```bash
USER_ID="<REPLACE_WITH_USER_UUID>"

curl -X DELETE "http://localhost:8080/api/providers/me/users/$USER_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

---

## 7. Multiple Buses Management (1 Provider -> Many Buses)

A provider can register and manage multiple buses with different types and seat layouts.

### 7.1 Create Bus #1 (Luxury 40 Seats)
```bash
curl -X POST "http://localhost:8080/api/buses" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "registrationNumber": "WP-NA-1001",
    "name": "Express Coach 01",
    "busType": "LUXURY",
    "seatCount": 40
  }'
```

### 7.2 Create Bus #2 (Super Luxury 44 Seats)
```bash
curl -X POST "http://localhost:8080/api/buses" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "registrationNumber": "WP-NB-2002",
    "name": "Express Coach 02 - Super Luxury",
    "busType": "SUPER_LUXURY",
    "seatCount": 44
  }'
```

### 7.3 Create Bus #3 (Sleeper 32 Seats)
```bash
curl -X POST "http://localhost:8080/api/buses" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "registrationNumber": "WP-NC-3003",
    "name": "Express Night Sleeper",
    "busType": "SLEEPER",
    "seatCount": 32
  }'
```

### 7.4 List All Buses for Current Provider
```bash
curl -X GET "http://localhost:8080/api/buses?page=0&size=10" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 7.5 Get Bus Details by ID
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"

curl -X GET "http://localhost:8080/api/buses/$BUS_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 7.6 Update Bus Details
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"

curl -X PUT "http://localhost:8080/api/buses/$BUS_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Express Coach 01 (Refurbished)",
    "busType": "LUXURY",
    "seatCount": 40,
    "status": "ACTIVE"
  }'
```

### 7.7 Delete Bus
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"

curl -X DELETE "http://localhost:8080/api/buses/$BUS_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

---

## 8. Seat Configuration & Customization

### 8.1 View All Physical Seats for a Bus
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"

curl -X GET "http://localhost:8080/api/buses/$BUS_ID/seats" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 8.2 Get Seat Details
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"
SEAT_ID="<REPLACE_WITH_SEAT_UUID>"

curl -X GET "http://localhost:8080/api/buses/$BUS_ID/seats/$SEAT_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 8.3 Add Custom Seat to Bus
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"

curl -X POST "http://localhost:8080/api/buses/$BUS_ID/seats" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "seatNumber": "VIP1",
    "rowNumber": 12,
    "columnNumber": 1
  }'
```

### 8.4 Enable / Disable a Physical Seat (e.g. Under Maintenance)
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"
SEAT_ID="<REPLACE_WITH_SEAT_UUID>"

curl -X PATCH "http://localhost:8080/api/buses/$BUS_ID/seats/$SEAT_ID/status" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DISABLED"
  }'
```

---

## 9. Multiple Routes Management (1 Provider -> Many Routes)

A provider can operate multiple bus routes.

### 9.1 Create Route #1: Colombo -> Kandy
```bash
curl -X POST "http://localhost:8080/api/routes" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Colombo",
    "destination": "Kandy",
    "distanceKm": 115.5,
    "estimatedDurationMinutes": 210
  }'
```

### 9.2 Create Route #2: Colombo -> Jaffna
```bash
curl -X POST "http://localhost:8080/api/routes" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Colombo",
    "destination": "Jaffna",
    "distanceKm": 395.0,
    "estimatedDurationMinutes": 420
  }'
```

### 9.3 Create Route #3: Colombo -> Galle
```bash
curl -X POST "http://localhost:8080/api/routes" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Colombo",
    "destination": "Galle",
    "distanceKm": 119.0,
    "estimatedDurationMinutes": 120
  }'
```

### 9.4 List All Routes for Current Provider
```bash
curl -X GET "http://localhost:8080/api/routes?page=0&size=10" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 9.5 Get Route by ID
```bash
ROUTE_ID="<REPLACE_WITH_ROUTE_UUID>"

curl -X GET "http://localhost:8080/api/routes/$ROUTE_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 9.6 Update Route
```bash
ROUTE_ID="<REPLACE_WITH_ROUTE_UUID>"

curl -X PUT "http://localhost:8080/api/routes/$ROUTE_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Colombo Fort",
    "destination": "Galle Express",
    "distanceKm": 119.0,
    "estimatedDurationMinutes": 110,
    "status": "ACTIVE"
  }'
```

### 9.7 Delete Route
```bash
ROUTE_ID="<REPLACE_WITH_ROUTE_UUID>"

curl -X DELETE "http://localhost:8080/api/routes/$ROUTE_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

---

## 10. Trip Management (Assign Any Bus to Any Route)

### 10.1 Public Customer Trip Search (No Auth Required)
```bash
# Search by origin and destination
curl -X GET "http://localhost:8080/api/trips/search?origin=Colombo&destination=Kandy"

# Search with filters (departureDate, price range, pagination)
curl -X GET "http://localhost:8080/api/trips/search?origin=Colombo&destination=Kandy&departureDate=2026-09-20&minPrice=1000&maxPrice=3000&page=0&size=10"
```

### 10.2 Create Trip (Assign Bus #1 to Route #1)
```bash
BUS_ID="<REPLACE_WITH_BUS_UUID>"
ROUTE_ID="<REPLACE_WITH_ROUTE_UUID>"

curl -X POST "http://localhost:8080/api/trips" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "busId": "'"$BUS_ID"'",
    "routeId": "'"$ROUTE_ID"'",
    "departureTime": "2026-09-20T08:00:00Z",
    "arrivalTime": "2026-09-20T11:30:00Z",
    "price": 2500.00,
    "currency": "LKR"
  }'
```

### 10.3 List Trips
```bash
curl -X GET "http://localhost:8080/api/trips?page=0&size=10" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 10.4 Get Trip Details
```bash
TRIP_ID="<REPLACE_WITH_TRIP_UUID>"

curl -X GET "http://localhost:8080/api/trips/$TRIP_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

### 10.5 Update Trip Status (SCHEDULED / BOARDING / IN_TRANSIT / COMPLETED / CANCELLED)
```bash
TRIP_ID="<REPLACE_WITH_TRIP_UUID>"

curl -X PATCH "http://localhost:8080/api/trips/$TRIP_ID/status" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "BOARDING"
  }'
```

### 10.6 Cancel / Delete Trip
```bash
TRIP_ID="<REPLACE_WITH_TRIP_UUID>"

curl -X DELETE "http://localhost:8080/api/trips/$TRIP_ID" \
  -H "Authorization: Bearer $PROVIDER_ADMIN_TOKEN"
```

---

## 11. Actuator & Health Checks

```bash
# Service Health
curl -X GET "http://localhost:8083/actuator/health"

# Service Info
curl -X GET "http://localhost:8083/actuator/info"
```
