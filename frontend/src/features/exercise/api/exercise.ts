import { request } from "../../../shared/api/http";
import type {
  ExerciseFilterOptionsResponse,
  SystemExerciseDetailResponse,
  SystemExerciseSearchQuery,
  SystemExerciseSearchResponse
} from "../types/exercise";

export function getSystemExerciseFilterOptions(accessToken: string) {
  return request<ExerciseFilterOptionsResponse>("/exercises/system/filter-options", {
    accessToken
  });
}

export function searchSystemExercises(
  accessToken: string,
  query: SystemExerciseSearchQuery = {}
) {
  return request<SystemExerciseSearchResponse>("/exercises/system", {
    accessToken,
    query: {
      keyword: query.keyword?.trim() || undefined,
      categoryCode: query.categoryCode,
      exerciseType: query.exerciseType,
      movementType: query.movementType,
      structureType: query.structureType,
      sceneType: query.sceneType,
      muscleId: query.muscleId,
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 20
    }
  });
}

export function getSystemExerciseDetail(accessToken: string, exerciseId: number) {
  return request<SystemExerciseDetailResponse>(`/exercises/system/${exerciseId}`, {
    accessToken
  });
}
