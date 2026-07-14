type DeleteLatestMetricDialogProps = {
  open: boolean;
  isSubmitting: boolean;
  errorMessage?: string | null;
  onClose: () => void;
  onConfirm: () => void;
};

export function DeleteLatestMetricDialog({
  open,
  isSubmitting,
  errorMessage,
  onClose,
  onConfirm
}: DeleteLatestMetricDialogProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-6">
      <div className="w-full max-w-lg rounded-[32px] border border-white/10 bg-stone-950 p-6 shadow-[0_24px_80px_rgba(0,0,0,0.55)]">
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
          Delete Latest Record
        </p>
        <h3 className="mt-4 text-2xl font-semibold text-white">
          确认删除最新一条身体指标记录？
        </h3>
        <p className="mt-3 leading-7 text-stone-300">
          只允许删除最新一条记录。删除后系统会重新计算当前身体状态快照，请确认这是你想执行的操作。
        </p>

        {errorMessage ? (
          <div className="mt-4 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
            {errorMessage}
          </div>
        ) : null}

        <div className="mt-6 flex flex-wrap justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8 disabled:cursor-not-allowed disabled:opacity-50"
          >
            取消
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isSubmitting}
            className="rounded-full bg-rose-400 px-4 py-2 text-sm font-medium text-stone-950 transition hover:bg-rose-300 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSubmitting ? "删除中..." : "确认删除"}
          </button>
        </div>
      </div>
    </div>
  );
}
