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
    const url = route.request().url();
    if (route.request().method() === "GET" && !url.includes("/search")) {
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
  await page.route("**/api/buyer/orders", (route) => {
    const url = route.request().url();
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

export async function setupAuthenticatedBuyer(page) {
  await mockUserProfile(page);

  await page.addInitScript(({ accessToken, refreshToken }) => {
    localStorage.setItem("trevo.accessToken", accessToken);
    localStorage.setItem("trevo.refreshToken", refreshToken);
  }, testTokens);
}
