import apiClient from "../lib/apiClient";

export async function createPaymentIntent(orderId) {
  const { data } = await apiClient.post("/payments/stripe/payment-intents", {
    orderId,
  });
  return data;
}

export async function capturePayPalPayment(paypalOrderId) {
  const { data } = await apiClient.post("/payments/paypal/capture", {
    paypalOrderId,
  });
  return data;
}
