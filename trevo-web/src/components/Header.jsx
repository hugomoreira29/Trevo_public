import { Link, NavLink, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Search, ShoppingCart, ClipboardList, User, LogOut } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { ROLES } from "../lib/roles";
import logoUrl from "../assets/trevo-logo.svg";

/**
 * Header — sticky top bar with brand, primary nav, search shortcut, cart, account.
 * Renders on every authenticated and public page. Behavior stays identical to
 * before (just adds chrome that wasn't there).
 */
export default function Header() {
  const { t } = useTranslation("common");
  const { isAuthenticated, role, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/");
  }

  const link = ({ isActive }) =>
    [
      "text-sm font-medium transition-colors",
      isActive ? "text-clover-700" : "text-ink-700 hover:text-ink-900",
    ].join(" ");

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-ink-100">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between gap-6">
        <div className="flex items-center gap-8 min-w-0">
          <Link to="/" className="flex items-center gap-2 shrink-0" aria-label="Trevo">
            <img src={logoUrl} alt="" className="w-7 h-7" />
            <span className="text-xl font-bold tracking-tightest">
              trevo<span className="text-clover-500">.</span>
            </span>
          </Link>
          <nav className="hidden md:flex items-center gap-6">
            <NavLink to="/products" className={link}>
              {t("products", "Produtos")}
            </NavLink>
            {isAuthenticated && role === ROLES.BUYER && (
              <NavLink to="/pro" className={link}>
                Trevo Pro
              </NavLink>
            )}
            {isAuthenticated && role === ROLES.SHOP_OWNER && (
              <NavLink to="/shop/dashboard" className={link}>
                {t("shopDashboard", "Painel da loja")}
              </NavLink>
            )}
            {isAuthenticated && role === ROLES.ADMIN && (
              <NavLink to="/admin" className={link}>
                {t("admin", "Administração")}
              </NavLink>
            )}
          </nav>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to="/products"
            className="hidden md:inline-flex items-center gap-2 h-9 px-3 rounded border border-ink-200 bg-white text-ink-500 hover:bg-ink-50 transition-colors text-sm"
            aria-label="Pesquisar"
          >
            <Search className="w-4 h-4" strokeWidth={1.7} />
            <span className="hidden lg:inline">Pesquisar produtos…</span>
          </Link>

          {isAuthenticated && role === ROLES.BUYER && (
            <>
              <Link
                to="/orders"
                className="inline-flex items-center justify-center w-9 h-9 rounded border border-ink-200 bg-white hover:bg-ink-50 transition-colors"
                aria-label={t("orders", "Encomendas")}
              >
                <ClipboardList className="w-[18px] h-[18px]" strokeWidth={1.7} />
              </Link>
              <Link
                to="/cart"
                className="inline-flex items-center justify-center w-9 h-9 rounded border border-ink-200 bg-white hover:bg-ink-50 transition-colors"
                aria-label={t("cart", "Carrinho")}
              >
                <ShoppingCart className="w-[18px] h-[18px]" strokeWidth={1.7} />
              </Link>
            </>
          )}

          {!isAuthenticated ? (
            <>
              <Link to="/login" className="btn-ghost">{t("login", "Entrar")}</Link>
              <Link to="/register" className="btn-ink">
                {t("register", "Registar")}
              </Link>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/profile"
                className="inline-flex items-center gap-2 h-9 px-3 rounded bg-ink-900 text-white hover:bg-ink-800 transition-colors text-sm font-medium"
              >
                <User className="w-4 h-4" strokeWidth={1.7} />
                <span className="hidden sm:inline">{t("profile", "Perfil")}</span>
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="inline-flex items-center justify-center w-9 h-9 rounded border border-ink-200 bg-white hover:bg-ink-50 transition-colors"
                aria-label={t("logout", "Terminar sessão")}
              >
                <LogOut className="w-[18px] h-[18px]" strokeWidth={1.7} />
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
