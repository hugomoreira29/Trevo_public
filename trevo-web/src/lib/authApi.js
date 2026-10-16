const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {}),
    },
    ...options,
  });

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") ?? "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const error = new Error(getErrorMessage(body));
    error.status = response.status;
    throw error;
  }

  return body;
}

function getErrorMessage(payload) {
  if (!payload) {
    return "Ocorreu um erro. Tenta novamente.";
  }

  if (typeof payload === "string") {
    return payload;
  }

  return payload.detail ?? payload.message ?? payload.title ?? "Ocorreu um erro. Tenta novamente.";
}

export function registerBuyer(payload) {
  return apiRequest("/auth/register/buyer", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function registerShopOwner(payload) {
  return apiRequest("/auth/register/shop-owner", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function loginUser(payload) {
  return apiRequest("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function fetchUserProfile(accessToken) {
  return apiRequest("/users/me", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function requestPasswordReset(email) {
  return apiRequest("/auth/password-reset/request", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function validatePasswordResetToken(token) {
  return apiRequest("/auth/password-reset/validate", {
    method: "POST",
    body: JSON.stringify({ token }),
  });
}

export function confirmPasswordReset(token, newPassword) {
  return apiRequest("/auth/password-reset/confirm", {
    method: "POST",
    body: JSON.stringify({ token, newPassword }),
  });
}
