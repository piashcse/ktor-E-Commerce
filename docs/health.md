# Health Checks & Probes

Production liveness, readiness, and monitoring probes for container orchestrators (Kubernetes, Docker, Cloud Run) and uptime monitoring.

---

## Overview

Ktor-E-Commerce provides dedicated health check endpoints outside the `/api/v1` version prefix. These are designed for load balancers, orchestrator readiness probes, and synthetic monitoring without incurring authentication overhead.

---

## Health Check Endpoints

| Method | Endpoint | Purpose | Upstream Checks | Typical HTTP Status |
|:---|:---|:---|:---|:---|
| <span class="method-badge get">GET</span> | `/health` | Basic application ping | Application runtime | `200 OK` |
| <span class="method-badge get">GET</span> | `/health/live` | Kubernetes Liveness probe | Application process active | `200 OK` |
| <span class="method-badge get">GET</span> | `/health/ready` | Kubernetes Readiness probe | Database connection pool (`SELECT 1`) | `200 OK` or `503 Service Unavailable` |

---

## Endpoint Details

### 1. Application Liveness

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/health/live</span>
  <span class="auth-badge public">Public / No Auth Required</span>
</div>

Verifies the JVM process is accepting HTTP traffic. Used by orchestrators to determine whether the container needs a restart.

#### Request

```bash
curl -i http://localhost:8080/health/live
```

#### Response

=== "200 OK"
    ```json
    {
      "message": "UP"
    }
    ```

---

### 2. Database Readiness Probe

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/health/ready</span>
  <span class="auth-badge public">Public / No Auth Required</span>
</div>

Executes an active test query (`SELECT 1`) against the PostgreSQL database connection pool with a **3,000 ms timeout**. If the database is unreachable or connection pools are exhausted, the endpoint responds with `503 Service Unavailable` so the load balancer temporarily diverts traffic.

#### Request

```bash
curl -i http://localhost:8080/health/ready
```

#### Responses

=== "200 OK (Healthy)"
    ```json
    {
      "message": "READY"
    }
    ```

=== "503 Service Unavailable (Database Down)"
    ```json
    {
      "message": "Database unreachable",
      "code": "SERVICE_UNAVAILABLE",
      "status": 503,
      "requestId": "req-9c8f61aa-7023"
    }
    ```

---

## Kubernetes Probe Configuration Example

```yaml
livenessProbe:
  httpGet:
    path: /health/live
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 15
  timeoutSeconds: 3

readinessProbe:
  httpGet:
    path: /health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3
```
