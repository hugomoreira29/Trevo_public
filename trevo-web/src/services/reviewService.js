import apiClient, { publicClient } from "../lib/apiClient";

export async function createReview(shopId, payload) {
  const { data } = await apiClient.post(`/buyer/reviews/shops/${shopId}`, payload);
  return data;
}

export async function getShopReviews(shopId, params) {
  const { data } = await publicClient.get(`/shops/${shopId}/reviews`, { params });
  return data;
}

export async function respondToReview(reviewId, responseText) {
  const { data } = await apiClient.post(`/reviews/${reviewId}/response`, {
    response: responseText,
  });
  return data;
}
