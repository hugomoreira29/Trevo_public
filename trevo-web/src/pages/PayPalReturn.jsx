import { useEffect, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { CheckCircle2, XCircle } from "lucide-react";
import { capturePayPalPayment } from "../services/paymentService";
import LoadingState from "../components/LoadingState";

export default function PayPalReturn() {
  const { t } = useTranslation("checkout");
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");

  const paypalOrderId = searchParams.get("token");

  useEffect(() => {
    if (!paypalOrderId) {
      setStatus("error");
      setError(t("paypalMissingToken"));
      return;
    }

    capturePayPalPayment(paypalOrderId)
      .then(() => setStatus("success"))
      .catch((err) => {
        setStatus("error");
        setError(
          err.response?.data?.detail ||
            err.response?.data?.message ||
            err.message ||
            t("paypalCaptureFailed")
        );
      });
  }, [paypalOrderId, t]);

  if (status === "loading") {
    return <LoadingState message={t("paypalProcessing")} />;
  }

  if (status === "error") {
    return (
      <div className="page-shell max-w-lg text-center">
        <div className="w-14 h-14 mx-auto rounded-full bg-red-50 border border-red-200 flex items-center justify-center text-red-600">
          <XCircle className="w-7 h-7" strokeWidth={1.6} />
        </div>
        <h1 className="h-page mt-5">{t("paypalFailed")}</h1>
        <p className="mt-3 text-ink-500">{error}</p>
        <Link to="/orders" className="btn-primary inline-block mt-6">
          {t("viewOrders")}
        </Link>
      </div>
    );
  }

  return (
    <div className="page-shell max-w-lg text-center">
      <div className="w-14 h-14 mx-auto rounded-full bg-green-50 border border-green-200 flex items-center justify-center text-green-600">
        <CheckCircle2 className="w-7 h-7" strokeWidth={1.6} />
      </div>
      <h1 className="h-page mt-5">{t("paypalSuccess")}</h1>
      <p className="mt-3 text-ink-500">{t("paypalPaymentConfirmed")}</p>
      <Link to="/orders" className="btn-primary inline-block mt-6">
        {t("viewOrders")}
      </Link>
    </div>
  );
}
