# TREVO — System Architecture

## Overview

TREVO is a second-hand marketplace web application. The system follows a **client-server architecture** with a clear separation between frontend, backend API, and database layers. All components are containerized with Docker for consistent development and deployment.

---

## High-Level Diagram

```
┌─────────────────────┐
│      Frontend        │
│  React + Tailwind    │
│     (Vite)           │
└────────┬────────────┘
         │ HTTP / REST (JSON)
         ▼
┌─────────────────────┐
│      Backend API     │
│   Spring Boot (Java) │
│   Spring Security    │
│       + JWT          │
└────────┬────────────┘
         │ JPA / Hibernate
         ▼
┌─────────────────────┐
│     Database         │
│    PostgreSQL        │
└─────────────────────┘

External:
  └── Cloudinary (image storage)
```

---

## Components

### Frontend — React + Vite + Tailwind CSS

- Single-page application (SPA) served independently from the backend.
- Communicates with the backend exclusively through REST API calls.
- Handles routing, state management, and UI rendering on the client side.
- Stores JWT tokens locally for authenticated requests.

### Backend — Spring Boot

- Exposes a RESTful API consumed by the frontend.
- Follows the **MVC pattern** with a service and repository layer:
  - **Controllers** — handle HTTP requests and responses.
  - **Services** — contain business logic.
  - **Repositories** — interface with the database via Spring Data JPA.
- **Spring Security + JWT** for stateless authentication and authorization.
- Integrates with **Cloudinary** for image upload and retrieval.
- Built and managed with **Maven**.

### Database — PostgreSQL

- Relational database storing all persistent application data (users, listings, messages, etc.).
- Accessed through **Spring Data JPA / Hibernate** — entities map directly to database tables.
- Schema migrations managed manually or through a tool like Flyway (to be decided).

---

## Communication

| From → To             | Protocol       | Format |
|------------------------|----------------|--------|
| Frontend → Backend     | HTTP (REST)    | JSON   |
| Backend → Database     | JDBC (via JPA) | SQL    |
| Backend → Cloudinary   | HTTPS          | Multipart / URL |

- The frontend never talks to the database directly — all data flows through the API.
- Authentication is stateless: the backend issues a JWT on login, and the frontend includes it in the `Authorization` header of subsequent requests.

---

## Docker Setup

Docker Compose orchestrates the local development environment with three services:

| Service    | Image / Build       | Port  |
|------------|---------------------|-------|
| `frontend` | Node / Vite dev     | 5173  |
| `backend`  | Spring Boot (Maven) | 8080  |
| `db`       | postgres:16         | 5432  |

Benefits:
- One command (`docker compose up`) spins up the full stack.
- Consistent environment across all team members.
- Database data persisted via Docker volumes.

---

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| API style | REST | Simple, well-understood, sufficient for CRUD-heavy marketplace operations |
| Auth | JWT (stateless) | No server-side session storage needed; scales easily |
| ORM | Spring Data JPA / Hibernate | Reduces boilerplate; integrates natively with Spring Boot |
| Image storage | Cloudinary | Offloads file storage; provides CDN and transformations out of the box |
| Containerization | Docker Compose | Eliminates "works on my machine" issues across a 6-person team |
