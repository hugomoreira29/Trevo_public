import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  Store,
  TrendingUp,
  ShoppingBag,
  Plus,
  ArrowRight,
  Receipt,
  LayoutGrid,
} from "lucide-react";
import { getShopDashboard } from "../services/shopService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const statusLabelKeys = {
  PENDING: "statuses.pending",
  ACTIVE: "statuses.active",
  SUSPENDED: "statuses.suspended",
};

const shopStatusBadge = {
  PENDING:   "badge-warning",
  ACTIVE:    "badge-success",
  SUSPENDED: "badge-danger",
};

const orderStatusLabelKeys = {
  PENDING_PAYMENT: "statuses.pendingPayment",
  PLACED: "statuses.placed",
  CONFIRMED: "statuses.confirmed",
  SHIPPED: "statuses.shipped",
  DELIVERED: "statuses.delivered",
  RETURNED: "statuses.returned",
  CANCELLED: "statuses.cancelled",
};

const orderBadge = {
  PENDING_PAYMENT: "badge-warning",
  PLACED:          "badge-info",
  CONFIRMED:       "badge-info",
  SHIPPED:         "badge-info",
  DELIVERED:       "badge-success",
  RETURNED:        "badge-neutral",
  CANCELLED:       "badge-danger",
};

function getStatusLabel(status, t) {
  return statusLabelKeys[status] ? t(statusLabelKeys[status]) : t("unknown");
}

function getOrderStatusLabel(status, t) {
  return orderStatusLabelKeys[status]
    ? t(orderStatusLabelKeys[status], { ns: "orders" })
    : t("unknown");
}

function formatRevenue(total, currency) {
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency }).format(total);
}

export default function ShopDashboard() {
  const { t } = useTranslation("shop");
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadDashboard() {
      try {
        const shopDashboard = await getShopDashboard();
        if (active) {
          setDashboard(shopDashboard);
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(
            loadError.response?.data?.message ||
              loadError.response?.data?.detail ||
              t("loadDashboardFailed")
          );
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadDashboard();
    return () => {
      active = false;
    };
  }, [t]);

  const shop = dashboard?.shop;
  const orders = dashboard?.orders || [];
  const revenue = dashboard?.revenue || { total: 0, currency: "EUR" };

  return (
    <div className="page-shell max-w-5xl">
      <p className="eyebrow text-clover-700">{t("shopDashboard")}</p>
      <h1 className="h-page mt-2">{shop?.name || t("shopDashboard")}</h1>

      {isLoading && <LoadingState label={t("loadingDashboard")} />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}

      {!isLoading && !error && shop && (
        <>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            <span className={`badge ${shopStatusBadge[shop.status] || "badge-neutral"}`}>
              {getStatusLabel(shop.status, t)}
            </span>
            {shop.description && (
              <p className="text-sm text-ink-500 max-w-2xl">{shop.description}</p>
            )}
          </div>

          {/* KPI cards */}
          <div className="mt-8 grid gap-4 md:grid-cols-3">
            <StatCard
              Icon={TrendingUp}
              label={t("revenue")}
              value={formatRevenue(revenue.total, revenue.currency)}
              actionTo="/shop/payouts"
              actionLabel={t("viewPayouts")}
              tabular
            />
            <StatCard
              Icon={ShoppingBag}
              label={t("orders")}
              value={orders.length}
              actionTo="/shop/orders"
              actionLabel="Gerir encomendas"
            />
            <StatCard
              Icon={Store}
              label="Catálogo"
              value={dashboard.productCount ?? 0}
              actionTo="/shop/products"
              actionLabel="Gerir produtos"
            />
            <StatCard
              Icon={LayoutGrid}
              label="Coleções"
              value="—"
              actionTo={`/shops/${shop.id}/collections`}
              actionLabel="Gerir coleções"
            />
          </div>

          {/* Orders list */}
          <section className="mt-10">
            <div className="flex items-baseline justify-between gap-4 mb-3">
              <h2 className="text-lg font-semibold tracking-tight">{t("orders")}</h2>
              <Link to="/shop/orders" className="text-sm text-clover-700 hover:text-clover-800 font-medium inline-flex items-center gap-1">
                Ver tudo
                <ArrowRight className="w-3.5 h-3.5" strokeWidth={1.8} />
              </Link>
            </div>

            {orders.length === 0 ? (
              <div className="card card-pad p-10 text-center text-ink-500">{t("noOrders")}</div>
            ) : (
              <ul className="card divide-y divide-ink-100">
                {orders.slice(0, 10).map((order) => (
                  <li
                    key={order.id}
                    className="flex items-center justify-between gap-4 p-4"
                  >
                    <div>
                      <p className="font-semibold tabular text-sm">#{order.id}</p>
                      {order.buyerName && (
                        <p className="text-xs text-ink-500 mt-0.5">{order.buyerName}</p>
                      )}
                    </div>
                    <span className={`badge ${orderBadge[order.status] || "badge-neutral"}`}>
                      {getOrderStatusLabel(order.status, t)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  );
}

function StatCard({ Icon, label, value, actionTo, actionLabel, actionIcon: ActionIcon = ArrowRight, tabular = false }) {
  return (
    <div className="card card-pad p-5">
      <div className="flex items-center gap-2 text-ink-500">
        <Icon className="w-4 h-4" strokeWidth={1.7} />
        <span className="text-xs uppercase tracking-wider font-medium">{label}</span>
      </div>
      <p className={"mt-2 text-2xl font-bold " + (tabular ? "tabular" : "")}>{value}</p>
      {actionTo && (
        <Link
          to={actionTo}
          className="mt-3 text-sm text-clover-700 hover:text-clover-800 font-medium inline-flex items-center gap-1"
        >
          {actionLabel}
          <ActionIcon className="w-3.5 h-3.5" strokeWidth={1.8} />
        </Link>
      )}
    </div>
  );
}
