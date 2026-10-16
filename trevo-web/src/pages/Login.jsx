import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import Alert from "../components/Alert";
import logoUrl from "../assets/trevo-logo.svg";

export default function Login() {
  const { t } = useTranslation("auth");
  const { login } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");
  const successMessage = location.state?.message || "";

  function handleChange(e) {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  }

  async function handleSubmit(e) {
    e.preventDefault();

    if (!form.email || !form.password) {
      setError(t("fillEmailAndPassword"));
      return;
    }

    try {
      setIsSubmitting(true);
      await login(form);
      setError("");
      navigate("/dashboard");
    } catch (error) {
      setError(error.response?.data?.message || t("invalidCredentials"));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center px-6 py-12 bg-ink-50/30">
      <div className="card card-pad w-full max-w-md p-8">
        <div className="flex items-center gap-2 mb-6">
          <img src={logoUrl} alt="" className="w-7 h-7" />
          <span className="text-xl font-bold tracking-tightest">
            trevo<span className="text-clover-500">.</span>
          </span>
        </div>

        <h1 className="text-2xl font-semibold tracking-tight mb-1.5">{t("loginTitle")}</h1>
        <p className="text-sm text-ink-500 mb-6">
          Entra para experimentar virtualmente, comprar e gerir as tuas encomendas.
        </p>

        {error && <Alert variant="error" className="mb-4">{error}</Alert>}
        {successMessage && <Alert variant="success" className="mb-4">{successMessage}</Alert>}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="field">
            <label className="field-label" htmlFor="email">Email</label>
            <input
              id="email"
              className="input"
              type="email"
              name="email"
              placeholder="tu@trevo.pt"
              value={form.email}
              onChange={handleChange}
              autoComplete="email"
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="password">{t("password")}</label>
            <input
              id="password"
              className="input"
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              autoComplete="current-password"
            />
          </div>

          <button type="submit" disabled={isSubmitting} className="btn-primary w-full mt-2 py-3">
            {isSubmitting ? t("loginSubmitting") : t("loginSubmit")}
          </button>
        </form>

        <div className="mt-5 flex items-center justify-between text-sm">
          <Link to="/forgot-password" className="text-clover-700 hover:text-clover-800 font-medium">
            {t("forgotPasswordLink")}
          </Link>
          <Link to="/register" className="text-ink-500 hover:text-ink-900">
            Criar conta
          </Link>
        </div>
      </div>
    </div>
  );
}
