import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { Lock, ArrowLeft } from "lucide-react";

export default function Unauthorized() {
  const { t } = useTranslation("common");

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center px-6 py-12">
      <div className="text-center max-w-md">
        <div className="w-12 h-12 mx-auto rounded bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-700 mb-5">
          <Lock className="w-5 h-5" strokeWidth={1.7} />
        </div>
        <h1 className="text-3xl font-bold tracking-tighter">{t("unauthorized")}</h1>
        <p className="mt-3 text-ink-500">
          Não tens permissão para aceder a esta página.
        </p>
        <Link to="/" className="btn-secondary mt-6 inline-flex">
          <ArrowLeft className="w-4 h-4" strokeWidth={1.7} />
          Voltar ao início
        </Link>
      </div>
    </div>
  );
}
