import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProductForm from "./ProductForm";

const {
  mockAssignProductCollections,
  mockCreateProduct,
  mockCreateProductVariant,
  mockDeleteProductVariant,
  mockGetProduct,
  mockGetShopDashboard,
  mockGetShopProfile,
  mockUpdateProduct,
  mockUpdateProductVariant,
} = vi.hoisted(() => ({
  mockAssignProductCollections: vi.fn(),
  mockCreateProduct: vi.fn(),
  mockCreateProductVariant: vi.fn(),
  mockDeleteProductVariant: vi.fn(),
  mockGetProduct: vi.fn(),
  mockGetShopDashboard: vi.fn(),
  mockGetShopProfile: vi.fn(),
  mockUpdateProduct: vi.fn(),
  mockUpdateProductVariant: vi.fn(),
}));

vi.mock("../services/productService", () => ({
  assignProductCollections: mockAssignProductCollections,
  createProduct: mockCreateProduct,
  createProductVariant: mockCreateProductVariant,
  deleteProductVariant: mockDeleteProductVariant,
  getProduct: mockGetProduct,
  updateProduct: mockUpdateProduct,
  updateProductVariant: mockUpdateProductVariant,
}));

vi.mock("../services/shopService", () => ({
  getShopDashboard: mockGetShopDashboard,
  getShopProfile: mockGetShopProfile,
}));

globalThis.React = React;

function renderProductForm(mode = "create", initialEntry = "/products/new") {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/products/new" element={<ProductForm mode={mode} />} />
          <Route path="/products/:productId/edit" element={<ProductForm mode={mode} />} />
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

function changeFiles(input, files) {
  Object.defineProperty(input, "files", {
    configurable: true,
    value: files,
  });

  act(() => {
    input.dispatchEvent(new Event("change", { bubbles: true }));
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

function fillBaseProduct(container) {
  changeInput(container.querySelector('input[name="name"]'), "Casaco azul");
  changeInput(container.querySelector('textarea[name="description"]'), "Casaco quente");
  changeInput(container.querySelector('input[name="price"]'), "49.99");
  changeInput(container.querySelector('input[name="brand"]'), "Trevo");
  changeInput(container.querySelector('input[name="materials"]'), "Lã");
}

function mockCollections() {
  mockGetShopDashboard.mockResolvedValue({ shop: { id: 1 } });
  mockGetShopProfile.mockResolvedValue({
    collections: [
      { id: 2, name: "Casacos", displayOrder: 2 },
      { id: 1, name: "Novidades", displayOrder: 1 },
    ],
  });
}

describe("ProductForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCollections();
    globalThis.URL.createObjectURL = vi.fn((file) => `blob:${file.name}`);
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it("adds and removes dynamic variant rows", async () => {
    const { container, unmount } = renderProductForm();

    await flushPromises();

    expect(container.querySelectorAll('input[name^="variant-size"]').length).toBe(1);

    act(() => {
      [...container.querySelectorAll("button")]
        .find((button) => button.textContent === "Adicionar variante")
        .click();
    });

    expect(container.querySelectorAll('input[name^="variant-size"]').length).toBe(2);

    act(() => {
      [...container.querySelectorAll("button")]
        .find((button) => button.textContent === "Remover")
        .click();
    });

    expect(container.querySelectorAll('input[name^="variant-size"]').length).toBe(1);

    unmount();
  });

  it("previews selected images, caps previews at 8, and shows the max image error", async () => {
    const { container, unmount } = renderProductForm();

    await flushPromises();

    const imageInput = container.querySelector('input[name="images"]');
    const files = Array.from({ length: 9 }, (_, index) =>
      new File(["image"], `image-${index + 1}.png`, { type: "image/png" })
    );

    changeFiles(imageInput, files);

    expect(container.querySelectorAll("img").length).toBe(8);
    expect(container.textContent).toContain("Só podes adicionar até 8 imagens.");
    expect(globalThis.URL.createObjectURL).toHaveBeenCalledTimes(8);

    unmount();
  });

  it("renders collection checkboxes and submits selected collections", async () => {
    mockCreateProduct.mockResolvedValue({ id: 10 });
    mockCreateProductVariant.mockResolvedValue({ id: 20 });
    mockAssignProductCollections.mockResolvedValue({ id: 10 });
    const { container, unmount } = renderProductForm();

    await flushPromises();

    const checkboxes = container.querySelectorAll('input[name="collectionIds"]');
    expect(checkboxes.length).toBe(2);
    expect(container.textContent).toContain("Novidades");
    expect(container.textContent).toContain("Casacos");

    fillBaseProduct(container);
    changeInput(container.querySelector('input[name="variant-size-0"]'), "M");
    changeInput(container.querySelector('input[name="variant-color-0"]'), "Azul");
    changeInput(container.querySelector('input[name="variant-stock-0"]'), "3");

    act(() => {
      checkboxes[0].click();
    });

    await submitForm(container);

    expect(mockAssignProductCollections).toHaveBeenCalledWith(10, [1]);

    unmount();
  });

  it("creates a product, creates variants, and assigns collections", async () => {
    mockCreateProduct.mockResolvedValue({ id: 10 });
    mockCreateProductVariant.mockResolvedValue({ id: 20 });
    mockAssignProductCollections.mockResolvedValue({ id: 10 });
    const { container, unmount } = renderProductForm();

    await flushPromises();

    fillBaseProduct(container);
    changeInput(container.querySelector('input[name="variant-size-0"]'), "M");
    changeInput(container.querySelector('input[name="variant-color-0"]'), "Azul");
    changeInput(container.querySelector('input[name="variant-stock-0"]'), "3");

    act(() => {
      container.querySelector('input[name="collectionIds"]').click();
    });

    await submitForm(container);

    expect(mockCreateProduct).toHaveBeenCalledWith({
      name: "Casaco azul",
      description: "Casaco quente",
      price: "49.99",
      brand: "Trevo",
      materials: "Lã",
    });
    expect(mockCreateProductVariant).toHaveBeenCalledWith(10, {
      size: "M",
      color: "Azul",
      stock: 3,
    });
    expect(mockAssignProductCollections).toHaveBeenCalledWith(10, [1]);
    expect(mockCreateProduct.mock.invocationCallOrder[0]).toBeLessThan(
      mockCreateProductVariant.mock.invocationCallOrder[0]
    );
    expect(mockCreateProductVariant.mock.invocationCallOrder[0]).toBeLessThan(
      mockAssignProductCollections.mock.invocationCallOrder[0]
    );

    unmount();
  });
});
