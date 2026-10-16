import { beforeEach, describe, expect, it, vi } from "vitest";

let mockGetAccessToken;
let mockGetRefreshToken;
let mockSaveAuthTokens;
let mockClearAuthTokens;
let mockDispatchAuthLogoutEvent;

function createAxiosClient() {
  const client = vi.fn((config) => Promise.resolve({ config }));

  client.interceptors = {
    request: {
      use: vi.fn((handler) => {
        client.requestHandler = handler;
      }),
    },
    response: {
      use: vi.fn((successHandler, errorHandler) => {
        client.responseSuccessHandler = successHandler;
        client.responseErrorHandler = errorHandler;
      }),
    },
  };

  client.post = vi.fn();

  return client;
}

async function loadApiClientModule() {
  mockGetAccessToken = vi.fn();
  mockGetRefreshToken = vi.fn();
  mockSaveAuthTokens = vi.fn();
  mockClearAuthTokens = vi.fn();
  mockDispatchAuthLogoutEvent = vi.fn();
  vi.spyOn(console, "warn").mockImplementation(() => {});

  vi.doMock("axios", () => ({
    default: {
      create: vi.fn(() => createAxiosClient()),
    },
  }));

  vi.doMock("./authStorage", () => ({
    getAccessToken: mockGetAccessToken,
    getRefreshToken: mockGetRefreshToken,
    saveAuthTokens: mockSaveAuthTokens,
    clearAuthTokens: mockClearAuthTokens,
  }));

  vi.doMock("./authEvents", () => ({
    dispatchAuthLogoutEvent: mockDispatchAuthLogoutEvent,
  }));

  return import("./apiClient");
}

describe("apiClient", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  it("attaches the bearer token to authenticated requests", async () => {
    const module = await loadApiClientModule();
    const apiClient = module.default;

    mockGetAccessToken.mockReturnValue("access-token");

    const config = apiClient.requestHandler({ headers: {} });

    expect(config.headers.Authorization).toBe("Bearer access-token");
  });

  it("refreshes the token and retries the request after a 401", async () => {
    const module = await loadApiClientModule();
    const publicClient = module.publicClient;
    const apiClient = module.default;

    mockGetRefreshToken.mockReturnValue("refresh-token");
    publicClient.post.mockResolvedValue({
      data: {
        accessToken: "new-access-token",
        refreshToken: "new-refresh-token",
      },
    });

    const originalRequest = { url: "/orders", headers: {} };

    await apiClient.responseErrorHandler({
      config: originalRequest,
      response: { status: 401 },
    });

    expect(publicClient.post).toHaveBeenCalledWith("/auth/refresh", {
      refreshToken: "refresh-token",
    });
    expect(mockSaveAuthTokens).toHaveBeenCalledWith({
      accessToken: "new-access-token",
      refreshToken: "new-refresh-token",
    });
    expect(originalRequest._retry).toBe(true);
    expect(originalRequest.headers.Authorization).toBe("Bearer new-access-token");
    expect(apiClient).toHaveBeenCalledWith(originalRequest);
  });

  it("clears auth state and dispatches logout when refresh fails", async () => {
    const module = await loadApiClientModule();
    const publicClient = module.publicClient;
    const apiClient = module.default;
    const refreshError = new Error("refresh failed");

    mockGetRefreshToken.mockReturnValue("refresh-token");
    publicClient.post.mockRejectedValue(refreshError);

    await expect(
      apiClient.responseErrorHandler({
        config: { url: "/orders", headers: {} },
        response: { status: 401 },
      })
    ).rejects.toBe(refreshError);

    expect(mockClearAuthTokens).toHaveBeenCalled();
    expect(mockDispatchAuthLogoutEvent).toHaveBeenCalledWith({
      reason: "refresh-failed",
    });
  });
});
