# merchant-dashboard-service

Backend service powering the merchant dashboard.

## Getting started

The build needs JDK 25. Gradle downloads it through the Foojay toolchain resolver when the default JDK is older. Docker is only needed for the local Postgres profile.

```shell
./gradlew check
```

`check` runs the tests, Spotless, and the JaCoCo line-coverage gate (90% of hand-written `src/main`; generated OpenAPI code is excluded). Tests use H2 in PostgreSQL mode and do not load demo data.

Local Postgres:

```shell
cp .env.example .env
```

Set `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `JWT_SECRET` in `.env`. The first two are the database login. `JWT_SECRET` is the HMAC signing key and needs at least 32 characters. `.env` is gitignored.

```shell
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
curl -s http://localhost:8080/actuator/health
```

The Compose service is Postgres 17 on host port 5433, database `merchant_dashboard`. The `local` profile reads the username and password from `.env` and points the app at that database. `docker compose down -v` deletes the volume so the next start migrates again.

`POST /api/v1/auth/token` and `/actuator/health` answer without a token. Other routes require a Bearer JWT.

Enable the committed Git hook once:

```shell
git config core.hooksPath .githooks
```

The hook runs `spotlessApply` and stops the commit if formatting changes files.

## License

TBD
