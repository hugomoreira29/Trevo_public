import { publicClient } from "../lib/apiClient";

export async function loginUser(credentials) {
  const { data } = await publicClient.post("/auth/login", credentials);
  return data;
}

export async function registerBuyer(payload) {
  const { data } = await publicClient.post("/auth/register/buyer", payload);
  return data;
}

export async function registerShopOwner(payload) {
  const { data } = await publicClient.post("/auth/register/shop-owner", payload);
  return data;
}

