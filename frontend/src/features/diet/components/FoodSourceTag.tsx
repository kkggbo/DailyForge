import type { FoodItem } from "../types/diet";

export function FoodSourceTag({ food }: { food: FoodItem }) {
  const isUser = food.source === "user";
  return (
    <span
      className={[
        "shrink-0 rounded-full px-3 py-1 text-xs",
        isUser
          ? "border border-sky-300/20 bg-sky-300/10 text-sky-100"
          : "border border-white/10 bg-white/8 text-stone-300"
      ].join(" ")}
    >
      {food.sourceLabel}
      {isUser && food.ownerNickname ? ` · ${food.ownerNickname}` : ""}
    </span>
  );
}
