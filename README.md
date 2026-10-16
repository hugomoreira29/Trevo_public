# Trevo

A platform where shop owners manage their storefronts and buyers browse products — featuring virtual try-on powered by AI.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, Vite 8, Tailwind CSS |
| Backend | Spring Boot 4, Java 21, Maven |
| Database | PostgreSQL 16 |
| Containers | Docker Compose |
| Image Storage | Cloudinary |
| Payments | Stripe, PayPal, MB WAY |

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Git | >= 2.40 | `git --version` |
| Docker Desktop | >= 4.25 | Includes Docker Compose v2 |
| JDK | 21 LTS | `sdk install java 21-tem` or [Adoptium](https://adoptium.net) |
| Node.js | 20 LTS | `nvm install 20` |
| Maven | >= 3.9 | Or use `./mvnw` (included) |

**OS notes:**
- **macOS:** Install Xcode CLI tools (`xcode-select --install`). Docker Desktop >= 4 GB RAM.
- **Windows:** WSL 2 backend recommended for Docker performance. If using WSL, clone inside the WSL filesystem for faster builds.
- **Linux:** Install Docker Engine + Compose plugin. Add user to `docker` group.

## Getting Started

```bash
# 1. Clone the repository
git clone https://github.com/2HandULP/Trevo.git
cd Trevo

# 2. Set up environment variables
cp .env.example .env
# Edit .env with your credentials (defaults work for local dev)

# 3. Start all services
docker compose up

# 4. Verify everything is running
#    Frontend:  http://localhost:5173
#    Backend:   http://localhost:8080/api/health
#    Database:  localhost:5432
```

If you change `POSTGRES_USER`, `POSTGRES_PASSWORD`, or `POSTGRES_DB` after the
database volume has already been created, Docker will keep the old database
credentials. In that case, recreate the containers and volumes with
`docker compose down -v` before running `docker compose up` again.

## Cloudinary Setup

Trevo uses a shared Cloudinary project environment for image uploads. Each team
member should copy the shared `CLOUDINARY_URL` into their local `.env`, but the
real credential must be stored outside the repository in the team's agreed
secret-sharing location.

1. Create or access the shared Trevo Cloudinary free-tier account.
2. Open the Cloudinary Console and go to the product environment or dashboard.
3. Copy the full `CLOUDINARY_URL` value.
4. Paste it into your local `.env`.
5. Restart Docker Compose or your local backend/frontend processes after
   updating env vars.

Expected value format:

```bash
CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>
```

`ENV-11` only prepares credentials and configuration. Actual upload validation
is deferred to `SHOP-B01` and `SHOP-F05`.

## HTTPS Deployment

Deployment HTTPS is handled by a Caddy reverse proxy in
`docker-compose.https.yml`. Caddy obtains and renews Let's Encrypt certificates
for `PUBLIC_DOMAIN`, redirects HTTP to HTTPS, proxies `/api/*` to the backend,
and proxies all other traffic to the frontend.

```bash
PUBLIC_DOMAIN=trevo.example.com
ACME_EMAIL=admin@example.com
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
```

See `docs/https-deployment.md` for DNS, environment variables, and routing
details.

## Development (without Docker)

If you prefer running services directly on your machine:

### Backend

```bash
cd trevo-api
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

### Frontend

```bash
cd trevo-web
npm install
npm run dev
```

The dev server starts on `http://localhost:5173` with hot reload.

> **Note:** When running without Docker, you need a local PostgreSQL instance running and must update `SPRING_DATASOURCE_URL` in your `.env` to point to `localhost` instead of `db`.

## Running Tests

```bash
# Backend tests
cd trevo-api
./mvnw test

# Frontend tests
cd trevo-web
npm test
```

## Project Structure

```
Trevo/
├── trevo-api/          # Spring Boot backend
│   ├── src/main/       # Application code
│   ├── src/test/       # Tests
│   └── pom.xml         # Maven config
├── trevo-web/          # React frontend
│   ├── src/            # Components, pages, hooks
│   ├── public/         # Static assets
│   └── package.json    # npm config
├── docs/               # Project documentation
│   ├── developmentplan.md
│   ├── requirements.md
│   └── system-architecture.md
├── docker-compose.yml  # Dev environment (3 services)
├── .env.example        # Environment variable template
└── .gitignore
```

## Team

| Name | Role |
|------|------|
| Miguel | Project Manager |
| Sergio | Fullstack Developer |
| Rafael | Frontend Developer |
| Mo | Backend Developer |
| Hugo | Backend Developer |
| Martim | Database Engineer |

## Branch Strategy

- `main` — stable, protected
- `feature/<task-id>` — individual work (e.g., `feature/AUTH-B01`)
- PRs require 1 approval before merge
