## Getting Started

### Prerequisites

- **Java 21** (Temurin recommended)
- **Docker** and **Docker Compose**
- **Gradle 8.x** (wrapper included)

### Setup and Running

1. **Start infrastructure dependencies**:
   Run the following command to start PostgreSQL, Kafka, and WireMock:
   ```bash
   docker-compose up -d
   ```

2. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

3. **Verify locally**:
   You can send a test event to Kafka to see the integration in action:
   ```bash
   echo '{"eventType":"PARKING_STARTED","parkingId":"park-123","licensePlate":"ABC-123","areaCode":"ZONE-1","startTime":"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"}' | docker exec -i hometask-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic parking.events
   ```

### How to run tests

- **Run all tests**:
  ```bash
  ./gradlew check
  ```
- **Run unit tests**:
  ```bash
  ./gradlew test
  ```
- **Run integration tests**:
  ```bash
  ./gradlew integrationTest
  ```

### Architecture decisions

* This implementation marks my first experience with **Kafka** and **Flyway**.
* **Spring Data JPA** was chosen for persistence because of its robust transaction management and seamless integration
  with
  both Spring Boot and Kafka.
* Event handling logic is encapsulated into dedicated handlers managed by the `ParkingEventHandlerFactory`.
* Configuration is strictly from the business logic.

### Known limitations or trade-offs

* **Retry mechanism**: The current implementation is basic and can be further refined for production use.
* **Event ordering**: Handling of out-of-order event consumption is not yet implemented.
* **API Integration**: As documentation for the Simple Park API was unavailable, integration logic is based on inferred
  behavior.
* **Security**: Omitted for the sake of simplicity in this task.
* **Atomicity**: Event processing is not fully transactional; failures occurring after an external API call may lead to
  state inconsistency.
* **Hardcoded values**: Some configuration properties are hardcoded to streamline the initial setup.
