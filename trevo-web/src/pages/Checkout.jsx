import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { CheckCircle2, ShoppingBag, CreditCard, Smartphone } from "lucide-react";
import { loadStripe } from "@stripe/stripe-js";
import StripePaymentForm from "./StripePaymentForm";
import { getCart } from "../services/cartService";
import { checkoutOrder } from "../services/orderService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";


const paymentMethods = [
  // Keep these values aligned with the backend PaymentMethod enum.
  { value: "MBWAY",  label: "MB WAY",  Icon: Smartphone },
  { value: "STRIPE", label: "Stripe",  Icon: CreditCard },
  { value: "PAYPAL", label: "PayPal",  Icon: CreditCard },
];

function formatPrice(value) {
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(value || 0));
}

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function orderTotal(orders) {
  return orders.reduce((total, order) => total + Number(order.totalAmount || 0), 0);
}

function normalizeMbWayPhone(phoneNumber) {
  const normalized = phoneNumber.replace(/\s/g, "");
  if (/^9\d{8}$/.test(normalized)) return normalized;
  if (/^\+3519\d{8}$/.test(normalized)) return normalized;
  return "";
}

function getStripePublishableKey() {
  return import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || "";
}

export default function Checkout() {
  const { t } = useTranslation("checkout");
  const [cart, setCart] = useState(null);
  const [orders, setOrders] = useState([]);
  const [pendingStripeOrders, setPendingStripeOrders] = useState([]);
  const [paymentMethod, setPaymentMethod] = useState("MBWAY");
  const [mbWayPhone, setMbWayPhone] = useState("");
  const [mbWayMessage, setMbWayMessage] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const stripePublishableKey = getStripePublishableKey();
  const stripePromise = useMemo(
    () => (stripePublishableKey ? loadStripe(stripePublishableKey) : null),
    [stripePublishableKey]
  );

  useEffect(() => {
    let active = true;

    async function loadCart() {
      try {
        const loadedCart = await getCart();

        if (active) {
          setCart(loadedCart);
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(getErrorMessage(loadError, t("loadCheckoutFailed")));
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadCart();

    return () => {
      active = false;
    };
  }, [t]);

  const shops = cart?.shops || [];
  const isEmpty = !isLoading && !error && shops.length === 0 && orders.length === 0;
  const hasValidCart = shops.length > 0;
  const isConfirmDisabled =
    isSubmitting ||
    orders.length > 0 ||
    pendingStripeOrders.length > 0 ||
    !hasValidCart ||
    !paymentMethod ||
    (paymentMethod === "STRIPE" && !stripePromise);
  const normalizedMbWayPhone = normalizeMbWayPhone(mbWayPhone);
  const isMbWaySubmitDisabled =
    isSubmitting || orders.length > 0 || !hasValidCart || !normalizedMbWayPhone;
  const successTotal = useMemo(() => orderTotal(orders), [orders]);
  const pendingStripeOrder = pendingStripeOrders[0] || null;

  function handlePaymentMethodChange(value) {
    setPaymentMethod(value);
    setError("");
    setMbWayMessage("");
  }

  async function handleConfirm() {
    if (isSubmitting) return;
    if (orders.length > 0) return;
    if (pendingStripeOrders.length > 0) return;
    if (!hasValidCart || !paymentMethod) return;
    if (paymentMethod === "STRIPE" && shops.length !== 1) {
      setError(t("stripeSingleShopOnly"));
      return;
    }
    if (paymentMethod === "STRIPE" && !stripePromise) {
      setError(t("stripeNotConfigured"));
      return;
    }

    setError("");

    try {
      setIsSubmitting(true);
      const result = await checkoutOrder(paymentMethod);
      const createdOrders = result.orders || result;

      if (paymentMethod === "STRIPE") {
        setPendingStripeOrders(createdOrders);
        return;
      }

      setOrders(createdOrders);
    } catch (checkoutError) {
      setError(getErrorMessage(checkoutError, t("confirmOrderFailed")));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handlePayPalPayment() {
    if (isSubmitting) return;
    if (orders.length > 0) return;
    if (!hasValidCart) return;

    setError("");
    setMbWayMessage("");

    try {
      setIsSubmitting(true);
      const result = await checkoutOrder("PAYPAL");
      if (result.approveUrl) {
        window.location.href = result.approveUrl;
        return;
      }
      setOrders(result.orders || result);
    } catch (checkoutError) {
      setError(getErrorMessage(checkoutError, t("startPayPalFailed")));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleMbWayPayment(event) {
    event.preventDefault();

    if (isMbWaySubmitDisabled) return;

    setError("");
    setMbWayMessage("");

    try {
      setIsSubmitting(true);
      await checkoutOrder("MBWAY", { phone: normalizedMbWayPhone });
      setMbWayMessage(t("mbWayStarted", { phone: normalizedMbWayPhone }));
    } catch (checkoutError) {
      setError(getErrorMessage(checkoutError, t("startMbWayFailed")));
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleStripeSuccess() {
    setOrders(pendingStripeOrders);
    setPendingStripeOrders([]);
  }

  if (orders.length > 0) {
    return (
      <div className="page-shell max-w-3xl text-center">
        <div className="w-14 h-14 mx-auto rounded-full bg-green-50 border border-green-200 flex items-center justify-center text-green-600">
          <CheckCircle2 className="w-7 h-7" strokeWidth={1.6} />
        </div>
        <h1 className="h-page mt-5">{t("orderConfirmed")}</h1>
        <p className="mt-3 text-ink-500">{t("checkoutRegistered")}</p>

        <section className="mt-10 text-left">
          <h2 className="text-base font-semibold mb-3">{t("createdOrders")}</h2>
          <ul className="space-y-3">
            {orders.map((order) => (
              <li key={order.id} className="card card-pad p-4">
                <p className="font-semibold">
                  {t("orderNumberShop", { id: order.id, shopName: order.shopName })}
                </p>
                <dl className="mt-1.5 text-sm text-ink-500 flex flex-wrap gap-x-4">
                  <span>{t("status")}: <span className="text-ink-900">{order.status}</span></span>
                  <span>{t("total")}: <span className="text-ink-900 tabular">{formatPrice(order.totalAmount)}</span></span>
                </dl>
              </li>
            ))}
          </ul>
          <p className="mt-6 text-right text-xl font-bold tabular">
            {t("total")}: {formatPrice(successTotal)}
          </p>
        </section>

        <Link to="/orders" className="btn-secondary mt-8 inline-flex">Ver as minhas encomendas</Link>
      </div>
    );
  }

  return (
    <div className="page-shell max-w-5xl">
      <h1 className="h-page">{t("checkout")}</h1>

      {isLoading && <LoadingState label={t("loadingCheckout")} />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}
      {isEmpty && (
        <div className="mt-10 card card-pad p-16 text-center">
          <div className="w-12 h-12 mx-auto rounded bg-ink-50 border border-ink-100 flex items-center justify-center text-ink-400">
            <ShoppingBag className="w-5 h-5" strokeWidth={1.7} />
          </div>
          <p className="mt-4 text-ink-500">{t("emptyCart")}</p>
        </div>
      )}

      {!isLoading && !isEmpty && shops.length > 0 && (
        <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,1fr)_360px]">
          <section aria-labelledby="order-summary-title">
            <h2 id="order-summary-title" className="text-lg font-semibold tracking-tight">
              {t("orderSummary")}
            </h2>

            <div className="mt-4 space-y-6">
              {shops.map((shop) => (
                <section key={shop.shopId} className="card card-pad p-5">
                  <div className="flex items-center justify-between gap-4 mb-3 pb-3 border-b border-ink-100">
                    <h3 className="font-semibold">{shop.shopName}</h3>
                    <p className="text-sm">
                      {t("subtotal")}: <span className="font-semibold tabular">{formatPrice(shop.subtotal)}</span>
                    </p>
                  </div>

                  <ul className="space-y-3">
                    {shop.items.map((item) => (
                      <li key={item.cartItemId || item.variantId} className="text-sm">
                        <div className="flex justify-between gap-4">
                          <div>
                            <p className="font-semibold">{item.productName}</p>
                            <p className="text-ink-500 text-xs mt-0.5">
                              {t("sizeColor", { size: item.size || "N/A", color: item.color || "N/A" })}
                            </p>
                            <p className="text-ink-500 text-xs mt-0.5">
                              {t("quantityUnit", { quantity: item.quantity, unitPrice: formatPrice(item.unitPrice) })}
                            </p>
                          </div>
                          <p className="font-semibold tabular">{formatPrice(item.subtotal)}</p>
                        </div>
                      </li>
                    ))}
                  </ul>
                </section>
              ))}
            </div>

            <div className="mt-6 flex items-baseline justify-between gap-4 pt-2">
              <span className="text-sm text-ink-500">{t("grandTotal")}</span>
              <strong className="text-2xl font-bold tabular">{formatPrice(cart.grandTotal)}</strong>
            </div>
          </section>

          <aside aria-labelledby="payment-title" className="card card-pad p-6 self-start">
            <h2 id="payment-title" className="text-lg font-semibold tracking-tight">
              {t("payment")}
            </h2>

            <fieldset className="mt-4">
              <legend className="field-label mb-2">{t("paymentMethod")}</legend>
              <div className="grid grid-cols-3 gap-2">
                {paymentMethods.map((method) => {
                  const { Icon } = method;
                  const selected = paymentMethod === method.value;
                  return (
                    <button
                      type="button"
                      key={method.value}
                      onClick={() => handlePaymentMethodChange(method.value)}
                      className={
                        "flex flex-col items-center gap-1.5 py-3 rounded border text-xs font-medium transition-colors " +
                        (selected
                          ? "bg-ink-900 text-white border-ink-900"
                          : "bg-white text-ink-700 border-ink-200 hover:bg-ink-50")
                      }
                    >
                      <Icon className="w-4 h-4" strokeWidth={1.7} />
                      {method.label}
                    </button>
                  );
                })}
              </div>
            </fieldset>

            {paymentMethod === "PAYPAL" && (
              <button
                type="button"
                onClick={handlePayPalPayment}
                disabled={isSubmitting || orders.length > 0 || !hasValidCart}
                className="btn-primary w-full mt-6 py-3"
              >
                {isSubmitting ? t("startingPayPal") : t("payWithPayPal")}
              </button>
            )}

            {paymentMethod === "MBWAY" && (
              <form onSubmit={handleMbWayPayment} className="mt-5">
                <div className="field">
                  <label className="field-label" htmlFor="mbWayPhone">{t("phoneNumber")}</label>
                  <input
                    id="mbWayPhone"
                    name="mbWayPhone"
                    type="tel"
                    value={mbWayPhone}
                    onChange={(event) => setMbWayPhone(event.target.value)}
                    className="input"
                    placeholder="9XX XXX XXX"
                  />
                </div>
                <button
                  type="submit"
                  disabled={isMbWaySubmitDisabled}
                  className="btn-primary w-full mt-4 py-3"
                >
                  {isSubmitting ? t("startingMbWay") : t("startMbWay")}
                </button>
                {mbWayMessage && <Alert variant="success" className="mt-3">{mbWayMessage}</Alert>}
              </form>
            )}

            {paymentMethod === "STRIPE" && !stripePromise && (
              <Alert variant="error" className="mt-5">{t("stripeNotConfigured")}</Alert>
            )}

            {paymentMethod === "STRIPE" && pendingStripeOrder ? (
              <StripePaymentForm
                order={pendingStripeOrder}
                onSuccess={handleStripeSuccess}
                stripePromise={stripePromise}
              />
            ) : paymentMethod === "STRIPE" && (
              <button
                type="button"
                onClick={handleConfirm}
                disabled={isConfirmDisabled}
                className="btn-primary w-full mt-6 py-3"
              >
                {isSubmitting ? t("confirming") : t("confirmOrder")}
              </button>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}
