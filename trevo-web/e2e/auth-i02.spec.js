import { expect, test } from "@playwright/test";

test("AUTH-I02: register shop owner and verify pending status", async ({ page }) => {
  const uniqueEmail = `owner-${Date.now()}@example.com`;
  const uniqueNif = `${Math.floor(Date.now() / 10)}`.slice(-9);

  await page.goto("/register");
  await page.getByPlaceholder("Nome").fill("Owner E2E");
  await page.getByPlaceholder("Email").fill(uniqueEmail);
  await page.getByPlaceholder("Password").fill("Secret123!");
  await page.getByPlaceholder("Telefone").fill("912345678");
  await page.locator('select[name="type"]').selectOption("owner");
  await page.getByPlaceholder("Nome da loja").fill("Trevo Pending Shop");
  await page.getByPlaceholder("NIF").fill(uniqueNif);
  await page.getByRole("button", { name: "Registar" }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(
    page.getByText("Conta de lojista criada com estado pendente. Faz login para acompanhar a aprovação."),
  ).toBeVisible();

  await page.getByPlaceholder("Email").fill(uniqueEmail);
  await page.getByPlaceholder("Password").fill("Secret123!");
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText("Perfil carregado")).toBeVisible();
  await expect(page.getByText("Tipo de conta: SHOP_OWNER")).toBeVisible();
  await expect(page.getByText("Estado da conta: PENDING")).toBeVisible();
  await expect(page.getByText("Loja: Trevo Pending Shop")).toBeVisible();
  await expect(page.getByText("Estado da loja: PENDING")).toBeVisible();
});
