import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Trash2, Plus, Upload, X } from "lucide-react";
import { getShopDashboard, getShopProfile } from "../services/shopService";
import {
  assignProductCollections,
  createProduct,
  createProductVariant,
  deleteProductVariant,
  getProduct,
  updateProduct,
  updateProductVariant,
} from "../services/productService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const initialForm = {
  name: "",
  description: "",
  price: "",
  brand: "",
  materials: "",
  garmentType: "",
};

const GARMENT_TYPE_OPTIONS = [
  { value: "UPPER_BODY", label: "Parte superior" },
  { value: "LOWER_BODY", label: "Parte inferior" },
  { value: "DRESSES", label: "Vestido" },
];

const emptyVariant = {
  size: "",
  color: "",
  stock: "",
};

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function sortCollections(collections) {
  return [...collections].sort((a, b) => a.displayOrder - b.displayOrder);
}

function isVariantFilled(variant) {
  return Boolean(variant.size.trim() || variant.color.trim() || variant.stock !== "");
}

function toVariantPayload(variant) {
  return {
    size: variant.size.trim(),
    color: variant.color.trim(),
    stock: Number(variant.stock),
  };
}

function toProductPayload(form) {
  return {
    name: form.name.trim(),
    description: form.description.trim(),
    price: form.price,
    brand: form.brand.trim(),
    materials: form.materials.trim(),
    garmentType: form.garmentType || null,
  };
}

export default function ProductForm({ mode }) {
  const { productId } = useParams();
  const isEditMode = mode === "edit";
  const [form, setForm] = useState(initialForm);
  const [variants, setVariants] = useState([{ ...emptyVariant }]);
  const [removedVariantIds, setRemovedVariantIds] = useState([]);
  const [images, setImages] = useState([]);
  const [collections, setCollections] = useState([]);
  const [selectedCollectionIds, setSelectedCollectionIds] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isLoading, setIsLoading] = useState(isEditMode);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadCollections(shopId) {
      if (!shopId) return;
      try {
        const profile = await getShopProfile(shopId);
        if (active) setCollections(sortCollections(profile.collections || []));
      } catch {
        if (active) setCollections([]);
      }
    }

    async function loadInitialData() {
      try {
        if (isEditMode) {
          const product = await getProduct(productId);
          if (active) {
            setForm({
              name: product.name || "",
              description: product.description || "",
              price: product.price == null ? "" : String(product.price),
              brand: product.brand || "",
              materials: product.materials || "",
              garmentType: product.garmentType || "",
            });
            setVariants(
              product.variants?.length
                ? product.variants.map((variant) => ({
                    id: variant.id,
                    size: variant.size || "",
                    color: variant.color || "",
                    stock: variant.stock == null ? "" : String(variant.stock),
                  }))
                : [{ ...emptyVariant }]
            );
          }
          await loadCollections(product.shopId);
          return;
        }

        const dashboard = await getShopDashboard();
        await loadCollections(dashboard?.shop?.id);
      } catch (loadError) {
        if (active && isEditMode) {
          setError(getErrorMessage(loadError, "Não foi possível carregar o produto."));
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadInitialData();
    return () => {
      active = false;
    };
  }, [isEditMode, productId]);

  useEffect(() => {
    return () => {
      images.forEach((image) => URL.revokeObjectURL(image.url));
    };
  }, [images]);

  function handleChange(event) {
    setForm((currentForm) => ({
      ...currentForm,
      [event.target.name]: event.target.value,
    }));
  }

  function handleVariantChange(index, field, value) {
    setVariants((currentVariants) =>
      currentVariants.map((variant, variantIndex) =>
        variantIndex === index ? { ...variant, [field]: value } : variant
      )
    );
  }

  function addVariant() {
    setVariants((currentVariants) => [...currentVariants, { ...emptyVariant }]);
  }

  function removeVariant(index) {
    setVariants((currentVariants) => {
      const variant = currentVariants[index];
      if (variant?.id) {
        setRemovedVariantIds((currentIds) => [...currentIds, variant.id]);
      }
      const nextVariants = currentVariants.filter((_, vi) => vi !== index);
      return nextVariants.length ? nextVariants : [{ ...emptyVariant }];
    });
  }

  function handleImagesChange(event) {
    const selectedFiles = [...event.target.files];
    setError("");

    setImages((currentImages) => {
      const nextFiles = [...currentImages.map((image) => image.file), ...selectedFiles];
      const cappedFiles = nextFiles.slice(0, 8);
      currentImages.forEach((image) => URL.revokeObjectURL(image.url));
      return cappedFiles.map((file) => ({ file, url: URL.createObjectURL(file) }));
    });

    if (images.length + selectedFiles.length > 8) {
      setError("Só podes adicionar até 8 imagens.");
    }
  }

  function toggleCollection(collectionId) {
    setSelectedCollectionIds((currentIds) =>
      currentIds.includes(collectionId)
        ? currentIds.filter((id) => id !== collectionId)
        : [...currentIds, collectionId]
    );
  }

  function validateVariants() {
    return variants.every((variant) => {
      if (!isVariantFilled(variant)) return true;
      const stock = Number(variant.stock);
      return Number.isFinite(stock) && stock >= 0;
    });
  }

  async function submitVariants(nextProductId) {
    for (const variant of variants.filter(isVariantFilled)) {
      const payload = toVariantPayload(variant);
      if (variant.id) {
        await updateProductVariant(nextProductId, variant.id, payload);
      } else {
        await createProductVariant(nextProductId, payload);
      }
    }
    for (const variantId of removedVariantIds) {
      await deleteProductVariant(nextProductId, variantId);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!form.name.trim() || !form.price) {
      setError("Preenche nome e preço.");
      return;
    }

    if (!form.garmentType) {
      setError("Seleciona o tipo de peça.");
      return;
    }

    if (!validateVariants()) {
      setError("O stock deve ser um número igual ou superior a 0.");
      return;
    }

    try {
      setIsSubmitting(true);
      const payload = toProductPayload(form);
      const savedProduct = isEditMode
        ? await updateProduct(productId, payload)
        : await createProduct(payload);
      const nextProductId = savedProduct.id || productId;

      await submitVariants(nextProductId);
      await assignProductCollections(nextProductId, selectedCollectionIds);

      setRemovedVariantIds([]);
      setSuccess(
        isEditMode
          ? "Produto atualizado com sucesso."
          : "Produto criado com sucesso."
      );
    } catch (submitError) {
      setError(
        getErrorMessage(
          submitError,
          isEditMode
            ? "Não foi possível atualizar o produto."
            : "Não foi possível criar o produto."
        )
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="page-shell-narrow">
      <p className="eyebrow text-clover-700">Loja · Catálogo</p>
      <h1 className="h-page mt-2">{isEditMode ? "Editar produto" : "Criar produto"}</h1>

      {isLoading && <LoadingState label="A carregar produto..." />}
      {error && <Alert variant="error" className="mt-6">{error}</Alert>}
      {success && <Alert variant="success" className="mt-6">{success}</Alert>}

      {!isLoading && (
        <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-6">
          <section className="card card-pad p-6">
            <h2 className="text-base font-semibold mb-4">Detalhes</h2>
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="field sm:col-span-2">
                <label htmlFor="product-name" className="field-label">Nome</label>
                <input id="product-name" className="input" type="text" name="name" value={form.name} onChange={handleChange} />
              </div>
              <div className="field sm:col-span-2">
                <label htmlFor="product-description" className="field-label">Descrição</label>
                <textarea id="product-description" className="input min-h-[112px] resize-y" name="description" value={form.description} onChange={handleChange} />
              </div>
              <div className="field">
                <label htmlFor="product-price" className="field-label">Preço (EUR)</label>
                <input id="product-price" className="input tabular" type="number" name="price" min="0" step="0.01" value={form.price} onChange={handleChange} placeholder="0,00" />
              </div>
              <div className="field">
                <label htmlFor="product-brand" className="field-label">Marca</label>
                <input id="product-brand" className="input" type="text" name="brand" value={form.brand} onChange={handleChange} />
              </div>
              <div className="field sm:col-span-2">
                <label htmlFor="product-materials" className="field-label">Materiais</label>
                <input id="product-materials" className="input" type="text" name="materials" value={form.materials} onChange={handleChange} placeholder="100% algodão" />
              </div>
              <div className="field sm:col-span-2">
                <label htmlFor="product-garment-type" className="field-label">Tipo de peça (experimentação virtual)</label>
                <select id="product-garment-type" className="input" name="garmentType" value={form.garmentType} onChange={handleChange}>
                  <option value="">Selecionar...</option>
                  {GARMENT_TYPE_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>
            </div>
          </section>

          <section className="card card-pad p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-semibold">Variantes</h2>
              <span className="text-xs text-ink-500">{variants.filter(isVariantFilled).length} preenchida(s)</span>
            </div>
            <div className="flex flex-col gap-3">
              {variants.map((variant, index) => (
                <div
                  key={variant.id || index}
                  className="grid gap-3 rounded border border-ink-200 bg-ink-50/30 p-3 md:grid-cols-[1fr_1fr_120px_44px]"
                >
                  <input
                    type="text"
                    name={`variant-size-${index}`}
                    placeholder="Tamanho · ex. M"
                    value={variant.size}
                    onChange={(event) => handleVariantChange(index, "size", event.target.value)}
                    className="input"
                  />
                  <input
                    type="text"
                    name={`variant-color-${index}`}
                    placeholder="Cor · ex. Verde"
                    value={variant.color}
                    onChange={(event) => handleVariantChange(index, "color", event.target.value)}
                    className="input"
                  />
                  <input
                    type="number"
                    name={`variant-stock-${index}`}
                    placeholder="Stock"
                    min="0"
                    value={variant.stock}
                    onChange={(event) => handleVariantChange(index, "stock", event.target.value)}
                    className="input tabular"
                  />
                  <button
                    type="button"
                    onClick={() => removeVariant(index)}
                    aria-label="Remover variante"
                    className="w-11 h-11 inline-flex items-center justify-center rounded border border-ink-200 text-ink-600 hover:bg-red-50 hover:text-red-600 hover:border-red-200 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" strokeWidth={1.7} />
                  </button>
                </div>
              ))}
            </div>
            <button type="button" onClick={addVariant} className="btn-secondary mt-4">
              <Plus className="w-4 h-4" strokeWidth={1.8} />
              Adicionar variante
            </button>
          </section>

          <section className="card card-pad p-6">
            <h2 className="text-base font-semibold mb-2">Imagens</h2>
            <p className="text-xs text-ink-500 mb-4">Até 8 imagens. Primeira é a destaque.</p>
            <label className="flex items-center justify-center gap-2 py-6 rounded border border-dashed border-ink-300 bg-ink-50/50 hover:bg-ink-50 cursor-pointer text-sm text-ink-600 transition-colors">
              <Upload className="w-4 h-4" strokeWidth={1.7} />
              Selecionar imagens
              <input
                type="file"
                name="images"
                accept="image/*"
                multiple
                onChange={handleImagesChange}
                className="sr-only"
              />
            </label>
            {images.length > 0 && (
              <ul className="grid grid-cols-2 gap-3 md:grid-cols-4 mt-4">
                {images.map((image, index) => (
                  <li key={image.url} className="relative">
                    <img
                      src={image.url}
                      alt={`Pré-visualização ${index + 1}`}
                      className="h-32 w-full rounded border border-ink-200 object-cover"
                    />
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card card-pad p-6">
            <h2 className="text-base font-semibold mb-4">Coleções</h2>
            {collections.length === 0 ? (
              <p className="text-sm text-ink-500">Sem coleções disponíveis.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {collections.map((collection) => {
                  const selected = selectedCollectionIds.includes(collection.id);
                  return (
                    <label
                      key={collection.id}
                      className={"opt-pill cursor-pointer " + (selected ? "opt-pill-selected" : "")}
                    >
                      <input
                        type="checkbox"
                        name="collectionIds"
                        value={collection.id}
                        checked={selected}
                        onChange={() => toggleCollection(collection.id)}
                        className="sr-only"
                      />
                      {collection.name}
                    </label>
                  );
                })}
              </div>
            )}
          </section>

          <div className="flex justify-between">
            <button type="button" onClick={() => window.history.back()} className="btn-secondary">
              Voltar
            </button>
            <div className="flex gap-3">
              {isEditMode && (
                <Link to={`/products/${productId}/stock`} className="btn-secondary">
                  Gerir stock
                </Link>
              )}
              <button type="submit" disabled={isSubmitting} className="btn-primary">
                {isSubmitting ? "A guardar..." : "Guardar produto"}
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  );
}
