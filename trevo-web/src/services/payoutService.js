import apiClient from "../lib/apiClient";

export async function getPayoutBalance() {
  const { data } = await apiClient.get("/payouts/pending-balance");
  return data;
}

export async function getPayoutHistory(params) {
  const { data } = await apiClient.get("/payouts/history", { params });
  return data;
}
