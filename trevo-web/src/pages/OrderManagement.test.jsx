import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import OrderManagement from "./OrderManagement";

const { mockGetShopOrders, mockUpdateOrderStatus } = vi.hoisted(() => ({
  mockGetShopOrders: vi.fn(),
  mockUpdateOrderStatus: vi.fn(),
}));

vi.mock("../services/orderService", () => ({
  getShopOrders: mockGetShopOrders,
  updateOrderStatus: mockUpdateOrderStatus,
}));

globalThis.React = React;

const orderPage = {
  totalElements: 4,
  totalPages: 2,
  page: 0,
  size: 20,
  orders: [
    {
      id: 10,
      buyerName: "Ana Costa",
      status: "PLACED",
      totalAmount: 59.98,
      createdAt: "2025-06-01T10:00:00Z",
      items: [{ id: 1, productName: "Camisa", quantity: 2, subtotal: 59.98 }],
    },
    {
      id: 11,
      buyerName: "Bruno Silva",
      status: "CONFIRMED",
      totalAmount: 40,
      createdAt: "2025-06-02T10:00:00Z",
      items: [],
    },
    {
      id: 12,
      buyerName: "Carla Reis",
      status: "SHIPPED",
      totalAmount: 20,
      createdAt: "2025-06-03T10:00:00Z",
      items: [],
    },
    {
      id: 13,
      buyerName: "Dinis Melo",
      status: "DELIVERED",
      totalAmount: 75,
      createdAt: "2025-06-04T10:00:00Z",
      items: [],
    },
  ],
};

function renderOrderManagement() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/shop/orders"]}>
        <Routes>
          <Route path="/shop/orders" element={<OrderManagement />} />
        </Routes>
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

async function click(button) {
  await act(async () => {
    button.click();
  });
}

function buttonByText(container, text) {
  return [...container.querySelectorAll("button")].find(
    (button) => button.textContent === text
  );
}

describe("OrderManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetShopOrders.mockResolvedValue(orderPage);
    mockUpdateOrderStatus.mockResolvedValue(null);
  });

  it("loads shop orders with pagination params and filters by status tabs", async () => {
    const { container, unmount } = renderOrderManagement();

    await flushPromises();

    expect(mockGetShopOrders).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(container.textContent).toContain("Order #10");
    expect(container.textContent).toContain("Recebida");
    expect(container.textContent).not.toContain("Order #11");

    await click(buttonByText(container, "Confirmada"));

    expect(container.textContent).toContain("Order #11");
    expect(container.textContent).not.toContain("Order #10");

    unmount();
  });

  it("updates an order status locally after a successful action", async () => {
    const { container, unmount } = renderOrderManagement();

    await flushPromises();
    await click(buttonByText(container, "Marcar como confirmada"));

    expect(mockUpdateOrderStatus).toHaveBeenCalledWith(10, "CONFIRMED");
    expect(container.textContent).toContain("Order #10");
    expect(container.textContent).toContain("Confirmada");
    expect(mockGetShopOrders).toHaveBeenCalledTimes(1);

    unmount();
  });

  it("shows an update error and keeps the order status when updateOrderStatus fails", async () => {
    mockUpdateOrderStatus.mockRejectedValue({
      response: { data: { message: "Transicao invalida" } },
    });
    const { container, unmount } = renderOrderManagement();

    await flushPromises();
    await click(buttonByText(container, "Marcar como confirmada"));
    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Transicao invalida"
    );
    expect(container.textContent).toContain("Order #10");
    expect(container.textContent).toContain("Recebida");

    unmount();
  });

  it("retries the initial load after an error", async () => {
    mockGetShopOrders
      .mockRejectedValueOnce(new Error("Network error"))
      .mockResolvedValueOnce(orderPage);
    const { container, unmount } = renderOrderManagement();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Nao foi possivel carregar as encomendas"
    );

    await click(buttonByText(container, "Tentar novamente"));
    await flushPromises();

    expect(mockGetShopOrders).toHaveBeenLastCalledWith({ page: 0, size: 20 });
    expect(container.textContent).toContain("Order #10");

    unmount();
  });

  it("loads the next page when clicking pagination", async () => {
    mockGetShopOrders
      .mockResolvedValueOnce(orderPage)
      .mockResolvedValueOnce({
        ...orderPage,
        page: 1,
        orders: [orderPage.orders[3]],
      });
    const { container, unmount } = renderOrderManagement();

    await flushPromises();
    await click(buttonByText(container, "Seguinte"));
    await flushPromises();

    expect(mockGetShopOrders).toHaveBeenLastCalledWith({ page: 1, size: 20 });

    unmount();
  });
});
