import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CreateShop from "./CreateShop";

const { mockCreateShop, mockUploadImage } = vi.hoisted(() => ({
  mockCreateShop: vi.fn(),
  mockUploadImage: vi.fn(),
}));

vi.mock("../services/shopService", () => ({
  createShop: mockCreateShop,
}));

vi.mock("../services/imageUploadService", () => ({
  uploadImage: mockUploadImage,
}));

globalThis.React = React;

function renderCreateShop() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(<CreateShop />);
  });

  return {
    container,
    unmount: () =>
      act(() => {
        root.unmount();
      }),
  };
}

function changeField(field, value) {
  const valueSetter = Object.getOwnPropertyDescriptor(
    field.constructor.prototype,
    "value"
  ).set;

  act(() => {
    valueSetter.call(field, value);
    field.dispatchEvent(new Event("input", { bubbles: true }));
  });
}

function changeFile(field, file) {
  Object.defineProperty(field, "files", {
    configurable: true,
    value: [file],
  });

  act(() => {
    field.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

async function submitForm(container) {
  await act(async () => {
    container.querySelector("form").dispatchEvent(
      new Event("submit", {
        bubbles: true,
        cancelable: true,
      })
    );
  });
}

describe("CreateShop", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL: vi.fn((file) => `blob:${file.name}`),
      revokeObjectURL: vi.fn(),
    });
  });

  it("validates required fields", async () => {
    const { container, unmount } = renderCreateShop();

    await submitForm(container);

    expect(mockCreateShop).not.toHaveBeenCalled();
    expect(container.textContent).toContain("Preenche nome, descrição e NIF");

    unmount();
  });

  it("validates NIF format before submitting", async () => {
    const { container, unmount } = renderCreateShop();

    changeField(container.querySelector('input[name="name"]'), "Loja Trevo");
    changeField(container.querySelector('textarea[name="description"]'), "Moda circular");
    changeField(container.querySelector('input[name="nif"]'), "123");

    await submitForm(container);

    expect(mockCreateShop).not.toHaveBeenCalled();
    expect(container.textContent).toContain(
      "O NIF deve ter exatamente 9 dígitos"
    );

    unmount();
  });

  it("submits valid shop data and resets the form after success", async () => {
    mockCreateShop.mockResolvedValue({ id: 1 });
    const { container, unmount } = renderCreateShop();

    const nameInput = container.querySelector('input[name="name"]');
    const descriptionInput = container.querySelector('textarea[name="description"]');
    const nifInput = container.querySelector('input[name="nif"]');

    changeField(nameInput, "Loja Trevo");
    changeField(descriptionInput, "Moda circular");
    changeField(nifInput, "123456789");

    await submitForm(container);

    expect(mockCreateShop).toHaveBeenCalledWith({
      name: "Loja Trevo",
      description: "Moda circular",
      nif: "123456789",
    });
    expect(container.textContent).toContain("Loja criada com sucesso");
    expect(nameInput.value).toBe("");
    expect(descriptionInput.value).toBe("");
    expect(nifInput.value).toBe("");

    unmount();
  });

  it("previews selected logo and banner images before upload", () => {
    const { container, unmount } = renderCreateShop();

    changeFile(
      container.querySelector('input[name="logo"]'),
      new File(["logo"], "logo.png", { type: "image/png" })
    );
    changeFile(
      container.querySelector('input[name="banner"]'),
      new File(["banner"], "banner.webp", { type: "image/webp" })
    );

    expect(container.querySelector('img[alt="Pré-visualização do logo"]').src).toBe(
      "blob:logo.png"
    );
    expect(container.querySelector('img[alt="Pré-visualização do banner"]').src).toBe(
      "blob:banner.webp"
    );

    unmount();
  });

  it("uploads selected images with progress before creating the shop", async () => {
    let resolveLogoUpload;
    let resolveBannerUpload;

    mockCreateShop.mockResolvedValue({ id: 1 });
    mockUploadImage
      .mockImplementationOnce((_file, onProgress) => {
        onProgress(45);
        return new Promise((resolve) => {
          resolveLogoUpload = () => resolve("https://res.cloudinary.com/demo/logo.png");
        });
      })
      .mockImplementationOnce((_file, onProgress) => {
        onProgress(80);
        return new Promise((resolve) => {
          resolveBannerUpload = () => resolve("https://res.cloudinary.com/demo/banner.webp");
        });
      });

    const { container, unmount } = renderCreateShop();

    changeField(container.querySelector('input[name="name"]'), "Loja Trevo");
    changeField(container.querySelector('textarea[name="description"]'), "Moda circular");
    changeField(container.querySelector('input[name="nif"]'), "123456789");
    changeFile(
      container.querySelector('input[name="logo"]'),
      new File(["logo"], "logo.png", { type: "image/png" })
    );
    changeFile(
      container.querySelector('input[name="banner"]'),
      new File(["banner"], "banner.webp", { type: "image/webp" })
    );

    await act(async () => {
      container.querySelector("form").dispatchEvent(
        new Event("submit", {
          bubbles: true,
          cancelable: true,
        })
      );
      await Promise.resolve();
    });

    expect(mockUploadImage).toHaveBeenCalledTimes(2);
    expect(container.querySelector('progress[aria-label="Progresso do upload do logo"]').value).toBe(
      45
    );
    expect(
      container.querySelector('progress[aria-label="Progresso do upload do banner"]').value
    ).toBe(80);

    await act(async () => {
      resolveLogoUpload();
      resolveBannerUpload();
      await Promise.resolve();
    });

    expect(mockCreateShop).toHaveBeenCalledWith({
      name: "Loja Trevo",
      description: "Moda circular",
      nif: "123456789",
      logoUrl: "https://res.cloudinary.com/demo/logo.png",
      bannerUrl: "https://res.cloudinary.com/demo/banner.webp",
    });

    unmount();
  });
});
