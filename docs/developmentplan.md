# TREVO — Development Plan

> **Methodology:** Test-Driven Development (TDD) — Red → Green → Refactor  
> **Workflow:** Write failing test → Write minimal code to pass → Refactor → Commit  
> **Branch strategy:** `feature/<ID>` branches, PR review required before merge to `develop`

---

## Table of Contents

1. [Team & Roles](#1-team--roles)
2. [Phase 0 — Environment Setup](#2-phase-0--environment-setup)
3. [Phase 1 — Database Foundation](#3-phase-1--database-foundation)
4. [Phase 2 — Auth & Accounts](#4-phase-2--auth--accounts)
5. [Phase 3 — Shop Management](#5-phase-3--shop-management)
6. [Phase 4 — Product Catalog](#6-phase-4--product-catalog)
7. [Phase 5 — Cart & Orders](#7-phase-5--cart--orders)
8. [Phase 6 — Ratings & Reviews](#8-phase-6--ratings--reviews)
9. [Phase 7 — Payments](#9-phase-7--payments)
10. [Phase 8 — AI Virtual Try-On](#10-phase-8--ai-virtual-tryon)
11. [Phase 9 — Polish & Non-Functional Requirements](#11-phase-9--polish--non-functional-requirements)
12. [Dependency Graph Summary](#12-dependency-graph-summary)

---

## 1. Team & Roles

| Name | Role | Primary Responsibilities |
|------|------|--------------------------|
| **Miguel** | Project Manager | Sprint planning, PR reviews, Jira maintenance, integration testing coordination, documentation |
| **Sérgio** | Fullstack Developer | API–UI integration, cross-cutting features (Cloudinary, notifications), E2E tests |
| **Rafael** | Frontend Developer | React components, pages, routing, state management, UI tests |
| **Mo** | Backend Developer | Spring Boot controllers, services, business logic, unit/integration tests |
| **Hugo** | Backend Developer | Spring Boot controllers, services, security layer, unit/integration tests |
| **Martim** | Database Engineer | Schema design, migrations, repository layer, query optimization, data integrity tests |

---

## 2. Phase 0 — Environment Setup

> **Goal:** Every team member can run `docker compose up` and have a fully working dev stack with hot-reload, test runners, and CI passing.

### 0.1 — Machine Prerequisites (All Members)

Each developer installs the following on their local machine:

| Tool | Version | Notes |
|------|---------|-------|
| **Git** | ≥ 2.40 | `git --version` to verify |
| **Docker Desktop** | ≥ 4.25 | Ensure Docker Compose v2 is included (`docker compose version`) |
| **JDK** | 21 (LTS) | Use SDKMAN (`sdk install java 21-tem`) or download from Adoptium |
| **Node.js** | 20 LTS | Use `nvm install 20` or download from nodejs.org |
| **Maven** | ≥ 3.9 | `mvn -version` (or rely on the Maven Wrapper `./mvnw` in the project) |
| **IDE (Backend)** | IntelliJ IDEA (recommended) | Install Lombok plugin; enable annotation processing |
| **IDE (Frontend)** | VS Code (recommended) | Install extensions: ESLint, Prettier, Tailwind CSS IntelliSense |
| **Postman / Bruno** | Latest | For manual API testing |

**OS-specific notes:**

- **macOS:** Install Xcode Command Line Tools (`xcode-select --install`). Docker Desktop allocates ≥4 GB RAM in Settings → Resources.
- **Windows:** Use WSL 2 backend for Docker Desktop. Clone the repo inside the WSL filesystem (`\\wsl$\Ubuntu\home\...`) for performance. Install JDK and Node inside WSL.
- **Linux (Ubuntu/Fedora):** Install Docker Engine + Docker Compose plugin (not Docker Desktop). Add your user to the `docker` group (`sudo usermod -aG docker $USER`).

---

### 0.2 — Repository & Project Scaffolding

| Task ID | Task | Assignee | Dependencies | Details |
|---------|------|----------|--------------|---------|
| **ENV-01** | Create GitHub organization & repository | Miguel | — | Repo `trevo-app`, branch protection on `main` and `develop`, require 1 PR approval |
| **ENV-02** | Configure Jira project with epics matching each Phase | Miguel | — | Epics: Auth, Shop, Product, Cart, Payments, AI, Reviews, NFRs |
| **ENV-03** | Scaffold Spring Boot project via Spring Initializr | Mo | — | Dependencies: Spring Web, Spring Data JPA, Spring Security, PostgreSQL Driver, Lombok, Validation, DevTools. Group: `pt.trevo`, Artifact: `trevo-api`, Java 21, Maven |
| **ENV-04** | Scaffold React + Vite project | Rafael | — | `npm create vite@latest trevo-web -- --template react`, install Tailwind CSS, configure `postcss.config.js` and `tailwind.config.js` |
| **ENV-05** | Create `docker-compose.yml` with three services | Sérgio | ENV-03, ENV-04 | Services: `frontend` (Node 20, port 5173), `backend` (Maven + JDK 21, port 8080), `db` (postgres:16, port 5432). Include volume for DB persistence and `.env` file for credentials |
| **ENV-06** | Add Maven Wrapper to backend | Mo | ENV-03 | `mvn wrapper:wrapper` — ensures everyone uses the same Maven version via `./mvnw` |
| **ENV-07** | Configure backend test framework | Hugo | ENV-03 | Add `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ), `testcontainers-postgresql` for integration tests. Create base test classes: `@SpringBootTest` integration base + unit test base |
| **ENV-08** | Configure frontend test framework | Rafael | ENV-04 | Install Vitest + React Testing Library + jsdom. Add `test` script to `package.json`. Create sample passing test |
| **ENV-09** | Configure CI pipeline (GitHub Actions) | Sérgio | ENV-05, ENV-07, ENV-08 | Workflow: on PR to `develop`/`main` → run backend tests (`./mvnw test`), run frontend tests (`npm test`), lint both. Block merge if tests fail |
| **ENV-10** | Create shared `.env.example` and `README.md` with setup instructions | Miguel | ENV-05 | Document: clone → copy `.env.example` → `docker compose up` → verify all 3 services healthy |
| **ENV-11** | Create Cloudinary account & configure env vars | Sérgio | — | Free-tier account, add `CLOUDINARY_URL` to `.env.example` |
| **ENV-12** | Configure `.gitignore` for both projects | Miguel | ENV-03, ENV-04 | Backend: `/target`, `.idea`, `*.iml`. Frontend: `node_modules`, `dist`. Shared: `.env` |

**Verification checkpoint:** Every team member clones the repo, runs `docker compose up`, and confirms: frontend on `localhost:5173`, backend health endpoint on `localhost:8080/api/health`, database accepting connections on `localhost:5432`.

---

## 3. Phase 1 — Database Foundation

> **Goal:** Schema is designed, versioned via Flyway, and covered by integrity tests. No application code yet — just the data layer.

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **DB-01** | Design ER diagram covering all entities (Users, Shops, Products, Variants, Collections, Cart, Orders, OrderItems, Reviews, Subscriptions, TryOnUsage) | Martim | ENV-10 | N/A (design artifact); reviewed by Mo and Hugo |
| **DB-02** | Configure Flyway for schema migrations | Martim | ENV-03 | Add `flyway-core` to `pom.xml`, create `db/migration` folder, write `V1__create_schema.sql` |
| **DB-03** | Write migration `V1__create_users_table.sql` | Martim | DB-02 | **Test first:** Write Testcontainers integration test asserting table exists with correct columns and constraints after migration runs |
| **DB-04** | Write migration `V2__create_shops_table.sql` | Martim | DB-03 | **Test first:** Assert FK to `users`, unique constraint on `owner_id`, `status` ENUM (`PENDING`, `ACTIVE`, `DEACTIVATED`) |
| **DB-05** | Write migration `V3__create_products_and_variants.sql` | Martim | DB-04 | **Test first:** Assert `products` FK to `shops`, `variants` FK to `products`, unique constraint on `(product_id, size, color)`, stock ≥ 0 check constraint |
| **DB-06** | Write migration `V4__create_collections_and_product_collections.sql` | Martim | DB-05 | **Test first:** Assert many-to-many join table, FK constraints, `display_order` column |
| **DB-07** | Write migration `V5__create_cart_and_cart_items.sql` | Martim | DB-05 | **Test first:** Assert FK to `users` and `variants`, quantity > 0 check |
| **DB-08** | Write migration `V6__create_orders_and_order_items.sql` | Martim | DB-05 | **Test first:** Assert order status ENUM (`PLACED`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `RETURNED`, `CANCELLED`), FKs to `users`, `shops`, `variants` |
| **DB-09** | Write migration `V7__create_reviews.sql` | Martim | DB-04 | **Test first:** Assert unique constraint on `(buyer_id, shop_id)`, rating CHECK 1–5, FK constraints |
| **DB-10** | Write migration `V8__create_subscriptions_and_tryon_usage.sql` | Martim | DB-03 | **Test first:** Assert subscription status ENUM, `tryon_usage` unique on `(buyer_id, product_id)` |
| **DB-14** | Write migration `V9__create_auth_tokens.sql` (`refresh_tokens` + `password_reset_tokens`) | Martim | DB-03 | **Test first:** Assert `refresh_tokens` table exists with columns (`id`, `user_id`, `token`, `revoked`, `expires_at`, `created_at`) and FK to `users`; assert `password_reset_tokens` table exists with columns (`id`, `user_id`, `token`, `used`, `expires_at`, `created_at`) and FK to `users`; assert `revoked` defaults to `false`, `used` defaults to `false` |
| **DB-15** | Write migration `V10__create_triggers.sql` (`update_updated_at()` function + BEFORE UPDATE triggers on users, shops, products, orders, reviews) | Martim | DB-03, DB-04, DB-05, DB-08, DB-09 | **Test first:** Insert a row into each of the 5 tables, then update it, and assert `updated_at` changed automatically |
| **DB-11** | Create JPA entity classes matching all tables | Mo, Hugo | DB-03 through DB-10, DB-14, DB-15 | **Mo:** User, Shop, Product, Variant, Collection. **Hugo:** Cart, CartItem, Order, OrderItem, Review, Subscription, TryOnUsage, RefreshToken, PasswordResetToken. **Test first:** Write unit tests asserting entity field names, annotations, and relationships via reflection. **Note:** Do NOT use `@PreUpdate` for `updated_at` — the database trigger (V10) handles it; mark `updatedAt` as `@Column(insertable = false, updatable = false)` |
| **DB-12** | Create Spring Data JPA repository interfaces | Mo, Hugo | DB-11 | **Mo:** UserRepository, ShopRepository, ProductRepository, VariantRepository, CollectionRepository. **Hugo:** CartRepository, OrderRepository, ReviewRepository, SubscriptionRepository, RefreshTokenRepository, PasswordResetTokenRepository. **Test first:** Write `@DataJpaTest` tests with Testcontainers verifying basic CRUD and custom query methods (include `findByTokenAndRevokedFalse` for refresh tokens, `findByTokenAndUsedFalse` for password reset tokens, `revokeAllByUserId` for session invalidation) |
| **DB-13** | Seed script for development data | Martim | DB-15 | `V999__dev_seed.sql` (only runs in `dev` profile) — 2 buyers, 2 shops, 5 products with variants, sample orders |

> **Convention — `updated_at` handling:** The `V10__create_triggers.sql` migration creates a database-level `BEFORE UPDATE` trigger for `updated_at` on all applicable tables. JPA entities must NOT use `@PreUpdate` for this field. Instead, map it as `@Column(insertable = false, updatable = false)` so Hibernate defers to the database trigger.

---

## 4. Phase 2 — Auth & Accounts

> **Goal:** Users can register (Buyer or Shop Owner), log in, receive JWTs, and access protected endpoints. Covers AUTH-01 through AUTH-10.

### Backend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **AUTH-B01** | Implement `JwtService` (generate, validate, extract claims) | Hugo | DB-11 | **Test first:** Unit test that a generated token contains correct claims (userId, role, expiry). Test expired token throws. Test tampered token throws |
| **AUTH-B02** | Implement `JwtAuthenticationFilter` (extract token from `Authorization` header, set SecurityContext) | Hugo | AUTH-B01 | **Test first:** MockMvc integration test — request with valid token returns 200; missing/invalid token returns 401 |
| **AUTH-B03** | Implement `SecurityConfig` (permit public paths, require auth elsewhere, RBAC roles) | Hugo | AUTH-B02 | **Test first:** Integration tests asserting: `/api/auth/**` is public, `/api/shop/**` requires `ROLE_SHOP_OWNER`, buyer-only endpoints reject shop owners |
| **AUTH-B04** | Implement `AuthService.registerBuyer()` | Mo | DB-12, AUTH-B01 | **Test first:** Unit test — valid input creates user with `BUYER` role and bcrypt-hashed password. Test duplicate email throws `409`. Test missing required fields throws `400` |
| **AUTH-B05** | Implement `AuthService.registerShopOwner()` | Mo | AUTH-B04 | **Test first:** Unit test — valid input with 9-digit NIF creates user with `SHOP_OWNER` role and `PENDING` status. Test invalid NIF format throws `400` |
| **AUTH-B06** | Implement `AuthService.login()` (returns access + refresh tokens) | Mo | AUTH-B01, DB-12 | **Test first:** Unit test — correct credentials return token pair (access + refresh); refresh token persisted to `refresh_tokens` table via `RefreshTokenRepository`. Wrong password throws `401`. Non-existent email throws `401` |
| **AUTH-B07** | Implement `AuthService.refreshToken()` | Mo | AUTH-B06 | **Test first:** Valid refresh token (looked up via `RefreshTokenRepository`, not revoked, not expired) returns new access token. Expired refresh token throws `401`. Revoked refresh token throws `401`. Old refresh token is revoked after use (token rotation) |
| **AUTH-B08** | Implement `AuthController` (POST `/api/auth/register/buyer`, `/register/shop-owner`, `/login`, `/refresh`) | Mo | AUTH-B04 through AUTH-B07 | **Test first:** MockMvc integration tests for each endpoint — happy path + error responses (400, 401, 409) |
| **AUTH-B09** | Implement password reset flow (generate reset token, send email, validate token, update password) | Hugo | AUTH-B04, DB-12 | **Test first:** Unit tests — reset token persisted to `password_reset_tokens` table via `PasswordResetTokenRepository` with `expires_at` = now + 15 min. Validation checks `used = false` and `expires_at > now`. After password update, token marked `used = true`. Expired token throws `400`. Already-used token throws `400`. (Email sending mocked in tests) |
| **AUTH-B10** | Implement `UserService.updateProfile()` | Mo | AUTH-B03 | **Test first:** Unit test — updates name/phone succeed; email change checks uniqueness; password change requires old password verification |
| **AUTH-B11** | Implement session invalidation (revoke all refresh tokens for user) | Hugo | AUTH-B01, DB-12 | **Test first:** After invalidation, all refresh tokens for the user are marked `revoked = true` via `RefreshTokenRepository.revokeAllByUserId()`. Previously valid refresh token returns 401. Access token still works until natural expiry (15 min) |

### Frontend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **AUTH-F01** | Set up React Router with route guards (public vs. authenticated vs. role-based) | Rafael | ENV-04 | **Test first:** Render test — unauthenticated user hitting `/dashboard` is redirected to `/login` |
| **AUTH-F02** | Create `AuthContext` and `useAuth` hook (store tokens, expose login/logout/register functions) | Rafael | AUTH-F01 | **Test first:** Unit test hook — after `login()`, `isAuthenticated` is `true` and token is stored |
| **AUTH-F03** | Create `apiClient` (Axios instance with interceptor to attach JWT + handle 401 refresh) | Sérgio | AUTH-F02 | **Test first:** Mock Axios — interceptor attaches `Authorization` header. On 401, interceptor calls refresh and retries |
| **AUTH-F04** | Build Registration page (Buyer form + Shop Owner form with NIF field) | Rafael | AUTH-F02 | **Test first:** Render test — form validates required fields; NIF field appears only for Shop Owner type; submit calls `authService.register()` |
| **AUTH-F05** | Build Login page | Rafael | AUTH-F02 | **Test first:** Render test — form submits credentials; on success, redirects to home; on error, shows message |
| **AUTH-F06** | Build Profile page (edit name, email, phone, password) | Rafael | AUTH-B10, AUTH-F03 | **Test first:** Render test — form pre-fills current data; submit calls `PUT /api/users/me` |
| **AUTH-F07** | Build Password Reset pages (request + reset form) | Rafael | AUTH-B09 | **Test first:** Render test — request form accepts email; reset form accepts token + new password |

### Integration

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **AUTH-I01** | End-to-end: Register buyer → Login → Access protected route → Logout | Sérgio | AUTH-B08, AUTH-F05 | Manual + automated E2E test (Playwright or Cypress) |
| **AUTH-I02** | End-to-end: Register shop owner → Verify pending status → Approve (admin or auto) | Sérgio | AUTH-B05, AUTH-F04 | Manual + automated E2E test |

---

## 5. Phase 3 — Shop Management

> **Goal:** Shop Owners can create and customize storefronts, manage collections. Covers SHOP-01 through SHOP-08.

### Backend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **SHOP-B01** | Implement `CloudinaryService` (upload image, delete image, return URL) | Sérgio | ENV-11 | **Test first:** Unit test with mocked Cloudinary SDK — upload returns URL; invalid file type throws. Integration test against real Cloudinary (tagged as `@Tag("cloud")`, skipped in CI) |
| **SHOP-B02** | Implement `ShopService.createShop()` | Mo | DB-12, AUTH-B03 | **Test first:** Unit test — creates shop linked to authenticated owner. Test owner already has shop throws `409`. Test non-shop-owner throws `403` |
| **SHOP-B03** | Implement `ShopService.updateShop()` (description, logo, banner via Cloudinary) | Mo | SHOP-B01, SHOP-B02 | **Test first:** Unit test — updates description, uploads new logo/banner. Test non-owner of shop throws `403` |
| **SHOP-B04** | Implement `ShopService.deactivateShop()` / `reactivateShop()` | Mo | SHOP-B02 | **Test first:** Deactivated shop is excluded from public queries. Reactivation makes it visible again |
| **SHOP-B05** | Implement `CollectionService` (create, rename, reorder, delete collections) | Hugo | SHOP-B02 | **Test first:** Unit tests for each operation. Test delete cascades (removes product-collection links). Test reorder updates `display_order` correctly |
| **SHOP-B06** | Implement `ShopController` (CRUD endpoints for shop and collections) | Mo, Hugo | SHOP-B02 through SHOP-B05 | **Test first:** MockMvc tests — RBAC enforced, correct HTTP status codes, response DTOs match spec |
| **SHOP-B07** | Implement public shop profile endpoint `GET /api/shops/{id}` | Hugo | SHOP-B02 | **Test first:** Returns shop info + aggregate rating + collections. Deactivated shops return `404` |

### Frontend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **SHOP-F01** | Build Shop Creation form (name, description, logo upload, banner upload) | Rafael | AUTH-F03, SHOP-B06 | **Test first:** Render test — validates required fields, image preview before upload, submit calls `POST /api/shops` |
| **SHOP-F02** | Build Shop Dashboard page (overview: orders list, revenue summary placeholder) | Rafael | SHOP-B06 | **Test first:** Render test — displays shop info; shows empty state when no orders |
| **SHOP-F03** | Build Collection Manager (create, rename, reorder via drag-and-drop, delete) | Rafael | SHOP-B05, SHOP-B06 | **Test first:** Render test — list renders collections in order; create adds to list; delete removes with confirmation dialog |
| **SHOP-F04** | Build public Shop Profile page (buyer-facing) | Rafael | SHOP-B07 | **Test first:** Render test — displays logo, banner, description, rating, collections; links to product lists |
| **SHOP-F05** | Integrate Cloudinary image upload from frontend (direct upload or via backend proxy) | Sérgio | SHOP-B01 | **Test first:** Upload function returns URL; progress indicator updates during upload |

---

## 6. Phase 4 — Product Catalog

> **Goal:** Shop Owners can manage products and variants. Buyers can browse, search, and filter. Covers PROD-01 through PROD-10.

### Backend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **PROD-B01** | Implement `ProductService.createProduct()` (with variants and image URLs) | Mo | SHOP-B02, SHOP-B01 | **Test first:** Unit test — creates product with ≥1 variant and ≥1 image. Test missing required fields throws `400`. Test non-owner throws `403` |
| **PROD-B02** | Implement `ProductService.updateProduct()` and `archiveProduct()` | Mo | PROD-B01 | **Test first:** Update modifies fields; archive sets status to `ARCHIVED` and hides from buyer queries |
| **PROD-B03** | Implement `VariantService` (add/update/remove variants, update stock) | Hugo | PROD-B01 | **Test first:** Unit test — stock update respects ≥0 constraint. Duplicate `(product_id, size, color)` throws `409`. Bulk stock update modifies multiple variants in one call |
| **PROD-B04** | Implement `ProductService.assignToCollections()` | Mo | SHOP-B05, PROD-B01 | **Test first:** Product appears in assigned collections. Removing from collection doesn't delete the product |
| **PROD-B05** | Implement platform-wide search (`GET /api/products/search?q=&minPrice=&maxPrice=&size=&color=`) | Hugo | PROD-B01 | **Test first:** Integration test (Testcontainers) — keyword matches name/description; price filter bounds work; size/color filters narrow results; only `ACTIVE` products returned; deactivated shops excluded |
| **PROD-B06** | Implement browse-by-shop and browse-by-collection endpoints | Hugo | PROD-B04 | **Test first:** Returns only products for given shop/collection, respects `ACTIVE` status |
| **PROD-B07** | Implement `ProductController` (all product + variant endpoints) | Mo, Hugo | PROD-B01 through PROD-B06 | **Test first:** MockMvc tests — pagination, sorting (date, price, name), correct DTOs |

### Frontend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **PROD-F01** | Build Product Creation/Edit form (fields, variant builder, image upload) | Rafael | PROD-B07, SHOP-F05 | **Test first:** Render test — dynamic variant rows (add/remove), image upload with preview (max 8), collection assignment checkboxes |
| **PROD-F02** | Build Product Detail page (images gallery, variant selector, stock status, brand/materials) | Rafael | PROD-B07 | **Test first:** Render test — image carousel works; selecting size+color shows stock; out-of-stock variants disabled |
| **PROD-F03** | Build Search Results page (search bar, filter sidebar, product grid) | Rafael | PROD-B05 | **Test first:** Render test — filters update URL params; results grid renders product cards; empty state shown when no results |
| **PROD-F04** | Build Shop Products page (products within a shop, collection tabs) | Rafael | PROD-B06, SHOP-F04 | **Test first:** Render test — tabs switch between collections; sorting dropdown works |
| **PROD-F05** | Build Stock Management page (for Shop Owners — bulk edit stock per variant) | Rafael | PROD-B03 | **Test first:** Render test — table of variants with editable stock fields; save sends bulk update |
| **PROD-F06** | Integrate product search with debounced input and URL-based filter state | Sérgio | PROD-F03 | **Test first:** Typing triggers search after 300ms debounce; filters persist in URL; back button restores filters |

---

## 7. Phase 5 — Cart & Orders

> **Goal:** Full checkout flow — cart management, multi-shop order splitting, order lifecycle. Covers CART-01 through CART-11.

### Backend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **CART-B01** | Implement `CartService.addItem()` (validate stock, add variant to cart) | Mo | PROD-B03, DB-12 | **Test first:** Unit test — adds item with valid stock. Test exceeding stock throws `400`. Test adding same variant increments quantity |
| **CART-B02** | Implement `CartService.getCart()` (items grouped by shop, subtotals, grand total) | Mo | CART-B01 | **Test first:** Unit test — groups items by shop; calculates subtotals and total correctly; includes variant details (size, color, product name, image) |
| **CART-B03** | Implement `CartService.updateQuantity()` and `removeItem()` | Mo | CART-B01 | **Test first:** Update validates stock; remove deletes cart item; removing last item from a shop group removes the group |
| **CART-B04** | Implement `OrderService.checkout()` (validate stock again, create one Order per shop, decrement stock, clear cart) | Hugo | CART-B02, PROD-B03 | **Test first:** Unit test — multi-shop cart creates 2 orders. Stock decremented per variant. Cart cleared. Test stock conflict (variant now 0) returns error listing affected items |
| **CART-B05** | Implement `OrderService.updateStatus()` (PLACED→CONFIRMED→SHIPPED→DELIVERED→RETURNED) | Hugo | CART-B04 | **Test first:** Unit test — valid transitions succeed; invalid transitions throw (e.g., PLACED→SHIPPED not allowed). Only shop owner of that order can update |
| **CART-B06** | Implement `OrderService.cancelOrder()` (buyer cancels while `PLACED`) | Hugo | CART-B04 | **Test first:** Cancel while PLACED succeeds and restores stock. Cancel after CONFIRMED throws `400` |
| **CART-B07** | Implement in-app notification for Shop Owners on new order | Sérgio | CART-B04 | **Test first:** After checkout, notification record created for shop owner. Notification endpoint returns unread count and list |
| **CART-B08** | Implement `CartController` and `OrderController` | Mo, Hugo | CART-B01 through CART-B07 | **Test first:** MockMvc tests — RBAC (buyers manage cart, shop owners manage order status), correct status codes |

### Frontend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **CART-F01** | Build Cart page (grouped by shop, quantity controls, subtotals, grand total) | Rafael | CART-B08 | **Test first:** Render test — items grouped under shop headers; +/- buttons call update; remove button calls delete; totals recalculate |
| **CART-F02** | Build Checkout flow (review order → confirm → payment placeholder → success) | Rafael | CART-B08 | **Test first:** Render test — shows order summary; on confirm, calls checkout endpoint; on stock conflict, shows error and removes affected items |
| **CART-F03** | Build Order History page (buyer — list of orders with statuses) | Rafael | CART-B08 | **Test first:** Render test — orders listed with status badges; clicking order shows details (items, tracking number if shipped) |
| **CART-F04** | Build Order Management page (shop owner — incoming orders, status update buttons) | Rafael | CART-B08 | **Test first:** Render test — orders listed per status tab; confirm/ship/deliver buttons call `PUT /api/orders/{id}/status`; ship shows tracking number input |
| **CART-F05** | Build Notification indicator (bell icon with unread count) | Sérgio | CART-B07 | **Test first:** Render test — badge shows count; clicking opens notification list; marking as read decrements count |
| **CART-F06** | Add "Add to Cart" button on Product Detail page (variant must be selected) | Rafael | PROD-F02, CART-B01 | **Test first:** Button disabled until size+color selected; click calls addItem; success shows toast; out-of-stock shows error |

---

## 8. Phase 6 — Ratings & Reviews

> **Goal:** Buyers can rate and review shops. Shop owners can respond. Covers REV-01 through REV-07.

### Backend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **REV-B01** | Implement `ReviewService.createReview()` (1–5 stars + text, one per buyer per shop, requires delivered order) | Hugo | CART-B04, DB-12 | **Test first:** Unit test — review created with valid rating. Test buyer with no delivered order from shop throws `403`. Test duplicate review throws `409` |
| **REV-B02** | Implement `ReviewService.updateReview()` and `deleteReview()` | Hugo | REV-B01 | **Test first:** Only the review author can update/delete. Update changes rating and recalculates shop average |
| **REV-B03** | Implement `ReviewService.respondToReview()` (shop owner public reply) | Hugo | REV-B01 | **Test first:** Only the shop owner of the reviewed shop can respond. One response per review |
| **REV-B04** | Implement basic profanity filter (keyword-based) | Mo | REV-B01 | **Test first:** Review containing blocked words is rejected with `400`. Clean review passes |
| **REV-B05** | Implement `ReviewController` (CRUD + paginated list per shop) | Hugo | REV-B01 through REV-B04 | **Test first:** MockMvc tests — pagination, includes shop owner responses, returns aggregate rating |

### Frontend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **REV-F01** | Build Review form (star rating component + text input) | Rafael | REV-B05 | **Test first:** Render test — stars are clickable; 0 stars not submittable; text is optional; submit calls API |
| **REV-F02** | Build Reviews section on Shop Profile page (paginated list, average rating display) | Rafael | REV-B05, SHOP-F04 | **Test first:** Render test — shows average with star icons; reviews list with buyer name, date, text; pagination loads more |
| **REV-F03** | Build Shop Owner review response UI (inline reply form on each review) | Rafael | REV-B03 | **Test first:** Reply button opens text input; submit calls respond endpoint; response displays under review |

---

## 9. Phase 7 — Payments

> **Goal:** Buyers can pay via Stripe/PayPal/MB WAY. Escrow holds funds until delivery. Covers PAY-01 through PAY-08.

### Backend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **PAY-B01** | Integrate Stripe SDK — create `PaymentService.createPaymentIntent()` | Sérgio | CART-B04 | **Test first:** Unit test (Stripe mocked) — creates PaymentIntent with correct amount in EUR cents. Test failed creation throws and logs |
| **PAY-B02** | Implement Stripe webhook handler (payment success → confirm order) | Sérgio | PAY-B01 | **Test first:** Unit test — valid webhook signature accepted; payment_intent.succeeded updates order to PLACED; invalid signature rejected |
| **PAY-B03** | Implement PayPal integration (create order, capture payment) | Sérgio | CART-B04 | **Test first:** Unit test (PayPal SDK mocked) — create and capture flow succeeds; failed capture throws |
| **PAY-B04** | Implement MB WAY integration | Sérgio | CART-B04 | **Test first:** Unit test — phone number validated; payment request created; callback updates order |
| **PAY-B05** | Implement escrow logic (hold funds until `DELIVERED`, then release minus commission) | Hugo | PAY-B01, CART-B05 | **Test first:** Unit test — payout record created when status → DELIVERED; payout amount = order total × (1 − commission). Payout not created for non-delivered orders |
| **PAY-B06** | Implement `PayoutService` (Shop Owner payout history + pending balance) | Hugo | PAY-B05 | **Test first:** Pending balance = sum of unreleased escrow amounts. Payout history returns released amounts with dates |
| **PAY-B07** | Implement invoice/receipt generation (PDF or structured JSON) | Mo | CART-B04 | **Test first:** Unit test — receipt contains order ID, items, total, date, buyer info. PDF generated without errors |
| **PAY-B08** | Implement `PaymentController` and `PayoutController` | Sérgio | PAY-B01 through PAY-B07 | **Test first:** MockMvc tests — create payment, webhook endpoint, payout history |

### Frontend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **PAY-F01** | Integrate Stripe Elements (card input component) into checkout flow | Rafael | PAY-B01, CART-F02 | **Test first:** Render test — Stripe Elements mounts; form submit creates PaymentIntent; success redirects to confirmation |
| **PAY-F02** | Add PayPal and MB WAY payment options to checkout | Rafael | PAY-B03, PAY-B04 | **Test first:** Render test — payment method selector; PayPal button triggers redirect; MB WAY shows phone input |
| **PAY-F03** | Build Payout Dashboard for Shop Owners (pending balance, payout history) | Rafael | PAY-B06 | **Test first:** Render test — shows balance; table of payouts with date, amount, status |
| **PAY-F04** | Build Receipt/Invoice view (buyer can view and download) | Rafael | PAY-B07 | **Test first:** Render test — receipt shows order details; download button triggers PDF download |

---

## 10. Phase 8 — AI Virtual Try-On

> **Goal:** Buyers can see products on pre-set models or on themselves (Pro). Covers AI-01 through AI-11.

### Backend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **AI-B01** | Implement `SubscriptionService` (create Pro subscription via Stripe recurring, cancel, check status) | Hugo | PAY-B01 | **Test first:** Unit test (Stripe mocked) — creates subscription; `isProUser()` returns true for active subscription; expired subscription returns false |
| **AI-B02** | Implement `TryOnService.generateTryOn()` (call AI image generation API, track usage) | Mo | PROD-B01, AI-B01 | **Test first:** Unit test — free user gets 1 generation per product; second attempt throws `402`. Pro user unlimited. Generated image URL stored |
| **AI-B03** | Implement `TryOnService.generatePersonalizedTryOn()` (Pro only, requires 3 user photos) | Mo | AI-B02, SHOP-B01 | **Test first:** Non-pro user throws `403`. Missing photos throws `400`. 3 photos + product triggers generation |
| **AI-B04** | Implement user photo upload endpoint (exactly 3 photos, stored in Cloudinary) | Sérgio | SHOP-B01 | **Test first:** Upload 3 photos succeeds; <3 or >3 throws `400`; stored URLs linked to buyer account |
| **AI-B05** | Implement try-on image caching (return cached result if already generated for same product + model) | Mo | AI-B02 | **Test first:** Second call for same product+model returns cached URL without calling AI API |
| **AI-B06** | Implement `TryOnController` and `SubscriptionController` | Mo, Hugo | AI-B01 through AI-B05 | **Test first:** MockMvc tests — usage tracking, Pro gating, photo upload validation |

### Frontend

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **AI-F01** | Build "Virtual Try-On" button and modal on Product Detail page | Rafael | AI-B06, PROD-F02 | **Test first:** Render test — button visible on product page; modal opens with model selector (≥4 body types); loading state during generation (up to 30s); result image displayed |
| **AI-F02** | Build Pro subscription page (plan details, subscribe button, manage subscription) | Rafael | AI-B01 | **Test first:** Render test — shows price; subscribe triggers Stripe checkout; active subscription shows cancel option |
| **AI-F03** | Build "See It On Yourself" feature (photo upload + personalized result) | Rafael | AI-B03, AI-B04 | **Test first:** Render test — upload zone accepts exactly 3 images; preview before submit; submit triggers generation; result displayed |
| **AI-F04** | Add Pro badge and usage counter on try-on modal | Sérgio | AI-B02, AI-F01 | **Test first:** Free user sees "1 free try-on" counter; Pro user sees "Unlimited" badge; exhausted free tier shows upgrade prompt |

---

## 11. Phase 9 — Polish & Non-Functional Requirements

> **Goal:** Performance, security hardening, accessibility, and final integration. Covers all NFR-* requirements.

| Task ID | Task | Assignee | Dependencies | TDD Approach |
|---------|------|----------|--------------|--------------|
| **NFR-01** | Add rate limiting to auth endpoints (10 req/min/IP) | Hugo | AUTH-B08 | **Test first:** Integration test — 11th request within 1 minute returns `429` |
| **NFR-02** | Add request logging and monitoring (structured JSON logs via SLF4J/Logback) | Mo | All backend phases | Configuration task; verify logs contain request ID, user ID, duration |
| **NFR-03** | Optimize product search query (add indexes, test p95 < 500ms with 10k products) | Martim | PROD-B05 | **Test first:** Load 10k products into Testcontainers DB; assert search completes under 500ms at p95 |
| **NFR-04** | Ensure all API responses < 300ms p95 for standard CRUD | Martim, Mo | All backend phases | **Test first:** JMeter or Gatling load test script; assert p95 latency |
| **NFR-05** | Async image upload processing (non-blocking Cloudinary calls) | Sérgio | SHOP-B01 | **Test first:** Upload endpoint returns immediately with `202`; image URL available via polling or callback |
| **NFR-06** | HTTPS configuration (TLS certificates for deployment) | Sérgio | — | Configuration task for deployment environment |
| **NFR-07** | Frontend accessibility audit (WCAG 2.1 AA for browse and checkout flows) | Rafael | All frontend phases | Use axe-core in tests; assert 0 critical/serious violations on key pages |
| **NFR-08** | Add alt text to all images (product images, shop logos, banners) | Rafael | PROD-F02, SHOP-F04 | **Test first:** Render test — every `<img>` has non-empty `alt` attribute |
| **NFR-09** | Portuguese localization (all UI text in PT-PT) | Rafael | All frontend phases | Extract all strings to i18n files; verify no hardcoded English text in components |
| **NFR-10** | Final E2E test suite (full buyer journey: register → search → add to cart → checkout → receive order) | Sérgio, Miguel | All phases | Playwright E2E test covering the golden path |
| **NFR-11** | Final E2E test suite (full shop owner journey: register → create shop → add products → receive order → confirm → ship → deliver) | Sérgio, Miguel | All phases | Playwright E2E test covering the golden path |
| **NFR-12** | Deployment configuration (Docker production images, environment-specific config) | Sérgio | All phases | Multi-stage Dockerfiles for frontend (Nginx) and backend (JRE-only). Production `docker-compose.prod.yml` |

---

## 12. Dependency Graph Summary

The following shows the critical path through the project. Each phase depends on the one above it completing its core backend tasks:

```
Phase 0: Environment Setup
    │
    ▼
Phase 1: Database Foundation (schema + entities + repositories)
    │
    ├──────────────────────┐
    ▼                      ▼
Phase 2: Auth & Accounts   (parallel frontend scaffolding)
    │
    ▼
Phase 3: Shop Management
    │
    ▼
Phase 4: Product Catalog
    │
    ├──────────────────────┐
    ▼                      ▼
Phase 5: Cart & Orders     Phase 6: Ratings & Reviews (parallel)
    │
    ▼
Phase 7: Payments
    │
    ▼
Phase 8: AI Virtual Try-On
    │
    ▼
Phase 9: Polish & NFRs
```

**Parallel work opportunities:**

- **Rafael** can start frontend scaffolding (AUTH-F01, AUTH-F02) in parallel with backend auth work.
- **Phase 6 (Reviews)** can run in parallel with Phase 5 (Cart & Orders) since reviews only depend on Phase 2 (Auth) and Phase 3 (Shop) being complete, plus the Order entity from DB-08.
- **Martim** works ahead on schema and indexes while backend devs build services on top.
- **Sérgio** handles cross-cutting concerns (Cloudinary, CI, E2E, payments) across phases.
- **Miguel** continuously manages the Jira board, reviews PRs, writes documentation, and coordinates integration checkpoints between phases.

---

*Document version: 1.0 — Generated for the TREVO project team.*
