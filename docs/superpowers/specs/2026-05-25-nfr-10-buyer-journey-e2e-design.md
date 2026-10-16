# NFR-10: Buyer Journey E2E Test Suite — Design Spec

## Overview

Playwright E2E test suite covering the full buyer journey: register → search → add to cart → checkout → receive order. Tests mock all API responses via `page.route()` for speed and determinism.

## Files

```
trevo-web/tests/e2e/
├── buyer-journey.spec.js       # Main test file
├── helpers/
│   └── api-mocks.js            # Reusable page.route() interceptors
└── fixtures/
    └── buyer-data.js           # Static mock response payloads
```

## Test Structure

### Golden Path (1 sequential test)

```
describe('Buyer Journey — Golden Path')
  test('Register → Search → Add to Cart → Checkout → View Order')
```

**Steps:**

1. **Register** — Navigate to `/register`, fill buyer form (name, email, password, phone), submit. Mock `POST /api/auth/register/buyer` → 201. Assert redirect to `/login`.
2. **Login** — Fill email/password, submit. Mock `POST /api/auth/login` → 200 with access + refresh tokens. Assert redirect to `/dashboard`.
3. **Search Products** — Navigate to `/products`, type search query. Mock `GET /api/products/search?q=...` → 200 with product list. Assert product cards rendered.
4. **View Product Detail** — Click a product card. Mock `GET /api/products/{id}` → 200 with product + variants. Assert product info, images, variant selector displayed.
5. **Add to Cart** — Select size + color variant, click "Add to Cart". Mock `POST /api/buyer/cart/items` → 200. Assert success feedback.
6. **View Cart** — Navigate to `/cart`. Mock `GET /api/buyer/cart` → 200 with cart grouped by shop. Assert items, quantities, subtotals, grand total.
7. **Checkout** — Click checkout, select Stripe payment method, confirm. Mock `POST /api/buyer/orders/checkout` → 200 with created orders. Assert redirect to order confirmation page.
8. **View Order in History** — Navigate to `/orders`. Mock `GET /api/buyer/orders` → 200 with order list. Assert order appears with status PLACED and correct items.

Each step asserts visible UI state before proceeding.

### Edge Cases (5 independent tests)

```
describe('Buyer Journey — Edge Cases')
  test('Multi-shop cart creates split orders')
  test('Out-of-stock at checkout shows error')
  test('Order cancellation while PLACED')
  test('Checkout with PayPal')
  test('Checkout with MB WAY')
```

All edge cases start with a pre-authenticated buyer (tokens injected into localStorage) to skip register/login.

#### Multi-shop cart creates split orders
- Mock cart with items from 2 different shops
- Mock checkout returning 2 separate orders (one per shop)
- Navigate to `/cart`, assert items grouped under 2 shop headers
- Proceed to checkout, assert success shows 2 orders
- Check order history shows both orders

#### Out-of-stock at checkout
- Mock cart with items, mock checkout returning 400 with stock conflict (affected items list)
- Navigate to `/cart`, proceed to checkout
- Assert error message displayed listing out-of-stock items

#### Order cancellation while PLACED
- Mock order history with a PLACED order
- Navigate to `/orders`, click cancel on the order
- Mock `POST /api/buyer/orders/{id}/cancel` → 200
- Assert order status updates to CANCELLED

#### Checkout with PayPal
- Mock cart and checkout with `paymentMethod: PAYPAL` → 200
- Navigate to `/cart`, proceed to checkout, select PayPal
- Assert PayPal-specific UI and order confirmation

#### Checkout with MB WAY
- Mock cart and checkout with `paymentMethod: MBWAY` → 200
- Navigate to `/cart`, proceed to checkout, select MB WAY
- Assert phone number input appears, fill and submit
- Assert order confirmation

## Mock Helpers (`helpers/api-mocks.js`)

Each function takes `page` (and optional data) and intercepts a specific API route:

| Function | Route | Response |
|----------|-------|----------|
| `mockRegisterBuyer(page)` | `POST /api/auth/register/buyer` | 201 |
| `mockLogin(page, userData)` | `POST /api/auth/login` | 200 + tokens |
| `mockProductSearch(page, products)` | `GET /api/products/search*` | 200 + product list |
| `mockProductDetail(page, product)` | `GET /api/products/:id` | 200 + product |
| `mockAddToCart(page)` | `POST /api/buyer/cart/items` | 200 |
| `mockGetCart(page, cartData)` | `GET /api/buyer/cart` | 200 + cart |
| `mockCheckout(page, orders)` | `POST /api/buyer/orders/checkout` | 200 + orders |
| `mockGetOrders(page, orders)` | `GET /api/buyer/orders*` | 200 + orders |
| `mockCancelOrder(page)` | `POST /api/buyer/orders/:id/cancel` | 200 |
| `mockCheckoutStockError(page, items)` | `POST /api/buyer/orders/checkout` | 400 + conflict |
| `setupAuthenticatedBuyer(page)` | Injects tokens into localStorage + mocks `GET /api/users/me` | Authenticated session |

## Fixtures (`fixtures/buyer-data.js`)

| Export | Description |
|--------|-------------|
| `testBuyer` | User object: id, name, email, phone, role BUYER |
| `testTokens` | `{ accessToken, refreshToken }` (fake JWT strings) |
| `testProducts` | Array of 3 products with variants, images, prices |
| `testProductDetail` | Single product with full variant/image/stock data |
| `testCart` | Cart with items from 1 shop (golden path) |
| `testMultiShopCart` | Cart with items from 2 shops (multi-shop edge case) |
| `testOrders` | Array of order objects with status PLACED, items, totals |
| `testSplitOrders` | 2 orders from different shops (multi-shop edge case) |
| `stockConflictError` | Error response body with affected variant IDs |

## Conventions

- Follow existing Playwright patterns from `password-reset.spec.js` and `auth-i01.spec.js`
- Use `page.route()` for all API mocking
- Use Playwright locators: `getByRole()`, `getByPlaceholder()`, `getByText()`
- Use `Date.now()` for unique test data (emails) in registration
- Use `page.evaluate()` to inject tokens into localStorage for auth shortcuts
- Assert UI state with `toBeVisible()`, `toHaveURL()`, `toHaveText()`

## Out of Scope

- Real backend integration (all API responses mocked)
- Visual regression testing
- Performance/load-time assertions
- Accessibility checks (covered by NFR-07)
- Cross-browser testing (Chromium only, matching current config)
- CI integration (separate task)
