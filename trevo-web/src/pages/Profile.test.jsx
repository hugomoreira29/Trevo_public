import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Profile from "./Profile";

const { mockApiClient, mockLogout } = vi.hoisted(() => ({
  mockApiClient: {
    get: vi.fn(),
    put: vi.fn(),
  },
  mockLogout: vi.fn(),
}));

vi.mock("../lib/apiClient", () => ({
  default: mockApiClient,
}));

vi.mock("../hooks/useAuth", () => ({
  useAuth: () => ({
    logout: mockLogout,
  }),
}));

globalThis.React = React;

function renderProfile() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter>
        <Profile />
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
    HTMLInputElement.prototype,
    "value"
  ).set;

  act(() => {
    valueSetter.call(input, value);
    input.dispatchEvent(new Event("input", { bubbles: true }));
  });
}

describe("Profile", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("pre-fills current data and submits profile updates", async () => {
    mockApiClient.get.mockResolvedValue({
      data: {
        fullName: "Buyer User",
        email: "buyer@trevo.pt",
        phone: "912345678",
      },
    });
    mockApiClient.put.mockResolvedValue({
      data: {
        fullName: "Updated Buyer",
        email: "updated@trevo.pt",
        phone: "934567890",
      },
    });

    const { container, unmount } = renderProfile();

    await flushPromises();

    const fullNameInput = container.querySelector('input[name="fullName"]');
    const emailInput = container.querySelector('input[name="email"]');
    const phoneInput = container.querySelector('input[name="phone"]');

    expect(mockApiClient.get).toHaveBeenCalledWith("/users/me");
    expect(fullNameInput.value).toBe("Buyer User");
    expect(emailInput.value).toBe("buyer@trevo.pt");
    expect(phoneInput.value).toBe("912345678");

    changeInput(fullNameInput, "Updated Buyer");
    changeInput(emailInput, "updated@trevo.pt");
    changeInput(phoneInput, "934567890");

    await act(async () => {
      container.querySelector("form").dispatchEvent(
        new Event("submit", {
          bubbles: true,
          cancelable: true,
        })
      );
    });

    expect(mockApiClient.put).toHaveBeenCalledWith("/users/me", {
      fullName: "Updated Buyer",
      email: "updated@trevo.pt",
      phone: "934567890",
    });

    unmount();
  });

  it("does not submit password updates with a short new password", async () => {
    mockApiClient.get.mockResolvedValue({
      data: {
        fullName: "Buyer User",
        email: "buyer@trevo.pt",
        phone: "912345678",
      },
    });

    const { container, unmount } = renderProfile();

    await flushPromises();

    changeInput(
      container.querySelector('input[name="currentPassword"]'),
      "password-atual"
    );
    changeInput(container.querySelector('input[name="newPassword"]'), "curta");

    await act(async () => {
      container.querySelector("form").dispatchEvent(
        new Event("submit", {
          bubbles: true,
          cancelable: true,
        })
      );
    });

    expect(mockApiClient.put).not.toHaveBeenCalled();
    expect(container.textContent).toContain(
      "A nova palavra-passe deve ter pelo menos 8 caracteres"
    );

    unmount();
  });
});
