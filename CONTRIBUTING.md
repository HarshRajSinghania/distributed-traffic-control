# Contributing to Distributed Traffic Control

Thank you for considering a contribution to Distributed Traffic Control.

This project focuses on distributed traffic control, Redis-backed coordination, capacity leasing, observability, and production-oriented distributed-systems engineering.

## Before you start

Please read:

- [README](README.md)
- [Benchmark & Validation](docs/benchmark-and-validation.md)
- [AWS Deployment](docs/aws-deployment.md)
- the relevant design documentation under `docs/`

For feature work, check the existing open issues before creating a new one. Prefer contributing to an existing issue so that the scope and acceptance criteria are clear.

## Development setup

### Requirements

- Java 21
- Docker Desktop
- Git

### Run the test suite

Linux/macOS:

```bash
./mvnw -B verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd -B verify
```

The integration suite uses Testcontainers for Redis-backed tests, so Docker must be available.

### Run the distributed local environment

```bash
docker compose build
docker compose up -d
```

Check the services:

## Updating container image versions

The `docker-compose.yml` pins Jaeger, Prometheus, Grafana, and Redis to specific reviewed tags via environment variables (with defaults). This keeps local stacks reproducible.

Default versions (as of the pin commit):

- Redis: `7.4-alpine`
- Jaeger: `2.22.0`
- Prometheus: `v3.13.4` (LTS line)
- Grafana: `13.2.4`

To override temporarily:

```bash
cp .env.example .env
# edit .env
docker compose up -d
```

### Process for updating a pinned version

1. Review the upstream release notes and changelog for breaking changes.
2. Update the default in `docker-compose.yml` and the value in `.env.example`.
3. Run `docker compose pull` and `docker compose up -d` to verify the stack starts and healthchecks pass.
4. Smoke-test tracing (Jaeger UI), metrics (Prometheus targets), and dashboards (Grafana).
5. Document the new versions and any required config adjustments in the PR.
