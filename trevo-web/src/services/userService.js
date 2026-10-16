import apiClient from "../lib/apiClient";

export async function getCurrentUserProfile() {
  const { data } = await apiClient.get("/users/me");
  return data;
}

export async function updateCurrentUserProfile(payload) {
  const { data } = await apiClient.put("/users/me", payload);
  return data;
}
