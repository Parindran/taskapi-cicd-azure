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

## CI/CD on Azure (GitHub Actions)

`.github/workflows/azure-deploy.yml` runs three jobs:

1. **Build and Test**: `mvn verify` on every push and pull request.
2. **Docker Build and Push to ACR**: on pushes to `main`, pushes
   `taskapicicdparin.azurecr.io/taskapi:<commit-sha>` and `:latest`.
3. **Deploy to AKS with Ansible**: connects `kubectl` to the AKS cluster and runs
   `ansible/deploy.yml`, which writes the new image into `k8s/deployment.yaml`,
   applies `k8s/`, records the image in the rollout history, waits for the
   rollout, then checks `/actuator/health` on the LoadBalancer IP and prints the URL.

### One-time Azure setup (Azure Cloud Shell, Bash)

```bash
az group create --name taskapi-rg --location centralindia
az aks create --resource-group taskapi-rg --name taskapi-aks --node-count 1 --node-vm-size Standard_B2s --generate-ssh-keys --attach-acr taskapicicdparin
```

If the registry `taskapicicdparin` is in another resource group, `--attach-acr`
still works by name. Give the GitHub app registration (the one behind
`AZURE_CLIENT_ID`) the **Contributor** role on `taskapi-rg`.

### GitHub settings

| Kind | Name | Default in workflow |
|---|---|---|
| Secret | `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID` | (required) |
| Variable | `ACR_NAME` | `taskapicicdparin` |
| Variable | `AKS_RESOURCE_GROUP` | `taskapi-rg` |
| Variable | `AKS_CLUSTER` | `taskapi-aks` |

The deployment runs **one replica** on purpose: tasks are stored in an
in-memory H2 database per pod, so several replicas would each show a different list.

### Versions and rollback

Each deploy creates a new Kubernetes revision (old ones are kept,
`revisionHistoryLimit: 10`). Bump `app.version` in `application.properties` to
see the change on the web page, then:

```bash
kubectl rollout history deployment/taskapi
kubectl rollout undo deployment/taskapi                  # back to the previous version
kubectl rollout undo deployment/taskapi --to-revision=2  # or a specific one
```
