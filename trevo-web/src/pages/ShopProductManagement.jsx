import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Plus, Pencil, Package, ImageOff, Archive, RotateCcw } from "lucide-react";
import { getMyProducts, archiveProduct, unarchiveProduct } from "../services/productService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const GARMENT_LABELS = {
  UPPER_BODY: "Parte superior",
  LOWER_BODY: "Parte inferior",
  DRESSES: "Vestido",
};

function formatPrice(price) {
  if (price === null || price === undefined) return "";
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(price));
}

function imageUrl(images) {
  if (!Array.isArray(images) || images.length === 0) return null;
  return images[0]?.imageUrl || null;
}

export default function ShopProductManagement() {
  const [products, setProducts] = useState([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [pendingId, setPendingId] = useState(null);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const data = await getMyProducts();
        if (active) setProducts(Array.isArray(data) ? data : data?.content || []);
      } catch (loadError) {
        if (active) {
          setError(
            loadError.response?.data?.message ||
            loadError.response?.data?.detail ||
            "Não foi possível carregar os produtos."
          );
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    load();
    return () => { active = false; };
  }, []);

  async function handleToggleStatus(product) {
    setPendingId(product.id);
    try {
      const updated = product.status === "ACTIVE"
        ? await archiveProduct(product.id)
        : await unarchiveProduct(product.id);
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
    } catch (toggleError) {
      setError(
        toggleError.response?.data?.message ||
        toggleError.response?.data?.detail ||
        "Não foi possível alterar o estado do produto."
      );
    } finally {
      setPendingId(null);
    }
  }

  return (
    <div className="page-shell max-w-5xl">
      <div className="flex items-end justify-between gap-4">
        <div>
          <p className="eyebrow text-clover-700">Loja · Catálogo</p>
          <h1 className="h-page mt-2">Gerir produtos</h1>
        </div>
        <Link to="/products/new" className="btn-primary">
          <Plus className="w-4 h-4" strokeWidth={1.8} />
          Adicionar produto
        </Link>
      </div>

      {isLoading && <LoadingState label="A carregar produtos..." />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}

      {!isLoading && !error && products.length === 0 && (
        <div className="card card-pad p-10 text-center text-ink-500 mt-8">
          Sem produtos. Cria o teu primeiro produto!
        </div>
      )}

      {!isLoading && !error && products.length > 0 && (
        <>
          <ProductSection
            title="Produtos ativos"
            products={products.filter((p) => p.status === "ACTIVE")}
            emptyMessage="Sem produtos ativos."
            pendingId={pendingId}
            onToggleStatus={handleToggleStatus}
          />
          <ProductSection
            title="Arquivados"
            products={products.filter((p) => p.status !== "ACTIVE")}
            emptyMessage={null}
            pendingId={pendingId}
            onToggleStatus={handleToggleStatus}
          />
        </>
      )}
    </div>
  );
}

function ProductSection({ title, products, emptyMessage, pendingId, onToggleStatus }) {
  if (products.length === 0 && !emptyMessage) return null;

  return (
    <section className="mt-8">
      <h2 className="text-lg font-semibold tracking-tight mb-3">{title}</h2>
      {products.length === 0 ? (
        <div className="card card-pad p-6 text-center text-ink-500 text-sm">{emptyMessage}</div>
      ) : (
        <div className="card divide-y divide-ink-100">
          {products.map((product) => (
            <ProductRow key={product.id} product={product} pendingId={pendingId} onToggleStatus={onToggleStatus} />
          ))}
        </div>
      )}
    </section>
  );
}

function ProductRow({ product, pendingId, onToggleStatus }) {
  const isActive = product.status === "ACTIVE";

  return (
    <div className="flex items-center gap-4 p-4">
      {imageUrl(product.images) ? (
        <img
          src={imageUrl(product.images)}
          alt={product.name}
          className="h-14 w-14 rounded border border-ink-200 object-cover shrink-0"
        />
      ) : (
        <div className="h-14 w-14 rounded border border-ink-200 bg-ink-50 flex items-center justify-center text-ink-300 shrink-0">
          <ImageOff className="w-5 h-5" strokeWidth={1.5} />
        </div>
      )}

      <div className="min-w-0 flex-1">
        <p className="font-semibold text-sm truncate">{product.name}</p>
        <div className="flex flex-wrap gap-x-4 gap-y-0.5 mt-1 text-xs text-ink-500">
          <span className="tabular">{formatPrice(product.price)}</span>
          {product.garmentType && (
            <span>{GARMENT_LABELS[product.garmentType] || product.garmentType}</span>
          )}
        </div>
      </div>

      <div className="flex items-center gap-2 shrink-0">
        <Link
          to={`/products/${product.id}/edit`}
          className="inline-flex items-center gap-1.5 h-9 px-3 rounded border border-ink-200 text-sm text-ink-700 hover:bg-ink-50 transition-colors"
        >
          <Pencil className="w-3.5 h-3.5" strokeWidth={1.7} />
          Editar
        </Link>
        <Link
          to={`/products/${product.id}/stock`}
          className="inline-flex items-center gap-1.5 h-9 px-3 rounded border border-ink-200 text-sm text-ink-700 hover:bg-ink-50 transition-colors"
        >
          <Package className="w-3.5 h-3.5" strokeWidth={1.7} />
          Stock
        </Link>
        <button
          type="button"
          onClick={() => onToggleStatus(product)}
          disabled={pendingId === product.id}
          className={
            "inline-flex items-center gap-1.5 h-9 px-3 rounded border text-sm transition-colors " +
            (isActive
              ? "border-red-200 text-red-600 hover:bg-red-50"
              : "border-green-200 text-green-600 hover:bg-green-50")
          }
        >
          {isActive ? (
            <>
              <Archive className="w-3.5 h-3.5" strokeWidth={1.7} />
              Arquivar
            </>
          ) : (
            <>
              <RotateCcw className="w-3.5 h-3.5" strokeWidth={1.7} />
              Reativar
            </>
          )}
        </button>
      </div>
    </div>
  );
}
