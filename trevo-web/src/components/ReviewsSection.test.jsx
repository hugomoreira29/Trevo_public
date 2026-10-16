import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ReviewsSection from "./ReviewsSection";

const { mockGetShopReviews, mockRespondToReview, mockAuthState } = vi.hoisted(() => ({
  mockGetShopReviews: vi.fn(),
  mockRespondToReview: vi.fn(),
  mockAuthState: {
    isAuthenticated: false,
    role: null,
  },
}));

vi.mock("../services/reviewService", () => ({
  getShopReviews: mockGetShopReviews,
  respondToReview: mockRespondToReview,
}));

vi.mock("../hooks/useAuth", () => ({
  useAuth: () => mockAuthState,
}));

globalThis.React = React;

const firstPage = {
  averageRating: 4.5,
  totalElements: 2,
  totalPages: 2,
  page: 0,
  size: 5,
  reviews: [
    {
      id: 10,
      buyerName: "Ana Silva",
      rating: 5,
      comment: "Excelente atendimento.",
      createdAt: "2026-05-01T10:00:00Z",
    },
  ],
};

function renderReviewsSection() {
  const container = document.createElement("div");
  document.body.appendChild(container);
  const root = createRoot(container);

  act(() => {
    root.render(<ReviewsSection shopId={7} />);
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

function setAuthState(authState) {
  mockAuthState.isAuthenticated = authState.isAuthenticated;
  mockAuthState.role = authState.role;
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

async function changeField(field, value) {
  await act(async () => {
    const valueSetter = Object.getOwnPropertyDescriptor(
      window.HTMLTextAreaElement.prototype,
      "value"
    ).set;

    valueSetter.call(field, value);
    field.dispatchEvent(new InputEvent("input", { bubbles: true }));
    field.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

function buttonByText(container, text) {
  return [...container.querySelectorAll("button")].find(
    (button) => button.textContent === text
  );
}

describe("ReviewsSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setAuthState({ isAuthenticated: false, role: null });
    mockGetShopReviews.mockResolvedValue(firstPage);
    mockRespondToReview.mockResolvedValue({
      id: 20,
      response: "Obrigado pelo feedback.",
    });
  });

  it("shows the average rating with star icons", async () => {
    const { container, unmount } = renderReviewsSection();

    await flushPromises();

    expect(mockGetShopReviews).toHaveBeenCalledWith(7, { page: 0, size: 5 });
    expect(container.textContent).toContain("Média:");
    expect(container.textContent).toContain("4.5");
    expect(container.textContent).toContain("★★★★★");
    expect(container.querySelector('[aria-label="4.5 estrelas"]')).not.toBeNull();

    unmount();
  });

  it("shows reviews with buyer name, date, and comment", async () => {
    const { container, unmount } = renderReviewsSection();

    await flushPromises();

    expect(container.textContent).toContain("Ana Silva");
    expect(container.textContent).toContain("01/05/26");
    expect(container.textContent).toContain("Excelente atendimento.");

    unmount();
  });

  it("loads more reviews and appends them", async () => {
    mockGetShopReviews
      .mockResolvedValueOnce(firstPage)
      .mockResolvedValueOnce({
        averageRating: 4.5,
        totalElements: 2,
        totalPages: 2,
        page: 1,
        size: 5,
        reviews: [
          {
            id: 11,
            buyerName: "Bruno Costa",
            rating: 4,
            comment: "Boa experiência.",
            createdAt: "2026-05-02T10:00:00Z",
          },
        ],
      });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();
    await click(buttonByText(container, "Carregar mais"));
    await flushPromises();

    expect(mockGetShopReviews).toHaveBeenLastCalledWith(7, { page: 1, size: 5 });
    expect(container.textContent).toContain("Ana Silva");
    expect(container.textContent).toContain("Bruno Costa");
    expect(buttonByText(container, "Carregar mais")).toBeUndefined();

    unmount();
  });

  it("shows an empty state when there are no reviews", async () => {
    mockGetShopReviews.mockResolvedValue({
      averageRating: null,
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 5,
      reviews: [],
    });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();

    expect(container.textContent).toContain("Sem avaliações");

    unmount();
  });

  it("shows an error when reviews cannot be loaded", async () => {
    mockGetShopReviews.mockRejectedValue({
      response: {
        data: {
          detail: "Shop not found",
        },
      },
    });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Shop not found"
    );

    unmount();
  });

  it("retries after an initial error and replaces the list", async () => {
    mockGetShopReviews
      .mockRejectedValueOnce(new Error("Network error"))
      .mockResolvedValueOnce(firstPage);
    const { container, unmount } = renderReviewsSection();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Não foi possível carregar avaliações"
    );

    await click(buttonByText(container, "Tentar novamente"));
    await flushPromises();

    expect(mockGetShopReviews).toHaveBeenLastCalledWith(7, { page: 0, size: 5 });
    expect(container.textContent).toContain("Ana Silva");
    expect(container.querySelector('[role="alert"]')).toBeNull();

    unmount();
  });

  it("opens an inline textarea when a shop owner clicks Responder", async () => {
    setAuthState({ isAuthenticated: true, role: "SHOP_OWNER" });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();
    await click(buttonByText(container, "Responder"));

    expect(container.querySelector("textarea")).not.toBeNull();
    expect(buttonByText(container, "Enviar resposta")).not.toBeUndefined();
    expect(buttonByText(container, "Cancelar")).not.toBeUndefined();

    unmount();
  });

  it("submits a response and shows it after success", async () => {
    setAuthState({ isAuthenticated: true, role: "SHOP_OWNER" });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();
    await click(buttonByText(container, "Responder"));
    await changeField(container.querySelector("textarea"), "Obrigado pela avaliação.");
    await click(buttonByText(container, "Enviar resposta"));
    await flushPromises();

    expect(mockRespondToReview).toHaveBeenCalledWith(
      10,
      "Obrigado pela avaliação."
    );
    expect(container.textContent).toContain("Resposta da loja");
    expect(container.textContent).toContain("Obrigado pelo feedback.");
    expect(buttonByText(container, "Responder")).toBeUndefined();
    expect(container.querySelector("textarea")).toBeNull();

    unmount();
  });

  it("disables response submit while the textarea is blank", async () => {
    setAuthState({ isAuthenticated: true, role: "SHOP_OWNER" });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();
    await click(buttonByText(container, "Responder"));
    await changeField(container.querySelector("textarea"), "   ");

    expect(buttonByText(container, "Enviar resposta").disabled).toBe(true);

    unmount();
  });

  it("does not show Responder for a buyer or visitor", async () => {
    setAuthState({ isAuthenticated: true, role: "BUYER" });
    const buyerRender = renderReviewsSection();

    await flushPromises();

    expect(buttonByText(buyerRender.container, "Responder")).toBeUndefined();
    buyerRender.unmount();

    setAuthState({ isAuthenticated: false, role: null });
    const visitorRender = renderReviewsSection();

    await flushPromises();

    expect(buttonByText(visitorRender.container, "Responder")).toBeUndefined();
    visitorRender.unmount();
  });

  it("does not show Responder when the review already has a response", async () => {
    setAuthState({ isAuthenticated: true, role: "SHOP_OWNER" });
    mockGetShopReviews.mockResolvedValue({
      ...firstPage,
      reviews: [
        {
          ...firstPage.reviews[0],
          response: { id: 20, response: "Resposta existente." },
        },
      ],
    });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();

    expect(container.textContent).toContain("Resposta existente.");
    expect(buttonByText(container, "Responder")).toBeUndefined();

    unmount();
  });

  it("shows an alert when responding fails", async () => {
    setAuthState({ isAuthenticated: true, role: "SHOP_OWNER" });
    mockRespondToReview.mockRejectedValue({
      response: {
        status: 409,
        data: {
          detail: "Already responded",
        },
      },
    });
    const { container, unmount } = renderReviewsSection();

    await flushPromises();
    await click(buttonByText(container, "Responder"));
    await changeField(container.querySelector("textarea"), "Obrigado.");
    await click(buttonByText(container, "Enviar resposta"));
    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Esta avaliação já tem resposta."
    );
    expect(container.querySelector("textarea")).not.toBeNull();

    unmount();
  });
});
