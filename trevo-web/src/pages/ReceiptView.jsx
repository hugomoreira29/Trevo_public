import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Download, Receipt as ReceiptIcon } from "lucide-react";
import { downloadReceiptPdf } from "../lib/receiptDownload";
import { getBuyerOrder } from "../services/orderService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const statusLabelKeys = {
  PENDING_PAYMENT: "statuses.pendingPayment",
  PLACED: "statuses.placed",
  CONFIRMED: "statuses.confirmed",
  SHIPPED: "statuses.shipped",
  DELIVERED: "statuses.delivered",
  RETURNED: "statuses.returned",
  CANCELLED: "statuses.cancelled",
};

function formatPrice(value) {
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(value || 0));
}

function formatDate(value, t) {
  if (!value) return t("dateUnavailable");
  return new Intl.DateTimeFormat("pt-PT", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
}

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function statusLabel(status, t) {
  return statusLabelKeys[status] ? t(statusLabelKeys[status]) : status || "N/A";
}

export default function ReceiptView() {
  const { t } = useTranslation("orders");
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadOrder() {
      setIsLoading(true);
      setError("");

      try {
        const orderData = await getBuyerOrder(orderId);
        if (active) setOrder(orderData);
      } catch (loadError) {
        if (active) {
          setOrder(null);
          setError(getErrorMessage(loadError, t("loadReceiptFailed")));
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadOrder();
    return () => {
      active = false;
    };
  }, [orderId, t]);

  const items = order?.items || [];
  const isEmpty = !isLoading && !error && !order;

  return (
    <div className="page-shell max-w-3xl">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-clover-700">
            <ReceiptIcon className="w-4 h-4" strokeWidth={1.7} />
            <p className="eyebrow text-clover-700">{t("receiptInvoice")}</p>
          </div>
          <h1 className="h-page mt-2">
            {order ? t("orderNumber", { id: order.id }) : t("receiptTitle")}
          </h1>
        </div>
        {order && (
          <button
            type="button"
            onClick={() => downloadReceiptPdf(order)}
            className="btn-secondary"
          >
            <Download className="w-4 h-4" strokeWidth={1.8} />
            {t("downloadPdf")}
          </button>
        )}
      </div>

      {isLoading && <LoadingState label={t("loadingReceipt")} />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}
      {isEmpty && <p className="mt-6 text-ink-500">{t("receiptNotFound")}</p>}

      {!isLoading && !error && order && (
        <section className="mt-8 card card-pad p-6" aria-label={t("receiptData")}>
          <dl className="grid gap-x-4 gap-y-4 sm:grid-cols-2">
            <div>
              <dt className="text-xs text-ink-500 uppercase tracking-wider font-medium">{t("orderNumberLabel")}</dt>
              <dd className="mt-1 font-semibold tabular">#{order.id}</dd>
            </div>
            <div>
              <dt className="text-xs text-ink-500 uppercase tracking-wider font-medium">{t("date")}</dt>
              <dd className="mt-1 font-semibold tabular">{formatDate(order.createdAt, t)}</dd>
            </div>
            <div>
              <dt className="text-xs text-ink-500 uppercase tracking-wider font-medium">{t("paymentMethod")}</dt>
              <dd className="mt-1 font-semibold">{order.paymentMethod || "N/A"}</dd>
            </div>
            <div>
              <dt className="text-xs text-ink-500 uppercase tracking-wider font-medium">{t("status")}</dt>
              <dd className="mt-1 font-semibold">{statusLabel(order.status, t)}</dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="text-xs text-ink-500 uppercase tracking-wider font-medium">{t("shop")}</dt>
              <dd className="mt-1 font-semibold">
                {order.shopId ? (
                  <Link to={`/shops/${order.shopId}`} className="hover:text-clover-700 transition-colors">
                    {order.shopName || "N/A"}
                  </Link>
                ) : (order.shopName || "N/A")}
              </dd>
            </div>
          </dl>

          <hr className="my-6 border-ink-100" />

          <h2 className="text-base font-semibold mb-3">{t("items")}</h2>
          {items.length === 0 ? (
            <p className="text-sm text-ink-500">{t("emptyItems")}</p>
          ) : (
            <ul className="divide-y divide-ink-100">
              {items.map((item) => (
                <li
                  key={item.id || `${item.productId}-${item.variantId}`}
                  className="flex flex-wrap justify-between gap-4 py-3 text-sm"
                >
                  <div>
                    <p className="font-semibold">
                      <Link to={`/products/${item.productId}`} className="hover:text-clover-700 transition-colors">
                        {item.productName}
                      </Link>
                    </p>
                    <p className="text-xs text-ink-500 mt-0.5">
                      {t("quantityUnit", { quantity: item.quantity, unitPrice: formatPrice(item.unitPrice) })}
                    </p>
                  </div>
                  <p className="font-semibold tabular">{formatPrice(item.subtotal)}</p>
                </li>
              ))}
            </ul>
          )}

          <div className="mt-6 pt-4 border-t border-ink-200 flex justify-between items-baseline">
            <span className="text-sm text-ink-500">{t("total")}</span>
            <strong className="text-2xl font-bold tabular">{formatPrice(order.totalAmount)}</strong>
          </div>
        </section>
      )}
    </div>
  );
}
