export const AUTH_LOGOUT_EVENT = "trevo:auth-logout";

export function dispatchAuthLogoutEvent(detail = {}) {
  if (typeof window === "undefined") {
    return;
  }

  window.dispatchEvent(new CustomEvent(AUTH_LOGOUT_EVENT, { detail }));
}
