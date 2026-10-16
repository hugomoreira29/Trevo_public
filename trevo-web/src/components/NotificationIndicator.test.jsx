import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import NotificationIndicator from "./NotificationIndicator";

const {
  mockGetNotifications,
  mockMarkNotificationAsRead,
  mockUseAuth,
} = vi.hoisted(() => ({
  mockGetNotifications: vi.fn(),
  mockMarkNotificationAsRead: vi.fn(),
  mockUseAuth: vi.fn(),
}));

vi.mock("../hooks/useAuth", () => ({
  useAuth: mockUseAuth,
}));

vi.mock("../services/notificationService", () => ({
  getNotifications: mockGetNotifications,
  markNotificationAsRead: mockMarkNotificationAsRead,
}));

globalThis.React = React;

const notificationResponse = {
  unreadCount: 1,
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
  notifications: [
    {
      id: 10,
      orderId: 77,
      type: "NEW_ORDER",
      title: "Nova encomenda recebida",
      message: "Recebeste uma nova encomenda na loja Trevo.",
      read: false,
      createdAt: "2026-05-08T10:00:00Z",
      readAt: null,
    },
    {
      id: 11,
      orderId: 78,
      type: "NEW_ORDER",
      title: "Encomenda ja lida",
      message: "Esta notificacao ja foi lida.",
      read: true,
      createdAt: "2026-05-08T09:00:00Z",
      readAt: "2026-05-08T09:30:00Z",
    },
  ],
};

function renderNotificationIndicator() {
  const container = document.createElement("div");
  document.body.appendChild(container);
  const root = createRoot(container);

  act(() => {
    root.render(<NotificationIndicator />);
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

async function click(element) {
  await act(async () => {
    element.click();
  });
}

function notificationButton(container) {
  return container.querySelector('button[aria-label="Notificacoes"]');
}

describe("NotificationIndicator", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      role: "SHOP_OWNER",
    });
    mockGetNotifications.mockResolvedValue(notificationResponse);
    mockMarkNotificationAsRead.mockResolvedValue({
      ...notificationResponse.notifications[0],
      read: true,
      readAt: "2026-05-08T10:05:00Z",
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    document.body.innerHTML = "";
  });

  it("renders unread badge for authenticated SHOP_OWNER users", async () => {
    const { container, unmount } = renderNotificationIndicator();

    await flushPromises();

    expect(mockGetNotifications).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(notificationButton(container)).not.toBeNull();
    expect(notificationButton(container).textContent).toContain("🔔");
    expect(container.textContent).toContain("1");

    unmount();
  });

  it("is hidden for non SHOP_OWNER users", async () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      role: "BUYER",
    });

    const { container, unmount } = renderNotificationIndicator();

    await flushPromises();

    expect(notificationButton(container)).toBeNull();
    expect(mockGetNotifications).not.toHaveBeenCalled();

    unmount();
  });

  it("closes the dropdown on outside click", async () => {
    const { container, unmount } = renderNotificationIndicator();

    await flushPromises();
    await click(notificationButton(container));

    expect(container.textContent).toContain("Nova encomenda recebida");

    await act(async () => {
      document.body.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
    });

    expect(container.textContent).not.toContain("Nova encomenda recebida");

    unmount();
  });

  it("marks a notification as read and updates the badge locally", async () => {
    const { container, unmount } = renderNotificationIndicator();

    await flushPromises();
    await click(notificationButton(container));
    await click(
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Marcar como lida"
      )
    );
    await flushPromises();

    expect(mockMarkNotificationAsRead).toHaveBeenCalledWith(10);
    expect(container.textContent).toContain("0 por ler");
    expect(container.textContent).not.toContain("Marcar como lida");

    unmount();
  });

  it("cleans up polling when unmounted", async () => {
    vi.useFakeTimers();
    const { unmount } = renderNotificationIndicator();

    await flushPromises();

    expect(mockGetNotifications).toHaveBeenCalledTimes(1);

    await act(async () => {
      vi.advanceTimersByTime(60000);
      await Promise.resolve();
    });

    expect(mockGetNotifications).toHaveBeenCalledTimes(2);

    unmount();

    await act(async () => {
      vi.advanceTimersByTime(60000);
      await Promise.resolve();
    });

    expect(mockGetNotifications).toHaveBeenCalledTimes(2);
  });
});
