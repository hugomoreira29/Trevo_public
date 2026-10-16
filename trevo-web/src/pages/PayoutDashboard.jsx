import { useCallback, useEffect, useState } from "react";
import { ChevronLeft, ChevronRight, Wallet, Inbox } from "lucide-react";
import { getPayoutBalance, getPayoutHistory } from "../services/payoutService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const PAGE_SIZE = 20;

const statusLabels = {
  PENDING:  "Pendente",
  RELEASED: "Pago",
};

const statusBadge = {
  PENDING:  "badge-warning",
  RELEASED: "badge-success",
};

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function formatCurrency(value) {
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(value || 0));
}

function formatDate(value) {
  if (!value) return "Data indisponível";
  return new Intl.DateTimeFormat("pt-PT", { dateStyle: "short" }).format(new Date(value));
}

function getStatusLabel(status) {
  return statusLabels[status] || "Desconhecido";
}

export default function PayoutDashboard() {
  const [balance, setBalance] = useState(null);
  const [payouts, setPayouts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  const loadPayouts = useCallback(async (nextPage = 0, options = {}) => {
    const { isActive = () => true } = options;

    setIsLoading(true);
    setError("");

    try {
      const historyResponse = await getPayoutHistory({ page: nextPage, size: PAGE_SIZE });

      if (isActive()) {
        setPayouts(historyResponse.payouts || []);
        setPage(historyResponse.page ?? nextPage);
        setTotalPages(historyResponse.totalPages || 0);
      }
    } catch (loadError) {
      if (isActive()) {
        setError(getErrorMessage(loadError, "Não foi possível carregar pagamentos."));
      }
    } finally {
      if (isActive()) setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    async function loadInitialPayouts() {
      setIsLoading(true);
      setError("");

      try {
        const [balanceResponse, historyResponse] = await Promise.all([
          getPayoutBalance(),
          getPayoutHistory({ page: 0, size: PAGE_SIZE }),
        ]);

        if (active) {
          setBalance(balanceResponse.pendingBalance ?? 0);
          setPayouts(historyResponse.payouts || []);
          setPage(historyResponse.page ?? 0);
          setTotalPages(historyResponse.totalPages || 0);
        }
      } catch (loadError) {
        if (active) {
          setError(getErrorMessage(loadError, "Não foi possível carregar pagamentos."));
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadInitialPayouts();
    return () => {
      active = false;
    };
  }, []);

  const hasPreviousPage = page > 0;
  const hasNextPage = page < totalPages - 1;
  const isEmpty = !isLoading && !error && payouts.length === 0;

  return (
    <div className="page-shell max-w-5xl">
      <p className="eyebrow text-clover-700">Loja</p>
      <h1 className="h-page mt-2">Pagamentos</h1>

      {isLoading && <LoadingState label="A carregar pagamentos..." />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}

      {!isLoading && !error && (
        <section className="mt-8 card card-pad p-6 relative overflow-hidden">
          <div aria-hidden="true" className="pointer-events-none absolute -top-12 -right-12 w-48 h-48 rounded-full bg-clover-100/50 blur-3xl" />
          <div className="relative flex items-start gap-4">
            <div className="w-11 h-11 rounded bg-clover-500 text-white flex items-center justify-center shrink-0">
              <Wallet className="w-5 h-5" strokeWidth={1.7} />
            </div>
            <div>
              <h2 className="text-sm font-medium text-ink-500">Saldo pendente</h2>
              <p className="mt-1 text-3xl font-bold tracking-tighter tabular">{formatCurrency(balance)}</p>
              <p className="mt-1 text-xs text-ink-500">Será pago no próximo ciclo.</p>
            </div>
          </div>
        </section>
      )}

      {!isLoading && !error && (
        <section className="mt-10">
          <h2 className="text-lg font-semibold tracking-tight">Histórico de pagamentos</h2>

          {isEmpty ? (
            <div className="mt-4 card card-pad p-12 text-center">
              <div className="w-12 h-12 mx-auto rounded bg-ink-50 border border-ink-100 flex items-center justify-center text-ink-400">
                <Inbox className="w-5 h-5" strokeWidth={1.7} />
              </div>
              <p className="mt-4 text-ink-500">Ainda não existem pagamentos.</p>
            </div>
          ) : (
            <>
              <div className="mt-4 card overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-ink-100 bg-ink-50/60">
                      <th className="py-3 px-5 text-xs uppercase tracking-wider text-ink-500 font-medium">Data</th>
                      <th className="py-3 px-5 text-xs uppercase tracking-wider text-ink-500 font-medium">Montante</th>
                      <th className="py-3 px-5 text-xs uppercase tracking-wider text-ink-500 font-medium">Estado</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {payouts.map((payout) => (
                      <tr key={payout.id}>
                        <td className="py-3 px-5 text-sm tabular">
                          {formatDate(payout.releasedAt || payout.createdAt)}
                        </td>
                        <td className="py-3 px-5 text-sm font-semibold tabular">
                          {formatCurrency(payout.payoutAmount)}
                        </td>
                        <td className="py-3 px-5">
                          <span className={`badge ${statusBadge[payout.status] || "badge-neutral"}`}>
                            {getStatusLabel(payout.status)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between gap-3">
                  <button
                    type="button"
                    onClick={() => loadPayouts(page - 1)}
                    disabled={!hasPreviousPage || isLoading}
                    className="btn-secondary"
                  >
                    <ChevronLeft className="w-4 h-4" strokeWidth={1.8} />
                    Anterior
                  </button>
                  <span className="text-sm text-ink-500 tabular">Página {page + 1} de {Math.max(totalPages, 1)}</span>
                  <button
                    type="button"
                    onClick={() => loadPayouts(page + 1)}
                    disabled={!hasNextPage || isLoading}
                    className="btn-secondary"
                  >
                    Seguinte
                    <ChevronRight className="w-4 h-4" strokeWidth={1.8} />
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      )}
    </div>
  );
}
