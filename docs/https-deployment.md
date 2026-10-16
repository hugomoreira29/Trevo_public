# HTTPS Deployment

NFR-06 is implemented with Caddy as the deployment reverse proxy. Caddy
terminates TLS, obtains and renews Let's Encrypt certificates, redirects HTTP to
HTTPS automatically, and forwards traffic to the existing Docker services.

## Required DNS

Create an `A` or `CNAME` record for the deployment hostname before starting the
stack. The value must resolve to the server running Docker Compose.

## Environment Variables

Add these values to the deployment `.env` file:

```bash
PUBLIC_DOMAIN=trevo.example.com
ACME_EMAIL=admin@example.com
HTTP_PORT=80
HTTPS_PORT=443
```

`PUBLIC_DOMAIN` must be only the hostname, without `https://`.

## Start With HTTPS

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
```

Caddy stores certificates in the `caddy_data` Docker volume so renewals survive
container restarts.

The HTTPS compose override removes the direct frontend, backend, and database
published ports from the base development compose file. In deployment, only
Caddy should publish ports 80 and 443; Postgres must not be reachable from the
internet.

## Network Topology

The override defines two Docker networks:

| Network | Members | Notes |
|---------|---------|-------|
| `frontend-net` | `caddy`, `frontend`, `backend` | Caddy can reach the app services. |
| `backend-net` | `backend`, `db` | Marked `internal: true`. Caddy cannot reach the database. |

The backend sits on both networks so it can accept proxied requests from Caddy
and still reach Postgres.

## Routing

| Request path | Upstream |
|--------------|----------|
| `/api/*` | `backend:8080` |
| all other paths | `frontend:5173` |

The frontend is configured to call the API through same-origin `/api` in the
HTTPS compose override. This avoids mixed-content requests and keeps tokens off
plain HTTP in deployed environments.

## Frontend Image Caveat (`VITE_API_BASE_URL`)

`VITE_API_BASE_URL` is a Vite environment variable. Vite inlines `VITE_*`
values at **build time** (`npm run build`), not at container startup. The
`environment:` block in the HTTPS override therefore only takes effect because
the bundled frontend image (`Dockerfile.dev`) runs `npm run dev`, which reads
the variable from the live process environment.

If you ship a production frontend built with `npm run build` (recommended for
real deployments), the value of `VITE_API_BASE_URL` must be baked into the
image at build time, e.g.:

```dockerfile
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build
```

In that case the override's `environment.VITE_API_BASE_URL: /api` is a no-op
and can be removed — the value is already compiled into the static bundle.
Until a production Dockerfile exists, this HTTPS override is only valid with
the dev-mode frontend image shipped today.

## Local Development

The default `docker-compose.yml` remains HTTP-only for local development. Use
the HTTPS override only in an environment where ports 80 and 443 are reachable
and DNS already points to the host.
