import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Checkout from "./Checkout";

const {
  mockCheckoutOrder,
  mockConfirmCardPayment,
  mockCreatePaymentIntent,
  mockGetCart,
  mockLoadStripe,
  mockRedirectTo,
} = vi.hoisted(() => ({
  mockCheckoutOrder: vi.fn(),
  mockConfirmCardPayment: vi.fn(),
  mockCreatePaymentIntent: vi.fn(),
  mockGetCart: vi.fn(),
  mockLoadStripe: vi.fn(),
  mockRedirectTo: vi.fn(),
}));

vi.mock("../services/cartService", () => ({
  getCart: mockGetCart,
}));

vi.mock("../services/orderService", () => ({
  checkoutOrder: mockCheckoutOrder,
}));

vi.mock("../lib/checkoutRedirect", () => ({
  redirectTo: mockRedirectTo,
}));

vi.mock("../services/paymentService", () => ({
  createPaymentIntent: mockCreatePaymentIntent,
}));

vi.mock("@stripe/stripe-js", () => ({
  loadStripe: mockLoadStripe,
}));

vi.mock("@stripe/react-stripe-js", () => ({
  CardElement: () => "Stripe card input",
  Elements: ({ children }) => children,
  useElements: () => ({
    getElement: () => ({ elementType: "card" }),
  }),
  useStripe: () => ({
    confirmCardPayment: mockConfirmCardPayment,
  }),
}));

globalThis.React = React;

const cartResponse = {
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
          image: null,
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

const singleShopCartResponse = {
  shops: [cartResponse.shops[0]],
  grandTotal: 59.98,
};

const orderResponse = [
  {
    id: 10,
    shopId: 301,
    shopName: "North Shop",
    status: "PLACED",
    totalAmount: 59.98,
    paymentMethod: "MBWAY",
    items: [],
  },
  {
    id: 11,
    shopId: 302,
    shopName: "South Shop",
    status: "PLACED",
    totalAmount: 40,
    paymentMethod: "MBWAY",
    items: [],
  },
];

const stripeOrderResponse = [
  {
    id: 20,
    shopId: 301,
    shopName: "North Shop",
    status: "PENDING_PAYMENT",
    totalAmount: 59.98,
    paymentMethod: "STRIPE",
    items: [],
  },
];

function renderCheckout() {
  const container = document.createElement("div");
  document.body.appendChild(container);
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/checkout"]}>
        <Routes>
          <Route path="/checkout" element={<Checkout />} />
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

async function changeSelect(select, value) {
  const valueSetter = Object.getOwnPropertyDescriptor(
    select.constructor.prototype,
    "value"
  ).set;

  await act(async () => {
    valueSetter.call(select, value);
    select.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

async function changeInput(input, value) {
  const valueSetter = Object.getOwnPropertyDescriptor(
    input.constructor.prototype,
    "value"
  ).set;

  await act(async () => {
    valueSetter.call(input, value);
    input.dispatchEvent(new Event("input", { bubbles: true }));
    input.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

function confirmButton(container) {
  return [...container.querySelectorAll("button")].find(
    (button) => button.textContent === "Confirmar encomenda"
  );
}

function buttonByText(container, text) {
  return [...container.querySelectorAll("button")].find(
    (button) => button.textContent === text
  );
}

describe("Checkout", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv("VITE_STRIPE_PUBLISHABLE_KEY", "pk_test_mock");
    mockGetCart.mockResolvedValue(cartResponse);
    mockCheckoutOrder.mockResolvedValue(orderResponse);
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
  });

  it("shows order summary", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();

    expect(container.textContent).toContain("North Shop");
    expect(container.textContent).toContain("Linen Shirt");
    expect(container.textContent).toContain("South Shop");
    expect(container.textContent).toContain("Denim Jacket");
    expect(container.textContent).toContain("59,98");
    expect(container.textContent).toContain("99,98");

    unmount();
  });

  it("payment method selector renders PayPal and MB WAY", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();

    const optionTexts = [...container.querySelectorAll("option")].map(
      (option) => option.textContent
    );

    expect(optionTexts).toContain("PayPal");
    expect(optionTexts).toContain("MB WAY");

    unmount();
  });

  it("shows stock conflict errors", async () => {
    mockGetCart.mockResolvedValue(singleShopCartResponse);
    mockCheckoutOrder.mockRejectedValue({
      response: {
        data: {
          detail: "Stock conflict for variants: [101]",
        },
      },
    });
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "STRIPE");
    await click(confirmButton(container));

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Stock conflict for variants: [101]"
    );

    unmount();
  });

  it("shows an error when the cart cannot be loaded", async () => {
    mockGetCart.mockRejectedValue({
      response: {
        data: {
          message: "Não foi possível carregar o carrinho",
        },
      },
    });
    const { container, unmount } = renderCheckout();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Não foi possível carregar o carrinho"
    );
    expect(confirmButton(container)).toBeUndefined();

    unmount();
  });

  it("shows an empty state when the cart has no items", async () => {
    mockGetCart.mockResolvedValue({ shops: [], grandTotal: 0 });
    const { container, unmount } = renderCheckout();

    await flushPromises();

    expect(container.textContent).toContain("O carrinho está vazio.");
    expect(confirmButton(container)).toBeUndefined();

    unmount();
  });

  it("shows a PayPal payment button when PayPal is selected", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "PAYPAL");

    expect(buttonByText(container, "Pagar com PayPal")).not.toBeUndefined();

    unmount();
  });

  it("clicking PayPal checks out with PayPal and triggers mock redirect", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "PAYPAL");
    await click(buttonByText(container, "Pagar com PayPal"));
    await flushPromises();

    expect(mockCheckoutOrder).toHaveBeenCalledWith("PAYPAL");
    expect(mockRedirectTo).toHaveBeenCalledWith("/checkout/paypal/mock");

    unmount();
  });

  it("shows a phone input when MB WAY is selected", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();

    expect(container.textContent).toContain("Número de telemóvel");
    expect(container.querySelector('input[name="mbWayPhone"]')).not.toBeNull();

    unmount();
  });

  it("disables the MB WAY button by default without a valid phone", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();

    expect(buttonByText(container, "Iniciar MB WAY").disabled).toBe(true);

    unmount();
  });

  it("does not submit MB WAY with an invalid phone", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeInput(container.querySelector('input[name="mbWayPhone"]'), "812345678");

    expect(buttonByText(container, "Iniciar MB WAY").disabled).toBe(true);
    expect(mockCheckoutOrder).not.toHaveBeenCalled();

    unmount();
  });

  it("submits MB WAY with a valid phone and shows the started message", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeInput(container.querySelector('input[name="mbWayPhone"]'), "+351 923 456 789");
    await click(buttonByText(container, "Iniciar MB WAY"));
    await flushPromises();

    expect(mockCheckoutOrder).toHaveBeenCalledWith("MBWAY", {
      phone: "+351923456789",
    });
    expect(container.querySelector('[role="status"]').textContent).toContain(
      "Pedido MB WAY iniciado para o número +351923456789"
    );

    unmount();
  });

  it("mounts Stripe Elements after creating a Stripe checkout order", async () => {
    mockGetCart.mockResolvedValue(singleShopCartResponse);
    mockCheckoutOrder.mockResolvedValue(stripeOrderResponse);
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "STRIPE");
    await click(confirmButton(container));
    await flushPromises();

    expect(mockCheckoutOrder).toHaveBeenCalledWith("STRIPE");
    expect(container.textContent).toContain("Stripe card input");
    expect(container.textContent).toContain("Confirmar pagamento Stripe");

    unmount();
  });

  it("creates a PaymentIntent and confirms the Stripe payment", async () => {
    mockGetCart.mockResolvedValue(singleShopCartResponse);
    mockCheckoutOrder.mockResolvedValue(stripeOrderResponse);
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "STRIPE");
    await click(confirmButton(container));
    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Confirmar pagamento Stripe"
      )
    );
    await flushPromises();

    expect(mockCreatePaymentIntent).toHaveBeenCalledWith(20);
    expect(mockConfirmCardPayment).toHaveBeenCalledWith("pi_123_secret_abc", {
      payment_method: {
        card: { elementType: "card" },
      },
    });

    unmount();
  });

  it("shows the confirmation state after a successful Stripe payment", async () => {
    mockGetCart.mockResolvedValue(singleShopCartResponse);
    mockCheckoutOrder.mockResolvedValue(stripeOrderResponse);
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "STRIPE");
    await click(confirmButton(container));
    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Confirmar pagamento Stripe"
      )
    );
    await flushPromises();

    expect(container.textContent).toContain("Encomenda confirmada");
    expect(container.textContent).toContain("20 - North Shop");
    expect(container.textContent).toContain("Estado: PENDING_PAYMENT");

    unmount();
  });

  it("shows a friendly message when Stripe PaymentIntent is blocked by backend status", async () => {
    mockGetCart.mockResolvedValue(singleShopCartResponse);
    mockCheckoutOrder.mockResolvedValue(stripeOrderResponse);
    mockCreatePaymentIntent.mockRejectedValue({
      response: {
        status: 409,
        data: {
          detail: "Order is not awaiting payment",
        },
      },
    });
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "STRIPE");
    await click(confirmButton(container));
    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Confirmar pagamento Stripe"
      )
    );
    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "O pagamento Stripe ainda não está disponível para esta encomenda."
    );

    unmount();
  });

  it("does not create Stripe checkout orders for multi-shop carts", async () => {
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "STRIPE");
    await click(confirmButton(container));

    expect(mockCheckoutOrder).not.toHaveBeenCalled();
    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "O pagamento Stripe só suporta uma loja por finalização de compra neste momento."
    );

    unmount();
  });

  it("shows a clear message when Stripe is not configured", async () => {
    mockGetCart.mockResolvedValue(singleShopCartResponse);
    vi.stubEnv("VITE_STRIPE_PUBLISHABLE_KEY", "");
    mockLoadStripe.mockReturnValue(null);
    const { container, unmount } = renderCheckout();

    await flushPromises();
    await changeSelect(container.querySelector('select[name="paymentMethod"]'), "STRIPE");

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Stripe não está configurado neste ambiente."
    );
    expect(confirmButton(container).disabled).toBe(true);

    unmount();
  });
});
