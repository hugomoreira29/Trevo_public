import apiClient from "../lib/apiClient";

const DEFAULT_POLL_INTERVAL_MS = 1000;
const DEFAULT_MAX_POLL_ATTEMPTS = 60;

export async function uploadImage(file, onProgress, options = {}) {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await apiClient.post("/uploads/images", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    onUploadProgress: (event) => {
      if (!onProgress || !event.total) {
        return;
      }

      onProgress(Math.round((event.loaded * 100) / event.total));
    },
  });

  if (!data?.statusUrl) {
    throw new Error("Image upload did not return a status URL");
  }

  return pollUploadResult(data.statusUrl, options);
}

async function pollUploadResult(statusUrl, options) {
  const pollIntervalMs = options.pollIntervalMs ?? DEFAULT_POLL_INTERVAL_MS;
  const maxPollAttempts = options.maxPollAttempts ?? DEFAULT_MAX_POLL_ATTEMPTS;
  const pollUrl = normalizeStatusUrl(statusUrl);

  for (let attempt = 0; attempt < maxPollAttempts; attempt += 1) {
    const { data } = await apiClient.get(pollUrl);

    if (data.status === "COMPLETED" && data.result?.url) {
      return data.result.url;
    }

    if (data.status === "FAILED") {
      throw new Error(data.error || "Image upload failed");
    }

    await wait(pollIntervalMs);
  }

  throw new Error("Image upload timed out");
}

function normalizeStatusUrl(statusUrl) {
  return statusUrl.startsWith("/api/") ? statusUrl.slice(4) : statusUrl;
}

function wait(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}
