import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PayoutDashboard from "./PayoutDashboard";

const { mockGetPayoutBalance, mockGetPayoutHistory } = vi.hoisted(() => ({
  mockGetPayoutBalance: vi.fn(),
  mockGetPayoutHistory: vi.fn(),
}));

vi.mock("../services/payoutService", () => ({
  getPayoutBalance: mockGetPayoutBalance,
  getPayoutHistory: mockGetPayoutHistory,
}));

globalThis.React = React;

const payoutPage = {
  totalElements: 2,
  totalPages: 1,
  page: 0,
  size: 20,
  payouts: [
    {
      id: 1,
      orderId: 77,
      payoutAmount: 90,
      status: "RELEASED",
      createdAt: "2026-05-10T09:00:00Z",
      releasedAt: "2026-05-10T10:00:00Z",
    },
    {
      id: 2,
      orderId: 78,
      payoutAmount: 35.5,
      status: "PENDING",
      createdAt: "2026-05-11T10:00:00Z",
      releasedAt: null,
    },
  ],
};

function renderPayoutDashboard() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(<PayoutDashboard />);
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

describe("PayoutDashboard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetPayoutBalance.mockResolvedValue({ pendingBalance: 270 });
    mockGetPayoutHistory.mockResolvedValue(payoutPage);
  });

  it("shows the pending balance", async () => {
    const { container, unmount } = renderPayoutDashboard();

    await flushPromises();

    expect(mockGetPayoutBalance).toHaveBeenCalled();
    expect(container.textContent).toContain("Saldo pendente");
    expect(container.textContent).toContain(
      new Intl.NumberFormat("pt-PT", {
        style: "currency",
        currency: "EUR",
      }).format(270)
    );

    unmount();
  });

  it("shows payout history with date, amount, and status", async () => {
    const { container, unmount } = renderPayoutDashboard();

    await flushPromises();

    expect(mockGetPayoutHistory).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(container.textContent).toContain("Data");
    expect(container.textContent).toContain("Montante");
    expect(container.textContent).toContain("Estado");
    expect(container.textContent).toContain("10/05/26");
    expect(container.textContent).toContain("90,00");
    expect(container.textContent).toContain("Pago");
    expect(container.textContent).toContain("11/05/26");
    expect(container.textContent).toContain("35,50");
    expect(container.textContent).toContain("Pendente");

    unmount();
  });

  it("loads the next payout page without reloading the balance", async () => {
    mockGetPayoutHistory
      .mockResolvedValueOnce({
        ...payoutPage,
        totalPages: 2,
        page: 0,
      })
      .mockResolvedValueOnce({
        ...payoutPage,
        totalPages: 2,
        page: 1,
        payouts: [
          {
            id: 3,
            orderId: 79,
            payoutAmount: 12,
            status: "RELEASED",
            createdAt: "2026-05-12T09:00:00Z",
            releasedAt: "2026-05-12T10:00:00Z",
          },
        ],
      });
    const { container, unmount } = renderPayoutDashboard();

    await flushPromises();
    await click(
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Seguinte"
      )
    );
    await flushPromises();

    expect(mockGetPayoutHistory).toHaveBeenLastCalledWith({ page: 1, size: 20 });
    expect(mockGetPayoutBalance).toHaveBeenCalledTimes(1);
    expect(container.textContent).toContain("12,00");

    unmount();
  });

  it("shows an empty state when there are no payouts", async () => {
    mockGetPayoutHistory.mockResolvedValue({
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 20,
      payouts: [],
    });
    const { container, unmount } = renderPayoutDashboard();

    await flushPromises();

    expect(container.textContent).toContain("Ainda não existem pagamentos.");

    unmount();
  });

  it("shows an error state", async () => {
    mockGetPayoutHistory.mockRejectedValue({
      response: {
        data: {
          detail: "Shop not found",
        },
      },
    });
    const { container, unmount } = renderPayoutDashboard();

    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Shop not found"
    );

    unmount();
  });
});
