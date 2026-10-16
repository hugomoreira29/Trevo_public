import axios from "axios";
import {
  clearAuthTokens,
  getAccessToken,
  getRefreshToken,
  saveAuthTokens,
} from "./authStorage";
import { dispatchAuthLogoutEvent } from "./authEvents";

const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL;
const API_BASE_URL = configuredApiBaseUrl || "http://localhost:8080/api";

if (!configuredApiBaseUrl) {
  console.warn(
    "VITE_API_BASE_URL is not set. Falling back to http://localhost:8080/api, which will not work from the frontend container."
  );
}

const publicClient = axios.create({
  baseURL: API_BASE_URL,
});

const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

let refreshRequest = null;

function isRefreshableRequest(error) {
  const { config, response } = error;
  if (!config || !response) {
    return false;
  }

  if (response.status !== 401 || config._retry) {
    return false;
  }

  return !config.url?.includes("/auth/login") && !config.url?.includes("/auth/refresh");
}

async function refreshAccessToken() {
  if (!refreshRequest) {
    const refreshToken = getRefreshToken();

    refreshRequest = publicClient
      .post("/auth/refresh", { refreshToken })
      .then(({ data }) => {
        saveAuthTokens(data);
        return data.accessToken;
      })
      .catch((error) => {
        clearAuthTokens();
        dispatchAuthLogoutEvent({ reason: "refresh-failed" });
        throw error;
      })
      .finally(() => {
        refreshRequest = null;
      });
  }

  return refreshRequest;
}

apiClient.interceptors.request.use((config) => {
  const accessToken = getAccessToken();

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const refreshToken = getRefreshToken();

    if (!refreshToken || !isRefreshableRequest(error)) {
      throw error;
    }

    const originalRequest = error.config;
    originalRequest._retry = true;

    const newAccessToken = await refreshAccessToken();
    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

    return apiClient(originalRequest);
  }
);

export { publicClient };
export default apiClient;
