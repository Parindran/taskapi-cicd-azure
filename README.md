# taskapi

A small Spring Boot 3.5.16 REST API for managing tasks, built with Maven on Java 21.

## Dependencies

| Dependency | Why |
|---|---|
| `spring-boot-starter-web` | REST controllers + embedded Tomcat |
| `spring-boot-starter-data-jpa` | Repository layer over Hibernate |
| `spring-boot-starter-validation` | `@NotBlank` / `@Size` checks on request bodies |
| `h2` (runtime) | In-memory database, so the app runs with no setup |

## Running

Maven is not on this machine's PATH, but a full distribution is cached under
`~/.m2/wrapper/dists`. Add it to PATH for the session:

```bash
export PATH="$HOME/.m2/wrapper/dists/apache-maven-3.9.16/0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0/bin:$PATH"
```

Then build and run:

```bash
mvn clean package && java -jar target/taskapi-0.0.1-SNAPSHOT.jar
```

The app listens on **port 8081** (8080 is taken by a local Jenkins instance).
Change it in `src/main/resources/application.properties` or pass
`--server.port=9090` on the command line.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/tasks` | Optional `?completed=true|false` filter |
| `GET` | `/api/tasks/{id}` | `404` if not found |
| `POST` | `/api/tasks` | `201` + `Location` header; `400` on a blank title |
| `PUT` | `/api/tasks/{id}` | Replaces title and completed flag |
| `DELETE` | `/api/tasks/{id}` | `204` on success, `404` if not found |

Three sample tasks are seeded at startup by `DataLoader`.

### Example

```bash
curl -X POST http://localhost:8081/api/tasks -H "Content-Type: application/json" -d '{"title":"Deploy to staging","completed":false}'
```

## Tests

```bash
mvn test
```

Five MockMvc tests cover context startup, listing, filtering, creation, and
validation rejection.

## H2 console

Enabled at http://localhost:8081/h2-console — JDBC URL `jdbc:h2:mem:taskdb`,
user `sa`, no password. Data is in memory and resets on every restart.
