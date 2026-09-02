import type { ActivityLevel } from "../../profile/types/profile";

export type MealType = "breakfast" | "lunch" | "dinner" | "snack";

export type FoodCategory =
  | "staple"
  | "meat_egg"
  | "vegetable"
  | "fruit"
  | "dairy"
  | "nut_bean"
  | "drink"
  | "other";

export type FoodSource = "system" | "user";

export type DietTargetBasis = "auto" | "custom";

export type NutrientValues = {
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
};

export type DietTarget = {
  basis: DietTargetBasis | null;
  caloriesKcal: number | null;
  proteinG: number | null;
  carbsG: number | null;
  fatG: number | null;
};

export type DietTargetResponse = DietTarget & {
  missingFields: string[];
};

export type MealLogItem = NutrientValues & {
  logId: number;
  foodId: number;
  foodName: string;
  grams: number;
};

export type DaySummary = {
  date: string;
  target: DietTarget | null;
  meals: Record<MealType, MealLogItem[]>;
  totals: NutrientValues;
  progress: {
    caloriesPct: number;
    proteinPct: number;
    carbsPct: number;
    fatPct: number;
  } | null;
};

export type FoodItem = {
  foodId: number;
  name: string;
  category: FoodCategory | null;
  source: FoodSource;
  sourceLabel: string;
  ownerNickname: string | null;
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  favorited: boolean;
};

export type FoodListResponse = {
  foods: FoodItem[];
};

export type FoodFilter = "all" | "recent" | "frequent" | "favorite";

export type FoodQuery = {
  keyword?: string;
  filter?: FoodFilter;
};

export type CreateMealLogPayload = {
  date: string;
  mealType: MealType;
  foodId: number;
  grams: number;
};

export type UpdateMealLogPayload = {
  grams: number;
  mealType: MealType;
  date: string;
};

export type UploadFoodPayload = {
  name: string;
  category?: FoodCategory | null;
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
};

// clear 时只携带 clear:true；自定义覆盖时携带四营养字段
export type SetDietTargetPayload =
  | {
      clear: true;
    }
  | {
      clear?: false;
      caloriesKcal: number;
      proteinG: number;
      carbsG: number;
      fatG: number;
    };

export type DietStats = {
  dailyCalories: Array<{ date: string; caloriesKcal: number }>;
  macroShare: { proteinPct: number; carbsPct: number; fatPct: number };
  weeklyAverage: Array<{
    weekStart: string;
    caloriesKcal: number;
    proteinG: number;
    carbsG: number;
    fatG: number;
  }>;
  goalAdherence: {
    daysWithinTarget: number;
    daysLogged: number;
    ratePct: number;
  } | null;
};

export type DietTimeRangeQuery = {
  from?: string;
  to?: string;
};

export type { ActivityLevel };
