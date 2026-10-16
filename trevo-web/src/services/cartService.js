import apiClient from "../lib/apiClient";

export async function getCart() {
  const { data } = await apiClient.get("/buyer/cart");
  return data;
}

export async function addCartItem(variantId, quantity) {
  const { data } = await apiClient.post("/buyer/cart/items", {
    variantId,
    quantity,
  });
  return data;
}

export async function updateCartItem(variantId, quantity) {
  const { data } = await apiClient.put(`/buyer/cart/items/${variantId}`, {
    quantity,
  });
  return data;
}

export async function removeCartItem(variantId) {
  await apiClient.delete(`/buyer/cart/items/${variantId}`);
}
