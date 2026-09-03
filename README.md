# Weather Service

A proof-of-concept REST API for registering weather sensors, recording measurements, querying readings, and calculating statistics.

## Live Application

- API base URL: [https://x7k29q.onrender.com](https://x7k29q.onrender.com)
- Swagger UI: [https://x7k29q.onrender.com/swagger-ui/index.html](https://x7k29q.onrender.com/swagger-ui/index.html)
- OpenAPI document: [https://x7k29q.onrender.com/v3/api-docs](https://x7k29q.onrender.com/v3/api-docs)

The application and PostgreSQL database run on Render's free tier. The web service may take a short time to wake up after a period of inactivity.

## Requirements

- Java 25
- Docker

## Run Locally

Clone the repository and enter the project directory:

```bash
git clone https://github.com/calvin224/x7k29q.git
cd x7k29q
```

Start PostgreSQL with Docker Compose:

```bash
docker compose -f docker/docker-compose.yaml up -d
```

Run the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The local application uses the database defaults defined in `application.yaml`:

```text
host: localhost
port: 5432
database: weather
username: weather
password: weather
```

Local URLs:

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI document: `http://localhost:8080/v3/api-docs`

Stop the local database with:

```bash
docker compose -f docker/docker-compose.yaml down
```

Add `-v` only when you also want to delete the local PostgreSQL volume and its data.

## Deployment

The production application is deployed to Render as a Docker web service. Deployment settings are defined in `render.yaml`:

- The `main` branch is the production branch.
- Render builds the application with `docker/Dockerfile`.
- The Docker build compiles and packages the Java 25 application with Maven.
- The runtime image starts the packaged Spring Boot JAR.
- Render supplies the `PORT` environment variable used by the HTTP server.
- Database connection variables are injected from the Render PostgreSQL service.
- Flyway applies pending database migrations when the application starts.
- Render checks `/v3/api-docs` to determine whether the deployed service is healthy.

### Automatic Deployment After Merging

Every merge into `main` creates a push event and starts the GitHub Actions workflow in `.github/workflows/maven.yml`:

1. The unit-test job runs the unit test suite.
2. The integration-test job runs repository tests against PostgreSQL Testcontainers.
3. After both test jobs pass, Maven runs `clean verify` and generates JaCoCo coverage.
4. SonarQube analyses the build using the repository's `SONAR_TOKEN` secret.
5. Render waits for the GitHub checks to pass because `autoDeployTrigger` is set to `checksPass`.
6. Render builds `docker/Dockerfile`, deploys the new container, and runs the health check.

If a required GitHub Actions check fails, Render does not automatically deploy that revision. No separate deployment command or GitHub Actions deploy secret is needed because Render is connected directly to the GitHub repository.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/sensors` | List registered sensor IDs and names |
| `POST` | `/api/v1/sensors` | Register a named or unnamed sensor |
| `POST` | `/api/v1/sensors/{sensorId}/measurements` | Record a sensor measurement |
| `POST` | `/api/v1/sensors/by-name/{sensorName}/measurements` | Record a measurement using an exact sensor name |
| `POST` | `/api/v1/measurements/query` | Query raw measurements or latest readings |
| `POST` | `/api/v1/statistics/query` | Calculate MIN, MAX, SUM, or AVERAGE |

Swagger groups these endpoints under `Sensors`, `Measurements`, and `Statistics` respectively.

## Sensor Creation

Sensor names are optional. Named sensors must have unique names, while multiple unnamed sensors are allowed.

Creating a sensor with a name that is already registered returns `409 Conflict` with the error code `DUPLICATE_SENSOR_NAME`. The service checks for an existing name before saving, and the PostgreSQL unique constraint remains the final safeguard against concurrent duplicate requests.

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

## Error Responses

API errors use Spring `ProblemDetail` and the `application/problem+json` media type. Responses contain a human-readable explanation and a stable `code` for clients:

```json
{
  "title": "Sensor not found",
  "status": 404,
  "detail": "Sensor 999 does not exist",
  "instance": "/api/v1/measurements/query",
  "code": "SENSOR_NOT_FOUND"
}
```

| Code | Status | Meaning |
| --- | --- | --- |
| `SENSOR_NOT_FOUND` | `404 Not Found` | A specifically requested sensor ID or name does not exist |
| `DUPLICATE_SENSOR_NAME` | `409 Conflict` | A named sensor already exists |
| `INVALID_DATE_RANGE` | `400 Bad Request` | The supplied date range is incomplete or outside the allowed duration |
| `INVALID_SENSOR_SELECTION` | `400 Bad Request` | `allSensors`, sensor IDs, and sensor names form an invalid combination |
| `VALIDATION_FAILED` | `400 Bad Request` | The request body or one of its fields is invalid |

Database, Hibernate, and stack-trace details are not exposed in API responses.

## Tests

Docker must be running because repository integration tests use an isolated PostgreSQL Testcontainer.

```bash
./mvnw clean test
```
