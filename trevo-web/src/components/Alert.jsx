import { AlertCircle, CheckCircle2, Info, AlertTriangle } from "lucide-react";

const variants = {
  error:   { Icon: AlertCircle,     cls: "bg-red-50 text-red-700 border-red-200" },
  success: { Icon: CheckCircle2,    cls: "bg-green-50 text-green-700 border-green-200" },
  warning: { Icon: AlertTriangle,   cls: "bg-amber-50 text-amber-800 border-amber-200" },
  info:    { Icon: Info,            cls: "bg-blue-50 text-blue-700 border-blue-200" },
};

/**
 * Inline status banner — error, success, warning, info.
 * Used for form errors, success toasts, etc.
 */
export default function Alert({ variant = "info", children, className = "" }) {
  const { Icon, cls } = variants[variant] || variants.info;
  return (
    <div
      role={variant === "error" ? "alert" : "status"}
      className={`flex items-start gap-2.5 px-3.5 py-2.5 border rounded text-sm ${cls} ${className}`}
    >
      <Icon className="w-4 h-4 mt-0.5 shrink-0" strokeWidth={1.8} />
      <div className="flex-1">{children}</div>
    </div>
  );
}
