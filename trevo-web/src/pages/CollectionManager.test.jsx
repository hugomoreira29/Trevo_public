import React from "react";
import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CollectionManager from "./CollectionManager";

const {
  mockCreateCollection,
  mockDeleteCollection,
  mockGetShopProfile,
  mockRenameCollection,
  mockReorderCollections,
} = vi.hoisted(() => ({
  mockCreateCollection: vi.fn(),
  mockDeleteCollection: vi.fn(),
  mockGetShopProfile: vi.fn(),
  mockRenameCollection: vi.fn(),
  mockReorderCollections: vi.fn(),
}));

vi.mock("../services/shopService", () => ({
  createCollection: mockCreateCollection,
  deleteCollection: mockDeleteCollection,
  getShopProfile: mockGetShopProfile,
  renameCollection: mockRenameCollection,
  reorderCollections: mockReorderCollections,
}));

globalThis.React = React;

function renderCollectionManager() {
  const container = document.createElement("div");
  const root = createRoot(container);

  act(() => {
    root.render(
      <MemoryRouter initialEntries={["/shops/1/collections"]}>
        <Routes>
          <Route path="/shops/:shopId/collections" element={<CollectionManager />} />
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

function changeInput(input, value) {
  const valueSetter = Object.getOwnPropertyDescriptor(
    HTMLInputElement.prototype,
    "value"
  ).set;

  act(() => {
    valueSetter.call(input, value);
    input.dispatchEvent(new Event("input", { bubbles: true }));
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

function collectionNames(container) {
  return [...container.querySelectorAll("li span")].map((node) => node.textContent);
}

describe("CollectionManager", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders collections in order, creates a collection, and deletes with confirmation", async () => {
    mockGetShopProfile.mockResolvedValue({
      name: "Loja Trevo",
      collections: [
        { id: 2, shopId: 1, name: "Casacos", displayOrder: 2 },
        { id: 1, shopId: 1, name: "Vestidos", displayOrder: 1 },
      ],
    });
    mockCreateCollection.mockResolvedValue({
      id: 3,
      shopId: 1,
      name: "Novidades",
      displayOrder: 3,
    });
    mockDeleteCollection.mockResolvedValue(undefined);

    const { container, unmount } = renderCollectionManager();

    await flushPromises();

    expect(mockGetShopProfile).toHaveBeenCalledWith("1");
    expect(collectionNames(container)).toEqual(["Vestidos", "Casacos"]);

    changeInput(container.querySelector('input[name="collectionName"]'), "Novidades");
    await submitForm(container.querySelector("form"));

    expect(mockCreateCollection).toHaveBeenCalledWith("1", "Novidades");
    expect(collectionNames(container)).toEqual(["Vestidos", "Casacos", "Novidades"]);

    const deleteButton = [...container.querySelectorAll("button")].find(
      (button) => button.textContent === "Apagar"
    );

    act(() => {
      deleteButton.click();
    });

    expect(container.querySelector('[role="dialog"]').textContent).toContain(
      "Apagar a coleção Vestidos?"
    );

    act(() => {
      [...container.querySelectorAll("button")]
        .find((button) => button.textContent === "Confirmar")
        .click();
    });

    await flushPromises();

    expect(mockDeleteCollection).toHaveBeenCalledWith("1", 1);
    expect(collectionNames(container)).toEqual(["Casacos", "Novidades"]);

    unmount();
  });

  it("shows an empty state when the shop has no collections", async () => {
    mockGetShopProfile.mockResolvedValue({
      name: "Loja Trevo",
      collections: [],
    });

    const { container, unmount } = renderCollectionManager();

    await flushPromises();

    expect(container.textContent).toContain("Gerir coleções");
    expect(container.textContent).toContain("Esta loja ainda não tem coleções.");
    expect(container.querySelector('input[name="collectionName"]').placeholder).toBe(
      "Nome da coleção"
    );

    unmount();
  });
});
