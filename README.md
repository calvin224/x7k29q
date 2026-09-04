# Weather Service

A proof-of-concept REST API for registering weather sensors, recording measurements, querying readings, and calculating statistics.

## Live Application

- API base URL: [https://x7k29q.onrender.com](https://x7k29q.onrender.com)
- Swagger UI: [https://x7k29q.onrender.com/swagger-ui/index.html](https://x7k29q.onrender.com/swagger-ui/index.html)
- OpenAPI document: [https://x7k29q.onrender.com/v3/api-docs](https://x7k29q.onrender.com/v3/api-docs)

The application and PostgreSQL database run on Render's free tier. The web service may take a short time to wake up after a period of inactivity.

## Requirements

- Java 25
- Docker with Docker Compose

The Maven wrapper is included, so a separate Maven installation is not required.

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

On Windows PowerShell, use `.\mvnw.cmd spring-boot:run` instead.

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

## OpenHop Architecture Diagram

[OpenHop](https://github.com/naorsabag/openhop) is a local interactive data-flow viewer. It renders the provided `docs/openhop/weather-service-architecture.yaml` file as a diagram that can be played, paused, and stepped through one hop at a time.

OpenHop requires Node.js and `npx`. From the repository root, start the server:

```bash
npx openhop serve
```

In a second terminal, push the provided YAML:

```bash
npx openhop push docs/openhop/weather-service-architecture.yaml --json
```

Open the `url` returned by the command, then use the viewer controls to explore the architecture flow. OpenHop is optional and is not required to run the weather service.

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

### Using Swagger UI and OpenAPI

[OpenAPI](https://www.openapis.org/) is a machine-readable description of the API's endpoints, parameters, request bodies, responses, and status codes. Springdoc generates this document from the application code at runtime. Swagger UI reads the document and provides interactive browser documentation, allowing the API to be explored without installing a separate client.

To call an endpoint through Swagger UI:

1. Start the application locally, or open the live Swagger UI linked above.
2. Expand an endpoint under `Sensors`, `Measurements`, or `Statistics`.
3. Select **Try it out**.
4. Enter any path parameters and edit the example JSON body.
5. Select **Execute** to view the generated `curl` command, request URL, response status, headers, and body.

For example, `POST /api/v1/sensors` accepts a named sensor:

```json
{
  "name": "Dublin City Sensor"
}
```

Send `{}` to create an unnamed sensor. A whitespace-only name such as `{ "name": "   " }` is rejected with `400 Bad Request` and the error code `VALIDATION_FAILED`.

The raw JSON specification is available from `/v3/api-docs`. It can be imported into Postman or Insomnia, or used to generate an API client. To save the local specification:

```bash
curl http://localhost:8080/v3/api-docs --output openapi.json
```

The local application must be running to use these local URLs. The API and its documentation endpoints do not require authentication.

## Sensor Creation

Sensor names are optional. Named sensors must have unique names, while multiple unnamed sensors are allowed.

Creating a sensor with a name that is already registered returns `409 Conflict` with the error code `DUPLICATE_SENSOR_NAME`. The service checks for an existing name before saving, and the PostgreSQL unique constraint remains the final safeguard against concurrent duplicate requests.

## Sensor Selection

Measurement and statistics queries accept `sensorIds`, `sensorNames`, or both when `allSensors` is `false`. Sensor names are matched exactly and resolved to their generated IDs before querying measurements. Duplicate selections are removed. An unknown sensor name returns `404 Not Found`.

When `allSensors` is `true`, both `sensorIds` and `sensorNames` must be empty. Unnamed sensors can only be selected by ID or through `allSensors`.

For specific-sensor queries, the service validates all distinct requested IDs in one bulk database lookup. If any requested sensor does not exist, the endpoint returns `404 Not Found` without querying measurement data. If every sensor exists but no measurements match the requested metrics and date range, the endpoint returns `200 OK` with an empty array. All-sensor queries do not perform specific-sensor validation and also return an empty array when no data matches.

## Measurement and Statistics Query Flow

Both query endpoints use the sensor and metric filters described above. Their date behaviour differs because measurement queries return raw readings, while statistics queries aggregate readings.

### Measurement Queries

`POST /api/v1/measurements/query` follows this flow:

- When both `from` and `to` are supplied, it returns every raw measurement matching the selected sensors, metrics, and inclusive date range.
- When both dates are omitted, it returns only the latest raw measurement for each selected sensor and metric combination. The latest timestamp is resolved independently for each combination.
- When only one date is supplied, it returns `400 Bad Request` with `INVALID_DATE_RANGE`.
- When the selection is valid but no measurements match, it returns `200 OK` with an empty array.

For example, if one sensor has temperature readings `10`, `20`, and `30` at increasing timestamps, a measurement query without dates returns only `30`.

### Statistics Queries

`POST /api/v1/statistics/query` follows this flow:

- When both `from` and `to` are supplied, it calculates the selected `MIN`, `MAX`, `SUM`, or `AVERAGE` independently for each sensor and metric across the inclusive date range.
- When both dates are omitted, it finds the newest timestamp matching the selected sensors and metrics, uses that timestamp as `to`, sets `from` to one day earlier, and calculates the statistic across that inclusive one-day range.
- When only one date is supplied, it returns `400 Bad Request` with `INVALID_DATE_RANGE`.
- When the selection is valid but no measurements match, it returns `200 OK` with an empty array.

The implicit statistics range is:

```text
to = latest matching measurement timestamp
from = to - 1 day
```

The important distinction is that a measurement query without dates returns one latest reading per sensor and metric, whereas a statistics query without dates aggregates all matching readings within the latest one-day window.

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
| `DUPLICATE_MEASUREMENT` | `409 Conflict` | The sensor already has the same metric at the supplied timestamp |
| `INVALID_DATE_RANGE` | `400 Bad Request` | The supplied date range is incomplete or outside the allowed duration |
| `INVALID_SENSOR_SELECTION` | `400 Bad Request` | `allSensors`, sensor IDs, and sensor names form an invalid combination |
| `VALIDATION_FAILED` | `400 Bad Request` | The request body or one of its fields is invalid |

Database, Hibernate, and stack-trace details are not exposed in API responses.

## Tests

Docker must be running because repository integration tests use an isolated PostgreSQL Testcontainer.

```bash
./mvnw clean test
```

On Windows PowerShell, use `.\mvnw.cmd clean test` instead.
