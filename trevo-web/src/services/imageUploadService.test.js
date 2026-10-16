import { beforeEach, describe, expect, it, vi } from "vitest";
import { uploadImage } from "./imageUploadService";

const { mockPost, mockGet } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
}));

vi.mock("../lib/apiClient", () => ({
  default: {
    post: mockPost,
    get: mockGet,
  },
}));

describe("imageUploadService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("uploads an image and returns its Cloudinary URL", async () => {
    const file = new File(["image-bytes"], "logo.png", { type: "image/png" });
    mockPost.mockResolvedValue({
      data: {
        uploadId: "upload-123",
        status: "PENDING",
        statusUrl: "/api/uploads/images/upload-123",
      },
    });
    mockGet.mockResolvedValue({
      data: {
        uploadId: "upload-123",
        status: "COMPLETED",
        result: {
          publicId: "trevo/logo",
          url: "https://res.cloudinary.com/demo/image/upload/logo.png",
        },
      },
    });

    const url = await uploadImage(file, undefined, { pollIntervalMs: 0 });

    expect(url).toBe("https://res.cloudinary.com/demo/image/upload/logo.png");
    expect(mockPost).toHaveBeenCalledWith(
      "/uploads/images",
      expect.any(FormData),
      expect.objectContaining({
        headers: {
          "Content-Type": "multipart/form-data",
        },
      })
    );
    expect(mockGet).toHaveBeenCalledWith("/uploads/images/upload-123");
  });

  it("reports upload progress as a percentage", async () => {
    const file = new File(["image-bytes"], "banner.webp", { type: "image/webp" });
    const onProgress = vi.fn();

    mockPost.mockImplementation((_url, _formData, config) => {
      config.onUploadProgress({ loaded: 24, total: 80 });
      return Promise.resolve({
        data: {
          uploadId: "upload-456",
          status: "PENDING",
          statusUrl: "/api/uploads/images/upload-456",
        },
      });
    });
    mockGet.mockResolvedValue({
      data: {
        uploadId: "upload-456",
        status: "COMPLETED",
        result: {
          publicId: "trevo/banner",
          url: "https://res.cloudinary.com/demo/image/upload/banner.webp",
        },
      },
    });

    await uploadImage(file, onProgress, { pollIntervalMs: 0 });

    expect(onProgress).toHaveBeenCalledWith(30);
  });

  it("waits while the async upload is still processing", async () => {
    const file = new File(["image-bytes"], "logo.png", { type: "image/png" });
    mockPost.mockResolvedValue({
      data: {
        uploadId: "upload-789",
        status: "PENDING",
        statusUrl: "/api/uploads/images/upload-789",
      },
    });
    mockGet
      .mockResolvedValueOnce({
        data: {
          uploadId: "upload-789",
          status: "PROCESSING",
        },
      })
      .mockResolvedValueOnce({
        data: {
          uploadId: "upload-789",
          status: "COMPLETED",
          result: {
            url: "https://res.cloudinary.com/demo/image/upload/logo.png",
          },
        },
      });

    const url = await uploadImage(file, undefined, { pollIntervalMs: 0 });

    expect(url).toBe("https://res.cloudinary.com/demo/image/upload/logo.png");
    expect(mockGet).toHaveBeenCalledTimes(2);
  });
});
