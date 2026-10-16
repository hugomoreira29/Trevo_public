import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProductDetail from "./ProductDetail";

const {
  mockAddCartItem,
  mockGetProduct,
  mockGetProStatus,
  mockGetTryOnUsage,
  mockUploadBuyerPhotos,
  mockUseAuth,
} = vi.hoisted(() => ({
  mockAddCartItem: vi.fn(),
  mockGetProduct: vi.fn(),
  mockGetProStatus: vi.fn(),
  mockGetTryOnUsage: vi.fn(),
  mockUploadBuyerPhotos: vi.fn(),
  mockUseAuth: vi.fn(),
}));

vi.mock("../hooks/useAuth", () => ({
  useAuth: mockUseAuth,
}));

vi.mock("../services/cartService", () => ({
  addCartItem: mockAddCartItem,
}));

vi.mock("../services/productService", () => ({
  getProduct: mockGetProduct,
}));

vi.mock("../services/subscriptionService", () => ({
  getProStatus: mockGetProStatus,
}));

vi.mock("../services/tryOnService", () => ({
  getTryOnUsage: mockGetTryOnUsage,
}));

vi.mock("../services/userPhotoService", () => ({
  uploadBuyerPhotos: mockUploadBuyerPhotos,
}));

globalThis.React = React;

const baseProduct = {
  id: 1,
  shopId: 10,
  name: "Camisa Linho",
  description: "Camisa leve em linho.",
  price: 35,
  brand: "Trevo",
  materials: "Linho 100%",
  status: "ACTIVE",
  createdAt: "2026-01-01T00:00:00",
};

function renderProductDetail(initialPath = "/products/1") {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/products/:productId" element={<ProductDetail />} />
        </Routes>
      </MemoryRouter>
    );
  });

  return {
    container,
    unmount: () =>
      act(() => {
        root.unmount();
      }),
  };
}

async function flushPromises() {
  await act(async () => {
    await Promise.resolve();
  });
}

function clickButton(button) {
  act(() => {
    button.click();
  });
}

async function clickButtonAsync(button) {
  await act(async () => {
    button.click();
  });
}

async function submitForm(form) {
  await act(async () => {
    form.dispatchEvent(
      new Event("submit", {
        bubbles: true,
        cancelable: true,
      })
    );
  });
}

function buttonByText(container, text) {
  return [...container.querySelectorAll("button")].find(
    (button) => button.textContent === text
  );
}

function changeFiles(input, files) {
  Object.defineProperty(input, "files", {
    configurable: true,
    value: files,
  });

  act(() => {
    input.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

describe("ProductDetail", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      role: "BUYER",
    });
    mockAddCartItem.mockResolvedValue({});
    mockUploadBuyerPhotos.mockResolvedValue({
      photos: [
        { id: 1, url: "https://example.test/user-1.jpg" },
        { id: 2, url: "https://example.test/user-2.jpg" },
        { id: 3, url: "https://example.test/user-3.jpg" },
      ],
    });
    mockGetProStatus.mockRejectedValue(new Error("not needed"));
    mockGetTryOnUsage.mockRejectedValue(new Error("not needed"));
    globalThis.URL.createObjectURL = vi.fn((file) => `blob:${file.name}`);
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it("changes the main image when the carousel advances", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [
        { id: 1, imageUrl: "https://example.test/front.jpg", alt: "Frente" },
        { id: 2, imageUrl: "https://example.test/back.jpg", alt: "Costas" },
      ],
      variants: [],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    expect(mockGetProduct).toHaveBeenCalledWith("1");
    expect(container.querySelector('img[alt="Frente"]').src).toBe(
      "https://example.test/front.jpg"
    );

    clickButton(buttonByText(container, "Seguinte"));

    expect(container.querySelector("section img").src).toBe(
      "https://example.test/back.jpg"
    );

    unmount();
  });

  it("shows stock after selecting size and color", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [{ id: 1, productId: 1, size: "M", color: "Blue", stock: 3 }],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    clickButton(buttonByText(container, "M"));
    clickButton(buttonByText(container, "Blue"));

    expect(container.textContent).toContain("3 em stock");

    unmount();
  });

  it("keeps add to cart disabled until size and color are selected", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [{ id: 1, productId: 1, size: "M", color: "Blue", stock: 3 }],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    const addButton = buttonByText(container, "Adicionar ao carrinho");

    expect(addButton.disabled).toBe(true);

    clickButton(buttonByText(container, "M"));

    expect(addButton.disabled).toBe(true);

    clickButton(buttonByText(container, "Blue"));

    expect(addButton.disabled).toBe(false);

    unmount();
  });

  it("keeps add to cart disabled for a variant without stock", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [{ id: 2, productId: 1, size: "L", color: "Black", stock: 0 }],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    clickButton(buttonByText(container, "L"));
    clickButton(buttonByText(container, "Black"));

    expect(buttonByText(container, "Adicionar ao carrinho").disabled).toBe(true);

    unmount();
  });

  it("adds the selected variant to the cart and shows success", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [{ id: 7, productId: 1, size: "M", color: "Blue", stock: 3 }],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    clickButton(buttonByText(container, "M"));
    clickButton(buttonByText(container, "Blue"));
    await clickButtonAsync(buttonByText(container, "Adicionar ao carrinho"));

    expect(mockAddCartItem).toHaveBeenCalledWith(7, 1);
    expect(container.querySelector('[role="status"]').textContent).toContain(
      "Produto adicionado ao carrinho."
    );

    unmount();
  });

  it("shows an error when adding the selected variant fails", async () => {
    mockAddCartItem.mockRejectedValue({
      response: {
        data: {
          detail: "Requested quantity exceeds available stock",
        },
      },
    });
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [{ id: 7, productId: 1, size: "M", color: "Blue", stock: 3 }],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    clickButton(buttonByText(container, "M"));
    clickButton(buttonByText(container, "Blue"));
    await clickButtonAsync(buttonByText(container, "Adicionar ao carrinho"));

    expect(mockAddCartItem).toHaveBeenCalledWith(7, 1);
    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Requested quantity exceeds available stock"
    );

    unmount();
  });

  it("disables out-of-stock variant options", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [
        { id: 1, productId: 1, size: "M", color: "Blue", stock: 3 },
        { id: 2, productId: 1, size: "L", color: "Black", stock: 0 },
      ],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    expect(buttonByText(container, "L").disabled).toBe(true);

    unmount();
  });

  it("renders a fallback when the product has no images", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    expect(container.textContent).toContain("Sem imagem disponivel");
    expect(container.textContent).toContain("Camisa Linho");

    unmount();
  });

  it("shows the virtual try-on button on product detail", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    expect(buttonByText(container, "Experimentação virtual")).not.toBeUndefined();

    unmount();
  });

  it("opens a virtual try-on modal when clicking the button", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Experimentação virtual"));

    const dialog = container.querySelector('[role="dialog"]');

    expect(dialog).not.toBeNull();
    expect(dialog.getAttribute("aria-modal")).toBe("true");
    expect(dialog.textContent).toContain("Experimentação virtual");

    unmount();
  });

  it("shows at least four body type options in the virtual try-on modal", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Experimentação virtual"));

    expect(container.querySelectorAll('input[name="bodyType"]').length).toBeGreaterThanOrEqual(4);
    expect(container.textContent).toContain("Magro");
    expect(container.textContent).toContain("Atlético");
    expect(container.textContent).toContain("Regular");
    expect(container.textContent).toContain("Tamanho grande");

    unmount();
  });

  it("shows loading while generating and then displays the try-on image", async () => {
    vi.useFakeTimers();
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [
        { id: 1, imageUrl: "https://example.test/front.jpg", alt: "Frente" },
      ],
      variants: [],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Experimentação virtual"));
    await clickButtonAsync(buttonByText(container, "Gerar experimentação"));

    expect(container.textContent).toContain("A gerar experimentação...");

    await act(async () => {
      vi.advanceTimersByTime(150);
      await Promise.resolve();
    });

    const resultImage = container.querySelector(
      'img[alt="Resultado da experimentação virtual Magro"]'
    );

    expect(resultImage).not.toBeNull();
    expect(resultImage.src).toBe("https://example.test/front.jpg");

    unmount();
    vi.useRealTimers();
  });

  it("accepts exactly three See It On Yourself images and shows previews", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [
        { id: 1, imageUrl: "https://example.test/front.jpg", alt: "Frente" },
      ],
      variants: [],
    });
    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
      new File(["photo-3"], "photo-3.png", { type: "image/png" }),
    ]);

    expect(container.querySelectorAll('img[alt^="Pré-visualização Ver em si"]').length).toBe(3);
    expect(container.querySelector('img[alt="Pré-visualização Ver em si 1"]').src).toBe(
      "blob:photo-1.png"
    );
    expect(container.querySelector('[role="alert"]')).toBeNull();

    unmount();
  });

  it("shows an error when See It On Yourself does not have exactly three images", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
    ]);
    await clickButtonAsync(buttonByText(container, "Gerar resultado"));

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Seleciona exatamente 3 imagens."
    );
    expect(mockUploadBuyerPhotos).not.toHaveBeenCalled();

    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
      new File(["photo-3"], "photo-3.png", { type: "image/png" }),
      new File(["photo-4"], "photo-4.png", { type: "image/png" }),
    ]);

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Seleciona exatamente 3 imagens."
    );

    unmount();
  });

  it("keeps the See It On Yourself submit button disabled without exactly three images", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));

    expect(buttonByText(container, "Gerar resultado").disabled).toBe(true);

    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
    ]);

    expect(buttonByText(container, "Gerar resultado").disabled).toBe(true);

    unmount();
  });

  it("shows the service error when a See It On Yourself file is not an image", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    mockUploadBuyerPhotos.mockRejectedValueOnce(
      new Error("Todos os ficheiros têm de ser imagens")
    );
    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["not-image"], "not-image.txt", { type: "text/plain" }),
      new File(["photo-3"], "photo-3.png", { type: "image/png" }),
    ]);
    await submitForm(container.querySelector('[role="dialog"] form'));
    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Todos os ficheiros têm de ser imagens"
    );

    unmount();
  });

  it("shows the service error when a See It On Yourself image is above 10MB", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    mockUploadBuyerPhotos.mockRejectedValueOnce(
      new Error("Cada imagem deve ter no máximo 10MB")
    );
    const largeImage = new File(
      [new Uint8Array(10 * 1024 * 1024 + 1)],
      "large.png",
      { type: "image/png" }
    );
    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      largeImage,
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
      new File(["photo-3"], "photo-3.png", { type: "image/png" }),
    ]);
    await submitForm(container.querySelector('[role="dialog"] form'));
    await flushPromises();

    expect(container.querySelector('[role="alert"]').textContent).toContain(
      "Cada imagem deve ter no máximo 10MB"
    );

    unmount();
  });

  it("uploads See It On Yourself photos and shows the mock result", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [
        { id: 1, imageUrl: "https://example.test/front.jpg", alt: "Frente" },
      ],
      variants: [],
    });
    const { container, unmount } = renderProductDetail();
    const files = [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
      new File(["photo-3"], "photo-3.png", { type: "image/png" }),
    ];

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), files);
    await flushPromises();
    await submitForm(container.querySelector('[role="dialog"] form'));
    await flushPromises();

    expect(mockUploadBuyerPhotos).toHaveBeenCalledWith(files);
    expect(container.querySelector('[role="status"]').textContent).toContain(
      "Fotos carregadas com sucesso."
    );
    expect(container.querySelector('img[alt="Resultado Ver em si"]').src).toBe(
      "https://example.test/front.jpg"
    );

    unmount();
  });

  it("revokes See It On Yourself preview URLs when closing the modal", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
      new File(["photo-3"], "photo-3.png", { type: "image/png" }),
    ]);
    clickButton(buttonByText(container, "Fechar"));

    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith("blob:photo-1.png");
    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith("blob:photo-2.png");
    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith("blob:photo-3.png");
    expect(container.querySelector('[role="dialog"]')).toBeNull();

    unmount();
  });

  it("revokes See It On Yourself preview URLs when replacing images", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Ver em si"));
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-1"], "photo-1.png", { type: "image/png" }),
      new File(["photo-2"], "photo-2.png", { type: "image/png" }),
      new File(["photo-3"], "photo-3.png", { type: "image/png" }),
    ]);
    changeFiles(container.querySelector('input[name="seeItPhotos"]'), [
      new File(["photo-4"], "photo-4.png", { type: "image/png" }),
      new File(["photo-5"], "photo-5.png", { type: "image/png" }),
      new File(["photo-6"], "photo-6.png", { type: "image/png" }),
    ]);

    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith("blob:photo-1.png");
    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith("blob:photo-2.png");
    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith("blob:photo-3.png");

    unmount();
  });

  it("renders ReviewForm only for authenticated buyers", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });

    const { container, unmount } = renderProductDetail();

    await flushPromises();

    expect(container.textContent).toContain("Avaliar esta loja");

    unmount();
  });

  it("does not render ReviewForm for shop owners or visitors", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      role: "SHOP_OWNER",
    });

    const shopOwnerRender = renderProductDetail();

    await flushPromises();

    expect(shopOwnerRender.container.textContent).not.toContain("Avaliar esta loja");

    shopOwnerRender.unmount();

    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      role: null,
    });

    const visitorRender = renderProductDetail();

    await flushPromises();

    expect(visitorRender.container.textContent).not.toContain("Avaliar esta loja");

    visitorRender.unmount();
  });

  it("shows PRO badge and Ilimitado in the Virtual Try-On modal for Pro users", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    mockGetProStatus.mockResolvedValue({ pro: true });
    mockGetTryOnUsage.mockResolvedValue({ used: false });

    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Experimentação virtual"));
    await flushPromises();

    expect(container.textContent).toContain("PRO · Ilimitado");

    unmount();
  });

  it("shows 1 free try-on counter in the Virtual Try-On modal for free users who have not used it", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    mockGetProStatus.mockResolvedValue({ pro: false });
    mockGetTryOnUsage.mockResolvedValue({ used: false });

    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Experimentação virtual"));
    await flushPromises();

    expect(container.textContent).toContain("1 experimento gratuito");

    unmount();
  });

  it("shows upgrade prompt in the Virtual Try-On modal when free tier is exhausted", async () => {
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [],
      variants: [],
    });
    mockGetProStatus.mockResolvedValue({ pro: false });
    mockGetTryOnUsage.mockResolvedValue({ used: true });

    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Experimentação virtual"));
    await flushPromises();

    expect(container.textContent).toContain("Experimenta gratuito esgotado.");
    expect(container.textContent).toContain("Atualiza para Pro");
    expect(buttonByText(container, "Gerar experimentação").disabled).toBe(true);

    unmount();
  });

  it("marks free try-on as used after generating and shows upgrade prompt", async () => {
    vi.useFakeTimers();
    mockGetProduct.mockResolvedValue({
      ...baseProduct,
      images: [
        { id: 1, imageUrl: "https://example.test/front.jpg", alt: "Frente" },
      ],
      variants: [],
    });
    mockGetProStatus.mockResolvedValue({ pro: false });
    mockGetTryOnUsage.mockResolvedValue({ used: false });

    const { container, unmount } = renderProductDetail();

    await flushPromises();
    clickButton(buttonByText(container, "Experimentação virtual"));
    await flushPromises();

    expect(container.textContent).toContain("1 experimento gratuito");

    await clickButtonAsync(buttonByText(container, "Gerar experimentação"));

    await act(async () => {
      vi.advanceTimersByTime(150);
      await Promise.resolve();
    });

    expect(container.textContent).toContain("Experimenta gratuito esgotado.");
    expect(buttonByText(container, "Gerar experimentação").disabled).toBe(true);

    unmount();
    vi.useRealTimers();
  });
});
