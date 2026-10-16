function formatPrice(value) {
  return new Intl.NumberFormat("pt-PT", {
    style: "currency",
    currency: "EUR",
  }).format(Number(value || 0));
}

function formatDate(value) {
  if (!value) {
    return "Data indisponivel";
  }

  return new Intl.DateTimeFormat("pt-PT", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function buildReceiptText(order) {
  const items = order.items || [];

  return [
    "Recibo Trevo",
    `Encomenda: ${order.id}`,
    `Data: ${formatDate(order.createdAt)}`,
    `Loja: ${order.shopName || "N/A"}`,
    `Metodo de pagamento: ${order.paymentMethod || "N/A"}`,
    `Estado: ${order.status || "N/A"}`,
    "",
    "Items:",
    ...items.map(
      (item) =>
        `${item.productName} - ${item.quantity} x ${formatPrice(item.unitPrice)} = ${formatPrice(item.subtotal)}`
    ),
    "",
    `Total: ${formatPrice(order.totalAmount)}`,
  ].join("\n");
}

export function downloadReceiptPdf(order) {
  // PAY-F04 esta mock/frontend-ready ate PAY-B07 expor endpoints HTTP reais.
  const blob = new Blob([buildReceiptText(order)], { type: "application/pdf" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = `recibo-encomenda-${order.id}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
