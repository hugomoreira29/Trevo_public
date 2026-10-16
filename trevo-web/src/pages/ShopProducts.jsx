import { useEffect, useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ImageOff } from "lucide-react";
import { browseProducts } from "../services/productService";
import { getShopProfile } from "../services/shopService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const sortOptions = [
  { value: "createdAt,desc", label: "Mais recentes" },
  { value: "name,asc",       label: "Nome A–Z" },
  { value: "price,asc",      label: "Preço crescente" },
  { value: "price,desc",     label: "Preço decrescente" },
];

function sortCollections(collections) {
  return [...collections].sort((a, b) => a.displayOrder - b.displayOrder);
}

function productsFromResponse(response) {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.content)) return response.content;
  return [];
}

function formatPrice(price) {
  if (price === null || price === undefined || price === "") return "";
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(price));
}

function ProductCard({ product }) {
  return (
    <Link to={`/products/${product.id}`} className="group card p-3 block transition-colors hover:border-clover-400">
      <div className="aspect-square bg-ink-50 rounded flex items-center justify-center text-ink-300">
        <ImageOff className="w-7 h-7" strokeWidth={1.5} />
      </div>
      <div className="mt-3 space-y-1">
        {product.brand && (
          <p className="text-[11px] uppercase tracking-wider text-ink-500 font-medium">{product.brand}</p>
        )}
        <h2 className="text-sm font-semibold leading-snug group-hover:text-clover-700 transition-colors">
          {product.name}
        </h2>
        {product.price !== undefined && product.price !== null && (
          <p className="text-sm font-semibold tabular">{formatPrice(product.price)}</p>
        )}
      </div>
    </Link>
  );
}

export default function ShopProducts() {
  const { shopId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [shop, setShop] = useState(null);
  const [products, setProducts] = useState([]);
  const [shopError, setShopError] = useState("");
  const [productsError, setProductsError] = useState("");
  const [isShopLoading, setIsShopLoading] = useState(true);
  const [isProductsLoading, setIsProductsLoading] = useState(true);

  const collectionId = searchParams.get("collectionId") || "";
  const sort = searchParams.get("sort") || "createdAt,desc";
  const collections = useMemo(
    () => sortCollections(shop?.collections || []),
    [shop]
  );

  useEffect(() => {
    let active = true;

    async function loadShop() {
      try {
        const profile = await getShopProfile(shopId);
        if (active) {
          setShop(profile);
          setShopError("");
        }
      } catch (loadError) {
        if (active) {
          setShopError(
            loadError.response?.data?.message ||
              loadError.response?.data?.detail ||
              "Não foi possível carregar a loja."
          );
        }
      } finally {
        if (active) setIsShopLoading(false);
      }
    }

    loadShop();
    return () => {
      active = false;
    };
  }, [shopId]);

  useEffect(() => {
    let active = true;

    async function loadProducts() {
      setIsProductsLoading(true);
      setProductsError("");

      try {
        const params = collectionId ? { collectionId, sort } : { shopId, sort };
        const response = await browseProducts(params);
        if (active) setProducts(productsFromResponse(response));
      } catch (loadError) {
        if (active) {
          setProducts([]);
          setProductsError(
            loadError.response?.data?.message ||
              loadError.response?.data?.detail ||
              "Não foi possível carregar os produtos."
          );
        }
      } finally {
        if (active) setIsProductsLoading(false);
      }
    }

    loadProducts();
    return () => {
      active = false;
    };
  }, [collectionId, shopId, sort]);

  function updateParams(nextValues) {
    const nextParams = new URLSearchParams(searchParams);
    Object.entries(nextValues).forEach(([key, value]) => {
      if (value) nextParams.set(key, value);
      else nextParams.delete(key);
    });
    setSearchParams(nextParams);
  }

  function selectCollection(nextCollectionId) {
    updateParams({ collectionId: nextCollectionId });
  }

  function changeSort(event) {
    updateParams({ sort: event.target.value });
  }

  return (
    <div className="page-shell">
      {isShopLoading && <LoadingState label="A carregar loja..." />}
      {shopError && <Alert variant="error">{shopError}</Alert>}

      {!isShopLoading && !shopError && shop && (
        <>
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <Link to={`/shops/${shopId}`} className="eyebrow text-clover-700 hover:underline">
                {shop.name}
              </Link>
              <h1 className="h-page mt-2">Produtos</h1>
            </div>

            <label className="field max-w-[220px]">
              <span className="field-label">Ordenar</span>
              <select name="sort" value={sort} onChange={changeSort} className="select">
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
          </div>

          <nav className="mt-6 flex flex-wrap gap-2" aria-label="Coleções">
            <button
              type="button"
              onClick={() => selectCollection("")}
              className={"opt-pill " + (!collectionId ? "opt-pill-selected" : "")}
            >
              Todos
            </button>
            {collections.map((collection) => (
              <button
                key={collection.id}
                type="button"
                onClick={() => selectCollection(String(collection.id))}
                className={"opt-pill " + (collectionId === String(collection.id) ? "opt-pill-selected" : "")}
              >
                {collection.name}
              </button>
            ))}
          </nav>

          <section className="mt-8">
            {isProductsLoading && <LoadingState label="A carregar produtos..." />}
            {productsError && <Alert variant="error">{productsError}</Alert>}

            {!isProductsLoading && !productsError && products.length === 0 && (
              <div className="card card-pad p-10 text-center text-ink-500">Nenhum produto encontrado.</div>
            )}

            {!isProductsLoading && !productsError && products.length > 0 && (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
