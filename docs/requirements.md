# TREVO — Software Requirements Specification (SRS)

---

## 1. Functional Requirements

### 1.1 Auth & Accounts

| ID | Requirement | Priority |
|----|-------------|----------|
| AUTH-01 | The system shall support two distinct account types: **Buyer** and **Shop Owner**. | P0 |
| AUTH-02 | A single email address may only be associated with one account type. | P0 |
| AUTH-03 | Buyers register with: email, password, full name, and optionally a phone number. | P0 |
| AUTH-04 | Shop Owners register with: email, password, full name, phone number, shop name, and a valid Portuguese company NIF. | P0 |
| AUTH-05 | The system shall validate the NIF format during registration (9-digit Portuguese tax number for companies). | P0 |
| AUTH-06 | Authentication shall use JWT (access + refresh token pair). | P0 |
| AUTH-07 | Users can reset their password via email link. | P0 |
| AUTH-08 | Users can update their profile information (name, email, phone, password). | P1 |
| AUTH-09 | Shop Owner accounts remain in a "pending" state until NIF verification is complete. | P0 |
| AUTH-10 | The system shall support session invalidation (logout from all devices). | P1 |

---

### 1.2 Shop Management

| ID | Requirement | Priority |
|----|-------------|----------|
| SHOP-01 | A Shop Owner can create exactly one storefront linked to their account. | P0 |
| SHOP-02 | The storefront supports customization: logo (image), banner image, and text description. | P0 |
| SHOP-03 | Shop Owners can create, rename, reorder, and delete custom product collections (e.g., "Summer 2026", "Dresses"). | P0 |
| SHOP-04 | There is no global/platform-wide category taxonomy — each shop manages its own collection structure. | P0 |
| SHOP-05 | Shops have a public profile page displaying: logo, banner, description, collections, products, and aggregate rating. | P0 |
| SHOP-06 | Shop Owners can view a dashboard with order history, revenue summary, and stock alerts. | P1 |
| SHOP-07 | Shop Owners can temporarily deactivate their storefront (hides it from search/browse). | P1 |
| SHOP-08 | Images (logo, banner) are uploaded to Cloudinary and stored as URLs. | P0 |

---

### 1.3 Product Catalog

| ID | Requirement | Priority |
|----|-------------|----------|
| PROD-01 | Shop Owners can create products with required fields: name, description, price (EUR), brand, materials, and at least one image. | P0 |
| PROD-02 | Each product has one or more **variants**, defined by a size + color combination. Each variant has its own stock count. | P0 |
| PROD-03 | Shop Owners can upload up to 8 images per product, stored via Cloudinary. | P0 |
| PROD-04 | Products can be assigned to one or more of the shop's custom collections. | P0 |
| PROD-05 | Products have a status: `ACTIVE` or `ARCHIVED`. Only `ACTIVE` products are visible to buyers. | P0 |
| PROD-06 | Buyers can browse products by shop, by collection within a shop, or via a platform-wide search. | P0 |
| PROD-07 | Platform-wide search supports filtering by: price range, size, color, and keyword (name/description). | P0 |
| PROD-08 | Product detail pages show all images, description, available variants (size/color with stock status), price, brand, and materials. | P0 |
| PROD-09 | Shop Owners can bulk-update stock quantities. | P1 |
| PROD-10 | Products within a shop can be sorted by: date added, price, or name. | P1 |

---

### 1.4 Cart & Orders

| ID | Requirement | Priority |
|----|-------------|----------|
| CART-01 | Buyers can add product variants (specific size + color) from any active shop to a single persistent cart. | P0 |
| CART-02 | The cart displays items grouped by shop, with subtotals per shop and a grand total. | P0 |
| CART-03 | Cart quantities are validated against available stock at add-time and again at checkout. | P0 |
| CART-04 | A single checkout produces one **Order** per shop involved (i.e., a multi-shop cart creates multiple sub-orders). | P0 |
| CART-05 | Each sub-order tracks its own lifecycle independently: `PLACED → CONFIRMED → SHIPPED → DELIVERED → RETURNED`. | P0 |
| CART-06 | Shop Owners receive a notification (in-app) when a new order is placed against their shop. | P0 |
| CART-07 | Shop Owners can update an order's status (confirm, mark as shipped with optional tracking number, mark as delivered). | P0 |
| CART-08 | Buyers can view their order history and current order statuses. | P0 |
| CART-09 | If stock for a variant drops to 0 between add-to-cart and checkout, the buyer is notified and the item is removed from the cart. | P0 |
| CART-10 | Shop Owners can mark an order as `RETURNED` after delivery. | P1 |
| CART-11 | Buyers can cancel an order only while it is still in `PLACED` status (not yet confirmed). | P1 |

---

### 1.5 Payments

| ID | Requirement | Priority |
|----|-------------|----------|
| PAY-01 | The platform processes payments via Stripe (card), PayPal, and MB WAY. | P0 |
| PAY-02 | All prices and transactions are in EUR (€). | P0 |
| PAY-03 | TREVO applies a commission fee (percentage TBD) to each sale. | P0 |
| PAY-04 | Funds are held in escrow until the order status reaches `DELIVERED`. | P0 |
| PAY-05 | Once delivery is confirmed, the net amount (sale price minus commission) is released to the Shop Owner. | P0 |
| PAY-06 | Shop Owners can view their payout history and pending balances. | P1 |
| PAY-07 | The system generates an invoice/receipt for each completed transaction (buyer-facing). | P1 |
| PAY-08 | Failed payment attempts are logged and the buyer is prompted to retry or use a different method. | P0 |

---

### 1.6 AI Virtual Try-On

| ID | Requirement | Priority |
|----|-------------|----------|
| AI-01 | Each product detail page offers a "Virtual Try-On" button. | P0 |
| AI-02 | Clicking Virtual Try-On generates an AI image of the product on a pre-set model (varied body types/poses available). | P0 |
| AI-03 | Free-tier buyers get **1 free AI-generated try-on image per product**. | P0 |
| AI-04 | The system tracks try-on usage per buyer per product. | P0 |
| AI-05 | A **Pro subscription** unlocks: unlimited try-on generations per product, plus the "See it on yourself" feature. | P0 |
| AI-06 | "See it on yourself" requires the buyer to upload exactly 3 photos of themselves. | P0 |
| AI-07 | Uploaded user photos are stored via Cloudinary and associated with the buyer's account. | P0 |
| AI-08 | No special data-retention or deletion rules for user-uploaded try-on photos in MVP. | P0 |
| AI-09 | The Pro subscription is a recurring monthly payment processed via Stripe. | P0 |
| AI-10 | Generated try-on images are cached/stored so the same generation isn't re-run if already available. | P1 |
| AI-11 | The system provides a selection of pre-set models (at least 4 distinct body types). | P1 |

---

### 1.7 Ratings & Reviews

| ID | Requirement | Priority |
|----|-------------|----------|
| REV-01 | Buyers can rate a shop on a 1–5 star scale after at least one delivered order from that shop. | P0 |
| REV-02 | Buyers can leave a text review alongside their star rating. | P0 |
| REV-03 | Reviews are tied to the **shop**, not individual products. | P0 |
| REV-04 | Each buyer may submit one review per shop; they can edit or delete it. | P0 |
| REV-05 | The shop's public page displays its average rating and list of reviews (paginated). | P0 |
| REV-06 | Shop Owners can respond publicly to reviews. | P1 |
| REV-07 | Reviews are moderated for profanity/spam (basic keyword filter). | P2 |

---

## 2. Non-Functional Requirements

### 2.1 Performance

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-PERF-01 | API responses for standard CRUD operations shall complete within 300ms (p95). | P0 |
| NFR-PERF-02 | Product search shall return results within 500ms for up to 10,000 products. | P1 |
| NFR-PERF-03 | AI try-on image generation may take up to 30 seconds; the UI shall show a progress/loading state. | P0 |
| NFR-PERF-04 | Image uploads shall be processed asynchronously via Cloudinary to avoid blocking the main thread. | P0 |

### 2.2 Security

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-SEC-01 | All API endpoints require authentication except: login, register, public product/shop browsing, and health check. | P0 |
| NFR-SEC-02 | Passwords shall be stored as bcrypt hashes (cost factor ≥ 12). | P0 |
| NFR-SEC-03 | JWT access tokens expire after 15 minutes; refresh tokens after 7 days. | P0 |
| NFR-SEC-04 | All traffic shall be served over HTTPS. | P0 |
| NFR-SEC-05 | Role-based access control (RBAC): Buyer endpoints are inaccessible to Shop Owners and vice versa where applicable. | P0 |
| NFR-SEC-06 | Payment data is never stored on TREVO servers — all payment processing is delegated to Stripe/PayPal. | P0 |
| NFR-SEC-07 | Rate limiting on authentication endpoints (max 10 attempts per minute per IP). | P1 |

### 2.3 Scalability

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-SCA-01 | The database schema shall support horizontal read scaling via read replicas. | P2 |
| NFR-SCA-02 | Image storage (Cloudinary) is decoupled from the application server. | P0 |
| NFR-SCA-03 | The AI try-on workload should be offloaded to an async job queue to avoid blocking the API. | P1 |

### 2.4 Accessibility

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-ACC-01 | The frontend shall meet WCAG 2.1 Level AA for key user flows (browsing, checkout). | P2 |
| NFR-ACC-02 | All images shall include alt text. | P1 |
| NFR-ACC-03 | The platform's primary language is Portuguese; all UI text shall be in PT-PT. | P0 |

---

## 3. User Stories

### Auth & Accounts

- **US-AUTH-01** (P0): As a **Buyer**, I want to register with my email and password so that I can browse and purchase products.
- **US-AUTH-02** (P0): As a **Shop Owner**, I want to register with my company NIF so that my business is verified before I can sell.
- **US-AUTH-03** (P0): As a **User**, I want to reset my password via email so that I can recover my account if I forget my credentials.
- **US-AUTH-04** (P1): As a **User**, I want to update my profile information so that my account stays current.

### Shop Management

- **US-SHOP-01** (P0): As a **Shop Owner**, I want to create my storefront with a logo, banner, and description so that buyers can discover and recognize my brand.
- **US-SHOP-02** (P0): As a **Shop Owner**, I want to organize my products into custom collections so that buyers can browse by category within my shop.
- **US-SHOP-03** (P1): As a **Shop Owner**, I want to see a dashboard with my orders and revenue so that I can track my business performance.
- **US-SHOP-04** (P1): As a **Shop Owner**, I want to temporarily deactivate my storefront so that I can pause sales when needed (e.g., holidays).

### Product Catalog

- **US-PROD-01** (P0): As a **Shop Owner**, I want to add products with details (name, price, sizes, colors, images) so that buyers can see what I sell.
- **US-PROD-02** (P0): As a **Shop Owner**, I want to manage stock per size-color variant so that inventory is accurate.
- **US-PROD-03** (P0): As a **Buyer**, I want to search for products across all shops so that I can find what I need quickly.
- **US-PROD-04** (P0): As a **Buyer**, I want to filter search results by size, color, and price so that I find relevant items.
- **US-PROD-05** (P0): As a **Buyer**, I want to view a product detail page with images, sizes, and availability so that I can make an informed purchase decision.

### Cart & Orders

- **US-CART-01** (P0): As a **Buyer**, I want to add items from different shops to a single cart so that I can check out once.
- **US-CART-02** (P0): As a **Buyer**, I want to see my cart items grouped by shop with subtotals so that I understand how my order will be split.
- **US-CART-03** (P0): As a **Shop Owner**, I want to be notified of new orders so that I can confirm and ship them promptly.
- **US-CART-04** (P0): As a **Shop Owner**, I want to update order status (confirm, ship, deliver) so that buyers can track progress.
- **US-CART-05** (P0): As a **Buyer**, I want to view my order history and statuses so that I know where my purchases are.
- **US-CART-06** (P1): As a **Buyer**, I want to cancel an order before the shop confirms it so that I have a window to change my mind.

### Payments

- **US-PAY-01** (P0): As a **Buyer**, I want to pay with Stripe, PayPal, or MB WAY so that I can use my preferred payment method.
- **US-PAY-02** (P0): As a **Shop Owner**, I want to receive payouts after delivery confirmation so that I get paid for my sales.
- **US-PAY-03** (P1): As a **Buyer**, I want to receive a receipt after purchase so that I have a record of my transaction.

### AI Virtual Try-On

- **US-AI-01** (P0): As a **Buyer**, I want to see a product on a pre-set model so that I can visualize how it looks on a person.
- **US-AI-02** (P0): As a **Buyer**, I want one free try-on per product so that I can test the feature before subscribing.
- **US-AI-03** (P0): As a **Pro subscriber**, I want unlimited try-ons and the ability to upload my own photos so that I can see how products look on me.
- **US-AI-04** (P0): As a **Buyer**, I want to subscribe to Pro monthly so that I get premium try-on features.

### Ratings & Reviews

- **US-REV-01** (P0): As a **Buyer**, I want to rate and review a shop after a delivered order so that I can share my experience.
- **US-REV-02** (P0): As a **Buyer**, I want to see shop ratings and reviews so that I can choose trustworthy sellers.
- **US-REV-03** (P1): As a **Shop Owner**, I want to respond to reviews so that I can address feedback publicly.

---

