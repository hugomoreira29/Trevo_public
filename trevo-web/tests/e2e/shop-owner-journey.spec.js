import { expect, test } from "@playwright/test";

// JWT with payload {"sub":"1","role":"SHOP_OWNER","exp":9999999999}
const SHOP_OWNER_TOKEN =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
  ".eyJzdWIiOiIxIiwicm9sZSI6IlNIT1BfT1dORVIiLCJleHAiOjk5OTk5OTk5OTl9" +
  ".fake-signature";

const SHOP_OWNER_PROFILE = {
  id: 1,
  role: "SHOP_OWNER",
  fullName: "Dono E2E",
  status: "ACTIVE",
  shopName: "Loja E2E",
  shopStatus: "ACTIVE",
};

const CREATED_SHOP = {
  id: 10,
  name: "Loja E2E",
  description: "Loja criada no teste E2E",
  status: "ACTIVE",
  collections: [],
};

const SHOP_DASHBOARD = {
  shop: CREATED_SHOP,
  orders: [],
  revenue: { total: 0, currency: "EUR" },
};

const PLACED_ORDER = {
  id: 1,
  status: "PLACED",
  createdAt: "2026-05-01T10:00:00",
  totalAmount: 29.99,
  buyerName: "Comprador Teste",
  items: [
    {
      id: 1,
      productId: 1,
      variantId: 1,
      productName: "Camisa E2E",
      quantity: 1,
      subtotal: 29.99,
    },
  ],
};

const ORDERS_PAGE = {
  content: [PLACED_ORDER],
  totalElements: 1,
  totalPages: 1,
  number: 0,
};

test("NFR-11: full shop owner golden path: register → shop → product → receive order → confirm → ship → deliver", async ({
  page,
}) => {
  const ownerEmail = `owner-${Date.now()}@trevo.test`;

  // ─── Global stubs ────────────────────────────────────────────────────────────

  await page.route("**/api/notifications**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, number: 0 }),
    });
  });

  // ─── Step 1: Register as shop owner ─────────────────────────────────────────

  await page.route("**/api/auth/register/shop-owner", async (route) => {
    await route.fulfill({ status: 201 });
  });

  await page.goto("/register");
  await page.getByPlaceholder("Nome").fill("Dono E2E");
  await page.getByPlaceholder("Email").fill(ownerEmail);
  await page.getByPlaceholder("Password").fill("Secret123!");
  await page.getByRole("combobox").selectOption("owner");
  await page.getByPlaceholder("Nome da loja").fill("Loja E2E");
  await page.getByPlaceholder("NIF").fill("123456789");
  await page.getByRole("button", { name: "Registar" }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(
    page.getByText("Conta criada com sucesso. Podes agora fazer login.")
  ).toBeVisible();

  // ─── Step 2: Login ───────────────────────────────────────────────────────────

  await page.route("**/api/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        accessToken: SHOP_OWNER_TOKEN,
        refreshToken: "refresh-token",
      }),
    });
  });

  await page.route("**/api/users/me", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ...SHOP_OWNER_PROFILE, email: ownerEmail }),
    });
  });

  await page.getByPlaceholder("Email").fill(ownerEmail);
  await page.getByPlaceholder("Password").fill("Secret123!");
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText("Tipo de conta: SHOP_OWNER")).toBeVisible();

  // ─── Step 3: Create shop ─────────────────────────────────────────────────────

  await page.route("**/api/shops", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify(CREATED_SHOP),
      });
    } else {
      await route.continue();
    }
  });

  await page.goto("/shops/new");
  await page.getByLabel("Nome da loja").fill("Loja E2E");
  await page.getByLabel("Descrição").fill("Loja criada no teste E2E");
  await page.getByLabel("NIF").fill("123456789");
  await page.getByRole("button", { name: "Criar loja" }).click();

  await expect(page.getByText("Loja criada com sucesso")).toBeVisible();

  // ─── Step 4: Add a product ───────────────────────────────────────────────────

  await page.route("**/api/shops/me/dashboard", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(SHOP_DASHBOARD),
    });
  });

  await page.route("**/api/products", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify({
          id: 1,
          name: "Camisa E2E",
          price: 29.99,
          status: "ACTIVE",
        }),
      });
    } else {
      await route.continue();
    }
  });

  await page.route("**/api/products/1/variants", async (route) => {
    await route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({ id: 1, size: "M", color: "Azul", stock: 10 }),
    });
  });

  await page.goto("/products/new");
  await page.getByLabel("Nome").fill("Camisa E2E");
  await page.getByLabel("Preço").fill("29.99");
  await page.getByLabel("Marca").fill("Trevo");
  await page.locator('input[placeholder="Tamanho"]').first().fill("M");
  await page.locator('input[placeholder="Cor"]').first().fill("Azul");
  await page.locator('input[placeholder="Stock"]').first().fill("10");
  await page.getByRole("button", { name: "Guardar produto" }).click();

  await expect(page.getByText("Produto criado com sucesso")).toBeVisible();

  // ─── Step 5: View incoming order ─────────────────────────────────────────────

  let currentOrder = { ...PLACED_ORDER };

  await page.route("**/api/orders**", async (route) => {
    if (route.request().method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ...ORDERS_PAGE, content: [currentOrder] }),
      });
    } else {
      await route.continue();
    }
  });

  await page.goto("/shop/orders");
  await expect(page.getByText("Order #1")).toBeVisible();
  await expect(page.getByText("Pendente")).toBeVisible();

  // ─── Step 6: Confirm order ───────────────────────────────────────────────────

  await page.route("**/api/orders/1/status", async (route) => {
    const { status } = await route.request().postDataJSON();
    currentOrder = { ...currentOrder, status };
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(currentOrder),
    });
  });

  await page.getByRole("button", { name: "Marcar como confirmada" }).click();

  // Order moves out of PLACED tab; switch to CONFIRMED tab
  await page.locator('nav[aria-label="Estados"] button:has-text("Confirmada")').click();
  await expect(page.getByText("Confirmada")).toBeVisible();

  // ─── Step 7: Ship order ──────────────────────────────────────────────────────

  await page.getByRole("button", { name: "Marcar como enviada" }).click();

  await page.locator('nav[aria-label="Estados"] button:has-text("Enviada")').click();
  await expect(page.getByText("Enviada")).toBeVisible();

  // ─── Step 8: Deliver order ───────────────────────────────────────────────────

  await page.getByRole("button", { name: "Marcar como entregue" }).click();

  await page.locator('nav[aria-label="Estados"] button:has-text("Entregue")').click();
  await expect(page.getByText("Entregue")).toBeVisible();
});
