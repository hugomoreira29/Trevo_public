import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AuthContext } from "./auth-context";
import {
  clearAuthTokens,
  getAccessToken,
  getRefreshToken,
  parseJwtClaims,
  saveAuthTokens,
} from "../lib/authStorage";
import { AUTH_LOGOUT_EVENT } from "../lib/authEvents";
import { loginUser, registerBuyer, registerShopOwner } from "../services/authService";

function getInitialAccessToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return getAccessToken();
}

function getInitialRefreshToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return getRefreshToken();
}

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(getInitialAccessToken);
  const [refreshToken, setRefreshToken] = useState(getInitialRefreshToken);

  const persistSession = useCallback((tokens) => {
    saveAuthTokens(tokens);
    setAccessToken(tokens.accessToken);
    setRefreshToken(tokens.refreshToken);
  }, []);

  const logout = useCallback(() => {
    clearAuthTokens();
    setAccessToken(null);
    setRefreshToken(null);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") {
      return undefined;
    }

    const handleLogout = () => {
      logout();
    };

    window.addEventListener(AUTH_LOGOUT_EVENT, handleLogout);
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, handleLogout);
  }, [logout]);

  const login = useCallback(
    async (credentials) => {
      const tokens = await loginUser(credentials);
      persistSession(tokens);
      return tokens;
    },
    [persistSession]
  );

  const register = useCallback(async (payload) => {
    if (payload.type === "owner") {
      return registerShopOwner({
        email: payload.email,
        password: payload.password,
        fullName: payload.fullName,
        phone: payload.phone || null,
        shopName: payload.shopName,
        nif: payload.nif,
      });
    }

    return registerBuyer({
      email: payload.email,
      password: payload.password,
      fullName: payload.fullName,
      phone: payload.phone || null,
    });
  }, []);

  const claims = useMemo(() => parseJwtClaims(accessToken), [accessToken]);

  const value = useMemo(
    () => ({
      token: accessToken,
      accessToken,
      refreshToken,
      userId: claims?.sub || null,
      role: claims?.role || null,
      isAuthenticated: !!accessToken,
      login,
      logout,
      register,
    }),
    [accessToken, claims, login, logout, refreshToken, register]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
