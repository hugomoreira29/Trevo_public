import apiClient from "../lib/apiClient";

export async function checkoutOrder(paymentMethod, payload = {}) {
  const { data } = await apiClient.post("/buyer/orders/checkout", {
    paymentMethod,
    ...payload,
  });
  return data;
}

export async function getBuyerOrders(params) {
  const { data } = await apiClient.get("/buyer/orders", { params });
  return data;
}

export async function getBuyerOrder(orderId) {
  const { data } = await apiClient.get(`/buyer/orders/${orderId}`);
  return data;
}

export async function cancelBuyerOrder(orderId) {
  const { data } = await apiClient.post(`/buyer/orders/${orderId}/cancel`);
  return data;
}

export async function getShopOrders({ page, size = 20 }) {
  const { data } = await apiClient.get("/orders", {
    params: { page, size },
  });
  return data;
}

export async function updateOrderStatus(orderId, status) {
  const { data } = await apiClient.patch(`/orders/${orderId}/status`, {
    status,
  });
  return data;
}
