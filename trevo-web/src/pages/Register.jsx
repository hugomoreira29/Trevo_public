import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import Alert from "../components/Alert";
import logoUrl from "../assets/trevo-logo.svg";

export default function Register() {
  const { t } = useTranslation("auth");
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    phone: "",
    type: "buyer",
    shopName: "",
    nif: "",
  });

  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleChange(e) {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  }

  async function handleSubmit(e) {
    e.preventDefault();

    if (!form.fullName || !form.email || !form.password) {
      setError(t("fillRequiredFields"));
      return;
    }

    if (form.type === "owner" && (!form.nif || !form.shopName)) {
      setError(t("shopOwnerRequired"));
      return;
    }

    try {
      setIsSubmitting(true);
      await register(form);
      setError("");
      navigate("/login", {
        state: { message: t("accountCreated") },
      });
    } catch (error) {
      setError(error.response?.data?.message || t("registerFailed"));
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

        <h1 className="text-2xl font-semibold tracking-tight mb-1.5">{t("registerTitle")}</h1>
        <p className="text-sm text-ink-500 mb-6">
          Cria uma conta para comprar ou para abrir a tua loja em Trevo.
        </p>

        {error && <Alert variant="error" className="mb-4">{error}</Alert>}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Account type toggle */}
          <div className="field">
            <span className="field-label">{t("type")}</span>
            <div className="grid grid-cols-2 gap-2">
              {[
                { v: "buyer", l: t("buyer") },
                { v: "owner", l: t("shopOwner") },
              ].map((o) => (
                <button
                  type="button"
                  key={o.v}
                  onClick={() => setForm({ ...form, type: o.v })}
                  className={
                    "opt-pill " + (form.type === o.v ? "opt-pill-selected" : "")
                  }
                >
                  {o.l}
                </button>
              ))}
            </div>
          </div>

          <div className="field">
            <label className="field-label" htmlFor="fullName">Nome</label>
            <input
              id="fullName"
              className="input"
              type="text"
              name="fullName"
              value={form.fullName}
              onChange={handleChange}
              autoComplete="name"
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="r-email">Email</label>
            <input
              id="r-email"
              className="input"
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              autoComplete="email"
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="r-password">{t("password")}</label>
            <input
              id="r-password"
              className="input"
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              autoComplete="new-password"
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="phone">Telefone <span className="text-ink-400 font-normal">(opcional)</span></label>
            <input
              id="phone"
              className="input"
              type="text"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              autoComplete="tel"
            />
          </div>

          {form.type === "owner" && (
            <>
              <div className="field">
                <label className="field-label" htmlFor="shopName">Nome da loja</label>
                <input
                  id="shopName"
                  className="input"
                  type="text"
                  name="shopName"
                  value={form.shopName}
                  onChange={handleChange}
                />
              </div>

              <div className="field">
                <label className="field-label" htmlFor="nif">NIF</label>
                <input
                  id="nif"
                  className="input"
                  type="text"
                  name="nif"
                  value={form.nif}
                  onChange={handleChange}
                />
              </div>
            </>
          )}

          <button type="submit" disabled={isSubmitting} className="btn-primary w-full mt-2 py-3">
            {isSubmitting ? t("registerSubmitting") : t("registerSubmit")}
          </button>
        </form>

        <p className="mt-5 text-sm text-ink-500 text-center">
          Já tens conta?{" "}
          <Link to="/login" className="text-clover-700 hover:text-clover-800 font-medium">
            {t("loginTitle")}
          </Link>
        </p>
      </div>
    </div>
  );
}
