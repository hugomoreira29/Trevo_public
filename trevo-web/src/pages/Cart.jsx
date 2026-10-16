import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { Minus, Plus, Trash2, ImageOff, ShoppingBag, ArrowRight } from "lucide-react";
import {
  getCart,
  removeCartItem,
  updateCartItem,
} from "../services/cartService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

function formatPrice(value) {
  return new Intl.NumberFormat("pt-PT", {
    style: "currency",
    currency: "EUR",
  }).format(Number(value || 0));
}

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

export default function Cart() {
  const { t } = useTranslation("checkout");
  const [cart, setCart] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [pendingVariantId, setPendingVariantId] = useState(null);

  async function loadCart() {
    const loadedCart = await getCart();
    setCart(loadedCart);
    setError("");
  }

  useEffect(() => {
    let active = true;

    async function loadInitialCart() {
      try {
        const loadedCart = await getCart();

        if (active) {
          setCart(loadedCart);
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(getErrorMessage(loadError, t("loadCartFailed")));
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadInitialCart();

    return () => {
      active = false;
    };
  }, [t]);

  async function handleQuantityChange(item, nextQuantity) {
    if (nextQuantity < 1) return;

    try {
      setPendingVariantId(item.variantId);
      const updatedCart = await updateCartItem(item.variantId, nextQuantity);
      setCart(updatedCart);
      setError("");
    } catch (updateError) {
      setError(getErrorMessage(updateError, t("updateCartFailed")));
    } finally {
      setPendingVariantId(null);
    }
  }

  async function handleRemove(item) {
    try {
      setPendingVariantId(item.variantId);
      await removeCartItem(item.variantId);
      await loadCart();
    } catch (removeError) {
      setError(getErrorMessage(removeError, t("removeItemFailed")));
    } finally {
      setPendingVariantId(null);
    }
  }

  const shops = cart?.shops || [];
  const isEmpty = !isLoading && !error && shops.length === 0;

  return (
    <div className="page-shell max-w-5xl">
      <h1 className="h-page">{t("cart")}</h1>

      {isLoading && <LoadingState label={t("loadingCart")} />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}

      {isEmpty && (
        <div className="mt-10 card card-pad p-16 text-center">
          <div className="w-12 h-12 mx-auto rounded bg-ink-50 border border-ink-100 flex items-center justify-center text-ink-400">
            <ShoppingBag className="w-5 h-5" strokeWidth={1.7} />
          </div>
          <p className="mt-4 text-ink-500">{t("emptyCart")}</p>
          <Link to="/products" className="btn-secondary mt-5 inline-flex">
            Explorar produtos
            <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
          </Link>
        </div>
      )}

      {!isLoading && !error && shops.length > 0 && (
        <div className="mt-8 space-y-8">
          {shops.map((shop) => (
            <section
              key={shop.shopId}
              aria-labelledby={`shop-${shop.shopId}`}
              className="border-b border-ink-100 pb-7"
            >
              <div className="flex items-center justify-between gap-4 mb-4">
                <h2 id={`shop-${shop.shopId}`} className="text-lg font-semibold tracking-tight">
                  <Link to={`/shops/${shop.shopId}`} className="hover:text-clover-700 transition-colors">
                    {shop.shopName}
                  </Link>
                </h2>
                <p className="text-sm text-ink-600">
                  {t("subtotal")}: <span className="font-semibold text-ink-900 tabular">{formatPrice(shop.subtotal)}</span>
                </p>
              </div>

              <ul className="space-y-3">
                {shop.items.map((item) => {
                  const isPending = pendingVariantId === item.variantId;

                  return (
                    <li
                      key={item.cartItemId || item.variantId}
                      className="card grid gap-4 p-4 md:grid-cols-[96px_minmax(0,1fr)_auto] items-center"
                    >
                      {item.image ? (
                        <img
                          src={item.image}
                          alt={t("productImageAlt", { name: item.productName || t("unnamedProduct") })}
                          className="h-24 w-24 object-cover rounded"
                        />
                      ) : (
                        <div className="flex h-24 w-24 items-center justify-center bg-ink-50 rounded text-ink-300">
                          <ImageOff className="w-5 h-5" strokeWidth={1.5} />
                        </div>
                      )}

                      <div>
                        <h3 className="font-semibold text-ink-900">
                          <Link to={`/products/${item.productId}`} className="hover:text-clover-700 transition-colors">
                            {item.productName}
                          </Link>
                        </h3>
                        <p className="mt-1 text-xs text-ink-500">
                          {t("sizeColor", { size: item.size || "N/A", color: item.color || "N/A" })}
                        </p>
                        <p className="mt-2 text-xs text-ink-500">
                          {t("unitPrice")}: <span className="tabular">{formatPrice(item.unitPrice)}</span>
                        </p>
                        <p className="text-sm font-semibold mt-1 tabular">
                          {t("subtotal")}: {formatPrice(item.subtotal)}
                        </p>
                      </div>

                      <div className="flex items-center gap-2 md:justify-end">
                        <div className="inline-flex items-center border border-ink-200 rounded">
                          <button
                            type="button"
                            onClick={() => handleQuantityChange(item, item.quantity - 1)}
                            disabled={isPending || item.quantity <= 1}
                            aria-label={t("decreaseQuantity", { name: item.productName })}
                            className="w-9 h-9 inline-flex items-center justify-center text-ink-700 hover:bg-ink-50 disabled:opacity-40 disabled:hover:bg-white transition-colors"
                          >
                            <Minus className="w-3.5 h-3.5" strokeWidth={2} />
                          </button>
                          <span
                            aria-label={t("quantityOf", { name: item.productName })}
                            className="min-w-[36px] text-center text-sm tabular"
                          >
                            {item.quantity}
                          </span>
                          <button
                            type="button"
                            onClick={() => handleQuantityChange(item, item.quantity + 1)}
                            disabled={isPending}
                            aria-label={t("increaseQuantity", { name: item.productName })}
                            className="w-9 h-9 inline-flex items-center justify-center text-ink-700 hover:bg-ink-50 disabled:opacity-40 disabled:hover:bg-white transition-colors"
                          >
                            <Plus className="w-3.5 h-3.5" strokeWidth={2} />
                          </button>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleRemove(item)}
                          disabled={isPending}
                          className="ml-1 w-9 h-9 inline-flex items-center justify-center border border-ink-200 rounded text-ink-700 hover:bg-red-50 hover:text-red-600 hover:border-red-200 disabled:opacity-40 transition-colors"
                          aria-label={t("remove")}
                        >
                          <Trash2 className="w-4 h-4" strokeWidth={1.7} />
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            </section>
          ))}

          <div className="flex flex-col-reverse sm:flex-row justify-between items-stretch sm:items-center gap-4">
            <Link to="/products" className="btn-secondary">Continuar a comprar</Link>
            <div className="flex items-center justify-between sm:justify-end gap-6">
              <span className="text-sm text-ink-500">{t("grandTotal")}</span>
              <strong className="text-2xl font-bold tabular">
                {formatPrice(cart.grandTotal)}
              </strong>
              <Link to="/checkout" className="btn-primary">
                {t("checkout")}
                <ArrowRight className="w-4 h-4" strokeWidth={1.8} />
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
