import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { axe } from "vitest-axe";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Cart from "./Cart";
import Checkout from "./Checkout";
import OrderHistory from "./OrderHistory";
import ProductDetail from "./ProductDetail";
import ProductSearch from "./ProductSearch";
import PublicShopProfile from "./PublicShopProfile";
import ReceiptView from "./ReceiptView";
import ShopProducts from "./ShopProducts";

const {
  mockAddCartItem,
  mockBrowseProducts,
  mockCheckoutOrder,
  mockConfirmCardPayment,
  mockCreatePaymentIntent,
  mockGetBuyerOrder,
  mockGetBuyerOrders,
  mockGetCart,
  mockGetProduct,
  mockGetShopProfile,
  mockLoadStripe,
  mockRemoveCartItem,
  mockSearchProducts,
  mockUpdateCartItem,
  mockUploadBuyerPhotos,
  mockUseAuth,
} = vi.hoisted(() => ({
  mockAddCartItem: vi.fn(),
  mockBrowseProducts: vi.fn(),
  mockCheckoutOrder: vi.fn(),
  mockConfirmCardPayment: vi.fn(),
  mockCreatePaymentIntent: vi.fn(),
  mockGetBuyerOrder: vi.fn(),
  mockGetBuyerOrders: vi.fn(),
  mockGetCart: vi.fn(),
  mockGetProduct: vi.fn(),
  mockGetShopProfile: vi.fn(),
  mockLoadStripe: vi.fn(),
  mockRemoveCartItem: vi.fn(),
  mockSearchProducts: vi.fn(),
  mockUpdateCartItem: vi.fn(),
  mockUploadBuyerPhotos: vi.fn(),
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

vi.mock("../services/orderService", () => ({
  checkoutOrder: mockCheckoutOrder,
  getBuyerOrder: mockGetBuyerOrder,
  getBuyerOrders: mockGetBuyerOrders,
}));

vi.mock("../services/paymentService", () => ({
  createPaymentIntent: mockCreatePaymentIntent,
}));

vi.mock("../services/productService", () => ({
  browseProducts: mockBrowseProducts,
  getProduct: mockGetProduct,
  searchProducts: mockSearchProducts,
}));

vi.mock("../services/shopService", () => ({
  getShopProfile: mockGetShopProfile,
}));

vi.mock("../services/userPhotoService", () => ({
  uploadBuyerPhotos: mockUploadBuyerPhotos,
}));

vi.mock("../lib/checkoutRedirect", () => ({
  redirectTo: vi.fn(),
}));

vi.mock("@stripe/stripe-js", () => ({
  loadStripe: mockLoadStripe,
}));

vi.mock("@stripe/react-stripe-js", () => ({
  CardElement: () => <div>Stripe card input</div>,
  Elements: ({ children }) => children,
  useElements: () => ({
    getElement: () => ({ elementType: "card" }),
  }),
  useStripe: () => ({
    confirmCardPayment: mockConfirmCardPayment,
  }),
}));

vi.mock("../components/ReviewsSection", () => ({
  default: ({ shopId }) => <section data-testid="reviews-section">Reviews {shopId}</section>,
}));

globalThis.React = React;

const productsPage = {
  content: [
    {
      id: 1,
      name: "Camisa Linho",
      price: 35,
      brand: "Trevo",
      materials: "Linho",
      images: [{ imageUrl: "https://example.test/camisa.jpg" }],
    },
  ],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
};

const product = {
  id: 1,
  shopId: 10,
  name: "Camisa Linho",
  description: "Camisa leve em linho.",
  price: 35,
  brand: "Trevo",
  materials: "Linho 100%",
  status: "ACTIVE",
  createdAt: "2026-01-01T00:00:00",
  images: [{ id: 1, imageUrl: "https://example.test/front.jpg", alt: "Frente" }],
  variants: [{ id: 7, productId: 1, size: "M", color: "Blue", stock: 3 }],
};

const shopProfile = {
  id: 1,
  name: "Vintage Porto",
  description: "Roupa vintage curada.",
  logoUrl: "https://example.test/logo.png",
  bannerUrl: "https://example.test/banner.png",
  averageRating: 4.5,
  collections: [
    { id: 2, shopId: 1, name: "Casacos", displayOrder: 2 },
    { id: 1, shopId: 1, name: "Vestidos", displayOrder: 1 },
  ],
};

const cart = {
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
  ],
  grandTotal: 59.98,
};

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

const orderPage = {
  totalElements: 1,
  totalPages: 1,
  page: 0,
  size: 20,
  orders: [order],
};

function renderRoute(initialEntry, path, element) {
  const container = document.createElement("div");
  const root = createRoot(container);

  document.body.appendChild(container);

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
        container.remove();
      }),
  };
}

async function flushPromises() {
  await act(async () => {
    await Promise.resolve();
  });
}

async function expectNoCriticalOrSeriousViolations(container) {
  const results = await axe(container);
  const blockingViolations = results.violations.filter((violation) =>
    ["critical", "serious"].includes(violation.impact)
  );

  expect(blockingViolations).toEqual([]);
}

describe("frontend accessibility", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
    vi.stubEnv("VITE_STRIPE_PUBLISHABLE_KEY", "pk_test_mock");

    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      role: "BUYER",
    });
    mockSearchProducts.mockResolvedValue(productsPage);
    mockBrowseProducts.mockResolvedValue(productsPage);
    mockGetProduct.mockResolvedValue(product);
    mockGetShopProfile.mockResolvedValue(shopProfile);
    mockGetCart.mockResolvedValue(cart);
    mockRemoveCartItem.mockResolvedValue(undefined);
    mockUpdateCartItem.mockResolvedValue(cart);
    mockCheckoutOrder.mockResolvedValue([]);
    mockCreatePaymentIntent.mockResolvedValue({
      id: "pi_123",
      clientSecret: "pi_123_secret_abc",
      amount: 5998,
      currency: "eur",
    });
    mockConfirmCardPayment.mockResolvedValue({
      paymentIntent: {
        id: "pi_123",
        status: "succeeded",
      },
    });
    mockLoadStripe.mockReturnValue({ stripe: "promise" });
    mockAddCartItem.mockResolvedValue({});
    mockUploadBuyerPhotos.mockResolvedValue({ photos: [] });
    mockGetBuyerOrders.mockResolvedValue(orderPage);
    mockGetBuyerOrder.mockResolvedValue(order);

    HTMLCanvasElement.prototype.getContext = vi.fn(() => ({
      measureText: () => ({ width: 0 }),
    }));
    const getComputedStyle = window.getComputedStyle.bind(window);
    window.getComputedStyle = vi.fn((element, pseudoElement) => {
      if (pseudoElement) {
        return {
          content: "none",
          getPropertyValue: () => "",
        };
      }

      return getComputedStyle(element);
    });
    globalThis.URL.createObjectURL = vi.fn((file) => `blob:${file.name}`);
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it("has no critical or serious violations on ProductSearch", async () => {
    const { container, unmount } = renderRoute("/products", "/products", <ProductSearch />);

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });

  it("has no critical or serious violations on ProductDetail", async () => {
    const { container, unmount } = renderRoute(
      "/products/1",
      "/products/:productId",
      <ProductDetail />
    );

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });

  it("has no critical or serious violations on PublicShopProfile", async () => {
    const { container, unmount } = renderRoute(
      "/shops/1",
      "/shops/:shopId",
      <PublicShopProfile />
    );

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });

  it("has no critical or serious violations on ShopProducts", async () => {
    const { container, unmount } = renderRoute(
      "/shops/1/products",
      "/shops/:shopId/products",
      <ShopProducts />
    );

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });

  it("has no critical or serious violations on Cart", async () => {
    const { container, unmount } = renderRoute("/cart", "/cart", <Cart />);

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });

  it("has no critical or serious violations on Checkout", async () => {
    const { container, unmount } = renderRoute("/checkout", "/checkout", <Checkout />);

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });

  it("has no critical or serious violations on OrderHistory", async () => {
    const { container, unmount } = renderRoute("/orders", "/orders", <OrderHistory />);

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });

  it("has no critical or serious violations on ReceiptView", async () => {
    const { container, unmount } = renderRoute(
      "/orders/42/receipt",
      "/orders/:orderId/receipt",
      <ReceiptView />
    );

    await flushPromises();
    await expectNoCriticalOrSeriousViolations(container);

    unmount();
  });
});
