import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { Receipt, ChevronLeft, ChevronRight, Inbox, CreditCard } from "lucide-react";
import { loadStripe } from "@stripe/stripe-js";
import { getBuyerOrder, getBuyerOrders } from "../services/orderService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";
import StripePaymentForm from "./StripePaymentForm";

const statusLabelKeys = {
  PENDING_PAYMENT: "statuses.pendingPayment",
  PLACED: "statuses.placed",
  CONFIRMED: "statuses.confirmed",
  SHIPPED: "statuses.shipped",
  DELIVERED: "statuses.delivered",
  RETURNED: "statuses.returned",
  CANCELLED: "statuses.cancelled",
};

const statusBadgeClass = {
  PENDING_PAYMENT: "badge-warning",
  PLACED:          "badge-info",
  CONFIRMED:       "badge-info",
  SHIPPED:         "badge-info",
  DELIVERED:       "badge-success",
  RETURNED:        "badge-neutral",
  CANCELLED:       "badge-danger",
};

const PAGE_SIZE = 20;

function formatPrice(value) {
  return new Intl.NumberFormat("pt-PT", {
    style: "currency",
    currency: "EUR",
  }).format(Number(value || 0));
}

function formatDate(value, t) {
  if (!value) return t("dateUnavailable");
  return new Intl.DateTimeFormat("pt-PT", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function statusLabel(status, t) {
  return statusLabelKeys[status] ? t(statusLabelKeys[status]) : status;
}

function StatusBadge({ status, t }) {
  return (
    <span className={`badge ${statusBadgeClass[status] || "badge-neutral"}`}>
      {statusLabel(status, t)}
    </span>
  );
}

export default function OrderHistory() {
  const { t } = useTranslation("orders");
  const [orderPage, setOrderPage] = useState(null);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [error, setError] = useState("");
  const [detailError, setDetailError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [isResumingPayment, setIsResumingPayment] = useState(false);
  const [paymentSuccessMessage, setPaymentSuccessMessage] = useState("");

  const stripePublishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || "";
  const stripePromise = useMemo(
    () => (stripePublishableKey ? loadStripe(stripePublishableKey) : null),
    [stripePublishableKey]
  );

  useEffect(() => {
    let active = true;

    async function loadOrders() {
      setIsLoading(true);
      setSelectedOrder(null);
      setSelectedOrderId(null);
      setDetailError("");

      try {
        const orders = await getBuyerOrders({ page, size: PAGE_SIZE });

        if (active) {
          setOrderPage(orders);
          setTotalPages(orders.totalPages || 0);
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(getErrorMessage(loadError, t("loadOrdersFailed")));
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadOrders();

    return () => {
      active = false;
    };
  }, [page, t]);

  async function handleSelectOrder(orderId) {
    setSelectedOrder(null);
    setSelectedOrderId(orderId);
    setDetailError("");
    setIsResumingPayment(false);
    setPaymentSuccessMessage("");
    setIsLoadingDetail(true);

    try {
      const order = await getBuyerOrder(orderId);
      setSelectedOrder(order);
    } catch (selectError) {
      setDetailError(getErrorMessage(selectError, t("loadDetailsFailed")));
    } finally {
      setIsLoadingDetail(false);
    }
  }

  async function handlePaymentSuccess() {
    setIsResumingPayment(false);
    setPaymentSuccessMessage(t("paymentResumed"));

    if (selectedOrderId) {
      try {
        const refreshedOrder = await getBuyerOrder(selectedOrderId);
        setSelectedOrder(refreshedOrder);
      } catch {
        // Order list will reflect updated status on next refresh; non-fatal.
      }
    }

    try {
      const refreshedPage = await getBuyerOrders({ page, size: PAGE_SIZE });
      setOrderPage(refreshedPage);
      setTotalPages(refreshedPage.totalPages || 0);
    } catch {
      // non-fatal
    }
  }

  const orders = orderPage?.orders || [];
  const isEmpty = !isLoading && !error && orders.length === 0;
  const isFirstPage = page === 0;
  const isLastPage = totalPages === 0 || page >= totalPages - 1;

  return (
    <div className="page-shell max-w-6xl">
      <h1 className="h-page">{t("ordersHistory")}</h1>

      {isLoading && <LoadingState label={t("loadingOrders")} />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}

      {isEmpty && (
        <div className="mt-10 card card-pad p-16 text-center">
          <div className="w-12 h-12 mx-auto rounded bg-ink-50 border border-ink-100 flex items-center justify-center text-ink-400">
            <Inbox className="w-5 h-5" strokeWidth={1.7} />
          </div>
          <p className="mt-4 text-ink-500">{t("emptyOrders")}</p>
        </div>
      )}

      {!isLoading && !error && orders.length > 0 && (
        <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(360px,0.9fr)]">
          <section aria-label={t("orderList")}>
            <ul className="space-y-3">
              {orders.map((order) => {
                const isSelected = selectedOrderId === order.id;
                return (
                  <li key={order.id}>
                    <button
                      type="button"
                      onClick={() => handleSelectOrder(order.id)}
                      className={
                        "w-full card text-left p-4 transition-colors " +
                        (isSelected
                          ? "border-clover-500 ring-1 ring-clover-500/40"
                          : "hover:border-ink-300")
                      }
                    >
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <p className="font-semibold tabular text-sm">
                          {t("orderNumber", { id: order.id })}
                        </p>
                        <StatusBadge status={order.status} t={t} />
                      </div>
                      <p className="mt-1.5 text-sm text-ink-800">
                        <Link to={`/shops/${order.shopId}`} className="hover:text-clover-700 transition-colors">
                          {order.shopName}
                        </Link>
                      </p>
                      <p className="text-xs text-ink-500">{formatDate(order.createdAt, t)}</p>
                      <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm">
                        <span>{t("total")}: <span className="font-semibold tabular">{formatPrice(order.totalAmount)}</span></span>
                        <span className="text-ink-500">{t("payment")}: {order.paymentMethod}</span>
                      </div>
                    </button>
                  </li>
                );
              })}
            </ul>

            <div className="mt-6 flex items-center justify-between gap-4">
              <button
                type="button"
                onClick={() => setPage((currentPage) => currentPage - 1)}
                disabled={isFirstPage}
                className="btn-secondary"
              >
                <ChevronLeft className="w-4 h-4" strokeWidth={1.8} />
                {t("previous", { ns: "common" })}
              </button>
              <span className="text-sm text-ink-500 tabular">
                {t("pageOf", { ns: "browse", page: page + 1, total: Math.max(totalPages, 1) })}
              </span>
              <button
                type="button"
                onClick={() => setPage((currentPage) => currentPage + 1)}
                disabled={isLastPage}
                className="btn-secondary"
              >
                {t("next", { ns: "common" })}
                <ChevronRight className="w-4 h-4" strokeWidth={1.8} />
              </button>
            </div>
          </section>

          <section aria-label={t("orderDetails")} className="lg:sticky lg:top-20 self-start">
            <h2 className="text-lg font-semibold tracking-tight">{t("details")}</h2>

            {isLoadingDetail && <LoadingState label={t("loadingDetails")} />}
            {detailError && <Alert variant="error" className="mt-3">{detailError}</Alert>}

            {!isLoadingDetail && !detailError && !selectedOrder && (
              <p className="mt-3 text-ink-500 text-sm">{t("selectOrder")}</p>
            )}

            {!isLoadingDetail && !detailError && selectedOrder && (
              <div className="mt-4 card card-pad p-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h3 className="font-semibold tabular text-sm">
                    {t("orderNumber", { id: selectedOrder.id })}
                  </h3>
                  <StatusBadge status={selectedOrder.status} t={t} />
                </div>
                <p className="mt-1.5 font-medium">
                  <Link to={`/shops/${selectedOrder.shopId}`} className="hover:text-clover-700 transition-colors">
                    {selectedOrder.shopName}
                  </Link>
                </p>
                {selectedOrder.trackingNumber && (
                  <p className="mt-2 text-sm text-ink-500">
                    {t("tracking")}: <span className="font-mono text-ink-900">{selectedOrder.trackingNumber}</span>
                  </p>
                )}

                <ul className="mt-5 space-y-3">
                  {selectedOrder.items.map((item) => (
                    <li key={item.id} className="border-t border-ink-100 pt-3">
                      <div className="flex justify-between gap-4">
                        <div>
                          <p className="font-semibold text-sm">
                            <Link to={`/products/${item.productId}`} className="hover:text-clover-700 transition-colors">
                              {item.productName}
                            </Link>
                          </p>
                          <p className="text-xs text-ink-500 mt-0.5">
                            {t("sizeColor", { size: item.size || "N/A", color: item.color || "N/A" })}
                          </p>
                          <p className="text-xs text-ink-500 mt-0.5">
                            {t("quantityUnit", { quantity: item.quantity, unitPrice: formatPrice(item.unitPrice) })}
                          </p>
                        </div>
                        <p className="font-semibold text-sm tabular">{formatPrice(item.subtotal)}</p>
                      </div>
                    </li>
                  ))}
                </ul>

                <div className="mt-5 pt-3 border-t border-ink-100 flex items-baseline justify-between">
                  <span className="text-sm text-ink-500">{t("total")}</span>
                  <strong className="text-xl font-bold tabular">{formatPrice(selectedOrder.totalAmount)}</strong>
                </div>

                {paymentSuccessMessage && (
                  <Alert variant="success" className="mt-4">{paymentSuccessMessage}</Alert>
                )}

                <div className="mt-5 flex flex-wrap gap-3">
                  {selectedOrder.status === "PENDING_PAYMENT"
                    && selectedOrder.paymentMethod === "STRIPE"
                    && !isResumingPayment && (
                    <button
                      type="button"
                      onClick={() => {
                        setIsResumingPayment(true);
                        setPaymentSuccessMessage("");
                      }}
                      className="btn-primary flex-1 min-w-[180px]"
                    >
                      <CreditCard className="w-4 h-4" strokeWidth={1.7} />
                      {t("resumePayment")}
                    </button>
                  )}

                  <Link
                    to={`/orders/${selectedOrder.id}/receipt`}
                    className="btn-secondary flex-1 min-w-[180px]"
                  >
                    <Receipt className="w-4 h-4" strokeWidth={1.7} />
                    {t("viewReceipt")}
                  </Link>
                </div>

                {isResumingPayment && (
                  <StripePaymentForm
                    order={selectedOrder}
                    onSuccess={handlePaymentSuccess}
                    stripePromise={stripePromise}
                  />
                )}
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
