import { useState } from "react";
import { Upload, Store } from "lucide-react";
import { uploadImage } from "../services/imageUploadService";
import { createShop } from "../services/shopService";
import Alert from "../components/Alert";

const initialForm = {
  name: "",
  description: "",
  nif: "",
};

const initialImages = {
  logo: { file: null, previewUrl: "", progress: 0 },
  banner: { file: null, previewUrl: "", progress: 0 },
};

export default function CreateShop() {
  const [form, setForm] = useState(initialForm);
  const [images, setImages] = useState(initialImages);
  const [imageInputResetKey, setImageInputResetKey] = useState(0);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleChange(event) {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }));
  }

  function handleImageChange(event) {
    const { name, files } = event.target;
    const file = files?.[0] || null;

    setImages((prev) => {
      if (prev[name].previewUrl) URL.revokeObjectURL(prev[name].previewUrl);
      return {
        ...prev,
        [name]: {
          file,
          previewUrl: file ? URL.createObjectURL(file) : "",
          progress: 0,
        },
      };
    });
  }

  function clearImages() {
    Object.values(images).forEach((image) => {
      if (image.previewUrl) URL.revokeObjectURL(image.previewUrl);
    });
    setImages(initialImages);
    setImageInputResetKey((key) => key + 1);
  }

  async function uploadSelectedImage(fieldName) {
    const selectedImage = images[fieldName];
    if (!selectedImage.file) return undefined;

    setImages((prev) => ({
      ...prev,
      [fieldName]: { ...prev[fieldName], progress: 1 },
    }));

    return uploadImage(selectedImage.file, (progress) => {
      setImages((prev) => ({
        ...prev,
        [fieldName]: { ...prev[fieldName], progress },
      }));
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!form.name.trim() || !form.description.trim() || !form.nif.trim()) {
      setError("Preenche nome, descrição e NIF.");
      return;
    }

    if (!/^\d{9}$/.test(form.nif.trim())) {
      setError("O NIF deve ter exatamente 9 dígitos.");
      return;
    }

    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      nif: form.nif.trim(),
    };

    try {
      setIsSubmitting(true);
      const [logoUrl, bannerUrl] = await Promise.all([
        uploadSelectedImage("logo"),
        uploadSelectedImage("banner"),
      ]);

      await createShop({
        ...payload,
        ...(logoUrl ? { logoUrl } : {}),
        ...(bannerUrl ? { bannerUrl } : {}),
      });
      setSuccess("Loja criada com sucesso.");
      setForm(initialForm);
      clearImages();
    } catch (submitError) {
      setError(
        submitError.response?.data?.message ||
          submitError.response?.data?.detail ||
          "Não foi possível criar a loja."
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="page-shell-narrow">
      <div className="flex items-center gap-3 mb-2">
        <div className="w-10 h-10 rounded bg-clover-50 border border-clover-100 flex items-center justify-center text-clover-600">
          <Store className="w-5 h-5" strokeWidth={1.7} />
        </div>
        <p className="eyebrow text-clover-700">Trevo · Comerciantes</p>
      </div>
      <h1 className="h-page">Criar loja</h1>
      <p className="mt-2 text-ink-500">Define a identidade da tua loja. Podes editar tudo mais tarde.</p>

      {error && <Alert variant="error" className="mt-6">{error}</Alert>}
      {success && <Alert variant="success" className="mt-6">{success}</Alert>}

      <form onSubmit={handleSubmit} className="mt-8 card card-pad p-6 flex flex-col gap-5">
        <div className="field">
          <label className="field-label" htmlFor="shop-name">Nome da loja</label>
          <input
            id="shop-name"
            className="input"
            type="text"
            name="name"
            placeholder="Ex.: Norden Lisboa"
            value={form.name}
            onChange={handleChange}
          />
        </div>

        <div className="field">
          <label className="field-label" htmlFor="shop-description">Descrição</label>
          <textarea
            id="shop-description"
            className="input min-h-[112px] resize-y"
            name="description"
            placeholder="Quem és, o que vendes, de onde és…"
            value={form.description}
            onChange={handleChange}
          />
        </div>

        <div className="field">
          <label className="field-label" htmlFor="shop-nif">NIF</label>
          <input
            id="shop-nif"
            className="input"
            type="text"
            name="nif"
            placeholder="9 dígitos"
            value={form.nif}
            onChange={handleChange}
            inputMode="numeric"
            maxLength={9}
          />
          <span className="field-help">Necessário para faturação. 9 dígitos.</span>
        </div>

        <div className="grid sm:grid-cols-2 gap-5">
          <ImageField
            id="shop-logo"
            label="Logo"
            help="Quadrado, mín. 256×256."
            name="logo"
            resetKey={imageInputResetKey}
            image={images.logo}
            onChange={handleImageChange}
            aspect="aspect-square"
          />
          <ImageField
            id="shop-banner"
            label="Banner"
            help="Largo, ~3:1."
            name="banner"
            resetKey={imageInputResetKey}
            image={images.banner}
            onChange={handleImageChange}
            aspect="aspect-[3/1]"
          />
        </div>

        <div className="flex justify-end pt-2">
          <button type="submit" disabled={isSubmitting} className="btn-primary">
            {isSubmitting ? "A criar..." : "Criar loja"}
          </button>
        </div>
      </form>
    </div>
  );
}

function ImageField({ id, label, help, name, resetKey, image, onChange, aspect }) {
  return (
    <div className="field">
      <label className="field-label" htmlFor={id}>{label}</label>
      <div className={"relative " + aspect + " rounded border border-dashed border-ink-300 bg-ink-50 overflow-hidden"}>
        {image.previewUrl ? (
          <img
            src={image.previewUrl}
            alt={`Pré-visualização do ${label.toLowerCase()}`}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="absolute inset-0 flex flex-col items-center justify-center text-ink-400">
            <Upload className="w-5 h-5" strokeWidth={1.7} />
            <span className="mt-1 text-xs">Carrega uma imagem</span>
          </div>
        )}
        <input
          key={`${name}-${resetKey}`}
          id={id}
          type="file"
          name={name}
          accept="image/jpeg,image/png,image/gif,image/webp"
          onChange={onChange}
          className="absolute inset-0 opacity-0 cursor-pointer"
          aria-label={label}
        />
      </div>
      <span className="field-help">{help}</span>
      {image.progress > 0 && image.progress < 100 && (
        <progress
          aria-label={`Progresso do upload do ${label.toLowerCase()}`}
          max="100"
          value={image.progress}
          className="w-full h-1.5 mt-1"
        />
      )}
    </div>
  );
}
