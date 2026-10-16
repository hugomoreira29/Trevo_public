import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import StockManagement from "./StockManagement";

const { mockBulkUpdateVariantStock, mockGetProduct } = vi.hoisted(() => ({
  mockBulkUpdateVariantStock: vi.fn(),
  mockGetProduct: vi.fn(),
}));

vi.mock("../services/productService", () => ({
  bulkUpdateVariantStock: mockBulkUpdateVariantStock,
  getProduct: mockGetProduct,
}));

globalThis.React = React;

const productWithVariants = {
  id: 7,
  name: "Casaco azul",
  variants: [
    { id: 101, productId: 7, size: "M", color: "Azul", stock: 3 },
    { id: 102, productId: 7, size: "L", color: "Preto", stock: 0 },
  ],
};

function renderStockManagement(initialEntry = "/products/7/stock") {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/products/:productId/stock" element={<StockManagement />} />
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

function changeInput(input, value) {
  const valueSetter = Object.getOwnPropertyDescriptor(
    input.constructor.prototype,
    "value"
  ).set;

  act(() => {
    valueSetter.call(input, value);
    input.dispatchEvent(new Event("input", { bubbles: true }));
  });
}

async function submitForm(container) {
  await act(async () => {
    container.querySelector("form").dispatchEvent(
      new Event("submit", {
        bubbles: true,
        cancelable: true,
      })
    );
  });
}

describe("StockManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetProduct.mockResolvedValue(productWithVariants);
    mockBulkUpdateVariantStock.mockResolvedValue(productWithVariants.variants);
  });

  it("renders a table of variants with editable stock fields", async () => {
    const { container, unmount } = renderStockManagement();

    await flushPromises();

    expect(mockGetProduct).toHaveBeenCalledWith("7");
    expect(container.textContent).toContain("Casaco azul");
    expect(container.querySelector("table")).not.toBeNull();
    expect(container.textContent).toContain("M");
    expect(container.textContent).toContain("Azul");
    expect(container.textContent).toContain("L");
    expect(container.textContent).toContain("Preto");

    const firstStockInput = container.querySelector('input[name="stock-101"]');

    expect(firstStockInput.value).toBe("3");
    changeInput(firstStockInput, "5");
    expect(firstStockInput.value).toBe("5");

    unmount();
  });

  it("sends bulk stock updates when saving", async () => {
    const { container, unmount } = renderStockManagement();

    await flushPromises();

    changeInput(container.querySelector('input[name="stock-101"]'), "8");
    changeInput(container.querySelector('input[name="stock-102"]'), "2");

    await submitForm(container);

    expect(mockBulkUpdateVariantStock).toHaveBeenCalledWith("7", [
      { variantId: 101, stock: 8 },
      { variantId: 102, stock: 2 },
    ]);

    unmount();
  });

  it("prevents negative stock values", async () => {
    const { container, unmount } = renderStockManagement();

    await flushPromises();

    const firstStockInput = container.querySelector('input[name="stock-101"]');

    changeInput(firstStockInput, "-4");

    expect(firstStockInput.value).toBe("0");

    unmount();
  });

  it("shows an empty state when the product has no variants", async () => {
    mockGetProduct.mockResolvedValue({
      id: 7,
      name: "Produto sem variantes",
      variants: [],
    });

    const { container, unmount } = renderStockManagement();

    await flushPromises();

    expect(container.textContent).toContain("Produto sem variantes");
    expect(container.textContent).toContain("Este produto ainda nao tem variantes.");
    expect(container.querySelector("table")).toBeNull();

    unmount();
  });

  it("shows a success message after saving", async () => {
    const { container, unmount } = renderStockManagement();

    await flushPromises();
    await submitForm(container);

    expect(container.textContent).toContain("Stock atualizado com sucesso");

    unmount();
  });
});
