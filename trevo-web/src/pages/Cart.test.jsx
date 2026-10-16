import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Cart from "./Cart";

const {
  mockGetCart,
  mockRemoveCartItem,
  mockUpdateCartItem,
} = vi.hoisted(() => ({
  mockGetCart: vi.fn(),
  mockRemoveCartItem: vi.fn(),
  mockUpdateCartItem: vi.fn(),
}));

vi.mock("../services/cartService", () => ({
  getCart: mockGetCart,
  removeCartItem: mockRemoveCartItem,
  updateCartItem: mockUpdateCartItem,
}));

globalThis.React = React;

const cartWithTwoShops = {
  shops: [
    {
      shopId: 301,
      shopName: "North Shop",
      items: [
        {
          cartItemId: 1,
          variantId: 101,
          productId: 201,
          productName: "Linen Shirt",
          image: null,
          size: "M",
          color: "Blue",
          quantity: 2,
          unitPrice: 29.99,
          subtotal: 59.98,
        },
      ],
      subtotal: 59.98,
    },
    {
      shopId: 302,
      shopName: "South Shop",
      items: [
        {
          cartItemId: 2,
          variantId: 102,
          productId: 202,
          productName: "Denim Jacket",
          image: "https://example.com/jacket.jpg",
          size: "L",
          color: "Black",
          quantity: 1,
          unitPrice: 40,
          subtotal: 40,
        },
      ],
      subtotal: 40,
    },
  ],
  grandTotal: 99.98,
};

function updatedCart() {
  return {
    shops: [
      {
        shopId: 301,
        shopName: "North Shop",
        items: [
          {
            cartItemId: 1,
            variantId: 101,
            productId: 201,
            productName: "Linen Shirt",
            image: null,
            size: "M",
            color: "Blue",
            quantity: 3,
            unitPrice: 29.99,
            subtotal: 89.97,
          },
        ],
        subtotal: 89.97,
      },
    ],
    grandTotal: 89.97,
  };
}

function renderCart() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/cart"]}>
        <Routes>
          <Route path="/cart" element={<Cart />} />
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

function buttonsByLabel(container, label) {
  return [...container.querySelectorAll("button")].filter((button) =>
    button.getAttribute("aria-label")?.includes(label)
  );
}

describe("Cart", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetCart.mockResolvedValue(cartWithTwoShops);
    mockRemoveCartItem.mockResolvedValue(undefined);
    mockUpdateCartItem.mockResolvedValue(cartWithTwoShops);
  });

  it("renders items grouped under shop headers", async () => {
    const { container, unmount } = renderCart();

    await flushPromises();

    const sections = container.querySelectorAll("section");

    expect(sections[0].textContent).toContain("North Shop");
    expect(sections[0].textContent).toContain("Linen Shirt");
    expect(sections[1].textContent).toContain("South Shop");
    expect(sections[1].textContent).toContain("Denim Jacket");

    unmount();
  });

  it("+/- buttons call update", async () => {
    const { container, unmount } = renderCart();

    await flushPromises();

    await click(buttonsByLabel(container, "Aumentar quantidade de Linen Shirt")[0]);

    expect(mockUpdateCartItem).toHaveBeenCalledWith(101, 3);

    await click(buttonsByLabel(container, "Diminuir quantidade de Linen Shirt")[0]);

    expect(mockUpdateCartItem).toHaveBeenLastCalledWith(101, 1);

    unmount();
  });

  it("remove button calls delete", async () => {
    const { container, unmount } = renderCart();

    await flushPromises();

    await click(
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Remover"
      )
    );

    expect(mockRemoveCartItem).toHaveBeenCalledWith(101);

    unmount();
  });

  it("recalculates totals from the update response", async () => {
    mockUpdateCartItem.mockResolvedValue(updatedCart());
    const { container, unmount } = renderCart();

    await flushPromises();

    expect(container.textContent).toContain("99,98");

    await click(buttonsByLabel(container, "Aumentar quantidade de Linen Shirt")[0]);

    expect(container.textContent).toContain("89,97");
    expect(container.textContent).not.toContain("99,98");

    unmount();
  });

  it("disables the decrease button when quantity is 1", async () => {
    const { container, unmount } = renderCart();

    await flushPromises();

    expect(
      buttonsByLabel(container, "Diminuir quantidade de Denim Jacket")[0].disabled
    ).toBe(true);

    unmount();
  });

  it("shows an empty state when the cart has no items", async () => {
    mockGetCart.mockResolvedValue({ shops: [], grandTotal: 0 });
    const { container, unmount } = renderCart();

    await flushPromises();

    expect(container.textContent).toContain("O carrinho está vazio.");

    unmount();
  });

  it("shows a simple error message", async () => {
    mockGetCart.mockRejectedValue(new Error("Network error"));
    const { container, unmount } = renderCart();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Não foi possível carregar o carrinho"
    );

    unmount();
  });
});
