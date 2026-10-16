import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ResetPassword from "./ResetPassword";

const {
  mockConfirmPasswordReset,
  mockValidatePasswordResetToken,
} = vi.hoisted(() => ({
  mockConfirmPasswordReset: vi.fn(),
  mockValidatePasswordResetToken: vi.fn(),
}));

vi.mock("../lib/authApi", () => ({
  confirmPasswordReset: mockConfirmPasswordReset,
  validatePasswordResetToken: mockValidatePasswordResetToken,
}));

globalThis.React = React;

function renderResetPassword() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/reset-password?token=abc123"]}>
        <ResetPassword />
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

describe("ResetPassword", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("accepts a token and new password and confirms the reset", async () => {
    const password = "new-password";
    mockValidatePasswordResetToken.mockResolvedValue(null);
    mockConfirmPasswordReset.mockResolvedValue(null);

    const { container, unmount } = renderResetPassword();

    await flushPromises();

    const newPasswordInput = container.querySelector('input[name="newPassword"]');
    const confirmPasswordInput = container.querySelector(
      'input[name="confirmPassword"]'
    );

    expect(mockValidatePasswordResetToken).toHaveBeenCalledWith("abc123");

    changeInput(newPasswordInput, password);
    changeInput(confirmPasswordInput, password);

    await act(async () => {
      container.querySelector("form").dispatchEvent(
        new Event("submit", {
          bubbles: true,
          cancelable: true,
        })
      );
    });

    expect(newPasswordInput.value).toBe(password);
    expect(mockConfirmPasswordReset).toHaveBeenCalledWith("abc123", password);

    unmount();
  });
});
