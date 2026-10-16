import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ArrowRight, Sparkles, Store, ShieldCheck } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import logoUrl from "../assets/trevo-logo.svg";

export default function Home() {
  const { t } = useTranslation("common");
  const { isAuthenticated, role } = useAuth();

  return (
    <div className="bg-white">
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-clover-50 via-white to-white" />
        <div className="max-w-6xl mx-auto px-6 pt-16 pb-20 md:pt-24 md:pb-28 grid md:grid-cols-[1.1fr_0.9fr] gap-12 items-center">
          <div>
            <p className="eyebrow text-clover-700">{t("home", "Início")} · marketplace</p>
            <h1 className="h-display mt-3 text-ink-900">
              Vê-te com{" "}
              <span className="text-clover-600">Trevo</span>.
            </h1>
            <p className="mt-5 text-lg text-ink-600 leading-relaxed max-w-xl">
              Compras de lojas independentes com experimentação virtual por IA.
              Encontra a peça certa antes de a comprar.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/products" className="btn-primary">
                Explorar produtos
                <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
              </Link>
              {!isAuthenticated && (
                <Link to="/register" className="btn-secondary">
                  {t("register", "Registar")}
                </Link>
              )}
              {isAuthenticated && role === "BUYER" && (
                <Link to="/pro" className="btn-secondary">
                  Conhecer Trevo Pro
                </Link>
              )}
              {isAuthenticated && role === "SHOP_OWNER" && (
                <Link to="/shop/dashboard" className="btn-secondary">
                  {t("shopDashboard", "Painel da loja")}
                </Link>
              )}
            </div>
          </div>

          <div className="hidden md:flex items-center justify-center">
            <div className="relative w-72 h-72 rounded-2xl bg-gradient-to-br from-clover-100 to-clover-50 border border-clover-100 flex items-center justify-center shadow-clover">
              <img src={logoUrl} alt="" className="w-32 h-32 drop-shadow-lg" />
            </div>
          </div>
        </div>
      </section>

      {/* Three pillars */}
      <section className="border-t border-ink-100">
        <div className="max-w-6xl mx-auto px-6 py-16 grid md:grid-cols-3 gap-6">
          <Feature
            Icon={Sparkles}
            title="Experimentação virtual"
            body="Carrega 3 fotos tuas e vê-te com qualquer peça. Trevo Pro dá-te gerações ilimitadas."
          />
          <Feature
            Icon={Store}
            title="Lojas independentes"
            body="Marcas portuguesas e europeias, pequenas e grandes, todas no mesmo sítio."
          />
          <Feature
            Icon={ShieldCheck}
            title="Pagamentos seguros"
            body="Stripe, PayPal e MB WAY. Comissão transparente, recibos automáticos."
          />
        </div>
      </section>
    </div>
  );
}

function Feature({ Icon, title, body }) {
  return (
    <div className="flex flex-col gap-3">
      <div className="w-10 h-10 rounded bg-clover-50 border border-clover-100 flex items-center justify-center text-clover-600">
        <Icon className="w-5 h-5" strokeWidth={1.7} />
      </div>
      <h3 className="text-lg font-semibold tracking-tight">{title}</h3>
      <p className="text-sm text-ink-600 leading-relaxed">{body}</p>
    </div>
  );
}
