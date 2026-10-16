import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { ChevronLeft, ChevronRight, ImageOff, Search } from "lucide-react";
import { productImageAlt } from "../lib/imageAlt";
import { browseProducts, searchProducts } from "../services/productService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const FILTER_KEYS = ["q", "minPrice", "maxPrice", "variantSize", "color"];
const SCOPE_KEYS = ["shopId", "collectionId"];
const SEARCH_DEBOUNCE_MS = 300;

function readFilters(searchParams) {
  return Object.fromEntries(
    [...FILTER_KEYS, ...SCOPE_KEYS].map((key) => [key, searchParams.get(key) ?? ""])
  );
}

function hasSearchFilters(filters) {
  return FILTER_KEYS.some((key) => Boolean(filters[key]?.trim()));
}

function buildSearchParams(filters) {
  const next = new URLSearchParams();

  [...FILTER_KEYS, ...SCOPE_KEYS].forEach((key) => {
    const value = filters[key]?.trim();

    if (value) {
      next.set(key, value);
    }
  });

  return next;
}

function productItems(page) {
  return page?.content || page?.products || [];
}

function pageNumber(page) {
  return page?.number ?? 0;
}

function totalPages(page) {
  return page?.totalPages ?? 0;
}

function productImage(product) {
  return product.imageUrl || product.images?.[0]?.url || product.images?.[0]?.imageUrl || "";
}

function formatPrice(price) {
  if (price === null || price === undefined || price === "") return "";
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(price));
}

export default function ProductSearch() {
  const { t } = useTranslation("browse");
  const [searchParams, setSearchParams] = useSearchParams();
  const urlFilters = useMemo(() => readFilters(searchParams), [searchParams]);
  const pageParam = searchParams.get("page") ?? "";
  const [filters, setFilters] = useState(urlFilters);
  const [results, setResults] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setFilters(urlFilters);
  }, [searchParams, urlFilters]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      const current = buildSearchParams(urlFilters).toString();
      const nextParams = buildSearchParams(filters);
      nextParams.delete("page");
      const next = nextParams.toString();

      if (current !== next) {
        setSearchParams(next, { replace: true });
      }
    }, SEARCH_DEBOUNCE_MS);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [filters, setSearchParams, urlFilters]);

  useEffect(() => {
    let active = true;

    async function loadProducts() {
      setIsLoading(true);
      setError("");

      try {
        const params = {
          ...urlFilters,
          page: pageParam,
        };
        const scopedBrowse =
          !hasSearchFilters(urlFilters) && (urlFilters.shopId || urlFilters.collectionId);
        const page = scopedBrowse
          ? await browseProducts(params)
          : await searchProducts(params);

        if (active) {
          setResults(page);
        }
      } catch (loadError) {
        if (active) {
          setError(
            loadError.response?.data?.message ||
              loadError.response?.data?.detail ||
              t("loadProductsFailed")
          );
          setResults(null);
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadProducts();

    return () => {
      active = false;
    };
  }, [pageParam, t, urlFilters]);

  function updateFilter(name, value) {
    setFilters((current) => ({
      ...current,
      [name]: value,
    }));
  }

  function updatePage(nextPage) {
    const next = new URLSearchParams(searchParams);

    if (nextPage > 0) {
      next.set("page", String(nextPage));
    } else {
      next.delete("page");
    }

    setSearchParams(next, { replace: false });
  }

  const products = productItems(results);
  const currentPage = pageNumber(results);
  const pageCount = totalPages(results);
  const hasPreviousPage = currentPage > 0;
  const hasNextPage = pageCount > 0 && currentPage + 1 < pageCount;

  return (
    <div className="page-shell grid gap-8 lg:grid-cols-[260px_1fr]">
      <aside className="card card-pad self-start space-y-4 sticky top-20">
        <h1 className="h-section">{t("products")}</h1>

        <div className="field">
          <label className="field-label" htmlFor="f-q">{t("search")}</label>
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" strokeWidth={1.8} />
            <input
              id="f-q"
              aria-label={t("search")}
              name="q"
              value={filters.q}
              onChange={(event) => updateFilter("q", event.target.value)}
              className="input pl-9"
              placeholder={t("searchPlaceholder")}
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="field">
            <label className="field-label" htmlFor="f-min">{t("minPrice")}</label>
            <input
              id="f-min"
              aria-label={t("minPrice")}
              name="minPrice"
              type="number"
              min="0"
              value={filters.minPrice}
              onChange={(event) => updateFilter("minPrice", event.target.value)}
              className="input"
            />
          </div>
          <div className="field">
            <label className="field-label" htmlFor="f-max">{t("maxPrice")}</label>
            <input
              id="f-max"
              aria-label={t("maxPrice")}
              name="maxPrice"
              type="number"
              min="0"
              value={filters.maxPrice}
              onChange={(event) => updateFilter("maxPrice", event.target.value)}
              className="input"
            />
          </div>
        </div>

        <div className="field">
          <label className="field-label" htmlFor="f-size">{t("size")}</label>
          <input
            id="f-size"
            aria-label={t("size")}
            name="variantSize"
            value={filters.variantSize}
            onChange={(event) => updateFilter("variantSize", event.target.value)}
            className="input"
            placeholder="S, M, 38…"
          />
        </div>

        <div className="field">
          <label className="field-label" htmlFor="f-color">{t("color")}</label>
          <input
            id="f-color"
            aria-label={t("color")}
            name="color"
            value={filters.color}
            onChange={(event) => updateFilter("color", event.target.value)}
            className="input"
            placeholder="Verde, preto…"
          />
        </div>
      </aside>

      <section>
        {isLoading && <LoadingState label={t("loadingProducts")} />}
        {error && <Alert variant="error">{error}</Alert>}

        {!isLoading && !error && products.length === 0 && (
          <div className="card card-pad p-10 text-center text-ink-500">{t("noProducts")}</div>
        )}

        {!error && products.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {products.map((product) => {
              const imageUrl = productImage(product);

              return (
                <Link
                  to={`/products/${product.id}`}
                  key={product.id}
                  className="group card p-3 transition-colors hover:border-clover-400"
                >
                  <div className="aspect-square bg-ink-50 rounded overflow-hidden flex items-center justify-center">
                    {imageUrl ? (
                      <img
                        src={imageUrl}
                        alt={productImageAlt(product.name)}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <ImageOff className="w-7 h-7 text-ink-300" strokeWidth={1.5} />
                    )}
                  </div>
                  {product.brand && (
                    <p className="mt-3 text-[11px] uppercase tracking-wider text-ink-500 font-medium">
                      {product.brand}
                    </p>
                  )}
                  <h2 className="mt-1 text-sm font-semibold leading-snug text-ink-900 group-hover:text-clover-700 transition-colors">
                    {product.name}
                  </h2>
                  <p className="mt-1 text-sm font-semibold tabular">
                    {formatPrice(product.price)}
                  </p>
                </Link>
              );
            })}
          </div>
        )}

        {!error && pageCount > 1 && (
          <nav
            aria-label={t("productPagination")}
            className="mt-8 flex items-center justify-between gap-3"
          >
            <button
              type="button"
              onClick={() => updatePage(currentPage - 1)}
              disabled={!hasPreviousPage || isLoading}
              className="btn-secondary"
            >
              <ChevronLeft className="w-4 h-4" strokeWidth={1.8} />
              {t("previous", { ns: "common" })}
            </button>
            <span className="text-sm text-ink-500 tabular">
              {t("pageOf", { page: currentPage + 1, total: pageCount })}
            </span>
            <button
              type="button"
              onClick={() => updatePage(currentPage + 1)}
              disabled={!hasNextPage || isLoading}
              className="btn-secondary"
            >
              {t("next", { ns: "common" })}
              <ChevronRight className="w-4 h-4" strokeWidth={1.8} />
            </button>
          </nav>
        )}
      </section>
    </div>
  );
}
