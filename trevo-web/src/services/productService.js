import apiClient from "../lib/apiClient";

export async function searchProducts(params = {}) {
  const { data } = await apiClient.get("/products/search", {
    params: cleanParams(params),
  });
  return data;
}

export async function browseProducts(params = {}) {
  const { data } = await apiClient.get("/products", {
    params: cleanParams(params),
  });
  return data;
}

export async function archiveProduct(productId) {
  const { data } = await apiClient.post(`/products/${productId}/archive`);
  return data;
}

export async function unarchiveProduct(productId) {
  const { data } = await apiClient.post(`/products/${productId}/unarchive`);
  return data;
}

export async function getMyProducts() {
  const { data } = await apiClient.get("/products/mine");
  return data;
}

export async function createProduct(payload) {
  const { data } = await apiClient.post("/products", payload);
  return data;
}

export async function updateProduct(productId, payload) {
  const { data } = await apiClient.put(`/products/${productId}`, payload);
  return data;
}

export async function getProduct(productId) {
  const { data } = await apiClient.get(`/products/${productId}`);
  return data;
}

export async function assignProductCollections(productId, collectionIds) {
  const { data } = await apiClient.put(`/products/${productId}/collections`, {
    collectionIds,
  });
  return data;
}

export async function createProductVariant(productId, payload) {
  const { data } = await apiClient.post(`/products/${productId}/variants`, payload);
  return data;
}

export async function updateProductVariant(productId, variantId, payload) {
  const { data } = await apiClient.put(
    `/products/${productId}/variants/${variantId}`,
    payload
  );
  return data;
}

export async function deleteProductVariant(productId, variantId) {
  await apiClient.delete(`/products/${productId}/variants/${variantId}`);
}

export async function bulkUpdateVariantStock(productId, updates) {
  const { data } = await apiClient.post(
    `/products/${productId}/variants/bulk-stock`,
    updates
  );
  return data;
}

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== "" && value != null)
  );
}
