import React, { useEffect } from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "../hooks/useAuth";
import { AUTH_LOGOUT_EVENT } from "../lib/authEvents";

const {
  mockLoginUser,
  mockRegisterBuyer,
  mockRegisterShopOwner,
} = vi.hoisted(() => ({
  mockLoginUser: vi.fn(),
  mockRegisterBuyer: vi.fn(),
  mockRegisterShopOwner: vi.fn(),
}));

vi.mock("../services/authService", () => ({
  loginUser: mockLoginUser,
  registerBuyer: mockRegisterBuyer,
  registerShopOwner: mockRegisterShopOwner,
}));

function encodeBase64Url(value) {
  return btoa(JSON.stringify(value))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function createJwtToken(payload) {
  return [
    encodeBase64Url({ alg: "HS256", typ: "JWT" }),
    encodeBase64Url(payload),
    "signature",
  ].join(".");
}

function renderAuthProvider() {
  const container = document.createElement("div");
  const root = createRoot(container);
  const authRef = {
    current: null,
  };

  function AuthProbe() {
    const auth = useAuth();

    useEffect(() => {
      authRef.current = auth;
    }, [auth]);

    return null;
  }

  act(() => {
    root.render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    );
  });

  return {
    getAuth: () => authRef.current,
    unmount: () =>
      act(() => {
        root.unmount();
      }),
  };
}

describe("AuthContext", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("updates auth state and localStorage after login", async () => {
    const accessToken = createJwtToken({ sub: "7", role: "BUYER" });
    mockLoginUser.mockResolvedValue({
      accessToken,
      refreshToken: "refresh-token",
    });

    const { getAuth, unmount } = renderAuthProvider();

    expect(getAuth().isAuthenticated).toBe(false);

    await act(async () => {
      await getAuth().login({ email: "buyer@trevo.pt", password: "secret" });
    });

    expect(getAuth().isAuthenticated).toBe(true);
    expect(getAuth().role).toBe("BUYER");
    expect(getAuth().userId).toBe("7");
    expect(localStorage.getItem("trevo.accessToken")).toBe(accessToken);
    expect(localStorage.getItem("trevo.refreshToken")).toBe("refresh-token");

    unmount();
  });

  it("clears state and localStorage on logout", async () => {
    localStorage.setItem(
      "trevo.accessToken",
      createJwtToken({ sub: "10", role: "SHOP_OWNER" })
    );
    localStorage.setItem("trevo.refreshToken", "refresh-token");

    const { getAuth, unmount } = renderAuthProvider();

    expect(getAuth().isAuthenticated).toBe(true);

    await act(async () => {
      getAuth().logout();
    });

    expect(getAuth().isAuthenticated).toBe(false);
    expect(getAuth().role).toBeNull();
    expect(localStorage.getItem("trevo.accessToken")).toBeNull();
    expect(localStorage.getItem("trevo.refreshToken")).toBeNull();

    unmount();
  });

  it("responds to the shared logout event used by the API client", async () => {
    localStorage.setItem(
      "trevo.accessToken",
      createJwtToken({ sub: "15", role: "BUYER" })
    );
    localStorage.setItem("trevo.refreshToken", "refresh-token");

    const { getAuth, unmount } = renderAuthProvider();

    expect(getAuth().isAuthenticated).toBe(true);

    await act(async () => {
      window.dispatchEvent(
        new CustomEvent(AUTH_LOGOUT_EVENT, {
          detail: { reason: "refresh-failed" },
        })
      );
    });

    expect(getAuth().isAuthenticated).toBe(false);
    expect(localStorage.getItem("trevo.accessToken")).toBeNull();
    expect(localStorage.getItem("trevo.refreshToken")).toBeNull();

    unmount();
  });

  it("routes buyer and owner registration through the correct service", async () => {
    const { getAuth, unmount } = renderAuthProvider();

    await act(async () => {
      await getAuth().register({
        type: "buyer",
        email: "buyer@trevo.pt",
        password: "secret",
        fullName: "Buyer User",
        phone: "",
      });
    });

    await act(async () => {
      await getAuth().register({
        type: "owner",
        email: "owner@trevo.pt",
        password: "secret",
        fullName: "Owner User",
        phone: "910000000",
        shopName: "Loja Trevo",
        nif: "123456789",
      });
    });

    expect(mockRegisterBuyer).toHaveBeenCalledWith({
      email: "buyer@trevo.pt",
      password: "secret",
      fullName: "Buyer User",
      phone: null,
    });
    expect(mockRegisterShopOwner).toHaveBeenCalledWith({
      email: "owner@trevo.pt",
      password: "secret",
      fullName: "Owner User",
      phone: "910000000",
      shopName: "Loja Trevo",
      nif: "123456789",
    });

    unmount();
  });
});
