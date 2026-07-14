type TemplateActionDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  isSubmitting: boolean;
  errorMessage?: string | null;
  danger?: boolean;
  onClose: () => void;
  onConfirm: () => void;
};

export function TemplateActionDialog({
  open,
  title,
  description,
  confirmLabel,
  isSubmitting,
  errorMessage,
  danger = false,
  onClose,
  onConfirm
}: TemplateActionDialogProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-4 backdrop-blur">
      <section className="w-full max-w-lg rounded-[28px] border border-white/10 bg-stone-950 p-6 shadow-2xl">
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        <p className="mt-3 leading-7 text-stone-300">{description}</p>
        {errorMessage ? (
          <div className="mt-4 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
            {errorMessage}
          </div>
        ) : null}
        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={onClose}
            className="rounded-full border border-white/10 px-5 py-3 text-sm text-stone-100 transition hover:bg-white/10 disabled:opacity-50"
          >
            取消
          </button>
          <button
            type="button"
            disabled={isSubmitting}
            onClick={onConfirm}
            className={[
              "rounded-full px-5 py-3 text-sm font-semibold transition disabled:opacity-50",
              danger
                ? "bg-rose-400 text-white hover:bg-rose-300"
                : "bg-amber-400 text-stone-950 hover:bg-amber-300"
            ].join(" ")}
          >
            {isSubmitting ? "处理中..." : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
