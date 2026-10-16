import { useTranslation } from "react-i18next";
import { ShieldCheck } from "lucide-react";

export default function Admin() {
  const { t } = useTranslation("common");

  return (
    <div className="page-shell-narrow">
      <div className="flex items-center gap-3 mb-2">
        <div className="w-10 h-10 rounded bg-clover-50 border border-clover-100 flex items-center justify-center text-clover-600">
          <ShieldCheck className="w-5 h-5" strokeWidth={1.7} />
        </div>
        <p className="eyebrow">Trevo · Operações</p>
      </div>
      <h1 className="h-page">{t("admin")}</h1>
      <p className="mt-3 text-ink-500">
        Painel administrativo. As ferramentas vão ser ligadas aqui.
      </p>
    </div>
  );
}
