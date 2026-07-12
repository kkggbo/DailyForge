import { request } from "../../../shared/api/http";
import type {
  BodyMetricPageQuery,
  BodyMetricSnapshotResponse,
  BodyMetricsPageResponse,
  CreateBodyMetricPayload,
  DeleteLatestBodyMetricResponse,
  ProfileBasicResponse,
  ProfileCompletionSummaryResponse,
  UpdateProfileBasicPayload,
  BodyMetricLogItemResponse
} from "../types/profile";

export async function getBasicProfile(accessToken: string) {
  return request<ProfileBasicResponse>("/profile/basic", {
    method: "GET",
    accessToken
  });
}

export async function updateBasicProfile(
  accessToken: string,
  payload: UpdateProfileBasicPayload
) {
  return request<ProfileBasicResponse>("/profile/basic", {
    method: "PUT",
    accessToken,
    body: payload
  });
}

export async function getCurrentBodyMetricSnapshot(accessToken: string) {
  return request<BodyMetricSnapshotResponse>("/profile/body-metrics/current", {
    method: "GET",
    accessToken
  });
}

export async function getBodyMetricsPage(
  accessToken: string,
  query: BodyMetricPageQuery = {}
) {
  return request<BodyMetricsPageResponse>("/profile/body-metrics", {
    method: "GET",
    accessToken,
    query: {
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 20
    }
  });
}

export async function createBodyMetric(
  accessToken: string,
  payload: CreateBodyMetricPayload
) {
  return request<BodyMetricLogItemResponse>("/profile/body-metrics", {
    method: "POST",
    accessToken,
    body: payload
  });
}

export async function deleteLatestBodyMetric(accessToken: string) {
  return request<DeleteLatestBodyMetricResponse>("/profile/body-metrics/latest", {
    method: "DELETE",
    accessToken
  });
}

export async function getProfileCompletionSummary(accessToken: string) {
  return request<ProfileCompletionSummaryResponse>("/profile/completion-summary", {
    method: "GET",
    accessToken
  });
}
