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

## Frontend

Open http://localhost:8081/ for a small web page that lists, adds, completes and
deletes tasks through the API. The header shows the app version and the host
(the pod name on GKE) that served the page.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/tasks` | Optional `?completed=true|false` filter |
| `GET` | `/api/tasks/{id}` | `404` if not found |
| `POST` | `/api/tasks` | `201` + `Location` header; `400` on a blank title |
| `PUT` | `/api/tasks/{id}` | Replaces title and completed flag |
| `DELETE` | `/api/tasks/{id}` | `204` on success, `404` if not found |
| `GET` | `/api/info` | App name, version and serving host |
| `GET` | `/actuator/health` | Health check (plus `/liveness` and `/readiness` for Kubernetes) |

Three sample tasks are seeded at startup by `DataLoader`.

### Example

```bash
curl -X POST http://localhost:8081/api/tasks -H "Content-Type: application/json" -d '{"title":"Deploy to staging","completed":false}'
```

## Tests

```bash
mvn test
```

Eight MockMvc tests cover context startup, listing, filtering, creation,
validation rejection, `/api/info`, the health endpoint and the frontend page.

## H2 console

Enabled at http://localhost:8081/h2-console — JDBC URL `jdbc:h2:mem:taskdb`,
user `sa`, no password. Data is in memory and resets on every restart.

## CI/CD on Google Cloud (GitHub Actions)

`.github/workflows/gcp-deploy.yml` runs three jobs:

1. **Build and Test** — `mvn verify` on every push and pull request.
2. **Docker Build and Push** — on pushes to `main`, builds the image and pushes it
   to Artifact Registry as `<region>-docker.pkg.dev/<project>/<repo>/taskapi:<sha>` and `:latest`.
3. **Deploy to GKE with Ansible** — connects `kubectl` to the GKE cluster and runs
   `ansible/deploy.yml`, which writes the new image into `k8s/deployment.yaml`,
   applies `k8s/`, waits for the rollout, then checks `/actuator/health` on the
   LoadBalancer IP and prints the app URL.

### One-time GCP setup

```bash
gcloud services enable artifactregistry.googleapis.com container.googleapis.com
gcloud artifacts repositories create taskapi --repository-format=docker --location=asia-south1
gcloud container clusters create-auto taskapi-gke --region=asia-south1
gcloud iam service-accounts create github-deployer
gcloud projects add-iam-policy-binding <PROJECT_ID> --member=serviceAccount:github-deployer@<PROJECT_ID>.iam.gserviceaccount.com --role=roles/artifactregistry.writer
gcloud projects add-iam-policy-binding <PROJECT_ID> --member=serviceAccount:github-deployer@<PROJECT_ID>.iam.gserviceaccount.com --role=roles/container.developer
gcloud iam service-accounts keys create key.json --iam-account=github-deployer@<PROJECT_ID>.iam.gserviceaccount.com
```

### GitHub settings

The values below are already set as defaults in the workflow's `env` block
(project `cia2-513606`). Only the secret is required; adding a repository
variable with the same name under **Settings → Secrets and variables → Actions**
overrides the default.

| Kind | Name | Example |
|---|---|---|
| Variable | `GCP_PROJECT_ID` | `my-project-123456` |
| Variable | `GCP_REGION` | `asia-south1` |
| Variable | `GAR_REPOSITORY` | `taskapi` |
| Variable | `GKE_CLUSTER` | `taskapi-gke` |
| Variable | `GKE_LOCATION` | optional — cluster region/zone, defaults to `GCP_REGION` |
| Secret | `GCP_SA_KEY` | full contents of `key.json` |

The deployment runs **one replica** on purpose: tasks are stored in an
in-memory H2 database per pod, so several replicas would each show a different list.
