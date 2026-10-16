import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ShopProducts from "./ShopProducts";

const { mockBrowseProducts, mockGetShopProfile } = vi.hoisted(() => ({
  mockBrowseProducts: vi.fn(),
  mockGetShopProfile: vi.fn(),
}));

vi.mock("../services/productService", () => ({
  browseProducts: mockBrowseProducts,
}));

vi.mock("../services/shopService", () => ({
  getShopProfile: mockGetShopProfile,
}));

globalThis.React = React;

const shopProfile = {
  id: 1,
  name: "Vintage Porto",
  collections: [
    { id: 2, shopId: 1, name: "Casacos", displayOrder: 2 },
    { id: 1, shopId: 1, name: "Vestidos", displayOrder: 1 },
  ],
};

function renderShopProducts(initialEntry = "/shops/1/products") {
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
          <Route path="/shops/:shopId/products" element={<ShopProducts />} />
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

function buttonByText(container, text) {
  return [...container.querySelectorAll("button")].find(
    (button) => button.textContent === text
  );
}

function changeSelect(select, value) {
  const valueSetter = Object.getOwnPropertyDescriptor(
    HTMLSelectElement.prototype,
    "value"
  ).set;

  act(() => {
    valueSetter.call(select, value);
    select.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

describe("ShopProducts", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetShopProfile.mockResolvedValue(shopProfile);
    mockBrowseProducts.mockResolvedValue({ content: [] });
  });

  it("switches tabs between collections and loads collection products", async () => {
    mockBrowseProducts
      .mockResolvedValueOnce({ content: [{ id: 1, name: "Produto geral", price: 10 }] })
      .mockResolvedValueOnce({
        content: [{ id: 2, name: "Vestido floral", price: 35 }],
      });

    const { container, getSearch, unmount } = renderShopProducts();

    await flushPromises();

    expect(mockGetShopProfile).toHaveBeenCalledWith("1");
    expect(mockBrowseProducts).toHaveBeenCalledWith({
      shopId: "1",
      sort: "createdAt,desc",
    });
    expect(container.textContent).toContain("Produto geral");

    act(() => {
      buttonByText(container, "Vestidos").click();
    });

    await flushPromises();

    const params = new URLSearchParams(getSearch());

    expect(params.get("collectionId")).toBe("1");
    expect(mockBrowseProducts).toHaveBeenLastCalledWith({
      collectionId: "1",
      sort: "createdAt,desc",
    });
    expect(container.textContent).toContain("Vestido floral");

    unmount();
  });

  it("updates URL and reloads products when sorting changes", async () => {
    mockBrowseProducts.mockResolvedValue({ content: [] });

    const { container, getSearch, unmount } = renderShopProducts();

    await flushPromises();

    changeSelect(container.querySelector('select[name="sort"]'), "price,asc");

    await flushPromises();

    const params = new URLSearchParams(getSearch());

    expect(params.get("sort")).toBe("price,asc");
    expect(mockBrowseProducts).toHaveBeenLastCalledWith({
      shopId: "1",
      sort: "price,asc",
    });

    unmount();
  });

  it("renders products in a grid", async () => {
    mockBrowseProducts.mockResolvedValue({
      content: [
        {
          id: 3,
          name: "Casaco azul",
          price: 45,
          brand: "Trevo",
          materials: "Ganga",
        },
      ],
    });

    const { container, unmount } = renderShopProducts();

    await flushPromises();

    expect(container.textContent).toContain("Casaco azul");
    expect(container.textContent).toContain("Marca: Trevo");
    expect(container.textContent).toContain("Materiais: Ganga");
    expect(container.textContent).toContain("Sem imagem");
    expect(container.querySelector('a[href="/products/3"]')).not.toBeNull();

    unmount();
  });

  it("shows an empty state when there are no products", async () => {
    mockBrowseProducts.mockResolvedValue({ content: [] });

    const { container, unmount } = renderShopProducts();

    await flushPromises();

    expect(container.textContent).toContain("Nenhum produto encontrado");

    unmount();
  });

  it("removes collectionId and uses shopId when selecting all products", async () => {
    mockBrowseProducts.mockResolvedValue({ content: [] });

    const { container, getSearch, unmount } = renderShopProducts(
      "/shops/1/products?collectionId=1&sort=name,asc"
    );

    await flushPromises();

    act(() => {
      buttonByText(container, "Todos").click();
    });

    await flushPromises();

    const params = new URLSearchParams(getSearch());

    expect(params.get("collectionId")).toBeNull();
    expect(params.get("sort")).toBe("name,asc");
    expect(mockBrowseProducts).toHaveBeenLastCalledWith({
      shopId: "1",
      sort: "name,asc",
    });

    unmount();
  });
});
