import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { Star, ArrowRight, Store } from "lucide-react";
import ReviewForm from "../components/ReviewForm";
import ReviewsSection from "../components/ReviewsSection";
import { useAuth } from "../hooks/useAuth";
import { normalizeRole, ROLES } from "../lib/roles";
import { getShopProfile } from "../services/shopService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

function sortCollections(collections) {
  return [...collections].sort((a, b) => a.displayOrder - b.displayOrder);
}

function formatRating(averageRating, t) {
  if (averageRating == null) return t("noReviews");
  return t("shopRating", { rating: Number(averageRating).toFixed(1) });
}

export default function PublicShopProfile() {
  const { t } = useTranslation("browse");
  const { shopId } = useParams();
  const { isAuthenticated, role } = useAuth();
  const [shop, setShop] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadShopProfile() {
      try {
        const profile = await getShopProfile(shopId);
        if (active) {
          setShop({
            ...profile,
            collections: sortCollections(profile.collections || []),
          });
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(
            loadError.response?.data?.message ||
              loadError.response?.data?.detail ||
              t("loadShopFailed")
          );
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadShopProfile();
    return () => {
      active = false;
    };
  }, [shopId, t]);

  return (
    <div className="page-shell max-w-4xl">
      {isLoading && <LoadingState label={t("loadingShop")} />}
      {error && <Alert variant="error">{error}</Alert>}

      {!isLoading && !error && shop && (
        <>
          {/* Banner */}
          {shop.bannerUrl ? (
            <div className="relative -mx-6 md:-mx-6 mb-6 overflow-hidden">
              <img
                src={shop.bannerUrl}
                alt={t("shopBannerAlt", { name: shop.name || t("anonymousShop") })}
                className="block w-full max-h-72 object-cover"
              />
            </div>
          ) : (
            <div className="h-32 rounded bg-gradient-to-br from-clover-100 to-clover-50 border border-clover-100 mb-6" />
          )}

          {/* Header row */}
          <div className="flex items-end gap-5 -mt-12 md:-mt-16 relative">
            {shop.logoUrl ? (
              <img
                src={shop.logoUrl}
                alt={t("shopLogoAlt", { name: shop.name || t("anonymousShop") })}
                className="w-24 h-24 rounded border-4 border-white bg-white object-cover shadow-md shrink-0"
              />
            ) : (
              <div className="w-24 h-24 rounded border-4 border-white bg-clover-50 flex items-center justify-center text-clover-600 shadow-md shrink-0">
                <Store className="w-8 h-8" strokeWidth={1.6} />
              </div>
            )}
            <div className="pb-2 min-w-0">
              <h1 className="text-3xl font-bold tracking-tighter truncate">{shop.name}</h1>
              <p className="mt-1 text-sm text-ink-500 flex items-center gap-1.5">
                <Star className="w-4 h-4 text-amber-500 fill-amber-400" strokeWidth={1.6} />
                {formatRating(shop.averageRating, t)}
              </p>
            </div>
          </div>

          <p className="mt-6 text-ink-600 leading-relaxed max-w-2xl">{shop.description}</p>

          <Link
            to={`/shops/${shopId}/products`}
            className="btn-primary mt-6 inline-flex"
          >
            {t("viewAllProducts")}
            <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
          </Link>

          {/* Collections */}
          <section className="mt-12">
            <h2 className="text-lg font-semibold tracking-tight mb-4">{t("collections")}</h2>
            {shop.collections.length === 0 ? (
              <p className="text-sm text-ink-500">{t("noCollections")}</p>
            ) : (
              <ul className="grid gap-2 sm:grid-cols-2 md:grid-cols-3">
                {shop.collections.map((collection) => (
                  <li key={collection.id}>
                    <Link
                      to={`/shops/${shopId}/products?collectionId=${collection.id}`}
                      className="card card-pad block hover:border-clover-400 transition-colors"
                    >
                      <span className="font-semibold">{collection.name}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {/* Reviews */}
          <section className="mt-12 pt-8 border-t border-ink-100">
            {isAuthenticated && normalizeRole(role) === ROLES.BUYER && (
              <div className="mb-10">
                <ReviewForm shopId={shopId} />
              </div>
            )}
            <ReviewsSection shopId={shopId} />
          </section>
        </>
      )}
    </div>
  );
}
