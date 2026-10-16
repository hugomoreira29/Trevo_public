import { describe, expect, it } from "vitest";
import { productImageAlt, shopMediaAlt } from "./imageAlt";

describe("image alt helpers", () => {
  it("builds product image alt text from a product name", () => {
    expect(productImageAlt("Vestido Azul")).toBe("Imagem do produto Vestido Azul");
  });

  it("uses a product fallback for empty names", () => {
    expect(productImageAlt("")).toBe("Imagem do produto sem nome");
    expect(productImageAlt("   ")).toBe("Imagem do produto sem nome");
    expect(productImageAlt(undefined)).toBe("Imagem do produto sem nome");
  });

  it("builds shop media alt text from media type and shop name", () => {
    expect(shopMediaAlt("Logo", "Loja Trevo")).toBe("Logo da loja Loja Trevo");
    expect(shopMediaAlt("Banner", "Loja Trevo")).toBe("Banner da loja Loja Trevo");
  });

  it("uses a shop fallback for empty names", () => {
    expect(shopMediaAlt("Logo", "")).toBe("Logo da loja sem nome");
    expect(shopMediaAlt("Banner", "   ")).toBe("Banner da loja sem nome");
    expect(shopMediaAlt("Logo", undefined)).toBe("Logo da loja sem nome");
  });
});
