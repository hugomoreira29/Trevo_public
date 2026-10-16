import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProSubscription from "./ProSubscription";

const {
  mockCancelSubscription,
  mockCreateSubscription,
  mockGetProStatus,
} = vi.hoisted(() => ({
  mockCancelSubscription: vi.fn(),
  mockCreateSubscription: vi.fn(),
  mockGetProStatus: vi.fn(),
}));

vi.mock("../services/subscriptionService", () => ({
  cancelSubscription: mockCancelSubscription,
  createSubscription: mockCreateSubscription,
  getProStatus: mockGetProStatus,
}));

globalThis.React = React;

function renderProSubscription() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/pro"]}>
        <Routes>
          <Route path="/pro" element={<ProSubscription />} />
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

async function click(button) {
  await act(async () => {
    button.click();
  });
}

function buttonByText(container, text) {
  return [...container.querySelectorAll("button")].find(
    (button) => button.textContent === text
  );
}

describe("ProSubscription", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetProStatus.mockResolvedValue({ pro: false });
    mockCreateSubscription.mockResolvedValue({
      id: 1,
      status: "ACTIVE",
      stripeSubscriptionId: "sub_123",
    });
    mockCancelSubscription.mockResolvedValue();
  });

  it("shows the Pro plan price", async () => {
    const { container, unmount } = renderProSubscription();

    await flushPromises();

    expect(container.textContent).toContain("9,99 EUR / mês");
    expect(container.textContent).toContain("Trevo Pro");

    unmount();
  });

  it("calls createSubscription when subscribing", async () => {
    const { container, unmount } = renderProSubscription();

    await flushPromises();
    await click(buttonByText(container, "Subscrever Pro"));

    expect(mockCreateSubscription).toHaveBeenCalledTimes(1);
    expect(container.textContent).toContain("Subscrição Pro ativa.");
    expect(container.textContent).toContain("Subscrição ativa");

    unmount();
  });

  it("shows cancel option when Pro is active", async () => {
    mockGetProStatus.mockResolvedValue({ pro: true });
    const { container, unmount } = renderProSubscription();

    await flushPromises();

    expect(container.textContent).toContain("Subscrição ativa");
    expect(buttonByText(container, "Cancelar subscrição")).not.toBeUndefined();

    unmount();
  });

  it("calls cancelSubscription when cancelling", async () => {
    mockGetProStatus.mockResolvedValue({ pro: true });
    const { container, unmount } = renderProSubscription();

    await flushPromises();
    await click(buttonByText(container, "Cancelar subscrição"));

    expect(mockCancelSubscription).toHaveBeenCalledTimes(1);
    expect(container.textContent).toContain("Subscrição Pro cancelada.");
    expect(container.textContent).toContain("Ainda não tens Pro ativo.");

    unmount();
  });

  it("shows loading state", () => {
    mockGetProStatus.mockReturnValue(new Promise(() => {}));
    const { container, unmount } = renderProSubscription();

    expect(container.querySelector('[aria-live="polite"]').textContent).toContain(
      "A carregar subscrição..."
    );

    unmount();
  });

  it("shows error state", async () => {
    mockGetProStatus.mockRejectedValue({
      response: {
        data: {
          detail: "Nao foi possivel carregar estado Pro",
        },
      },
    });
    const { container, unmount } = renderProSubscription();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Nao foi possivel carregar estado Pro"
    );

    unmount();
  });
});
