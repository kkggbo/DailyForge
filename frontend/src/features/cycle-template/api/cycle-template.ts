import { request } from "../../../shared/api/http";
import type {
  ActivateCycleTemplatePayload,
  ActivateCycleTemplateResponse,
  AiGenerateCycleTemplatePayload,
  CreateOrCopyCycleTemplateResponse,
  CurrentActiveTemplateResponse,
  CycleTemplateDetailResponse,
  DeleteCycleTemplateResponse,
  DraftTemplateListResponse,
  FormalTemplateListResponse,
  SaveCycleTemplatePayload
} from "../types/cycle-template";

export function getFormalTemplates(accessToken: string) {
  return request<FormalTemplateListResponse>("/cycle-templates/formal", {
    accessToken
  });
}

export function getDraftTemplates(accessToken: string) {
  return request<DraftTemplateListResponse>("/cycle-templates/drafts", {
    accessToken
  });
}

export function getCycleTemplateDetail(accessToken: string, templateId: number) {
  return request<CycleTemplateDetailResponse>(`/cycle-templates/${templateId}`, {
    accessToken
  });
}

export function createDraftTemplate(
  accessToken: string,
  payload: SaveCycleTemplatePayload
) {
  return request<CreateOrCopyCycleTemplateResponse>("/cycle-templates/drafts", {
    method: "POST",
    accessToken,
    body: payload
  });
}

export function generateDraftTemplateByAi(
  accessToken: string,
  payload: AiGenerateCycleTemplatePayload
) {
  return request<CreateOrCopyCycleTemplateResponse>(
    "/cycle-templates/drafts/ai-generate",
    {
      method: "POST",
      accessToken,
      body: payload
    }
  );
}

export function updateDraftTemplate(
  accessToken: string,
  templateId: number,
  payload: SaveCycleTemplatePayload
) {
  return request<void>(`/cycle-templates/drafts/${templateId}`, {
    method: "PUT",
    accessToken,
    body: payload
  });
}

export function updateFormalTemplate(
  accessToken: string,
  templateId: number,
  payload: SaveCycleTemplatePayload
) {
  return request<void>(`/cycle-templates/${templateId}`, {
    method: "PUT",
    accessToken,
    body: payload
  });
}

export function copyCycleTemplate(
  accessToken: string,
  templateId: number,
  templateName: string
) {
  return request<CreateOrCopyCycleTemplateResponse>(
    `/cycle-templates/${templateId}/copy`,
    {
      method: "POST",
      accessToken,
      body: { templateName }
    }
  );
}

export function activateCycleTemplate(
  accessToken: string,
  templateId: number,
  payload: ActivateCycleTemplatePayload = {}
) {
  return request<ActivateCycleTemplateResponse>(
    `/cycle-templates/${templateId}/activate`,
    {
      method: "POST",
      accessToken,
      body: payload
    }
  );
}

export function getCurrentActiveTemplate(accessToken: string) {
  return request<CurrentActiveTemplateResponse>("/cycle-templates/active/current", {
    accessToken
  });
}

export function deleteCycleTemplate(accessToken: string, templateId: number) {
  return request<DeleteCycleTemplateResponse>(`/cycle-templates/${templateId}`, {
    method: "DELETE",
    accessToken
  });
}
