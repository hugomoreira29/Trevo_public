import apiClient from "../lib/apiClient";

export async function getNotifications({ page = 0, size = 20 } = {}) {
  const { data } = await apiClient.get("/notifications", {
    params: { page, size },
  });
  return data;
}

export async function markNotificationAsRead(notificationId) {
  const { data } = await apiClient.patch(`/notifications/${notificationId}/read`);
  return data;
}
