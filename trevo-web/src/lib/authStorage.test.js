import {
  clearAuthTokens,
  getAccessToken,
  getRefreshToken,
  parseJwtClaims,
  saveAuthTokens,
} from "./authStorage";

function encodeBase64Url(value) {
  return btoa(JSON.stringify(value))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

describe("authStorage", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("persists and clears auth tokens", () => {
    saveAuthTokens({
      accessToken: "access-token",
      refreshToken: "refresh-token",
    });

    expect(getAccessToken()).toBe("access-token");
    expect(getRefreshToken()).toBe("refresh-token");

    clearAuthTokens();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });

  it("parses JWT claims from a valid token", () => {
    const token = [
      encodeBase64Url({ alg: "HS256", typ: "JWT" }),
      encodeBase64Url({ sub: "42", role: "BUYER" }),
      "signature",
    ].join(".");

    expect(parseJwtClaims(token)).toEqual({
      sub: "42",
      role: "BUYER",
    });
  });

  it("returns null for malformed tokens", () => {
    expect(parseJwtClaims("not-a-jwt")).toBeNull();
    expect(parseJwtClaims("header.invalid-payload.signature")).toBeNull();
    expect(parseJwtClaims(null)).toBeNull();
  });
});
