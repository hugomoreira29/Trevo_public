import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { AlertTriangle, ArrowLeft } from "lucide-react";
import { bulkUpdateVariantStock, getProduct } from "../services/productService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function stockFromValue(value) {
  const stock = Number(value);
  return Number.isFinite(stock) && stock >= 0 ? Math.floor(stock) : 0;
}

export default function StockManagement() {
  const { productId } = useParams();
  const [product, setProduct] = useState(null);
  const [stocks, setStocks] = useState({});
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadProduct() {
      try {
        const loadedProduct = await getProduct(productId);
        if (active) {
          setProduct(loadedProduct);
          setStocks(
            (loadedProduct.variants || []).reduce((nextStocks, variant) => {
              nextStocks[variant.id] = String(variant.stock ?? 0);
              return nextStocks;
            }, {})
          );
          setError("");
        }
      } catch (loadError) {
        if (active) setError(getErrorMessage(loadError, "Não foi possível carregar o produto."));
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadProduct();
    return () => {
      active = false;
    };
  }, [productId]);

  function handleStockChange(variantId, value) {
    setSuccess("");
    setStocks((currentStocks) => ({
      ...currentStocks,
      [variantId]: String(stockFromValue(value)),
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    const updates = (product?.variants || []).map((variant) => ({
      variantId: variant.id,
      stock: stockFromValue(stocks[variant.id]),
    }));

    try {
      setIsSaving(true);
      const updatedVariants = await bulkUpdateVariantStock(productId, updates);
      setProduct((currentProduct) => ({
        ...currentProduct,
        variants: updatedVariants,
      }));
      setStocks(
        updatedVariants.reduce((nextStocks, variant) => {
          nextStocks[variant.id] = String(variant.stock ?? 0);
          return nextStocks;
        }, {})
      );
      setSuccess("Stock atualizado com sucesso.");
    } catch (saveError) {
      setError(getErrorMessage(saveError, "Não foi possível atualizar o stock."));
    } finally {
      setIsSaving(false);
    }
  }

  const variants = product?.variants || [];

  return (
    <div className="page-shell-narrow">
      <Link
        to="#"
        onClick={(e) => { e.preventDefault(); window.history.back(); }}
        className="inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900 mb-4"
      >
        <ArrowLeft className="w-4 h-4" strokeWidth={1.7} />
        Voltar
      </Link>
      <p className="eyebrow text-clover-700">Loja · Catálogo</p>
      <h1 className="h-page mt-2">Gestão de stock</h1>

      {isLoading && <LoadingState label="A carregar produto..." />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}
      {success && <Alert variant="success" className="mt-6">{success}</Alert>}

      {!isLoading && product && (
        <>
          <h2 className="mt-6 text-lg font-semibold tracking-tight">{product.name}</h2>

          {variants.length === 0 ? (
            <div className="mt-6 card card-pad p-10 text-center">
              <div className="w-12 h-12 mx-auto rounded bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-700">
                <AlertTriangle className="w-5 h-5" strokeWidth={1.7} />
              </div>
              <p className="mt-4 text-ink-500">Este produto ainda não tem variantes.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="mt-6">
              <div className="card overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-ink-100 bg-ink-50/60">
                      <th className="py-3 px-5 text-xs uppercase tracking-wider text-ink-500 font-medium">Tamanho</th>
                      <th className="py-3 px-5 text-xs uppercase tracking-wider text-ink-500 font-medium">Cor</th>
                      <th className="py-3 px-5 text-xs uppercase tracking-wider text-ink-500 font-medium">Stock</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {variants.map((variant) => {
                      const stock = stockFromValue(stocks[variant.id] ?? "0");
                      const low = stock <= 2;
                      return (
                        <tr key={variant.id}>
                          <td className="py-3 px-5 font-medium">{variant.size}</td>
                          <td className="py-3 px-5">{variant.color}</td>
                          <td className="py-3 px-5">
                            <div className="flex items-center gap-2">
                              <input
                                type="number"
                                name={`stock-${variant.id}`}
                                aria-label={`Stock ${variant.size} ${variant.color}`}
                                min="0"
                                value={stocks[variant.id] ?? "0"}
                                onChange={(event) => handleStockChange(variant.id, event.target.value)}
                                className="input tabular w-28"
                              />
                              {low && (
                                <span className="badge badge-warning">Baixo</span>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div className="mt-6 flex justify-end">
                <button type="submit" disabled={isSaving} className="btn-primary">
                  {isSaving ? "A guardar..." : "Guardar alterações"}
                </button>
              </div>
            </form>
          )}
        </>
      )}
    </div>
  );
}
