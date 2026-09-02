import type {
  FoodCategory,
  FoodFilter,
  FoodSource,
  MealType
} from "../types/diet";

export const mealTypeOptions: Array<{ value: MealType; label: string }> = [
  { value: "breakfast", label: "早餐" },
  { value: "lunch", label: "午餐" },
  { value: "dinner", label: "晚餐" },
  { value: "snack", label: "加餐" }
];

export const mealTypeOrder: MealType[] = [
  "breakfast",
  "lunch",
  "dinner",
  "snack"
];

export const foodCategoryOptions: Array<{
  value: FoodCategory;
  label: string;
}> = [
  { value: "staple", label: "主食" },
  { value: "meat_egg", label: "肉蛋水产" },
  { value: "vegetable", label: "蔬菜" },
  { value: "fruit", label: "水果" },
  { value: "dairy", label: "奶制品" },
  { value: "nut_bean", label: "坚果豆类" },
  { value: "drink", label: "饮品" },
  { value: "other", label: "其它" }
];

export const foodSourceLabels: Record<FoodSource, string> = {
  system: "官方",
  user: "用户"
};

export const foodFilterOptions: Array<{ value: FoodFilter; label: string }> = [
  { value: "all", label: "全部" },
  { value: "recent", label: "最近使用" },
  { value: "frequent", label: "最常食用" },
  { value: "favorite", label: "我的收藏" }
];

export function getMealTypeLabel(type: MealType): string {
  return mealTypeOptions.find((option) => option.value === type)?.label ?? type;
}

export function getFoodCategoryLabel(category: FoodCategory | null): string {
  if (!category) {
    return "";
  }
  return (
    foodCategoryOptions.find((option) => option.value === category)?.label ??
    category
  );
}

export function getFoodSourceLabel(source: FoodSource): string {
  return foodSourceLabels[source] ?? source;
}

export const dietMissingFieldLabels: Record<string, string> = {
  gender: "性别",
  birthDate: "出生日期",
  heightCm: "身高",
  currentWeightKg: "体重",
  goalType: "训练目标",
  activityLevel: "活动量"
};

export function getDietMissingFieldLabel(field: string): string {
  return dietMissingFieldLabels[field] ?? field;
}
