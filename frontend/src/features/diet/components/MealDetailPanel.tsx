import { MealRowItem } from "./MealRowItem";
import { getMealTypeLabel } from "../lib/diet-enums";
import type { MealLogItem, MealType } from "../types/diet";

type MealDetailPanelProps = {
  mealType: MealType;
  items: MealLogItem[];
  onUpdate: (mealType: MealType, logId: number, grams: number) => Promise<void>;
  onDelete: (logId: number) => Promise<void>;
};

export function MealDetailPanel({
  mealType,
  items,
  onUpdate,
  onDelete
}: MealDetailPanelProps) {
  return (
    <section className="rounded-[32px] border border-white/10 bg-black/20 p-5 backdrop-blur">
      <h3 className="text-lg font-semibold text-white">
        {getMealTypeLabel(mealType)}明细
      </h3>
      {items.length === 0 ? (
        <p className="mt-4 text-sm text-stone-400">暂无记录。</p>
      ) : (
        <div className="mt-4 space-y-2">
          {items.map((item) => (
            <MealRowItem
              key={item.logId}
              item={item}
              onUpdate={(logId, grams) => onUpdate(mealType, logId, grams)}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </section>
  );
}
