import { request } from "../../../shared/api/http";
import type {
  CreateMealLogPayload,
  DaySummary,
  DietStats,
  DietTargetResponse,
  FoodItem,
  FoodListResponse,
  FoodQuery,
  MealLogItem,
  SetDietTargetPayload,
  UpdateMealLogPayload,
  UploadFoodPayload
} from "../types/diet";

export function getDaySummary(accessToken: string, date: string) {
  return request<DaySummary>("/diet/summary", {
    accessToken,
    query: { date }
  });
}

export function createMealLog(
  accessToken: string,
  payload: CreateMealLogPayload
) {
  return request<MealLogItem>("/diet/logs", {
    method: "POST",
    accessToken,
    body: payload
  });
}

export function updateMealLog(
  accessToken: string,
  logId: number,
  payload: UpdateMealLogPayload
) {
  return request<MealLogItem>(`/diet/logs/${logId}`, {
    method: "PUT",
    accessToken,
    body: payload
  });
}

export function deleteMealLog(accessToken: string, logId: number) {
  return request<void>(`/diet/logs/${logId}`, {
    method: "DELETE",
    accessToken
  });
}

export function searchFoods(accessToken: string, query: FoodQuery = {}) {
  return request<FoodListResponse>("/diet/foods", {
    accessToken,
    query: {
      ...(query.keyword ? { keyword: query.keyword } : {}),
      filter: query.filter ?? "all",
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 20
    }
  });
}

export function getFoodDetail(accessToken: string, foodId: number) {
  return request<FoodItem>(`/diet/foods/${foodId}`, {
    accessToken
  });
}

export function uploadFood(accessToken: string, payload: UploadFoodPayload) {
  return request<FoodItem>("/diet/foods", {
    method: "POST",
    accessToken,
    body: payload
  });
}

export function addFavorite(accessToken: string, foodId: number) {
  return request<void>(`/diet/favorites/${foodId}`, {
    method: "POST",
    accessToken
  });
}

export function removeFavorite(accessToken: string, foodId: number) {
  return request<void>(`/diet/favorites/${foodId}`, {
    method: "DELETE",
    accessToken
  });
}

export function getDietTargets(accessToken: string) {
  return request<DietTargetResponse>("/diet/targets", {
    accessToken
  });
}

export function setDietTarget(accessToken: string, payload: SetDietTargetPayload) {
  return request<DietTargetResponse>("/diet/targets", {
    method: "PUT",
    accessToken,
    body: payload
  });
}

export function getDietStats(
  accessToken: string,
  query: { from?: string; to?: string } = {}
) {
  return request<DietStats>("/diet/stats", {
    accessToken,
    query: {
      ...(query.from ? { from: query.from } : {}),
      ...(query.to ? { to: query.to } : {})
    }
  });
}
