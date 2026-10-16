import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { CheckCircle2, ArrowRight, Plus } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { fetchUserProfile } from "../lib/authApi";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

export default function Dashboard() {
  const { t } = useTranslation("common");
  const { accessToken, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadProfile() {
      if (!accessToken) {
        setIsLoading(false);
        return;
      }

      try {
        const currentUserProfile = await fetchUserProfile(accessToken);
        if (active) {
          setProfile(currentUserProfile);
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(loadError.message || t("loadProfileFailed", { ns: "profile" }));

          if (loadError.status === 401) {
            logout();
            navigate("/login", { replace: true });
          }
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadProfile();

    return () => {
      active = false;
    };
  }, [accessToken, logout, navigate, t]);

  return (
    <div className="page-shell">
      <p className="eyebrow">{t("home", "Início")}</p>
      <h1 className="h-page mt-2">{t("dashboard")}</h1>
      <p className="mt-3 text-ink-500 flex items-center gap-2">
        {isAuthenticated && <CheckCircle2 className="w-4 h-4 text-green-600" strokeWidth={2} />}
        {t("authenticated")}: {isAuthenticated ? t("yes") : t("no")}
      </p>

      <div className="mt-8">
        {isLoading && <LoadingState label={t("loadingProfile")} />}
        {error && <Alert variant="error">{error}</Alert>}

        {profile && (
          <div className="card card-pad p-6 max-w-2xl">
            <h2 className="text-lg font-semibold mb-1">{t("profileLoaded")}</h2>
            <dl className="mt-4 grid grid-cols-[160px_1fr] gap-y-2.5 text-sm">
              <dt className="text-ink-500">{t("accountType")}</dt>
              <dd className="text-ink-900">{profile.role}</dd>
              <dt className="text-ink-500">Email</dt>
              <dd className="text-ink-900">{profile.email}</dd>
              <dt className="text-ink-500">{t("name")}</dt>
              <dd className="text-ink-900">{profile.fullName}</dd>
              <dt className="text-ink-500">{t("accountStatus")}</dt>
              <dd className="text-ink-900">{profile.status}</dd>
              {profile.shopName && (
                <>
                  <dt className="text-ink-500">{t("shop")}</dt>
                  <dd className="text-ink-900">{profile.shopName}</dd>
                </>
              )}
              {profile.shopStatus && (
                <>
                  <dt className="text-ink-500">{t("shopStatus")}</dt>
                  <dd className="text-ink-900">{profile.shopStatus}</dd>
                </>
              )}
            </dl>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link to="/profile" className="btn-secondary inline-flex">
                Gerir conta
                <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
              </Link>
              {profile.role === "BUYER" && (
                <Link to="/orders" className="btn-secondary inline-flex">
                  As minhas encomendas
                  <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
                </Link>
              )}
              {profile.role === "SHOP_OWNER" && !profile.shopName && (
                <Link to="/shops/new" className="btn-primary inline-flex">
                  <Plus className="w-4 h-4" strokeWidth={1.8} />
                  Criar loja
                </Link>
              )}
              {profile.role === "SHOP_OWNER" && profile.shopName && (
                <Link to="/shop/dashboard" className="btn-secondary inline-flex">
                  Painel da loja
                  <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
                </Link>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
