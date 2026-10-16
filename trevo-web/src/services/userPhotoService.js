import apiClient from "../lib/apiClient";

const REQUIRED_PHOTO_COUNT = 3;
const MAX_PHOTO_SIZE_BYTES = 10 * 1024 * 1024;

export async function uploadBuyerPhotos(files) {
  if (!Array.isArray(files) || files.length !== REQUIRED_PHOTO_COUNT) {
    throw new Error("Seleciona exatamente 3 imagens");
  }

  files.forEach((file) => {
    if (!file.type?.startsWith("image/")) {
      throw new Error("Todos os ficheiros têm de ser imagens");
    }

    if (file.size > MAX_PHOTO_SIZE_BYTES) {
      throw new Error("Cada imagem deve ter no máximo 10MB");
    }
  });

  const formData = new FormData();
  files.forEach((file) => {
    formData.append("photos", file);
  });

  const { data } = await apiClient.post("/buyer/photos", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });

  return data;
}
