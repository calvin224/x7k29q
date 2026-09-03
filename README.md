# Weather Service

A proof-of-concept REST API for registering weather sensors, recording measurements, querying readings, and calculating statistics.

## Requirements

- Java 25
- Docker

## Run Locally

Start PostgreSQL:

```bash
docker compose -f docker/docker-compose.yaml up -d
```

Start the application:

```bash
./mvnw spring-boot:run
```

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/sensors` | List registered sensor IDs and names |
| `POST` | `/api/v1/sensors` | Register a named or unnamed sensor |
| `POST` | `/api/v1/sensors/{sensorId}/measurements` | Record a sensor measurement |
| `POST` | `/api/v1/sensors/by-name/{sensorName}/measurements` | Record a measurement using an exact sensor name |
| `POST` | `/api/v1/measurements/query` | Query raw measurements or latest readings |
| `POST` | `/api/v1/statistics/query` | Calculate MIN, MAX, SUM, or AVERAGE |

## Sensor Selection

Measurement and statistics queries accept `sensorIds`, `sensorNames`, or both when `allSensors` is `false`. Sensor names are matched exactly and resolved to their generated IDs before querying measurements. Duplicate selections are removed. An unknown sensor name returns `404 Not Found`.

When `allSensors` is `true`, both `sensorIds` and `sensorNames` must be empty. Unnamed sensors can only be selected by ID or through `allSensors`.

For specific-sensor queries, the service validates all distinct requested IDs in one bulk database lookup. If any requested sensor does not exist, the endpoint returns `404 Not Found` without querying measurement data. If every sensor exists but no measurements match the requested metrics and date range, the endpoint returns `200 OK` with an empty array. All-sensor queries do not perform specific-sensor validation and also return an empty array when no data matches.

## Statistics Without Dates

When a statistical query omits a date range, the service uses the latest one-day period of available matching data. The end of the range is the newest measurement timestamp matching the requested sensors and metrics, rather than the current time. This interpretation was chosen because the challenge defines one day as the minimum valid query range while allowing the date range to be omitted in favour of the latest data.

The implicit range is inclusive:

```text
to = latest matching measurement timestamp
from = to - 1 day
```

If no measurements match the selected sensors and metrics, the endpoint returns an empty result list.

## Tests

Docker must be running because repository integration tests use an isolated PostgreSQL Testcontainer.

```bash
./mvnw clean test
```
