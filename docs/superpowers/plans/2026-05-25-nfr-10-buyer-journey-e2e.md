# NFR-10: Buyer Journey E2E Test Suite — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Playwright E2E tests covering the full buyer journey (register → search → add to cart → checkout → view order) with mocked API responses.

**Architecture:** Single spec file with shared mock helpers and fixtures. All backend responses mocked via `page.route()`. Golden path is one sequential test; edge cases are independent tests using a pre-authenticated shortcut.

**Tech Stack:** Playwright 1.57, JavaScript (ESM), existing Vite dev server

---

## File Map

| File | Purpose |
|------|---------|
| `trevo-web/tests/e2e/fixtures/buyer-data.js` | Static mock response payloads for all API endpoints |
| `trevo-web/tests/e2e/helpers/api-mocks.js` | Reusable `page.route()` interceptor functions |
| `trevo-web/tests/e2e/buyer-journey.spec.js` | Golden path + 4 edge case tests |

**Design note — cancel order edge case dropped:** The spec listed "Order cancellation while PLACED" but `OrderHistory.jsx` has no cancel button (the `cancelBuyerOrder` service function exists but is unused in UI). This test is deferred until the cancel UI is built.

**Design note — checkout payment methods:**
- **MB WAY** (golden path): Stays on page, shows success message — simplest to test.
- **PayPal** (edge case): Calls checkout then redirects to `/checkout/paypal/mock`.
- **Stripe** (edge case): Calls checkout, then renders `StripePaymentForm`. Since `loadStripe` won't load in tests (no key), we test only the checkout API call, not the Stripe form.

---

## Task 1: Create Test Fixtures

**Files:**
- Create: `trevo-web/tests/e2e/fixtures/buyer-data.js`

The AuthContext reads JWT claims via `parseJwtClaims()` which decodes the base64url payload segment. The mock token must contain `{"sub":"1","role":"BUYER"}` as a valid JWT payload so the app recognizes the user as an authenticated buyer.

- [ ] **Step 1: Create the fixtures file**

```js
// trevo-web/tests/e2e/fixtures/buyer-data.js

// JWT payload: {"sub":"1","role":"BUYER"}
// AuthContext reads claims.sub (userId) and claims.role from the token.
const BUYER_JWT_PAYLOAD = btoa(JSON.stringify({ sub: "1", role: "BUYER" }));

export const testTokens = {
  accessToken: `eyJhbGciOiJIUzI1NiJ9.${BUYER_JWT_PAYLOAD}.fake-signature`,
  refreshToken: "fake-refresh-token",
};

export const testBuyer = {
  id: 1,
  fullName: "Buyer E2E",
  email: "buyer-e2e@example.com",
  phone: "912345678",
  role: "BUYER",
  status: "ACTIVE",
};

export const testProducts = {
  content: [
    {
      id: 101,
      name: "Vestido Floral",
      brand: "TrevoStyle",
      price: 49.99,
      imageUrl: "https://placehold.co/400x400?text=Vestido",
    },
    {
      id: 102,
      name: "Casaco Inverno",
      brand: "TrevoStyle",
      price: 89.99,
      imageUrl: "https://placehold.co/400x400?text=Casaco",
    },
  ],
  number: 0,
  totalPages: 1,
};

export const testProductDetail = {
  id: 101,
  name: "Vestido Floral",
  brand: "TrevoStyle",
  description: "Um vestido floral elegante para todas as ocasioes.",
  price: 49.99,
  materials: "Algodao 100%",
  shopId: 10,
  images: [
    { id: 1, imageUrl: "https://placehold.co/400x400?text=Vestido1", displayOrder: 0 },
    { id: 2, imageUrl: "https://placehold.co/400x400?text=Vestido2", displayOrder: 1 },
  ],
  variants: [
    { id: 201, size: "M", color: "Vermelho", stock: 5, price: 49.99 },
    { id: 202, size: "L", color: "Vermelho", stock: 3, price: 49.99 },
    { id: 203, size: "M", color: "Azul", stock: 0, price: 49.99 },
  ],
};

export const testCart = {
  shops: [
    {
      shopId: 10,
      shopName: "Loja TrevoStyle",
      subtotal: 49.99,
      items: [
        {
          cartItemId: 1,
          variantId: 201,
          productName: "Vestido Floral",
          size: "M",
          color: "Vermelho",
          quantity: 1,
          unitPrice: 49.99,
          subtotal: 49.99,
          image: "https://placehold.co/400x400?text=Vestido1",
        },
      ],
    },
  ],
  grandTotal: 49.99,
};

export const testMultiShopCart = {
  shops: [
    {
      shopId: 10,
      shopName: "Loja TrevoStyle",
      subtotal: 49.99,
      items: [
        {
          cartItemId: 1,
          variantId: 201,
          productName: "Vestido Floral",
          size: "M",
          color: "Vermelho",
          quantity: 1,
          unitPrice: 49.99,
          subtotal: 49.99,
          image: "https://placehold.co/400x400?text=Vestido1",
        },
      ],
    },
    {
      shopId: 20,
      shopName: "Loja UrbanWear",
      subtotal: 89.99,
      items: [
        {
          cartItemId: 2,
          variantId: 301,
          productName: "Casaco Premium",
          size: "L",
          color: "Preto",
          quantity: 1,
          unitPrice: 89.99,
          subtotal: 89.99,
          image: "https://placehold.co/400x400?text=Casaco",
        },
      ],
    },
  ],
  grandTotal: 139.98,
};

export const testOrders = [
  {
    id: 501,
    shopName: "Loja TrevoStyle",
    status: "PLACED",
    totalAmount: 49.99,
    paymentMethod: "MBWAY",
    createdAt: "2026-05-25T10:00:00Z",
  },
];

export const testOrderDetail = {
  id: 501,
  shopName: "Loja TrevoStyle",
  status: "PLACED",
  totalAmount: 49.99,
  paymentMethod: "MBWAY",
  createdAt: "2026-05-25T10:00:00Z",
  items: [
    {
      id: 1,
      productName: "Vestido Floral",
      size: "M",
      color: "Vermelho",
      quantity: 1,
      unitPrice: 49.99,
      subtotal: 49.99,
    },
  ],
};

export const testSplitOrders = [
  {
    id: 501,
    shopName: "Loja TrevoStyle",
    status: "PLACED",
    totalAmount: 49.99,
    paymentMethod: "MBWAY",
    createdAt: "2026-05-25T10:00:00Z",
  },
  {
    id: 502,
    shopName: "Loja UrbanWear",
    status: "PLACED",
    totalAmount: 89.99,
    paymentMethod: "MBWAY",
    createdAt: "2026-05-25T10:00:00Z",
  },
];

export const testOrdersPage = {
  orders: testOrders,
  totalPages: 1,
};

export const testSplitOrdersPage = {
  orders: testSplitOrders,
  totalPages: 1,
};

export const stockConflictError = {
  message: "Stock insuficiente para alguns itens",
  affectedItems: [{ variantId: 201, productName: "Vestido Floral", available: 0 }],
};
```

- [ ] **Step 2: Verify the file is syntactically valid**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && node -e "import('./tests/e2e/fixtures/buyer-data.js').then(() => console.log('OK'))"`
Expected: `OK`

- [ ] **Step 3: Commit**

```bash
git add trevo-web/tests/e2e/fixtures/buyer-data.js
git commit -m "test(nfr-10): add E2E buyer journey fixtures"
```

---

## Task 2: Create API Mock Helpers

**Files:**
- Create: `trevo-web/tests/e2e/helpers/api-mocks.js`

Key details from the codebase:
- `publicClient` (auth endpoints) and `apiClient` (protected endpoints) both use base URL `http://localhost:8080/api` by default.
- Dashboard's `fetchUserProfile` uses raw `fetch` with path `/api/users/me`. Playwright's `page.route()` intercepts all network requests regardless of client.
- Route patterns use `**/api/...` to match regardless of host.

- [ ] **Step 1: Create the mock helpers file**

```js
// trevo-web/tests/e2e/helpers/api-mocks.js
import { testTokens, testBuyer } from "../fixtures/buyer-data.js";

function json(route, body, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

export async function mockRegisterBuyer(page) {
  await page.route("**/api/auth/register/buyer", (route) => {
    if (route.request().method() === "POST") {
      return json(route, { message: "Conta criada" }, 201);
    }
    return route.continue();
  });
}

export async function mockLogin(page) {
  await page.route("**/api/auth/login", (route) => {
    if (route.request().method() === "POST") {
      return json(route, testTokens);
    }
    return route.continue();
  });
}

export async function mockUserProfile(page, profile = testBuyer) {
  await page.route("**/api/users/me", (route) => {
    if (route.request().method() === "GET") {
      return json(route, profile);
    }
    return route.continue();
  });
}

export async function mockProductSearch(page, results) {
  await page.route("**/api/products/search*", (route) => {
    if (route.request().method() === "GET") {
      return json(route, results);
    }
    return route.continue();
  });
}

export async function mockProductDetail(page, product) {
  await page.route("**/api/products/*", (route) => {
    if (route.request().method() === "GET") {
      return json(route, product);
    }
    return route.continue();
  });
}

export async function mockAddToCart(page) {
  await page.route("**/api/buyer/cart/items", (route) => {
    if (route.request().method() === "POST") {
      return json(route, { message: "Item adicionado" });
    }
    return route.continue();
  });
}

export async function mockGetCart(page, cart) {
  await page.route("**/api/buyer/cart", (route) => {
    if (route.request().method() === "GET") {
      return json(route, cart);
    }
    return route.continue();
  });
}

export async function mockCheckout(page, orders) {
  await page.route("**/api/buyer/orders/checkout", (route) => {
    if (route.request().method() === "POST") {
      return json(route, orders);
    }
    return route.continue();
  });
}

export async function mockCheckoutStockError(page, errorBody) {
  await page.route("**/api/buyer/orders/checkout", (route) => {
    if (route.request().method() === "POST") {
      return json(route, errorBody, 400);
    }
    return route.continue();
  });
}

export async function mockGetOrders(page, ordersPage) {
  await page.route("**/api/buyer/orders?*", (route) => {
    if (route.request().method() === "GET") {
      return json(route, ordersPage);
    }
    return route.continue();
  });
  // Also handle /buyer/orders without query params
  await page.route("**/api/buyer/orders", (route) => {
    const url = route.request().url();
    // Avoid double-matching the checkout route
    if (route.request().method() === "GET" && !url.includes("/checkout")) {
      return json(route, ordersPage);
    }
    return route.continue();
  });
}

export async function mockGetOrderDetail(page, order) {
  await page.route("**/api/buyer/orders/*", (route) => {
    const url = route.request().url();
    if (route.request().method() === "GET" && !url.includes("checkout")) {
      return json(route, order);
    }
    return route.continue();
  });
}

/**
 * Injects auth tokens into localStorage and mocks the /users/me endpoint
 * so the app treats the session as authenticated with BUYER role.
 * Use this as a shortcut in edge-case tests that skip register/login.
 */
export async function setupAuthenticatedBuyer(page) {
  await mockUserProfile(page);

  await page.addInitScript(({ accessToken, refreshToken }) => {
    localStorage.setItem("trevo.accessToken", accessToken);
    localStorage.setItem("trevo.refreshToken", refreshToken);
  }, testTokens);
}
```

- [ ] **Step 2: Verify the file is syntactically valid**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && node -e "import('./tests/e2e/helpers/api-mocks.js').then(() => console.log('OK'))"`
Expected: `OK`

- [ ] **Step 3: Commit**

```bash
git add trevo-web/tests/e2e/helpers/api-mocks.js
git commit -m "test(nfr-10): add E2E API mock helpers"
```

---

## Task 3: Write the Golden Path Test

**Files:**
- Create: `trevo-web/tests/e2e/buyer-journey.spec.js`

This test walks through the full buyer flow sequentially. Each step asserts UI state before proceeding.

**UI labels (Portuguese) from the actual components:**
- Register: placeholders "Nome", "Email", "Palavra-passe", "Telefone (opcional)"; button "Registar"
- Login: placeholders "Email", "Palavra-passe"; button "Entrar"
- Search: aria-label "Pesquisar produtos"; product link "Ver produto"
- Product Detail: size/color buttons by text; "Adicionar ao carrinho"; success "Produto adicionado ao carrinho."
- Cart: heading "Carrinho"; shop names as section headers; "Total geral:"
- Checkout: heading "Checkout"; payment select `#paymentMethod`; MB WAY phone input `#mbWayPhone`; button "Iniciar MB WAY"; success text "Pedido MB WAY iniciado para o número..."
- Order History: heading "Histórico de encomendas"; order buttons "Encomenda n.º 501"

- [ ] **Step 1: Create the spec file with the golden path test**

```js
// trevo-web/tests/e2e/buyer-journey.spec.js
import { expect, test } from "@playwright/test";
import {
  testCart,
  testOrderDetail,
  testOrdersPage,
  testProductDetail,
  testProducts,
  testOrders,
} from "./fixtures/buyer-data.js";
import {
  mockAddToCart,
  mockCheckout,
  mockGetCart,
  mockGetOrderDetail,
  mockGetOrders,
  mockLogin,
  mockProductDetail,
  mockProductSearch,
  mockRegisterBuyer,
  mockUserProfile,
} from "./helpers/api-mocks.js";

test.describe("Buyer Journey — Golden Path", () => {
  test("Register → Search → Add to Cart → Checkout → View Order", async ({ page }) => {
    // --- Register ---
    await mockRegisterBuyer(page);
    await page.goto("/register");
    await page.getByPlaceholder("Nome").fill("Buyer E2E");
    await page.getByPlaceholder("Email").fill(`buyer-${Date.now()}@example.com`);
    await page.getByPlaceholder("Palavra-passe").fill("Secret123!");
    await page.getByPlaceholder("Telefone (opcional)").fill("912345678");
    await page.getByRole("button", { name: "Registar" }).click();

    await expect(page).toHaveURL(/\/login$/);

    // --- Login ---
    await mockLogin(page);
    await mockUserProfile(page);
    await page.getByPlaceholder("Email").fill("buyer-e2e@example.com");
    await page.getByPlaceholder("Palavra-passe").fill("Secret123!");
    await page.getByRole("button", { name: "Entrar" }).click();

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByText("Perfil carregado")).toBeVisible();

    // --- Search Products ---
    await mockProductSearch(page, testProducts);
    await page.goto("/products");
    await page.getByLabel("Pesquisar produtos").fill("vestido");
    await expect(page.getByText("Vestido Floral")).toBeVisible();
    await expect(page.getByText("49,99")).toBeVisible();

    // --- View Product Detail ---
    await mockProductDetail(page, testProductDetail);
    await page.getByRole("link", { name: "Ver produto" }).first().click();

    await expect(page.getByRole("heading", { name: "Vestido Floral" })).toBeVisible();
    await expect(page.getByText("TrevoStyle")).toBeVisible();
    await expect(page.getByText("Algodao 100%")).toBeVisible();

    // --- Select Variant & Add to Cart ---
    await mockAddToCart(page);
    await page.getByRole("button", { name: "M" }).click();
    await page.getByRole("button", { name: "Vermelho" }).click();
    await expect(page.getByText("5 em stock")).toBeVisible();
    await page.getByRole("button", { name: "Adicionar ao carrinho" }).click();
    await expect(page.getByText("Produto adicionado ao carrinho.")).toBeVisible();

    // --- View Cart ---
    await mockGetCart(page, testCart);
    await page.goto("/cart");
    await expect(page.getByRole("heading", { name: "Carrinho" })).toBeVisible();
    await expect(page.getByText("Loja TrevoStyle")).toBeVisible();
    await expect(page.getByText("Vestido Floral")).toBeVisible();
    await expect(page.getByText("Total geral:")).toBeVisible();

    // --- Checkout with MB WAY ---
    await mockGetCart(page, testCart);
    await mockCheckout(page, testOrders);
    await page.goto("/checkout");
    await expect(page.getByRole("heading", { name: "Checkout" })).toBeVisible();
    await expect(page.getByText("Resumo da encomenda")).toBeVisible();

    await page.locator("#paymentMethod").selectOption("MBWAY");
    await page.locator("#mbWayPhone").fill("912345678");
    await page.getByRole("button", { name: "Iniciar MB WAY" }).click();
    await expect(page.getByText("Pedido MB WAY iniciado para o número 912345678")).toBeVisible();

    // --- View Order in History ---
    await mockGetOrders(page, testOrdersPage);
    await mockGetOrderDetail(page, testOrderDetail);
    await page.goto("/orders");
    await expect(page.getByRole("heading", { name: "Histórico de encomendas" })).toBeVisible();
    await expect(page.getByText("Encomenda n.º 501")).toBeVisible();
    await expect(page.getByText("Criada")).toBeVisible();

    // Click order to see detail
    await page.getByText("Encomenda n.º 501").click();
    await expect(page.getByText("Vestido Floral")).toBeVisible();
  });
});
```

- [ ] **Step 2: Run the golden path test**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && npx playwright test buyer-journey --headed 2>&1 | tail -20`
Expected: 1 test passed. If it fails, read the error and fix the locators or mock data.

- [ ] **Step 3: Commit**

```bash
git add trevo-web/tests/e2e/buyer-journey.spec.js
git commit -m "test(nfr-10): add golden path buyer journey E2E test"
```

---

## Task 4: Add Edge Case — Multi-Shop Cart Split Orders

**Files:**
- Modify: `trevo-web/tests/e2e/buyer-journey.spec.js`

Append the edge-case describe block with the first test. Uses `setupAuthenticatedBuyer` to skip login.

- [ ] **Step 1: Add the edge cases describe and multi-shop test**

Update the imports at the top of `buyer-journey.spec.js` to include the new fixtures and helpers:

```js
import {
  testCart,
  testMultiShopCart,
  testOrderDetail,
  testOrdersPage,
  testOrders,
  testProductDetail,
  testProducts,
  testSplitOrders,
  testSplitOrdersPage,
} from "./fixtures/buyer-data.js";
import {
  mockAddToCart,
  mockCheckout,
  mockGetCart,
  mockGetOrderDetail,
  mockGetOrders,
  mockLogin,
  mockProductDetail,
  mockProductSearch,
  mockRegisterBuyer,
  mockUserProfile,
  setupAuthenticatedBuyer,
} from "./helpers/api-mocks.js";
```

Then append after the golden path `describe` block:

```js
test.describe("Buyer Journey — Edge Cases", () => {
  test("Multi-shop cart creates split orders", async ({ page }) => {
    await setupAuthenticatedBuyer(page);
    await mockGetCart(page, testMultiShopCart);
    await mockCheckout(page, testSplitOrders);
    await mockGetOrders(page, testSplitOrdersPage);

    await page.goto("/cart");
    await expect(page.getByText("Loja TrevoStyle")).toBeVisible();
    await expect(page.getByText("Loja UrbanWear")).toBeVisible();
    await expect(page.getByText("Vestido Floral")).toBeVisible();
    await expect(page.getByText("Casaco Premium")).toBeVisible();

    // Checkout with MB WAY
    await mockGetCart(page, testMultiShopCart);
    await page.goto("/checkout");
    await page.locator("#paymentMethod").selectOption("MBWAY");
    await page.locator("#mbWayPhone").fill("912345678");
    await page.getByRole("button", { name: "Iniciar MB WAY" }).click();
    await expect(page.getByText("Pedido MB WAY iniciado")).toBeVisible();

    // Verify both orders in history
    await page.goto("/orders");
    await expect(page.getByText("Encomenda n.º 501")).toBeVisible();
    await expect(page.getByText("Encomenda n.º 502")).toBeVisible();
  });
```

- [ ] **Step 2: Run the new test**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && npx playwright test buyer-journey -g "Multi-shop" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add trevo-web/tests/e2e/buyer-journey.spec.js
git commit -m "test(nfr-10): add multi-shop cart split orders E2E test"
```

---

## Task 5: Add Edge Case — Out-of-Stock at Checkout

**Files:**
- Modify: `trevo-web/tests/e2e/buyer-journey.spec.js`

Add import for `stockConflictError` and `mockCheckoutStockError` at the top, then add the test inside the edge cases describe.

- [ ] **Step 1: Add the out-of-stock test**

Add to imports at top:

```js
import { stockConflictError } from "./fixtures/buyer-data.js";
import { mockCheckoutStockError } from "./helpers/api-mocks.js";
```

Add inside the `"Buyer Journey — Edge Cases"` describe block:

```js
  test("Out-of-stock at checkout shows error", async ({ page }) => {
    await setupAuthenticatedBuyer(page);
    await mockGetCart(page, testCart);
    await mockCheckoutStockError(page, stockConflictError);

    await page.goto("/checkout");
    await expect(page.getByText("Resumo da encomenda")).toBeVisible();

    await page.locator("#paymentMethod").selectOption("MBWAY");
    await page.locator("#mbWayPhone").fill("912345678");
    await page.getByRole("button", { name: "Iniciar MB WAY" }).click();

    await expect(page.getByText("Stock insuficiente")).toBeVisible();
  });
```

- [ ] **Step 2: Run the test**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && npx playwright test buyer-journey -g "Out-of-stock" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add trevo-web/tests/e2e/buyer-journey.spec.js
git commit -m "test(nfr-10): add out-of-stock checkout E2E test"
```

---

## Task 6: Add Edge Case — Checkout with PayPal

**Files:**
- Modify: `trevo-web/tests/e2e/buyer-journey.spec.js`

PayPal checkout calls `checkoutOrder("PAYPAL")` then redirects to `/checkout/paypal/mock` via `redirectTo()`. The test verifies the checkout API is called and the redirect is attempted.

- [ ] **Step 1: Add the PayPal test**

Add inside the `"Buyer Journey — Edge Cases"` describe block:

```js
  test("Checkout with PayPal triggers redirect", async ({ page }) => {
    await setupAuthenticatedBuyer(page);
    await mockGetCart(page, testCart);
    await mockCheckout(page, testOrders);

    await page.goto("/checkout");
    await page.locator("#paymentMethod").selectOption("PAYPAL");
    await expect(page.getByRole("button", { name: "Pagar com PayPal" })).toBeVisible();
    await page.getByRole("button", { name: "Pagar com PayPal" }).click();

    // PayPal redirects to /checkout/paypal/mock
    await expect(page).toHaveURL(/\/checkout\/paypal\/mock/);
  });
```

- [ ] **Step 2: Run the test**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && npx playwright test buyer-journey -g "PayPal" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add trevo-web/tests/e2e/buyer-journey.spec.js
git commit -m "test(nfr-10): add PayPal checkout E2E test"
```

---

## Task 7: Add Edge Case — Checkout with MB WAY

**Files:**
- Modify: `trevo-web/tests/e2e/buyer-journey.spec.js`

MB WAY has phone validation — only accepts Portuguese mobile format (9XXXXXXXX or +3519XXXXXXXX). This test verifies the phone input and success message.

- [ ] **Step 1: Add the MB WAY test**

Add inside the `"Buyer Journey — Edge Cases"` describe block, then close the describe:

```js
  test("Checkout with MB WAY shows phone input and confirms", async ({ page }) => {
    await setupAuthenticatedBuyer(page);
    await mockGetCart(page, testCart);
    await mockCheckout(page, testOrders);

    await page.goto("/checkout");
    await page.locator("#paymentMethod").selectOption("MBWAY");
    await expect(page.locator("#mbWayPhone")).toBeVisible();

    await page.locator("#mbWayPhone").fill("967123456");
    await page.getByRole("button", { name: "Iniciar MB WAY" }).click();

    await expect(page.getByText("Pedido MB WAY iniciado para o número 967123456")).toBeVisible();
  });
});
```

- [ ] **Step 2: Run all tests**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && npx playwright test buyer-journey 2>&1 | tail -20`
Expected: 6 tests passed (1 golden path + 4 edge cases + the describe setup)

Actually expected: 5 tests passed (1 golden path + 4 edge cases).

- [ ] **Step 3: Commit**

```bash
git add trevo-web/tests/e2e/buyer-journey.spec.js
git commit -m "test(nfr-10): add MB WAY checkout E2E test"
```

---

## Task 8: Final Verification & Cleanup

**Files:**
- No new files. Verify everything runs together.

- [ ] **Step 1: Run the full E2E suite**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && npx playwright test buyer-journey --reporter=list 2>&1`
Expected output:
```
  ✓ Buyer Journey — Golden Path > Register → Search → Add to Cart → Checkout → View Order
  ✓ Buyer Journey — Edge Cases > Multi-shop cart creates split orders
  ✓ Buyer Journey — Edge Cases > Out-of-stock at checkout shows error
  ✓ Buyer Journey — Edge Cases > Checkout with PayPal triggers redirect
  ✓ Buyer Journey — Edge Cases > Checkout with MB WAY shows phone input and confirms

  5 passed
```

- [ ] **Step 2: Run existing E2E tests to check for regressions**

Run: `cd /home/dawn/Documents/Trevo/trevo-web && npx playwright test 2>&1 | tail -10`
Expected: All tests pass (buyer-journey + password-reset).

- [ ] **Step 3: Verify the final file structure**

Run: `find /home/dawn/Documents/Trevo/trevo-web/tests/e2e -type f | sort`
Expected:
```
trevo-web/tests/e2e/buyer-journey.spec.js
trevo-web/tests/e2e/fixtures/buyer-data.js
trevo-web/tests/e2e/helpers/api-mocks.js
trevo-web/tests/e2e/password-reset.spec.js
```

- [ ] **Step 4: Final commit if any fixups were needed**

```bash
git add trevo-web/tests/e2e/
git commit -m "test(nfr-10): finalize buyer journey E2E suite"
```
