import apiClient, { publicClient } from "../lib/apiClient";

export async function createShop(payload) {
  const { data } = await apiClient.post("/shops", payload);
  return data;
}

export async function getShopDashboard() {
  // TODO: Endpoint GET /shops/me/dashboard depende de implementação backend futura.
  const { data } = await apiClient.get("/shops/me/dashboard");
  return data;
}

export async function getShopProfile(shopId) {
  const { data } = await publicClient.get(`/shops/${shopId}`);
  return data;
}

export async function createCollection(shopId, name) {
  const { data } = await apiClient.post(`/shops/${shopId}/collections`, { name });
  return data;
}

export async function renameCollection(shopId, collectionId, name) {
  const { data } = await apiClient.patch(
    `/shops/${shopId}/collections/${collectionId}`,
    { name }
  );
  return data;
}

export async function reorderCollections(shopId, collectionIds) {
  const { data } = await apiClient.put(`/shops/${shopId}/collections/reorder`, {
    collectionIds,
  });
  return data;
}

export async function deleteCollection(shopId, collectionId) {
  await apiClient.delete(`/shops/${shopId}/collections/${collectionId}`);
}
