import apiClient from "../lib/apiClient";

export async function createSubscription() {
  const { data } = await apiClient.post("/buyer/subscription");
  return data;
}

export async function cancelSubscription() {
  await apiClient.delete("/buyer/subscription");
}

export async function getProStatus() {
  const { data } = await apiClient.get("/buyer/subscription/pro-status");
  return data;
}
