import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProductSearchResults from "./ProductSearchResults";

const { mockBrowseProducts, mockSearchProducts } = vi.hoisted(() => ({
  mockBrowseProducts: vi.fn(),
  mockSearchProducts: vi.fn(),
}));

vi.mock("../services/productService", () => ({
  browseProducts: mockBrowseProducts,
  searchProducts: mockSearchProducts,
}));

globalThis.React = React;

function renderProductSearchResults(initialEntry = "/products") {
  const container = document.createElement("div");
  const root = createRoot(container);

  function LocationProbe() {
    const location = useLocation();
    return <span data-testid="location-search">{location.search}</span>;
  }

  act(() => {
    root.render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <LocationProbe />
        <Routes>
          <Route path="/products" element={<ProductSearchResults />} />
        </Routes>
      </MemoryRouter>
    );
  });

  return {
    container,
    getSearch: () =>
      container.querySelector('[data-testid="location-search"]').textContent,
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

async function submitFilters(container) {
  await act(async () => {
    container.querySelector("form").dispatchEvent(
      new Event("submit", {
        bubbles: true,
        cancelable: true,
      })
    );
  });
}

describe("ProductSearchResults", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchProducts.mockResolvedValue({ content: [] });
    mockBrowseProducts.mockResolvedValue({ content: [] });
  });

  it("updates URL params when filters are applied", async () => {
    const { container, getSearch, unmount } = renderProductSearchResults();

    await flushPromises();

    changeInput(container.querySelector('input[name="q"]'), "dress");
    changeInput(container.querySelector('input[name="minPrice"]'), "10");
    changeInput(container.querySelector('input[name="maxPrice"]'), "50");
    changeInput(container.querySelector('input[name="size"]'), "M");
    changeInput(container.querySelector('input[name="color"]'), "Blue");

    await submitFilters(container);

    const params = new URLSearchParams(getSearch());

    expect(params.get("q")).toBe("dress");
    expect(params.get("minPrice")).toBe("10");
    expect(params.get("maxPrice")).toBe("50");
    expect(params.get("size")).toBe("M");
    expect(params.get("color")).toBe("Blue");

    unmount();
  });

  it("renders product cards from search results", async () => {
    mockSearchProducts.mockResolvedValue({
      content: [
        {
          id: 1,
          name: "Vestido floral",
          price: 35,
          brand: "Trevo",
          materials: "Algodao",
        },
        {
          id: 2,
          name: "Casaco azul",
          price: 45,
          brand: "Vintage Porto",
          materials: "Ganga",
        },
      ],
    });

    const { container, unmount } = renderProductSearchResults("/products?q=vestido");

    await flushPromises();

    expect(mockSearchProducts).toHaveBeenCalledWith({ q: "vestido" });
    expect(container.textContent).toContain("Vestido floral");
    expect(container.textContent).toContain("Casaco azul");
    expect(container.textContent).toContain("Marca: Trevo");
    expect(container.textContent).toContain("Materiais: Ganga");
    expect(container.querySelector('a[href="/products/1"]')).not.toBeNull();
    expect(container.textContent).toContain("Sem imagem");

    unmount();
  });

  it("shows an empty state when there are no results", async () => {
    mockSearchProducts.mockResolvedValue({ content: [] });

    const { container, unmount } = renderProductSearchResults();

    await flushPromises();

    expect(container.textContent).toContain("Nenhum produto encontrado");

    unmount();
  });

  it("uses browseProducts when shopId is present", async () => {
    mockBrowseProducts.mockResolvedValue([
      {
        id: 3,
        name: "Saia midi",
        price: 28,
      },
    ]);

    const { container, unmount } = renderProductSearchResults("/products?shopId=1");

    await flushPromises();

    expect(mockBrowseProducts).toHaveBeenCalledWith({ shopId: "1" });
    expect(mockSearchProducts).not.toHaveBeenCalled();
    expect(container.textContent).toContain("Saia midi");

    unmount();
  });
});
