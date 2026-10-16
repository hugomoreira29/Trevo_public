import React, { act } from "react";
import { createRoot } from "react-dom/client";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductSearch from "./ProductSearch";

const { mockBrowseProducts, mockSearchProducts } = vi.hoisted(() => ({
  mockBrowseProducts: vi.fn(),
  mockSearchProducts: vi.fn(),
}));

vi.mock("../services/productService", () => ({
  browseProducts: mockBrowseProducts,
  searchProducts: mockSearchProducts,
}));

globalThis.React = React;

function LocationProbe() {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <div>
      <span data-testid="location">{location.pathname + location.search}</span>
      <button type="button" onClick={() => navigate(-1)}>
        Voltar
      </button>
    </div>
  );
}

function renderProductSearch(initialEntries = ["/products"], initialIndex = 0) {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={initialEntries} initialIndex={initialIndex}>
        <LocationProbe />
        <Routes>
          <Route
            path="/products"
            element={<ProductSearch />}
          />
          <Route path="*" element={null} />
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

function productsPage(products = [], overrides = {}) {
  return {
    content: products,
    totalElements: products.length,
    totalPages: 1,
    number: 0,
    size: 20,
    ...overrides,
  };
}

async function advanceDebounce(ms = 300) {
  await act(async () => {
    vi.advanceTimersByTime(ms);
  });
  await flushPromises();
}

describe("ProductSearch", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    mockSearchProducts.mockResolvedValue(productsPage());
    mockBrowseProducts.mockResolvedValue(productsPage());
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("triggers search after a 300ms debounce when typing", async () => {
    const { container, unmount } = renderProductSearch();

    await flushPromises();
    mockSearchProducts.mockClear();

    changeInput(container.querySelector('input[name="q"]'), "dress");
    await advanceDebounce(299);

    expect(mockSearchProducts).not.toHaveBeenCalled();
    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/products");

    await advanceDebounce(1);

    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/products?q=dress");
    expect(mockSearchProducts).toHaveBeenCalledWith(
      expect.objectContaining({
        q: "dress",
      })
    );

    unmount();
  });

  it("replaces filter edits in browser history", async () => {
    const { container, unmount } = renderProductSearch(["/landing", "/products?q=old"], 1);

    await flushPromises();

    changeInput(container.querySelector('input[name="q"]'), "dress");
    await advanceDebounce();

    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/products?q=dress");

    act(() => {
      [...container.querySelectorAll("button")]
        .find((button) => button.textContent === "Voltar")
        .click();
    });
    await flushPromises();

    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/landing");

    unmount();
  });

  it("persists filters in the URL and sends them to product search", async () => {
    const { container, unmount } = renderProductSearch();

    await flushPromises();
    mockSearchProducts.mockClear();

    changeInput(container.querySelector('input[name="minPrice"]'), "10");
    changeInput(container.querySelector('input[name="maxPrice"]'), "50");
    changeInput(container.querySelector('input[name="size"]'), "M");
    changeInput(container.querySelector('input[name="color"]'), "Azul");

    await advanceDebounce();

    const location = container.querySelector('[data-testid="location"]').textContent;
    expect(location).toContain("minPrice=10");
    expect(location).toContain("maxPrice=50");
    expect(location).toContain("size=M");
    expect(location).toContain("color=Azul");
    expect(mockSearchProducts).toHaveBeenCalledWith(
      expect.objectContaining({
        minPrice: "10",
        maxPrice: "50",
        size: "M",
        color: "Azul",
      })
    );

    unmount();
  });

  it("restores URL filter state when navigating back", async () => {
    const { container, unmount } = renderProductSearch(
      ["/products?q=dress&size=M", "/products?q=coat&color=Blue"],
      1
    );

    await flushPromises();
    expect(container.querySelector('input[name="q"]').value).toBe("coat");
    expect(container.querySelector('input[name="color"]').value).toBe("Blue");

    act(() => {
      [...container.querySelectorAll("button")]
        .find((button) => button.textContent === "Voltar")
        .click();
    });
    await flushPromises();

    expect(container.querySelector('[data-testid="location"]').textContent).toBe(
      "/products?q=dress&size=M"
    );
    expect(container.querySelector('input[name="q"]').value).toBe("dress");
    expect(container.querySelector('input[name="size"]').value).toBe("M");
    expect(container.querySelector('input[name="color"]').value).toBe("");
    expect(mockSearchProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({
        q: "dress",
        size: "M",
      })
    );

    unmount();
  });

  it("uses browse endpoint for shop scoped product links without search filters", async () => {
    const { container, unmount } = renderProductSearch(["/products?shopId=7"]);

    await flushPromises();

    expect(mockBrowseProducts).toHaveBeenCalledWith(
      expect.objectContaining({
        shopId: "7",
      })
    );
    expect(mockSearchProducts).not.toHaveBeenCalled();
    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/products?shopId=7");

    unmount();
  });

  it("renders pagination controls and requests the selected page", async () => {
    mockSearchProducts.mockResolvedValue(
      productsPage(
        [
          {
            id: 1,
            name: "Vestido azul",
            price: "25.00",
          },
        ],
        {
          totalElements: 45,
          totalPages: 3,
          number: 0,
        }
      )
    );

    const { container, unmount } = renderProductSearch(["/products?q=vestido"]);

    await flushPromises();

    expect(container.textContent).toContain("Página 1 de 3");
    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/products?q=vestido");

    mockSearchProducts.mockResolvedValue(
      productsPage(
        [
          {
            id: 2,
            name: "Vestido verde",
            price: "30.00",
          },
        ],
        {
          totalElements: 45,
          totalPages: 3,
          number: 1,
        }
      )
    );

    act(() => {
      [...container.querySelectorAll("button")]
        .find((button) => button.textContent === "Seguinte")
        .click();
    });
    await flushPromises();

    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/products?q=vestido&page=1");
    expect(mockSearchProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({
        q: "vestido",
        page: "1",
      })
    );
    expect(container.textContent).toContain("Página 2 de 3");

    unmount();
  });

  it("clears the page param when filters change", async () => {
    const { container, unmount } = renderProductSearch(["/products?q=vestido&page=2"]);

    await flushPromises();
    mockSearchProducts.mockClear();

    changeInput(container.querySelector('input[name="q"]'), "casaco");
    await advanceDebounce();

    expect(container.querySelector('[data-testid="location"]').textContent).toBe("/products?q=casaco");
    expect(mockSearchProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({
        q: "casaco",
        page: "",
      })
    );

    unmount();
  });
});
