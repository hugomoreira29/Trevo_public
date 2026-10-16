import { expect, test } from "@playwright/test";

test("forgot-password submits the email and shows the neutral success state", async ({ page }) => {
  let requestBody;

  await page.route("**/api/auth/password-reset/request", async (route) => {
    requestBody = route.request().postDataJSON();
    await route.fulfill({ status: 204 });
  });

  await page.goto("/forgot-password");
  await page.getByPlaceholder("Email").fill("buyer@example.com");
  await page.getByRole("button", { name: "Enviar link de recuperacao" }).click();

  await expect(page.getByText("Se o email existir, enviamos as instrucoes de recuperacao.")).toBeVisible();
  expect(requestBody).toEqual({ email: "buyer@example.com" });
});

test("reset-password validates the token, submits the new password, and returns to login", async ({ page }) => {
  const requests = [];

  await page.route("**/api/auth/password-reset/validate", async (route) => {
    requests.push({
      path: "validate",
      body: route.request().postDataJSON(),
    });
    await route.fulfill({ status: 204 });
  });

  await page.route("**/api/auth/password-reset/confirm", async (route) => {
    requests.push({
      path: "confirm",
      body: route.request().postDataJSON(),
    });
    await route.fulfill({ status: 204 });
  });

  await page.goto("/reset-password?token=valid-token");
  await expect(page.getByPlaceholder("Nova password")).toBeVisible();

  await page.getByPlaceholder("Nova password").fill("new-password");
  await page.getByPlaceholder("Confirmar password").fill("new-password");
  await page.getByRole("button", { name: "Guardar nova password" }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByText("Password redefinida com sucesso. Podes iniciar sessao.")).toBeVisible();
  expect(requests).toContainEqual({ path: "validate", body: { token: "valid-token" } });
  expect(requests.filter((request) => request.path === "confirm")).toEqual([
    { path: "confirm", body: { token: "valid-token", newPassword: "new-password" } },
  ]);
});
