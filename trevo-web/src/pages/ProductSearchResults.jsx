import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ImageOff, Filter } from "lucide-react";
import { browseProducts, searchProducts } from "../services/productService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const filterKeys = ["q", "minPrice", "maxPrice", "size", "color"];
const scopeKeys = ["shopId", "collectionId"];

function paramsToObject(searchParams) {
  return [...filterKeys, ...scopeKeys].reduce((params, key) => {
    const value = searchParams.get(key);

    if (value) {
      params[key] = value;
    }

    return params;
  }, {});
}

function getProductsFromResponse(response) {
  if (Array.isArray(response)) {
    return response;
  }

  if (Array.isArray(response?.content)) {
    return response.content;
  }

  return [];
}

function formatPrice(price) {
  if (price === null || price === undefined || price === "") {
    return "";
  }

  return new Intl.NumberFormat("pt-PT", {
    style: "currency",
    currency: "EUR",
  }).format(Number(price));
}

function ProductCard({ product }) {
  return (
    <article className="card p-3">
      <div className="aspect-square bg-ink-50 rounded flex items-center justify-center text-ink-300">
        <ImageOff className="w-7 h-7" strokeWidth={1.5} />
      </div>
      <div className="mt-3 space-y-1.5">
        <h2 className="text-sm font-semibold leading-snug">
          <Link to={`/products/${product.id}`} className="hover:text-clover-700 transition-colors">
            {product.name}
          </Link>
        </h2>
        {product.price !== undefined && product.price !== null && (
          <p className="text-sm font-semibold tabular">{formatPrice(product.price)}</p>
        )}
        {product.brand && <p className="text-xs text-ink-500">Marca: {product.brand}</p>}
        {product.materials && (
          <p className="text-xs text-ink-500 leading-snug line-clamp-2">Materiais: {product.materials}</p>
        )}
        {product.shopId && product.shopName && (
          <Link
            to={`/shops/${product.shopId}`}
            className="text-xs text-clover-700 hover:underline font-medium"
          >
            {product.shopName}
          </Link>
        )}
      </div>
    </article>
  );
}

export default function ProductSearchResults() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [form, setForm] = useState(() => ({
    q: searchParams.get("q") || "",
    minPrice: searchParams.get("minPrice") || "",
    maxPrice: searchParams.get("maxPrice") || "",
    size: searchParams.get("size") || "",
    color: searchParams.get("color") || "",
  }));
  const [products, setProducts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  const requestParams = useMemo(() => paramsToObject(searchParams), [searchParams]);
  const isScopedBrowse = Boolean(requestParams.shopId || requestParams.collectionId);

  useEffect(() => {
    setForm({
      q: searchParams.get("q") || "",
      minPrice: searchParams.get("minPrice") || "",
      maxPrice: searchParams.get("maxPrice") || "",
      size: searchParams.get("size") || "",
      color: searchParams.get("color") || "",
    });
  }, [searchParams]);

  useEffect(() => {
    let active = true;

    async function loadProducts() {
      setIsLoading(true);
      setError("");

      try {
        const response = isScopedBrowse
          ? await browseProducts(requestParams)
          : await searchProducts(requestParams);

        if (active) {
          setProducts(getProductsFromResponse(response));
        }
      } catch (loadError) {
        if (active) {
          setProducts([]);
          setError(
            loadError.response?.data?.message ||
              loadError.response?.data?.detail ||
              "Não foi possível carregar os produtos."
          );
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
  }, [isScopedBrowse, requestParams]);

  function handleChange(event) {
    setForm((currentForm) => ({
      ...currentForm,
      [event.target.name]: event.target.value,
    }));
  }

  function applyFilters(event) {
    event.preventDefault();

    const nextParams = new URLSearchParams();

    scopeKeys.forEach((key) => {
      const value = searchParams.get(key);

      if (value) {
        nextParams.set(key, value);
      }
    });

    filterKeys.forEach((key) => {
      const value = form[key]?.trim();

      if (value) {
        nextParams.set(key, value);
      }
    });

    setSearchParams(nextParams);
  }

  return (
    <div className="page-shell">
      <h1 className="h-page">Produtos</h1>

      <div className="mt-8 grid gap-8 md:grid-cols-[260px_minmax(0,1fr)]">
        <aside className="card card-pad self-start sticky top-20">
          <div className="flex items-center gap-2 mb-4 text-sm font-semibold text-ink-700">
            <Filter className="w-4 h-4" strokeWidth={1.7} />
            Filtros
          </div>
          <form onSubmit={applyFilters} className="flex flex-col gap-4">
            <div className="field">
              <label htmlFor="product-search" className="field-label">Pesquisa</label>
              <input
                id="product-search"
                name="q"
                type="search"
                value={form.q}
                onChange={handleChange}
                className="input"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="field">
                <label htmlFor="min-price" className="field-label">Preço mínimo</label>
                <input
                  id="min-price"
                  name="minPrice"
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.minPrice}
                  onChange={handleChange}
                  className="input"
                />
              </div>
              <div className="field">
                <label htmlFor="max-price" className="field-label">Preço máximo</label>
                <input
                  id="max-price"
                  name="maxPrice"
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.maxPrice}
                  onChange={handleChange}
                  className="input"
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="size" className="field-label">Tamanho</label>
              <input id="size" name="size" type="text" value={form.size} onChange={handleChange} className="input" />
            </div>

            <div className="field">
              <label htmlFor="color" className="field-label">Cor</label>
              <input id="color" name="color" type="text" value={form.color} onChange={handleChange} className="input" />
            </div>

            <button type="submit" className="btn-primary w-full">Aplicar filtros</button>
          </form>
        </aside>

        <section>
          {isLoading && <LoadingState label="A carregar produtos..." />}
          {error && <Alert variant="error">{error}</Alert>}

          {!isLoading && !error && products.length === 0 && (
            <div className="card card-pad p-10 text-center text-ink-500">Nenhum produto encontrado.</div>
          )}

          {!isLoading && !error && products.length > 0 && (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
