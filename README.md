# TripGrid - Bus Booking Platform

TripGrid is an event-driven microservices-based bus booking platform engineered with Spring Boot, Spring Cloud Gateway, PostgreSQL, Redis, RabbitMQ, and Mailpit.

---

## Architecture Overview

```
                        +----------------------+
                        |   API Gateway (8080) |
                        +----------+-----------+
                                   |
         +-------------------------+-------------------------+
         |                                                   |
+--------v---------+                               +---------v--------+
|   Auth Service   |                               |  Email Service   |
|     (8081)       |                               |      (8082)      |
+---+----+----+----+                               +---------^--------+
    |    |    |                                              |
    |    |    +-------------------- RabbitMQ ----------------+
    |    |               (tripgrid.events exchange)
    |    |          [user.registered, email.verification.requested]
    |    |
    |    +------ Redis (Code Cache & Rate Limiting)
    |
    +----------- PostgreSQL (Users & Refresh Tokens)
```

### Services & Port Allocation

| Component / Service | Type | Port | Description |
|---|---|---|---|
| **`api-gateway`** | Microservice | `8080` | Central entry point, CORS handling & reverse proxy routes |
| **`auth-service`** | Microservice | `8081` | Authentication, JWT issuing, verification & user management |
| **`email-service`** | Microservice | `8082` | Asynchronous RabbitMQ consumer for email notifications |
| **PostgreSQL** | Infrastructure | `5432` | Relational database (`auth_db`) |
| **Redis** | Infrastructure | `6379` | Temporary codes, rate limiting & cache |
| **RabbitMQ** | Infrastructure | `5672` (AMQP), `15672` (UI) | Event message broker |
| **Mailpit** | Infrastructure | `1025` (SMTP), `8025` (UI) | Local email server & web interface |

---