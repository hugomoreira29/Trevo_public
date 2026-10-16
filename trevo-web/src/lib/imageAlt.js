export function productImageAlt(productName) {
  const name = productName?.trim();

  return name ? `Imagem do produto ${name}` : "Imagem do produto sem nome";
}

export function shopMediaAlt(type, shopName) {
  const name = shopName?.trim();

  return name ? `${type} da loja ${name}` : `${type} da loja sem nome`;
}
