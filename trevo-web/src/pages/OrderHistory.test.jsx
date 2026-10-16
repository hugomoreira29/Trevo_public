import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import OrderHistory from "./OrderHistory";

const { mockGetBuyerOrder, mockGetBuyerOrders } = vi.hoisted(() => ({
  mockGetBuyerOrder: vi.fn(),
  mockGetBuyerOrders: vi.fn(),
}));

vi.mock("../services/orderService", () => ({
  getBuyerOrder: mockGetBuyerOrder,
  getBuyerOrders: mockGetBuyerOrders,
}));

globalThis.React = React;

const orderPage = {
  totalElements: 2,
  totalPages: 1,
  page: 0,
  size: 20,
  orders: [
    {
      id: 10,
      shopId: 301,
      shopName: "North Shop",
      status: "PLACED",
      trackingNumber: null,
      totalAmount: 59.98,
      commissionAmount: 6,
      paymentMethod: "MBWAY",
      paymentReference: null,
      createdAt: "2025-06-01T10:00:00Z",
      updatedAt: "2025-06-01T10:00:00Z",
      items: [],
    },
    {
      id: 11,
      shopId: 302,
      shopName: "South Shop",
      status: "SHIPPED",
      trackingNumber: "TRACK-123",
      totalAmount: 40,
      commissionAmount: 4,
      paymentMethod: "PAYPAL",
      paymentReference: null,
      createdAt: "2025-06-02T10:00:00Z",
      updatedAt: "2025-06-02T10:00:00Z",
      items: [],
    },
  ],
};

const orderDetail = {
  id: 11,
  shopId: 302,
  shopName: "South Shop",
  status: "SHIPPED",
  trackingNumber: "TRACK-123",
  totalAmount: 40,
  commissionAmount: 4,
  paymentMethod: "PAYPAL",
  paymentReference: null,
  createdAt: "2025-06-02T10:00:00Z",
  updatedAt: "2025-06-02T10:00:00Z",
  items: [
    {
      id: 1,
      variantId: 101,
      productId: 201,
      productName: "Linen Shirt",
      size: "M",
      color: "Blue",
      quantity: 2,
      unitPrice: 20,
      subtotal: 40,
    },
  ],
};

function renderOrderHistory() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/orders"]}>
        <Routes>
          <Route path="/orders" element={<OrderHistory />} />
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

describe("OrderHistory", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetBuyerOrders.mockResolvedValue(orderPage);
    mockGetBuyerOrder.mockResolvedValue(orderDetail);
  });

  it("lists orders with status badges", async () => {
    const { container, unmount } = renderOrderHistory();

    await flushPromises();

    expect(container.textContent).toContain("Encomenda n.º 10");
    expect(container.textContent).toContain("North Shop");
    expect(container.textContent).toContain("59,98");
    expect(container.textContent).toContain("Criada");
    expect(container.textContent).toContain("Encomenda n.º 11");
    expect(container.textContent).toContain("South Shop");
    expect(container.textContent).toContain("Enviada");

    unmount();
  });

  it("shows details after clicking an order", async () => {
    const { container, unmount } = renderOrderHistory();

    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find((button) =>
        button.textContent.includes("Encomenda n.º 11")
      )
    );

    expect(mockGetBuyerOrder).toHaveBeenCalledWith(11);
    expect(container.textContent).toContain("Seguimento: TRACK-123");
    expect(container.textContent).toContain("Linen Shirt");
    expect(container.textContent).toContain("Tamanho: M");
    expect(container.textContent).toContain("Cor: Blue");
    expect(container.textContent).toContain("Quantidade: 2");

    unmount();
  });

  it("loads the next page when clicking pagination", async () => {
    mockGetBuyerOrders
      .mockResolvedValueOnce({
        ...orderPage,
        totalPages: 2,
        page: 0,
      })
      .mockResolvedValueOnce({
        ...orderPage,
        totalPages: 2,
        page: 1,
        orders: [orderPage.orders[1]],
      });
    const { container, unmount } = renderOrderHistory();

    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Seguinte"
      )
    );
    await flushPromises();

    expect(mockGetBuyerOrders).toHaveBeenLastCalledWith({ page: 1, size: 20 });

    unmount();
  });

  it("shows an error when loading order detail fails", async () => {
    mockGetBuyerOrder.mockRejectedValue({
      response: {
        data: {
          message: "Não foi possível carregar o detalhe",
        },
      },
    });
    const { container, unmount } = renderOrderHistory();

    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find((button) =>
        button.textContent.includes("Encomenda n.º 11")
      )
    );
    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Não foi possível carregar o detalhe"
    );

    unmount();
  });

  it("shows an empty state when there are no orders", async () => {
    mockGetBuyerOrders.mockResolvedValue({
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 20,
      orders: [],
    });
    const { container, unmount } = renderOrderHistory();

    await flushPromises();

    expect(container.textContent).toContain("Ainda não existem encomendas.");

    unmount();
  });

  it("shows a simple error when loading orders fails", async () => {
    mockGetBuyerOrders.mockRejectedValue(new Error("Network error"));
    const { container, unmount } = renderOrderHistory();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Não foi possível carregar as encomendas"
    );

    unmount();
  });
});
