import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Cart from "./Cart";
import CreateShop from "./CreateShop";
import ProductDetail from "./ProductDetail";
import ProductForm from "./ProductForm";
import ProductSearch from "./ProductSearch";
import PublicShopProfile from "./PublicShopProfile";

const {
  mockAddCartItem,
  mockAssignProductCollections,
  mockBrowseProducts,
  mockCreateProduct,
  mockCreateProductVariant,
  mockCreateShop,
  mockDeleteProductVariant,
  mockGetCart,
  mockGetProduct,
  mockGetShopDashboard,
  mockGetShopProfile,
  mockRemoveCartItem,
  mockSearchProducts,
  mockUpdateCartItem,
  mockUpdateProduct,
  mockUpdateProductVariant,
  mockUploadBuyerPhotos,
  mockUploadImage,
  mockUseAuth,
} = vi.hoisted(() => ({
  mockAddCartItem: vi.fn(),
  mockAssignProductCollections: vi.fn(),
  mockBrowseProducts: vi.fn(),
  mockCreateProduct: vi.fn(),
  mockCreateProductVariant: vi.fn(),
  mockCreateShop: vi.fn(),
  mockDeleteProductVariant: vi.fn(),
  mockGetCart: vi.fn(),
  mockGetProduct: vi.fn(),
  mockGetShopDashboard: vi.fn(),
  mockGetShopProfile: vi.fn(),
  mockRemoveCartItem: vi.fn(),
  mockSearchProducts: vi.fn(),
  mockUpdateCartItem: vi.fn(),
  mockUpdateProduct: vi.fn(),
  mockUpdateProductVariant: vi.fn(),
  mockUploadBuyerPhotos: vi.fn(),
  mockUploadImage: vi.fn(),
  mockUseAuth: vi.fn(),
}));

vi.mock("../hooks/useAuth", () => ({
  useAuth: mockUseAuth,
}));

vi.mock("../services/cartService", () => ({
  addCartItem: mockAddCartItem,
  getCart: mockGetCart,
  removeCartItem: mockRemoveCartItem,
  updateCartItem: mockUpdateCartItem,
}));

vi.mock("../services/imageUploadService", () => ({
  uploadImage: mockUploadImage,
}));

vi.mock("../services/productService", () => ({
  assignProductCollections: mockAssignProductCollections,
  browseProducts: mockBrowseProducts,
  createProduct: mockCreateProduct,
  createProductVariant: mockCreateProductVariant,
  deleteProductVariant: mockDeleteProductVariant,
  getProduct: mockGetProduct,
  searchProducts: mockSearchProducts,
  updateProduct: mockUpdateProduct,
  updateProductVariant: mockUpdateProductVariant,
}));

vi.mock("../services/shopService", () => ({
  createShop: mockCreateShop,
  getShopDashboard: mockGetShopDashboard,
  getShopProfile: mockGetShopProfile,
}));

vi.mock("../services/userPhotoService", () => ({
  uploadBuyerPhotos: mockUploadBuyerPhotos,
}));

vi.mock("../components/ReviewForm", () => ({
  default: () => null,
}));

vi.mock("../components/ReviewsSection", () => ({
  default: () => null,
}));

globalThis.React = React;

function renderRoute(initialEntry, path, element) {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path={path} element={element} />
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

function renderElement(element) {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(element);
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

function changeFiles(input, files) {
  Object.defineProperty(input, "files", {
    configurable: true,
    value: files,
  });

  act(() => {
    input.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

function expectEveryImageHasNonEmptyAlt(container) {
  const images = [...container.querySelectorAll("img")];

  expect(images.length).toBeGreaterThan(0);

  images.forEach((image) => {
    expect(image.hasAttribute("alt")).toBe(true);
    expect(image.getAttribute("alt").trim().length).toBeGreaterThan(0);
  });
}

describe("image alt text", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      role: "BUYER",
    });
    mockAddCartItem.mockResolvedValue({});
    mockRemoveCartItem.mockResolvedValue(undefined);
    mockUpdateCartItem.mockResolvedValue({ shops: [], grandTotal: 0 });
    mockUploadBuyerPhotos.mockResolvedValue({ photos: [] });
    mockGetShopDashboard.mockResolvedValue({ shop: { id: 1 } });
    mockGetShopProfile.mockResolvedValue({
      id: 1,
      name: "",
      description: "Moda circular",
      logoUrl: "https://example.test/logo.png",
      bannerUrl: "https://example.test/banner.png",
      averageRating: null,
      collections: [],
    });
    mockBrowseProducts.mockResolvedValue({ content: [] });
    mockSearchProducts.mockResolvedValue({
      content: [
        {
          id: 1,
          name: "Vestido Azul",
          price: 35,
          imageUrl: "https://example.test/product.jpg",
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    });

    globalThis.URL.createObjectURL = vi.fn((file) => `blob:${file.name}`);
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it("renders Cart images with non-empty alt text", async () => {
    mockGetCart.mockResolvedValue({
      shops: [
        {
          shopId: 1,
          shopName: "Loja",
          items: [
            {
              cartItemId: 1,
              variantId: 10,
              productId: 20,
              productName: "",
              image: "https://example.test/cart-product.jpg",
              size: "M",
              color: "Azul",
              quantity: 1,
              unitPrice: 20,
              subtotal: 20,
            },
          ],
          subtotal: 20,
        },
      ],
      grandTotal: 20,
    });
    const { container, unmount } = renderRoute("/cart", "/cart", <Cart />);

    await flushPromises();

    expectEveryImageHasNonEmptyAlt(container);
    expect(container.querySelector("img").getAttribute("alt")).toBe(
      "Imagem do produto sem nome"
    );

    unmount();
  });

  it("renders CreateShop preview images with non-empty alt text", () => {
    const { container, unmount } = renderElement(<CreateShop />);

    changeFiles(container.querySelector('input[name="logo"]'), [
      new File(["logo"], "logo.png", { type: "image/png" }),
    ]);
    changeFiles(container.querySelector('input[name="banner"]'), [
      new File(["banner"], "banner.webp", { type: "image/webp" }),
    ]);

    expectEveryImageHasNonEmptyAlt(container);

    unmount();
  });

  it("renders ProductDetail images with non-empty alt text", async () => {
    mockGetProduct.mockResolvedValue({
      id: 1,
      shopId: 10,
      name: "",
      description: "Produto sem nome.",
      price: 35,
      brand: "Trevo",
      materials: "Linho",
      status: "ACTIVE",
      images: [{ id: 1, imageUrl: "https://example.test/front.jpg", alt: "" }],
      variants: [{ id: 7, productId: 1, size: "M", color: "Azul", stock: 3 }],
    });
    const { container, unmount } = renderRoute(
      "/products/1",
      "/products/:productId",
      <ProductDetail />
    );

    await flushPromises();

    expectEveryImageHasNonEmptyAlt(container);
    expect(container.querySelector("img").getAttribute("alt")).toBe(
      "Imagem do produto sem nome"
    );

    unmount();
  });

  it("renders ProductForm preview images with non-empty alt text", async () => {
    const { container, unmount } = renderRoute(
      "/products/new",
      "/products/new",
      <ProductForm mode="create" />
    );

    await flushPromises();
    changeFiles(container.querySelector('input[name="images"]'), [
      new File(["image"], "product-1.png", { type: "image/png" }),
      new File(["image"], "product-2.png", { type: "image/png" }),
    ]);

    expectEveryImageHasNonEmptyAlt(container);

    unmount();
  });

  it("renders ProductSearch images with non-empty alt text", async () => {
    const { container, unmount } = renderRoute(
      "/products",
      "/products",
      <ProductSearch />
    );

    await flushPromises();

    expectEveryImageHasNonEmptyAlt(container);
    expect(container.querySelector("img").getAttribute("alt")).toBe(
      "Imagem do produto Vestido Azul"
    );

    unmount();
  });

  it("renders PublicShopProfile images with non-empty alt text", async () => {
    const { container, unmount } = renderRoute(
      "/shops/1",
      "/shops/:shopId",
      <PublicShopProfile />
    );

    await flushPromises();

    expectEveryImageHasNonEmptyAlt(container);
    expect([...container.querySelectorAll("img")].map((image) => image.getAttribute("alt"))).toEqual([
      "Banner da loja sem nome",
      "Logo da loja sem nome",
    ]);

    unmount();
  });
});
