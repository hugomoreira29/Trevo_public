import { expect, test } from "@playwright/test";

test("AUTH-I01: register buyer, login, access protected route, and logout", async ({
  page,
}) => {
  const uniqueEmail = `buyer-${Date.now()}@example.com`;

  await page.goto("/register");
  await page.getByPlaceholder("Nome").fill("Buyer E2E");
  await page.getByPlaceholder("Email").fill(uniqueEmail);
  await page.getByPlaceholder("Password").fill("Secret123!");
  await page.getByPlaceholder("Telefone").fill("912345678");
  await page.getByRole("button", { name: "Registar" }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText("Conta criada com sucesso. Faz login para continuar.")).toBeVisible();

  await page.getByPlaceholder("Email").fill(uniqueEmail);
  await page.getByPlaceholder("Password").fill("Secret123!");
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText("Perfil carregado")).toBeVisible();
  await expect(page.getByText("Tipo de conta: BUYER")).toBeVisible();
  await expect(page.getByText(`Email: ${uniqueEmail}`)).toBeVisible();

  await page.goto("/");
  await page.getByRole("button", { name: "Logout" }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText("Autenticado: Não")).toBeVisible();
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});
