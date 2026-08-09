# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Running the application

### Docker Compose

The simplest option starts PostgreSQL, the bank simulator, and the payment gateway together:

```shell
docker compose up --build
```

Wait until the `payment_gateway` service has started. The application is then available at
`http://localhost:8090`, and Swagger UI is available at
`http://localhost:8090/swagger-ui/index.html`.

Run Compose in the background by adding `-d`:

```shell
docker compose up --build -d
docker compose logs -f payment_gateway
```

Stop the services while preserving PostgreSQL data:

```shell
docker compose down
```

To stop the services and remove the local database volume:

```shell
docker compose down --volumes
```

### Run the application locally

Start only PostgreSQL and the bank simulator in Docker:

```shell
docker compose up -d postgres bank_simulator
```

Then start Spring Boot from the project root:

```shell
./gradlew bootRun
```

The default local configuration connects to PostgreSQL on port `5432` and the bank simulator on
port `8080`. Stop the Spring Boot process with `Ctrl+C`, then stop its supporting services with:

```shell
docker compose down
```

### Run the tests

Tests use an in-memory H2 database and do not require Docker services:

```shell
./gradlew test
```

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

## Idempotent payment creation

`POST /payments` requires an `Idempotency-Key` header (1–255 characters).

### Behavior

- The first request for a key processes the payment and returns HTTP `201 Created`.
- Repeating the same key with the same request returns the original payment response without
  calling the bank again.
- Reusing the key with a different request returns HTTP `409 Conflict`.
- Missing, blank, or oversized keys return HTTP `400 Bad Request`.

### Persistence and concurrency

Each idempotency key has its own durable row in PostgreSQL. The row contains the key, a SHA-256
request fingerprint, the creation time, and the resulting payment reference. Card and CVV values
are not stored directly in the idempotency record.

The first request creates a pending row. Requests using that key then acquire a pessimistic lock
on that exact row before checking the fingerprint or calling the bank. Consequently:

- concurrent requests with the same key are serialized;
- one request executes the payment while matching requests replay its committed result;
- requests with different keys use different rows and do not block one another;
- coordination works across application restarts and multiple application instances sharing the
  same PostgreSQL database.

The payment and its association with the idempotency record are committed in the same transaction.
If processing fails before that commit, the pending record remains available for a later retry.

> A database transaction cannot atomically commit an external HTTP operation. To prevent a second
> bank operation after a crash occurring between the bank response and the database commit, the
> acquiring bank must also support the same idempotency key.

Example:

```shell
curl -X POST http://localhost:8090/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: merchant-order-123' \
  -d '{"card_number":"2222405343248877","expiry_month":12,"expiry_year":2099,"currency":"USD","amount":100,"cvv":"123"}'
```

**Feel free to change the structure of the solution, use a different library etc.**

## Interview Documentation

### Report 

File that will describe steps done to complete the project
File directory: [interview_report.md](docs/interview_report.md)

### Architecture

Documentation that will describe the architecture and possible steps related to it.

File directory: [interview_report.md](docs/architecture.md)
