import apiClient from "../lib/apiClient";

export async function getTryOnUsage() {
  const { data } = await apiClient.get("/buyer/try-on/usage");
  return data;
}

export async function generateTryOn(productId, photo) {
  const formData = new FormData();
  formData.append("productId", productId);
  formData.append("photo", photo);

  const { data } = await apiClient.post("/buyer/try-on", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}
