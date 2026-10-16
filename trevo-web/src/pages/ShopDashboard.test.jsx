import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ShopDashboard from "./ShopDashboard";

const { mockGetShopDashboard } = vi.hoisted(() => ({
  mockGetShopDashboard: vi.fn(),
}));

vi.mock("../services/shopService", () => ({
  getShopDashboard: mockGetShopDashboard,
}));

globalThis.React = React;

function renderShopDashboard() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter>
        <ShopDashboard />
      </MemoryRouter>
    );
  });

  return {
    container,
    unmount: () =>
      act(() => {
        root.unmount();
      }),
  };
}

async function flushPromises() {
  await act(async () => {
    await Promise.resolve();
  });
}

describe("ShopDashboard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("displays shop info and shows an empty state when there are no orders", async () => {
    mockGetShopDashboard.mockResolvedValue({
      shop: {
        name: "Loja Trevo",
        description: "Moda circular",
        status: "PENDING",
      },
      orders: [],
      revenue: {
        total: 0,
        currency: "EUR",
      },
    });

    const { container, unmount } = renderShopDashboard();

    await flushPromises();

    expect(mockGetShopDashboard).toHaveBeenCalled();
    expect(container.textContent).toContain("Loja Trevo");
    expect(container.textContent).toContain("Moda circular");
    expect(container.textContent).toContain("Estado: Pendente");
    expect(container.textContent).toContain(
      new Intl.NumberFormat("pt-PT", {
        style: "currency",
        currency: "EUR",
      }).format(0)
    );
    expect(container.textContent).toContain("Ainda não existem encomendas.");

    expect(container.querySelector('a[href="/shop/payouts"]').textContent).toBe(
      "Ver pagamentos"
    );

    unmount();
  });

  it("displays the fallback error as an alert", async () => {
    mockGetShopDashboard.mockRejectedValue(new Error("Network error"));

    const { container, unmount } = renderShopDashboard();

    await flushPromises();

    const alert = container.querySelector('[role="alert"]');

    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain(
      "Não foi possível carregar o painel da loja"
    );
    expect(container.textContent).not.toContain("Network error");

    unmount();
  });
});
