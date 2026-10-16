import { expect, test } from "@playwright/test";
import {
  testCart,
  testOrderDetail,
  testOrdersPage,
  testProductDetail,
  testProducts,
  testOrders,
  testMultiShopCart,
  testSplitOrders,
  testSplitOrdersPage,
  stockConflictError,
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
  mockCheckoutStockError,
} from "./helpers/api-mocks.js";

test.describe("Buyer Journey — Golden Path", () => {
  test("Register → Search → Add to Cart → Checkout → View Order", async ({ page }) => {
    // --- Register ---
    await mockRegisterBuyer(page);
    await page.goto("/register");
    await page.getByLabel("Nome").fill("Buyer E2E");
    await page.getByLabel("Email").first().fill(`buyer-${Date.now()}@example.com`);
    await page.getByLabel("Palavra-passe").fill("Secret123!");
    await page.getByLabel("Telefone").fill("912345678");
    await page.getByRole("button", { name: "Registar" }).click();

    await expect(page).toHaveURL(/\/login$/);

    // --- Login ---
    await mockLogin(page);
    await mockUserProfile(page);
    await page.getByLabel("Email").fill("buyer-e2e@example.com");
    await page.getByLabel("Palavra-passe").fill("Secret123!");
    await page.getByRole("button", { name: "Entrar" }).click();

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByText("Perfil carregado")).toBeVisible();

    // --- Search Products ---
    await mockProductSearch(page, testProducts);
    await page.goto("/products");
    await page.getByLabel("Pesquisar").fill("vestido");
    // The search is debounced — wait for products to appear
    await expect(page.getByText("Vestido Floral")).toBeVisible();
    await expect(page.getByText("49,99")).toBeVisible();

    // --- View Product Detail ---
    await mockProductDetail(page, testProductDetail);
    await page.getByRole("link", { name: "Vestido Floral" }).first().click();

    await expect(page.getByRole("heading", { name: "Vestido Floral" })).toBeVisible();
    await expect(page.getByText("TrevoStyle").first()).toBeVisible();
    await expect(page.getByText("Algodao 100%")).toBeVisible();

    // --- Select Variant & Add to Cart ---
    await mockAddToCart(page);
    await page.getByRole("button", { name: "M", exact: true }).click();
    await page.getByRole("button", { name: "Vermelho", exact: true }).click();
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
    await expect(page.getByRole("heading", { name: "Finalizar compra" })).toBeVisible();
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
