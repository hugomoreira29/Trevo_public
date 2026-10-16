import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ReviewForm from "./ReviewForm";

const { mockCreateReview } = vi.hoisted(() => ({
  mockCreateReview: vi.fn(),
}));

vi.mock("../services/reviewService", () => ({
  createReview: mockCreateReview,
}));

globalThis.React = React;

function renderReviewForm() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(<ReviewForm shopId={5} />);
  });

  return {
    container,
    unmount: () =>
      act(() => {
        root.unmount();
      }),
  };
}

async function click(button) {
  await act(async () => {
    button.click();
  });
}

function changeTextarea(textarea, value) {
  act(() => {
    const valueSetter = Object.getOwnPropertyDescriptor(
      HTMLTextAreaElement.prototype,
      "value"
    ).set;
    valueSetter.call(textarea, value);
    textarea.dispatchEvent(new Event("input", { bubbles: true }));
  });
}

async function submitForm(container) {
  await act(async () => {
    container.querySelector("form").dispatchEvent(
      new Event("submit", { bubbles: true, cancelable: true })
    );
  });
}

function submitButton(container) {
  return container.querySelector('button[type="submit"]');
}

describe("ReviewForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCreateReview.mockResolvedValue({
      id: 10,
      shopId: 5,
      rating: 5,
      comment: null,
    });
  });

  it("lets users click stars", async () => {
    const { container, unmount } = renderReviewForm();

    await click(container.querySelector('button[aria-label="4 estrelas"]'));

    expect(
      container.querySelector('button[aria-label="4 estrelas"]').getAttribute(
        "aria-pressed"
      )
    ).toBe("true");
    expect(
      container.querySelector('button[aria-label="5 estrelas"]').getAttribute(
        "aria-pressed"
      )
    ).toBe("false");

    unmount();
  });

  it("does not submit with zero stars", async () => {
    const { container, unmount } = renderReviewForm();

    expect(submitButton(container).disabled).toBe(true);

    await submitForm(container);

    expect(mockCreateReview).not.toHaveBeenCalled();

    unmount();
  });

  it("submits with optional empty text", async () => {
    const { container, unmount } = renderReviewForm();

    await click(container.querySelector('button[aria-label="5 estrelas"]'));
    await submitForm(container);

    expect(mockCreateReview).toHaveBeenCalledWith(5, {
      rating: 5,
      comment: null,
    });

    unmount();
  });

  it("submits rating and comment", async () => {
    const { container, unmount } = renderReviewForm();

    await click(container.querySelector('button[aria-label="3 estrelas"]'));
    changeTextarea(container.querySelector("textarea"), "Boa experiencia.");
    await submitForm(container);

    expect(mockCreateReview).toHaveBeenCalledWith(5, {
      rating: 3,
      comment: "Boa experiencia.",
    });

    unmount();
  });

  it("shows success after submit and hides the form", async () => {
    const { container, unmount } = renderReviewForm();

    await click(container.querySelector('button[aria-label="5 estrelas"]'));
    await submitForm(container);

    expect(container.querySelector('[role="status"]').textContent).toContain(
      "sucesso"
    );
    expect(container.querySelector("form")).toBeNull();

    unmount();
  });

  it("does not allow another submission after success", async () => {
    const { container, unmount } = renderReviewForm();

    await click(container.querySelector('button[aria-label="5 estrelas"]'));
    await submitForm(container);

    expect(container.querySelector("form")).toBeNull();
    expect(submitButton(container)).toBeNull();
    expect(mockCreateReview).toHaveBeenCalledTimes(1);

    unmount();
  });

  it("shows backend error after submit failure", async () => {
    mockCreateReview.mockRejectedValue({
      response: {
        data: {
          detail: "Buyer has no delivered order from this shop",
        },
      },
    });
    const { container, unmount } = renderReviewForm();

    await click(container.querySelector('button[aria-label="5 estrelas"]'));
    await submitForm(container);

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Buyer has no delivered order from this shop"
    );

    unmount();
  });

  it("shows a Portuguese message for duplicate review errors", async () => {
    mockCreateReview.mockRejectedValue({
      response: {
        status: 409,
        data: {
          message: "Buyer already reviewed this shop",
        },
      },
    });
    const { container, unmount } = renderReviewForm();

    await click(container.querySelector('button[aria-label="5 estrelas"]'));
    await submitForm(container);

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Já avaliou esta loja."
    );
    expect(container.querySelector('[role="alert"]').textContent).not.toContain(
      "Buyer already reviewed this shop"
    );

    unmount();
  });
});
