import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ForgotPassword from "./ForgotPassword";

const { mockRequestPasswordReset } = vi.hoisted(() => ({
  mockRequestPasswordReset: vi.fn(),
}));

vi.mock("../lib/authApi", () => ({
  requestPasswordReset: mockRequestPasswordReset,
}));

globalThis.React = React;

function renderForgotPassword() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter>
        <ForgotPassword />
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

describe("ForgotPassword", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("accepts an email and requests a password reset", async () => {
    mockRequestPasswordReset.mockResolvedValue(null);

    const { container, unmount } = renderForgotPassword();
    const emailInput = container.querySelector('input[name="email"]');

    changeInput(emailInput, "buyer@trevo.pt");

    await act(async () => {
      container.querySelector("form").dispatchEvent(
        new Event("submit", {
          bubbles: true,
          cancelable: true,
        })
      );
    });

    expect(emailInput.value).toBe("buyer@trevo.pt");
    expect(mockRequestPasswordReset).toHaveBeenCalledWith("buyer@trevo.pt");

    unmount();
  });
});
