import { useState } from "react";
import { useTranslation } from "react-i18next";
import { CardElement, Elements, useElements, useStripe } from "@stripe/react-stripe-js";
import { createPaymentIntent } from "../services/paymentService";

function getErrorMessage(error, fallback, unavailableMessage) {
  if (error.response?.status === 409) {
    return unavailableMessage;
  }

  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    error.message ||
    fallback
  );
}

function StripePaymentFormFields({ order, onSuccess }) {
  const { t } = useTranslation("checkout");
  const stripe = useStripe();
  const elements = useElements();
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();

    if (!stripe || !elements || isSubmitting) {
      return;
    }

    setError("");
    setIsSubmitting(true);

    try {
      const paymentIntent = await createPaymentIntent(order.id);
      const cardElement = elements.getElement(CardElement);
      const result = await stripe.confirmCardPayment(paymentIntent.clientSecret, {
        payment_method: {
          card: cardElement,
        },
      });

      if (result.error) {
        setError(result.error.message || t("confirmStripePaymentFailed"));
        return;
      }

      onSuccess(result.paymentIntent);
    } catch (submitError) {
      setError(
        getErrorMessage(
          submitError,
          t("startStripePaymentFailed"),
          t("stripeUnavailableForOrder")
        )
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-5 border border-gray-200 bg-gray-50 p-4">
      <h3 className="font-semibold">{t("stripePayment")}</h3>
      <div className="mt-3 border border-gray-300 bg-white p-3">
        <CardElement />
      </div>
      {error && (
        <p className="mt-3 text-sm text-red-600" role="alert">
          {error}
        </p>
      )}
      <button
        type="submit"
        disabled={!stripe || !elements || isSubmitting}
        className="mt-4 w-full bg-black px-4 py-3 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isSubmitting ? t("confirmingStripePayment") : t("confirmStripePayment")}
      </button>
    </form>
  );
}

export default function StripePaymentForm({ order, onSuccess, stripePromise }) {
  const { t } = useTranslation("checkout");

  if (!stripePromise) {
    return (
      <p className="mt-5 text-red-600" role="alert">
        {t("stripeNotConfigured")}
      </p>
    );
  }

  return (
    <Elements stripe={stripePromise}>
      <StripePaymentFormFields order={order} onSuccess={onSuccess} />
    </Elements>
  );
}
