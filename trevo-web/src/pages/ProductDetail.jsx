import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import {
  ChevronLeft,
  ChevronRight,
  ImageOff,
  Camera,
  X,
  CheckCircle2,
} from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { productImageAlt } from "../lib/imageAlt";
import { normalizeRole, ROLES } from "../lib/roles";
import { addCartItem } from "../services/cartService";
import { getProduct } from "../services/productService";
import { generateTryOn, getTryOnUsage } from "../services/tryOnService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

function formatPrice(price) {
  if (price === null || price === undefined || price === "") {
    return "";
  }
  return new Intl.NumberFormat("pt-PT", { style: "currency", currency: "EUR" }).format(Number(price));
}

function imageUrlFrom(image) {
  if (typeof image === "string") return image;
  return image?.imageUrl || image?.url || image?.src || "";
}

function normalizeImages(images, productName) {
  if (!Array.isArray(images)) return [];
  const fallbackAlt = productImageAlt(productName);
  return images
    .map((image, index) => ({
      id: image?.id ?? imageUrlFrom(image) ?? index,
      url: imageUrlFrom(image),
      alt: image?.alt?.trim() || image?.altText?.trim() || fallbackAlt,
      displayOrder: image?.displayOrder ?? index,
    }))
    .filter((image) => image.url)
    .sort((a, b) => a.displayOrder - b.displayOrder);
}

function uniqueValues(values) {
  return [...new Set(values.filter(Boolean))];
}

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    error.message ||
    fallback
  );
}

export default function ProductDetail() {
  const { t } = useTranslation("browse");
  const { productId } = useParams();
  const { isAuthenticated, role } = useAuth();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [selectedSize, setSelectedSize] = useState("");
  const [selectedColor, setSelectedColor] = useState("");
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [cartSuccess, setCartSuccess] = useState("");
  const [cartError, setCartError] = useState("");
  const [isTryOnOpen, setIsTryOnOpen] = useState(false);
  const [isGeneratingTryOn, setIsGeneratingTryOn] = useState(false);
  const [generatedTryOnImage, setGeneratedTryOnImage] = useState("");
  const [tryOnError, setTryOnError] = useState("");
  const [tryOnPhoto, setTryOnPhoto] = useState(null);
  const [isPro, setIsPro] = useState(null);
  const [generationsLeft, setGenerationsLeft] = useState(null);

  useEffect(() => {
    let active = true;

    async function loadProduct() {
      try {
        const loadedProduct = await getProduct(productId);

        if (active) {
          setProduct(loadedProduct);
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(
            loadError.response?.data?.message ||
              loadError.response?.data?.detail ||
              t("loadProductFailed")
          );
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadProduct();

    return () => {
      active = false;
    };
  }, [productId, t]);

  useEffect(() => {
    setCartSuccess("");
    setCartError("");
  }, [selectedSize, selectedColor]);

  const images = useMemo(
    () => normalizeImages(product?.images, product?.name),
    [product]
  );
  const variants = useMemo(() => product?.variants || [], [product]);
  const sizes = useMemo(
    () => uniqueValues(variants.map((variant) => variant.size)),
    [variants]
  );
  const colors = useMemo(
    () => uniqueValues(variants.map((variant) => variant.color)),
    [variants]
  );

  const selectedVariant = variants.find(
    (variant) => variant.size === selectedSize && variant.color === selectedColor
  );
  const freeTierExhausted = isPro === false && generationsLeft !== null && generationsLeft <= 0;

  function isSizeDisabled(size) {
    return !variants.some(
      (variant) =>
        variant.size === size &&
        variant.stock > 0 &&
        (!selectedColor || variant.color === selectedColor)
    );
  }

  function isColorDisabled(color) {
    return !variants.some(
      (variant) =>
        variant.color === color &&
        variant.stock > 0 &&
        (!selectedSize || variant.size === selectedSize)
    );
  }

  function showNextImage() {
    setActiveImageIndex((current) => (current + 1) % images.length);
  }

  function showPreviousImage() {
    setActiveImageIndex((current) =>
      current === 0 ? images.length - 1 : current - 1
    );
  }

  async function handleAddToCart() {
    if (!selectedVariant || selectedVariant.stock <= 0) return;

    setCartSuccess("");
    setCartError("");
    setIsAddingToCart(true);

    try {
      await addCartItem(selectedVariant.id, 1);
      setCartSuccess("Produto adicionado ao carrinho.");
    } catch (addError) {
      setCartError(getErrorMessage(addError, t("addToCartFailed")));
    } finally {
      setIsAddingToCart(false);
    }
  }

  async function loadTryOnStatus() {
    if (!isAuthenticated || normalizeRole(role) !== ROLES.BUYER) return;
    try {
      const usage = await getTryOnUsage();
      setIsPro(usage.pro);
      setGenerationsLeft(usage.generationsLeft);
    } catch {
      // Status unavailable
    }
  }

  function handleOpenTryOn() {
    setIsTryOnOpen(true);
    loadTryOnStatus();
  }

  function closeTryOnModal() {
    if (tryOnPhoto) {
      URL.revokeObjectURL(tryOnPhoto.previewUrl);
    }
    setTryOnPhoto(null);
    setIsTryOnOpen(false);
    setGeneratedTryOnImage("");
    setTryOnError("");
  }

  function handleTryOnPhotoChange(event) {
    const file = event.target.files[0];
    if (!file) return;

    if (tryOnPhoto) {
      URL.revokeObjectURL(tryOnPhoto.previewUrl);
    }

    setTryOnPhoto({ file, previewUrl: URL.createObjectURL(file) });
    setGeneratedTryOnImage("");
    setTryOnError("");
  }

  async function handleGenerateTryOn() {
    if (!tryOnPhoto) return;

    if (tryOnPhoto.file.size > 5 * 1024 * 1024) {
      setTryOnError(t("tryOnPhotoTooLarge"));
      return;
    }

    setGeneratedTryOnImage("");
    setTryOnError("");
    setIsGeneratingTryOn(true);

    try {
      const result = await generateTryOn(Number(productId), tryOnPhoto.file);
      setGeneratedTryOnImage(result.imageUrl);
      loadTryOnStatus();
    } catch (err) {
      setTryOnError(getErrorMessage(err, t("tryOnFailed")));
    } finally {
      setIsGeneratingTryOn(false);
    }
  }

  const isAddToCartDisabled =
    !selectedSize ||
    !selectedColor ||
    !selectedVariant ||
    selectedVariant.stock <= 0 ||
    isAddingToCart;

  return (
    <div className="page-shell max-w-6xl">
      {isLoading && <LoadingState label="A carregar produto..." />}
      {error && <Alert variant="error">{error}</Alert>}

      {!isLoading && !error && product && (
        <div className="grid gap-10 md:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)]">
          {/* Gallery */}
          <section aria-label={t("productGallery")}>
            {images.length > 0 ? (
              <>
                <div className="relative aspect-square overflow-hidden rounded border border-ink-200 bg-ink-50">
                  <img
                    src={images[activeImageIndex].url}
                    alt={images[activeImageIndex].alt}
                    className="h-full w-full object-cover"
                  />
                  {images.length > 1 && (
                    <>
                      <button
                        type="button"
                        onClick={showPreviousImage}
                        className="absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white/90 hover:bg-white border border-ink-200 flex items-center justify-center shadow-sm transition-colors"
                        aria-label={t("previous", { ns: "common" })}
                      >
                        <ChevronLeft className="w-4 h-4" strokeWidth={1.8} />
                      </button>
                      <button
                        type="button"
                        onClick={showNextImage}
                        className="absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white/90 hover:bg-white border border-ink-200 flex items-center justify-center shadow-sm transition-colors"
                        aria-label={t("next", { ns: "common" })}
                      >
                        <ChevronRight className="w-4 h-4" strokeWidth={1.8} />
                      </button>
                    </>
                  )}
                </div>

                {images.length > 1 && (
                  <div className="mt-3 flex gap-2 overflow-x-auto">
                    {images.map((image, index) => (
                      <button
                        key={image.id}
                        type="button"
                        onClick={() => setActiveImageIndex(index)}
                        aria-label={t("showImage", { index: index + 1 })}
                        className={`shrink-0 h-16 w-16 overflow-hidden rounded transition-all ${
                          activeImageIndex === index
                            ? "ring-2 ring-ink-900 ring-offset-2"
                            : "border border-ink-200 hover:border-ink-400"
                        }`}
                      >
                        <img
                          src={image.url}
                          alt={image.alt}
                          className="h-full w-full object-cover"
                        />
                      </button>
                    ))}
                  </div>
                )}
              </>
            ) : (
              <div className="flex aspect-square items-center justify-center rounded border border-dashed border-ink-300 bg-ink-50 text-ink-400">
                <div className="flex flex-col items-center gap-2">
                  <ImageOff className="w-7 h-7" strokeWidth={1.5} />
                  <span className="text-xs">Sem imagem disponível</span>
                </div>
              </div>
            )}
          </section>

          {/* Buy panel */}
          <section>
            {product.shopName && (
              <Link to={`/shops/${product.shopId}`} className="text-sm font-medium text-clover-700 hover:underline">
                {product.shopName}
              </Link>
            )}
            <p className={product.shopName ? "eyebrow mt-1" : "eyebrow"}>{product.brand || t("brandMissing")}</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight leading-tight">{product.name}</h1>
            {product.price !== undefined && product.price !== null && (
              <p className="mt-3 text-2xl font-bold tabular">{formatPrice(product.price)}</p>
            )}
            {product.description && (
              <p className="mt-5 leading-relaxed text-ink-600">{product.description}</p>
            )}

            <dl className="mt-6 grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5 text-sm">
              <dt className="text-ink-500">{t("brand")}</dt>
              <dd className="text-ink-900">{product.brand || t("notIndicated")}</dd>
              <dt className="text-ink-500">{t("materials")}</dt>
              <dd className="text-ink-900">{product.materials || t("notIndicatedPlural")}</dd>
            </dl>

            <div className="mt-8">
              <h2 className="text-sm font-semibold mb-2.5">{t("size")}</h2>
              <div className="flex flex-wrap gap-2">
                {sizes.length === 0 && (
                  <p className="text-sm text-ink-500">{t("noSizes")}</p>
                )}
                {sizes.map((size) => (
                  <button
                    key={size}
                    type="button"
                    disabled={isSizeDisabled(size)}
                    onClick={() => setSelectedSize(size)}
                    className={
                      "opt-pill " +
                      (selectedSize === size ? "opt-pill-selected" : "")
                    }
                  >
                    {size}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-6">
              <h2 className="text-sm font-semibold mb-2.5">{t("color")}</h2>
              <div className="flex flex-wrap gap-2">
                {colors.length === 0 && (
                  <p className="text-sm text-ink-500">{t("noColors")}</p>
                )}
                {colors.map((color) => (
                  <button
                    key={color}
                    type="button"
                    disabled={isColorDisabled(color)}
                    onClick={() => setSelectedColor(color)}
                    className={
                      "opt-pill " +
                      (selectedColor === color ? "opt-pill-selected" : "")
                    }
                  >
                    {color}
                  </button>
                ))}
              </div>
            </div>

            {/* Stock status */}
            <div
              className={
                "mt-6 px-3.5 py-2.5 rounded border text-sm " +
                (!selectedSize || !selectedColor
                  ? "bg-ink-50 border-ink-200 text-ink-600"
                  : selectedVariant && selectedVariant.stock > 0
                  ? "bg-green-50 border-green-200 text-green-700"
                  : "bg-red-50 border-red-200 text-red-700")
              }
            >
              {!selectedSize || !selectedColor ? (
                t("selectSizeColor")
              ) : selectedVariant ? (
                selectedVariant.stock > 0
                  ? t("inStock", { count: selectedVariant.stock })
                  : t("outOfStock")
              ) : (
                t("combinationUnavailable")
              )}
            </div>

            <button
              type="button"
              onClick={handleAddToCart}
              disabled={isAddToCartDisabled}
              className="btn-primary w-full mt-4 py-3.5"
            >
              {isAddingToCart ? t("addingToCart") : t("addToCart")}
            </button>

            <button
              type="button"
              onClick={handleOpenTryOn}
              disabled={!isAuthenticated}
              title={!isAuthenticated ? t("loginRequired") : ""}
              className="btn-secondary w-full mt-3 py-3"
            >
              <Camera className="w-4 h-4 text-clover-500" strokeWidth={1.8} />
              {t("virtualTryOn")}
            </button>

            {cartSuccess && (
              <Alert variant="success" className="mt-3">{cartSuccess}</Alert>
            )}
            {cartError && (
              <Alert variant="error" className="mt-3">{cartError}</Alert>
            )}

          </section>
        </div>
      )}

      {/* Virtual try-on modal */}
      {isTryOnOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/45 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="virtual-try-on-title"
            className="w-full max-w-lg rounded-md bg-white p-6 shadow-xl"
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 id="virtual-try-on-title" className="text-xl font-semibold tracking-tight">
                    {t("tryOnTitle")}
                  </h2>
                  {isPro === true && (
                    <span className="badge badge-pro" aria-label="Conta Pro: gerações ilimitadas">
                      PRO · Ilimitado
                    </span>
                  )}
                  {isPro === false && generationsLeft !== null && generationsLeft > 0 && (
                    <span className="badge badge-neutral">
                      {t("generationsLeft", { count: generationsLeft })}
                    </span>
                  )}
                </div>
                <p className="mt-2 text-sm text-ink-500">{t("tryOnDescription")}</p>
              </div>
              <button
                type="button"
                onClick={closeTryOnModal}
                className="w-8 h-8 rounded inline-flex items-center justify-center border border-ink-200 hover:bg-ink-50 transition-colors"
                aria-label={t("close", { ns: "common" })}
              >
                <X className="w-4 h-4" strokeWidth={1.8} />
              </button>
            </div>

            {freeTierExhausted && (
              <Alert variant="warning" className="mt-4">
                {t("freeTierExhausted")}{" "}
                <Link to="/pro" className="font-semibold underline text-clover-700">
                  {t("upgradeForMore")}
                </Link>
              </Alert>
            )}

            <div className="mt-6">
              <label htmlFor="try-on-photo" className="text-sm font-semibold">
                {t("tryOnPhotoLabel")}
              </label>
              <p className="text-xs text-ink-500 mt-0.5 mb-2">{t("tryOnPhotoHelp")}</p>
              <input
                id="try-on-photo"
                type="file"
                accept="image/*"
                onChange={handleTryOnPhotoChange}
                className="input file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:bg-ink-100 file:text-ink-800 file:font-medium file:text-xs hover:file:bg-ink-200"
              />
            </div>

            {tryOnPhoto && (
              <div className="mt-4">
                <img
                  src={tryOnPhoto.previewUrl}
                  alt={t("tryOnPreviewAlt")}
                  className="h-32 w-32 rounded border border-ink-200 object-cover"
                />
              </div>
            )}

            <button
              type="button"
              onClick={handleGenerateTryOn}
              disabled={
                isGeneratingTryOn ||
                !tryOnPhoto ||
                freeTierExhausted
              }
              className="btn-primary w-full mt-6 py-3"
            >
              <Camera className="w-4 h-4" strokeWidth={1.8} />
              {isGeneratingTryOn ? t("generatingTryOn") : t("generateTryOn")}
            </button>

            {isGeneratingTryOn && (
              <p className="mt-3 text-sm text-ink-500">
                {t("tryOnWait")}
              </p>
            )}

            {tryOnError && (
              <p className="mt-3 text-sm text-red-600" role="alert">
                {tryOnError}
              </p>
            )}

            {generatedTryOnImage && (
              <div className="mt-6">
                <h3 className="text-sm font-semibold flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-green-600" strokeWidth={1.8} />
                  {t("result")}
                </h3>
                <img
                  src={generatedTryOnImage}
                  alt={t("tryOnResultAlt")}
                  className="mt-3 aspect-square w-full rounded border border-ink-200 object-cover"
                />
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
