import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ChevronLeft,
  ChevronRight,
  ArrowRight,
  RefreshCw,
  Inbox,
} from "lucide-react";
import { getShopOrders, updateOrderStatus } from "../services/orderService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const PAGE_SIZE = 20;

const statusLabels = {
  PENDING_PAYMENT: "Pagamento pendente",
  PLACED:          "Recebida",
  CONFIRMED:       "Confirmada",
  SHIPPED:         "Enviada",
  DELIVERED:       "Entregue",
};

const statusBadge = {
  PENDING_PAYMENT: "badge-warning",
  PLACED:          "badge-warning",
  CONFIRMED:       "badge-info",
  SHIPPED:         "badge-info",
  DELIVERED:       "badge-success",
};

const statusTabs = ["PENDING_PAYMENT", "PLACED", "CONFIRMED", "SHIPPED", "DELIVERED"];

const nextStatusByStatus = {
  PLACED:    "CONFIRMED",
  CONFIRMED: "SHIPPED",
  SHIPPED:   "DELIVERED",
};

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function ordersFromResponse(response) {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.orders)) return response.orders;
  if (Array.isArray(response?.content)) return response.content;
  return [];
}

function formatPrice(value) {
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(value || 0));
}

function formatDate(value) {
  if (!value) return "Data indisponível";
  return new Intl.DateTimeFormat("pt-PT", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
}

function statusLabel(status) {
  return statusLabels[status] || status;
}

function actionLabel(status) {
  const nextStatus = nextStatusByStatus[status];
  if (!nextStatus) return "";
  return `Marcar como ${statusLabel(nextStatus).toLowerCase()}`;
}

export default function OrderManagement() {
  const [orderPage, setOrderPage] = useState(null);
  const [page, setPage] = useState(0);
  const [activeStatus, setActiveStatus] = useState("PLACED");
  const [error, setError] = useState("");
  const [updateError, setUpdateError] = useState("");
  const [updatingOrderId, setUpdatingOrderId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadOrders() {
      setIsLoading(true);
      setError("");
      setUpdateError("");

      try {
        const orders = await getShopOrders({ page, size: PAGE_SIZE });
        if (active) setOrderPage(orders);
      } catch (loadError) {
        if (active) {
          setError(getErrorMessage(loadError, "Não foi possível carregar as encomendas."));
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadOrders();
    return () => {
      active = false;
    };
  }, [page]);

  const orders = useMemo(() => ordersFromResponse(orderPage), [orderPage]);
  const filteredOrders = useMemo(
    () => orders.filter((order) => order.status === activeStatus),
    [activeStatus, orders]
  );
  const totalPages = orderPage?.totalPages || 0;
  const isFirstPage = page === 0;
  const isLastPage = totalPages === 0 || page >= totalPages - 1;
  const isEmpty = !isLoading && !error && filteredOrders.length === 0;

  async function retryLoad() {
    setIsLoading(true);
    setError("");
    setUpdateError("");

    try {
      setOrderPage(await getShopOrders({ page, size: PAGE_SIZE }));
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Não foi possível carregar as encomendas."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleUpdateStatus(order) {
    const nextStatus = nextStatusByStatus[order.status];
    if (!nextStatus) return;

    setUpdatingOrderId(order.id);
    setUpdateError("");

    try {
      const updatedOrder = await updateOrderStatus(order.id, nextStatus);
      const nextOrder = {
        ...order,
        ...(updatedOrder || {}),
        status: updatedOrder?.status || nextStatus,
      };

      setOrderPage((currentPage) => {
        if (!currentPage) return currentPage;
        if (Array.isArray(currentPage)) {
          return currentPage.map((o) => (o.id === order.id ? nextOrder : o));
        }
        if (Array.isArray(currentPage.orders)) {
          return {
            ...currentPage,
            orders: currentPage.orders.map((o) => (o.id === order.id ? nextOrder : o)),
          };
        }
        if (Array.isArray(currentPage.content)) {
          return {
            ...currentPage,
            content: currentPage.content.map((o) => (o.id === order.id ? nextOrder : o)),
          };
        }
        return currentPage;
      });
      setActiveStatus(nextStatus);
    } catch (updateStatusError) {
      setUpdateError(
        getErrorMessage(updateStatusError, "Não foi possível atualizar o estado da encomenda.")
      );
    } finally {
      setUpdatingOrderId(null);
    }
  }

  return (
    <div className="page-shell">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="eyebrow text-clover-700">Loja</p>
          <h1 className="h-page mt-2">Gestão de encomendas</h1>
        </div>
      </div>

      <nav className="mt-6 flex flex-wrap gap-2" aria-label="Estados">
        {statusTabs.map((status) => (
          <button
            key={status}
            type="button"
            onClick={() => setActiveStatus(status)}
            className={"opt-pill " + (activeStatus === status ? "opt-pill-selected" : "")}
          >
            {statusLabel(status)}
          </button>
        ))}
      </nav>

      {isLoading && <LoadingState label="A carregar encomendas..." />}

      {error && (
        <div className="mt-6 space-y-3" role="alert">
          <Alert variant="error">{error}</Alert>
          <button type="button" onClick={retryLoad} className="btn-secondary">
            <RefreshCw className="w-4 h-4" strokeWidth={1.7} />
            Tentar novamente
          </button>
        </div>
      )}

      {updateError && <Alert variant="error" className="mt-6">{updateError}</Alert>}

      {isEmpty && !isLoading && !error && (
        <div className="mt-10 card card-pad p-16 text-center">
          <div className="w-12 h-12 mx-auto rounded bg-ink-50 border border-ink-100 flex items-center justify-center text-ink-400">
            <Inbox className="w-5 h-5" strokeWidth={1.7} />
          </div>
          <p className="mt-4 text-ink-500">Não existem encomendas neste estado.</p>
        </div>
      )}

      {!isLoading && !error && filteredOrders.length > 0 && (
        <section className="mt-8" aria-label="Encomendas da loja">
          <ul className="space-y-4">
            {filteredOrders.map((order) => {
              const nextStatus = nextStatusByStatus[order.status];

              return (
                <li key={order.id} className="card card-pad p-5">
                  <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-3">
                        <h2 className="text-base font-semibold tabular">
                          Encomenda #{order.id}
                        </h2>
                        <span className={`badge ${statusBadge[order.status] || "badge-neutral"}`}>
                          {statusLabel(order.status)}
                        </span>
                      </div>
                      {order.buyerName && (
                        <p className="mt-2 text-sm text-ink-700">Cliente: {order.buyerName}</p>
                      )}
                      <p className="mt-1 text-xs text-ink-500">{formatDate(order.createdAt)}</p>
                      <p className="mt-3 font-semibold tabular">
                        Total: {formatPrice(order.totalAmount)}
                      </p>
                    </div>

                    {nextStatus && (
                      <button
                        type="button"
                        onClick={() => handleUpdateStatus(order)}
                        disabled={updatingOrderId === order.id}
                        className="btn-ink"
                      >
                        {updatingOrderId === order.id ? "A atualizar..." : actionLabel(order.status)}
                        <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
                      </button>
                    )}
                  </div>

                  {Array.isArray(order.items) && order.items.length > 0 && (
                    <ul className="mt-4 divide-y divide-ink-100 border-t border-ink-100">
                      {order.items.map((item) => (
                        <li
                          key={item.id || `${item.productId}-${item.variantId}`}
                          className="flex flex-wrap justify-between gap-3 py-2.5 text-sm"
                        >
                          <span>
                            <Link to={`/products/${item.productId}/edit`} className="hover:text-clover-700 transition-colors">
                              {item.productName}
                            </Link>
                            {" × "}{item.quantity}
                          </span>
                          <span className="tabular font-medium">{formatPrice(item.subtotal)}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              );
            })}
          </ul>

          <div className="mt-6 flex items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => setPage((p) => p - 1)}
              disabled={isFirstPage}
              className="btn-secondary"
            >
              <ChevronLeft className="w-4 h-4" strokeWidth={1.8} />
              Anterior
            </button>
            <span className="text-sm text-ink-500 tabular">
              Página {page + 1} de {Math.max(totalPages, 1)}
            </span>
            <button
              type="button"
              onClick={() => setPage((p) => p + 1)}
              disabled={isLastPage}
              className="btn-secondary"
            >
              Seguinte
              <ChevronRight className="w-4 h-4" strokeWidth={1.8} />
            </button>
          </div>
        </section>
      )}
    </div>
  );
}
