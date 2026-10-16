import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PublicShopProfile from "./PublicShopProfile";

const { mockGetShopProfile } = vi.hoisted(() => ({
  mockGetShopProfile: vi.fn(),
}));

vi.mock("../services/shopService", () => ({
  getShopProfile: mockGetShopProfile,
}));

vi.mock("../components/ReviewsSection", () => ({
  default: ({ shopId }) => <section data-testid="reviews-section">Reviews {shopId}</section>,
}));

globalThis.React = React;

function renderPublicShopProfile() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/shops/1"]}>
        <Routes>
          <Route path="/shops/:shopId" element={<PublicShopProfile />} />
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

function collectionNames(container) {
  return [...container.querySelectorAll("section li a")].map(
    (link) => link.textContent
  );
}

describe("PublicShopProfile", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("displays shop media, description, rating, ordered collections, and product links", async () => {
    mockGetShopProfile.mockResolvedValue({
      id: 1,
      name: "Vintage Porto",
      description: "Roupa vintage curada.",
      logoUrl: "https://example.com/logo.png",
      bannerUrl: "https://example.com/banner.png",
      averageRating: 4.5555,
      collections: [
        { id: 2, shopId: 1, name: "Casacos", displayOrder: 2 },
        { id: 1, shopId: 1, name: "Vestidos", displayOrder: 1 },
      ],
    });

    const { container, unmount } = renderPublicShopProfile();

    await flushPromises();

    const logo = container.querySelector('img[alt="Logo da loja Vintage Porto"]');
    const banner = container.querySelector('img[alt="Banner da loja Vintage Porto"]');
    const allProductsLink = [...container.querySelectorAll("a")].find(
      (link) => link.textContent === "Ver todos os produtos"
    );

    expect(mockGetShopProfile).toHaveBeenCalledWith("1");
    expect(logo.src).toBe("https://example.com/logo.png");
    expect(banner.src).toBe("https://example.com/banner.png");
    expect(container.textContent).toContain("Roupa vintage curada.");
    expect(container.textContent).toContain("Avaliação: 4.6");
    expect(collectionNames(container)).toEqual(["Vestidos", "Casacos"]);
    expect(allProductsLink.getAttribute("href")).toBe("/shops/1/products");
    expect(
      container.querySelector('a[href="/shops/1/products?collectionId=1"]').textContent
    ).toBe("Vestidos");
    expect(
      container.querySelector('a[href="/shops/1/products?collectionId=2"]').textContent
    ).toBe("Casacos");

    unmount();
  });

  it("shows no-rating and no-collections messages when there is no rating or collections", async () => {
    mockGetShopProfile.mockResolvedValue({
      id: 1,
      name: "Vintage Porto",
      description: "Roupa vintage curada.",
      logoUrl: null,
      bannerUrl: null,
      averageRating: null,
      collections: [],
    });

    const { container, unmount } = renderPublicShopProfile();

    await flushPromises();

    expect(container.textContent).toContain("Sem avaliações");
    expect(container.textContent).toContain("Coleções");
    expect(container.textContent).toContain("Sem coleções.");
    expect(container.querySelector("section ul")).toBeNull();

    unmount();
  });
});
