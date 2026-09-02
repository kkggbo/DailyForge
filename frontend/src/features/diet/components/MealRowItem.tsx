import { useState } from "react";
import type { MealLogItem } from "../types/diet";

type MealRowItemProps = {
  item: MealLogItem;
  onUpdate: (logId: number, grams: number) => Promise<void>;
  onDelete: (logId: number) => Promise<void>;
};

export function MealRowItem({
  item,
  onUpdate,
  onDelete
}: MealRowItemProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [gramsText, setGramsText] = useState(String(item.grams));
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    const grams = Number(gramsText);
    if (!Number.isFinite(grams) || grams <= 0) {
      setError("请输入有效的克数。");
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      await onUpdate(item.logId, grams);
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存失败。");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 px-4 py-3">
      {isEditing ? (
        <div className="flex flex-wrap items-center gap-2">
          <input
            type="number"
            min="1"
            value={gramsText}
            onChange={(event) => setGramsText(event.target.value)}
            className="w-28 rounded-2xl border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
          />
          <button
            type="button"
            disabled={isSaving}
            onClick={() => void handleSave()}
            className="rounded-full bg-amber-400 px-3 py-1.5 text-xs font-semibold text-stone-950"
          >
            {isSaving ? "保存中..." : "保存"}
          </button>
          <button
            type="button"
            onClick={() => setIsEditing(false)}
            className="rounded-full border border-white/10 px-3 py-1.5 text-xs text-stone-200"
          >
            取消
          </button>
          {error ? <span className="text-xs text-rose-300">{error}</span> : null}
        </div>
      ) : (
        <div className="flex items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="truncate font-medium text-white">{item.foodName}</p>
            <p className="text-xs text-stone-400">
              {item.grams} g · {Math.round(item.caloriesKcal)} 千卡 · 蛋白{" "}
              {item.proteinG}g · 碳水 {item.carbsG}g · 脂肪 {item.fatG}g
            </p>
          </div>
          <div className="flex shrink-0 gap-2">
            <button
              type="button"
              onClick={() => {
                setGramsText(String(item.grams));
                setIsEditing(true);
              }}
              className="rounded-full border border-white/10 px-3 py-1.5 text-xs text-stone-200 hover:bg-white/10"
            >
              编辑
            </button>
            <button
              type="button"
              onClick={() => void onDelete(item.logId)}
              className="rounded-full border border-rose-400/20 px-3 py-1.5 text-xs text-rose-200 hover:bg-rose-400/10"
            >
              删除
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
