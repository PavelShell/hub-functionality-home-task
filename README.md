# Hub Functionality - Home Task

## Overview

This is a take-home assignment to evaluate your ability to work with the Hub Functionality module architecture. You'll build a simplified parking hub integration that demonstrates real-world skills used in our production system.

**What We're Testing**:
- Spring Boot with Kotlin
- Kafka message consumption
- Database persistence with Flyway migrations
- REST API client integration
- Unit and integration testing
- Code quality and architecture

---

## The Task: Build "SimplePark Hub" Integration

### Business Context

SimplePark is a fictional parking enforcement operator with a REST API for managing parking sessions. Your task is to integrate SimplePark with our parking system by:

1. **Consuming parking events** from Kafka (START/STOP/EXTEND parking session events)
2. **Calling SimplePark API** to register parking sessions in the external system (For local enforcement)
3. **Storing parking data** in PostgreSQL (Update after each event)
4. **Writing unit & integration tests** to verify the integration

---

## Core Requirements

### 1. Database Schema (Flyway Migration)

Create a Flyway migration to add a `simple_park_parking` table:

**Required fields**:
- `id` 
- `internal_parking_id` (varchar, unique, not null) - Our internal parking ID
- `external_parking_id` (varchar, nullable) - SimplePark's parking ID
- `license_plate` (varchar, not null)
- `area_code` (varchar, not null)
- `start_time` (timestamp, not null)
- `end_time` (timestamp, nullable)
- `status` (varchar, not null) - ACTIVE, STOPPED, FAILED
- `created_at` (timestamp, default now())
- `updated_at` (timestamp, default now())

---

### 2. Kafka Consumer

Create a Kafka listener that consumes parking events from topic `parking.events`.

**Message format** (JSON):
```json
{
  "eventType": "PARKING_STARTED",
  "parkingId": "epic-12345",
  "licensePlate": "ABC123",
  "areaCode": "ZONE-A",
  "startTime": "2024-01-15T10:30:00Z",
  "endTime": "2024-01-15T12:30:00Z",
  "priceAmount": 5.50,
  "currency": "EUR"
}
```

**Event types**: `PARKING_STARTED`, `PARKING_EXTENDED`, `PARKING_STOPPED`

**Requirements**:
- Handle all three event types
- Error handling 

**File**: `src/main/kotlin/net/arrive/hometask/listener/ParkingEventListener.kt`

---

### 3. SimplePark REST Client

Create a REST client to call SimplePark API.

**Base URL**: `http://localhost:8090/api/v1` (WireMock in tests)

**Endpoints**:

**Start Parking**: `POST /parking/start`
```json
Request:
{
  "licensePlate": "ABC123",
  "areaCode": "ZONE-A",
  "startTime": "2024-01-15T10:30:00Z",
  "endTime": "2024-01-15T12:30:00Z"
}

Response (200 OK):
{
  "parkingId": "sp-98765",
  "status": "ACTIVE"
}
```

**Extend Parking**: `POST /parking/{parkingId}/extend`
```json
Request:
{
  "newEndTime": "2024-01-15T14:30:00Z"
}

Response (200 OK):
{
  "parkingId": "sp-98765",
  "status": "ACTIVE"
}
```

**Stop Parking**: `POST /parking/{parkingId}/stop`
```json
Request:
{
  "actualEndTime": "2024-01-15T11:45:00Z"
}

Response (200 OK):
{
  "parkingId": "sp-98765",
  "status": "STOPPED"
}
```

**Requirements**:
- Configurable base URL via `application.properties`
- Proper timeout configuration (5 seconds)
- Error handling (log and throw custom exception)
- Add `X-API-Key` header with value from config

**File**: `src/main/kotlin/net/arrive/hometask/client/SimpleParkClient.kt`

---

---

### 4. Testing

**Unit Tests** (Mockito/MockK):
- Test business logic (validation, error handling)

**Integration Tests**:
- Use WireMock for SimplePark API
- Test full flow: Kafka → Service → Database → API
- Test error scenarios (API timeout, invalid data)

---

## Optional Improvements

If you have time over, then pick some to implement:

### Retry Logic with Exponential Backoff

**Requirement**: If SimplePark API call fails (timeout, 5xx error), implement retry logic:
**Optional**: EXTEND/STOP event arrives before START has been successfully processed:

- **Max retries**: 3
- **Backoff strategy**: Exponential (1s, 2s, 4s)
- **Retryable errors**: HTTP 5xx, timeout, connection errors
- **Non-retryable errors**: HTTP 4xx (client errors)

**Verification**: Integration test should verify retry attempts in WireMock.

---

### Idempotency Handling

**Requirement**: Kafka can deliver duplicate messages. Ensure processing is idempotent:

**Challenge**: Same `PARKING_STARTED` event arrives twice
- **Expected**: Only one parking session created
- **Expected**: SimplePark API called only once

**Implementation ideas**:
- Database unique constraint on `internal_parking_id`
- Check for existing parking before processing

**Verification**: Integration test sends same message twice, verifies single database row.

---

### Monitoring 

**Requirement**: Add metrics and logging for production readiness:

**Logging**:
- Use structured logging (SLF4J with MDC)
- Add correlation ID for request tracing
- Log important business events (start/stop parking)

---

### Circuit Breaker Pattern

Implement circuit breaker for SimplePark API using Resilience4j:
- Open circuit after 5 consecutive failures
- Half-open after 30 seconds
- Fallback: Save parking for retry (if implemented)

---

## Deliverables

### Required Files

1. **Source code** in a Git repository with clean commit history
2. **README.md** with:
   - Setup instructions (prerequisites, how to run, if there are any special steps)
   - Architecture decisions (why you chose certain approaches)
   - How to run tests
   - Known limitations or trade-offs
3. **Working tests** 
4. **Database migration** that creates schema
5. **docker-compose.yml** to start dependencies (PostgreSQL, Kafka, WireMock)

### Commit Guidelines

- Use meaningful commit messages
- Commit frequently (we want to see your thought process)
- Don't squash commits (we review commit history)

---

## Getting Started

### Prerequisites

- **Java 21** (Temurin recommended)
- **Docker** and **Docker Compose**
- **Gradle 8.x** (wrapper included)
- **Git**

### Project Setup

1. **Initialize Gradle project**:
```bash
gradle init --type kotlin-application --dsl kotlin --test-framework junit-jupiter \
  --project-name hub-functionality-home-task --package com.arrive.hometask
```

2. **Add dependencies** to `build.gradle.kts` (see provided template)

3. **Start dependencies**:
```bash
docker-compose up -d
```

4. **Run tests**:
```bash
./gradlew test integrationTest
```

---

## Questions?

If anything is unclear or you need clarification, please email us or add a question to your README. We value communication skills!

**Good luck!** 🚀
