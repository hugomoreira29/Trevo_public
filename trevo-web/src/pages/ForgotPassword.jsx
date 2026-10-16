import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { requestPasswordReset } from "../lib/authApi";
import Alert from "../components/Alert";

export default function ForgotPassword() {
  const { t } = useTranslation("auth");
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSubmitted(false);

    if (!email.trim()) {
      setError(t("fillEmail"));
      return;
    }

    try {
      setIsSubmitting(true);
      await requestPasswordReset(email.trim());
      setSubmitted(true);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center px-6 py-12 bg-ink-50/30">
      <div className="card card-pad w-full max-w-md p-8">
        <h1 className="text-2xl font-semibold tracking-tight mb-1.5">{t("forgotPasswordTitle")}</h1>
        <p className="text-sm text-ink-500 mb-6">{t("forgotPasswordDescription")}</p>

        {error && <Alert variant="error" className="mb-4">{error}</Alert>}
        {submitted && <Alert variant="success" className="mb-4">{t("forgotPasswordSuccess")}</Alert>}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="field">
            <label className="field-label" htmlFor="fp-email">Email</label>
            <input
              id="fp-email"
              className="input"
              type="email"
              name="email"
              placeholder="tu@trevo.pt"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
            />
          </div>

          <button type="submit" disabled={isSubmitting} className="btn-primary w-full mt-2 py-3">
            {isSubmitting ? t("forgotPasswordSubmitting") : t("forgotPasswordSubmit")}
          </button>
        </form>

        <Link to="/login" className="mt-5 inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900">
          <ArrowLeft className="w-4 h-4" strokeWidth={1.7} />
          {t("backToLogin")}
        </Link>
      </div>
    </div>
  );
}
