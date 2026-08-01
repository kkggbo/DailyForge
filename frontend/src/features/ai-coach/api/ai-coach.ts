import { request } from "../../../shared/api/http";
import type {
  AiCoachCapabilities,
  AiTaskAcceptedResponse,
  CreateCycleSummaryPayload,
  CreateTemplateGenerationPayload,
  CycleSummaryTaskResponse,
  TemplateGenerationTaskResponse
} from "../types/ai-coach";

export function getAiCoachCapabilities(accessToken: string) {
  return request<AiCoachCapabilities>("/ai-coach/capabilities", {
    accessToken
  });
}

export function createTemplateGenerationTask(
  accessToken: string,
  payload: CreateTemplateGenerationPayload
) {
  return request<AiTaskAcceptedResponse<"template_generation">>(
    "/ai-coach/template-generations",
    {
      method: "POST",
      accessToken,
      body: payload
    }
  );
}

export function getTemplateGenerationTask(
  accessToken: string,
  taskId: number
) {
  return request<TemplateGenerationTaskResponse>(
    `/ai-coach/template-generations/${taskId}`,
    {
      accessToken
    }
  );
}

export function createCycleSummaryTask(
  accessToken: string,
  payload: CreateCycleSummaryPayload
) {
  return request<AiTaskAcceptedResponse<"cycle_summary">>(
    "/ai-coach/cycle-summaries",
    {
      method: "POST",
      accessToken,
      body: payload
    }
  );
}

export function getCycleSummaryTask(accessToken: string, taskId: number) {
  return request<CycleSummaryTaskResponse>(
    `/ai-coach/cycle-summaries/${taskId}`,
    {
      accessToken
    }
  );
}
