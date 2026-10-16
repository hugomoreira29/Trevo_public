import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ReceiptView from "./ReceiptView";

const { mockGetBuyerOrder } = vi.hoisted(() => ({
  mockGetBuyerOrder: vi.fn(),
}));

vi.mock("../services/orderService", () => ({
  getBuyerOrder: mockGetBuyerOrder,
}));

globalThis.React = React;

const order = {
  id: 42,
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

function renderReceiptView() {
  const container = document.createElement("div");
  const root = createRoot(container);

  document.body.appendChild(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/orders/42/receipt"]}>
        <Routes>
          <Route path="/orders/:orderId/receipt" element={<ReceiptView />} />
        </Routes>
      </MemoryRouter>
    );
  });

  return {
    container,
    unmount: () =>
      act(() => {
        root.unmount();
        container.remove();
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

describe("ReceiptView", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetBuyerOrder.mockResolvedValue(order);

    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: vi.fn(() => "blob:receipt"),
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: vi.fn(),
    });
  });

  it("shows order details", async () => {
    const { container, unmount } = renderReceiptView();

    await flushPromises();

    expect(mockGetBuyerOrder).toHaveBeenCalledWith("42");
    expect(container.textContent).toContain("Encomenda n.º 42");
    expect(container.textContent).toContain("South Shop");
    expect(container.textContent).toContain("PAYPAL");
    expect(container.textContent).toContain("Enviada");
    expect(container.textContent).toContain("Linen Shirt");
    expect(container.textContent).toContain("Quantidade: 2");
    expect(container.textContent).toContain("40,00");

    unmount();
  });

  it("download button triggers download helper", async () => {
    const anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});
    let createdAnchor;
    const originalCreateElement = document.createElement.bind(document);
    const createElement = vi
      .spyOn(document, "createElement")
      .mockImplementation((tagName, options) => {
        const element = originalCreateElement(tagName, options);

        if (tagName === "a") {
          createdAnchor = element;
        }

        return element;
      });
    const { container, unmount } = renderReceiptView();

    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find((button) =>
        button.textContent.includes("Descarregar PDF")
      )
    );

    expect(URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob));
    expect(URL.createObjectURL.mock.calls[0][0].type).toBe("application/pdf");
    expect(createdAnchor.download).toBe("recibo-encomenda-42.pdf");
    expect(anchorClick).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:receipt");

    createElement.mockRestore();
    anchorClick.mockRestore();
    unmount();
  });

  it("shows error state", async () => {
    mockGetBuyerOrder.mockRejectedValue({
      response: {
        data: {
          message: "Nao foi possivel carregar esta encomenda",
        },
      },
    });
    const { container, unmount } = renderReceiptView();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Nao foi possivel carregar esta encomenda"
    );

    unmount();
  });

  it("shows loading state", () => {
    mockGetBuyerOrder.mockReturnValue(new Promise(() => {}));
    const { container, unmount } = renderReceiptView();

    expect(container.querySelector('[aria-live="polite"]').textContent).toContain(
      "A carregar recibo..."
    );

    unmount();
  });
});
