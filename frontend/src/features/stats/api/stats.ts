import { request } from "../../../shared/api/http";
import type {
  BodyMetricKey,
  BodyMetricsSeries,
  ExerciseProgression,
  StatsSummary,
  StatsTimeRangeQuery
} from "../types/stats";

function toStatsQuery(query: StatsTimeRangeQuery): { from?: string; to?: string } {
  return {
    ...(query.from ? { from: query.from } : {}),
    ...(query.to ? { to: query.to } : {})
  };
}

export function getStatsSummary(
  accessToken: string,
  query: StatsTimeRangeQuery = {}
) {
  return request<StatsSummary>("/stats/summary", {
    accessToken,
    query: toStatsQuery(query)
  });
}

export function getExerciseProgression(
  accessToken: string,
  exerciseId: number,
  query: StatsTimeRangeQuery = {}
) {
  return request<ExerciseProgression>(`/stats/exercise/${exerciseId}`, {
    accessToken,
    query: toStatsQuery(query)
  });
}

export function getBodyMetrics(
  accessToken: string,
  metric: BodyMetricKey,
  query: StatsTimeRangeQuery = {}
) {
  return request<BodyMetricsSeries>("/stats/body-metrics", {
    accessToken,
    query: {
      metric,
      ...toStatsQuery(query)
    }
  });
}
