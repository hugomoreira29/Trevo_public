import { Loader2 } from "lucide-react";

/**
 * Small inline loading state — shows a spinner + optional label.
 * Use inside pages that are awaiting data.
 */
export default function LoadingState({ label = "A carregar..." }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-ink-500">
      <Loader2 className="w-5 h-5 animate-spin" strokeWidth={1.8} />
      <span className="text-sm">{label}</span>
    </div>
  );
}
