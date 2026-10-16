import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Sparkles, Check, X } from "lucide-react";
import {
  cancelSubscription,
  createSubscription,
  getProStatus,
} from "../services/subscriptionService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

export default function ProSubscription() {
  const { t } = useTranslation("shop");
  const [isPro, setIsPro] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadProStatus() {
      try {
        const status = await getProStatus();
        if (active) {
          setIsPro(Boolean(status.pro));
          setError("");
        }
      } catch (loadError) {
        if (active) setError(getErrorMessage(loadError, t("loadSubscriptionFailed")));
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadProStatus();
    return () => {
      active = false;
    };
  }, [t]);

  async function handleSubscribe() {
    setError("");
    setSuccess("");
    setIsSubmitting(true);

    try {
      await createSubscription();
      setIsPro(true);
      setSuccess(t("subscriptionActivated"));
    } catch (subscribeError) {
      setError(getErrorMessage(subscribeError, t("activateSubscriptionFailed")));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleCancel() {
    setError("");
    setSuccess("");
    setIsSubmitting(true);

    try {
      await cancelSubscription();
      setIsPro(false);
      setSuccess(t("subscriptionCanceled"));
    } catch (cancelError) {
      setError(getErrorMessage(cancelError, t("cancelSubscriptionFailed")));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="page-shell-narrow">
      <p className="eyebrow text-clover-700">{t("proPlan")}</p>
      <h1 className="h-page mt-2">{t("proSubscription")}</h1>

      {isLoading && <LoadingState label={t("loadingSubscription")} />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}
      {success && <Alert variant="success" className="mt-6">{success}</Alert>}

      {!isLoading && (
        <section className="mt-8 card card-pad p-8 relative overflow-hidden">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -top-12 -right-12 w-48 h-48 rounded-full bg-clover-100/60 blur-3xl"
          />

          <div className="relative flex flex-wrap items-start justify-between gap-6">
            <div className="flex items-start gap-3">
              <div className="w-11 h-11 rounded bg-clover-500 text-white flex items-center justify-center shrink-0 shadow-clover">
                <Sparkles className="w-5 h-5" strokeWidth={1.8} />
              </div>
              <div>
                <h2 className="text-2xl font-semibold tracking-tight">{t("trevoPro")}</h2>
                <p className="mt-2 text-ink-600 max-w-md">{t("proDescription")}</p>
              </div>
            </div>
            <div className="text-right shrink-0">
              <p className="text-3xl font-bold tracking-tighter tabular">{t("proPrice")}</p>
              <p className="text-xs text-ink-500 mt-0.5">por mês</p>
            </div>
          </div>

          <ul className="relative mt-7 space-y-2.5">
            {[t("proBenefitAccount"), t("proBenefitManage"), t("proBenefitCancel")].map((benefit) => (
              <li key={benefit} className="flex items-start gap-2 text-sm text-ink-700">
                <Check className="w-4 h-4 text-clover-600 mt-0.5 shrink-0" strokeWidth={2} />
                {benefit}
              </li>
            ))}
          </ul>

          {isPro ? (
            <div className="relative mt-8 p-4 rounded border border-green-200 bg-green-50">
              <p className="font-semibold text-green-800 flex items-center gap-2">
                <Check className="w-4 h-4" strokeWidth={2} />
                {t("subscriptionActive")}
              </p>
              <p className="mt-1.5 text-sm text-green-700">{t("proActiveMessage")}</p>
              <button
                type="button"
                onClick={handleCancel}
                disabled={isSubmitting}
                className="mt-5 inline-flex items-center gap-2 px-4 py-2 rounded border border-red-300 text-red-700 font-semibold text-sm hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <X className="w-4 h-4" strokeWidth={1.8} />
                {isSubmitting ? t("canceling") : t("cancelSubscription")}
              </button>
            </div>
          ) : (
            <div className="relative mt-8 p-4 rounded border border-ink-100 bg-ink-50">
              <p className="font-semibold">{t("noActivePro")}</p>
              <p className="mt-1.5 text-sm text-ink-600">{t("activateProMessage")}</p>
              <button
                type="button"
                onClick={handleSubscribe}
                disabled={isSubmitting}
                className="btn-primary mt-5"
                style={{ boxShadow: "0 12px 32px -8px rgba(42, 157, 84, 0.35)" }}
              >
                <Sparkles className="w-4 h-4" strokeWidth={1.8} />
                {isSubmitting ? t("subscribing") : t("subscribePro")}
              </button>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
