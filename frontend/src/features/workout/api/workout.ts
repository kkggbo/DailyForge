import { request } from "../../../shared/api/http";
import type { DayDetail, RecentWorkouts, RestartResponse, SavePayload, SessionDetail, Workspace } from "../types/workout";

export const getWorkspace = (accessToken: string) => request<Workspace>("/workouts/context", { accessToken });
export const initializeCurrentDay = (accessToken: string) => request<{ sessionCreated: boolean; day: DayDetail }>("/workouts/current-day/session", { method: "POST", accessToken });
export const getDay = (accessToken: string, dayIndex: number) => request<DayDetail>(`/workouts/days/${dayIndex}`, { accessToken });
export const saveSession = (accessToken: string, sessionId: number, payload: SavePayload) => request<{ sessionId: number; sessionStatus: "in_progress"; savedAt: string }>(`/workouts/sessions/${sessionId}`, { method: "PUT", accessToken, body: payload });
export const completeSession = (accessToken: string, sessionId: number, payload: SavePayload) => request<{ sessionId: number; sessionStatus: "completed"; completedAt: string; completedDayIndex: number; cycleRunId: number; cycleRunStatus: "active" | "completed" | "cancelled"; nextCurrentDayIndex: number | null; completedDay: DayDetail }>(`/workouts/sessions/${sessionId}/complete`, { method: "POST", accessToken, body: payload });
export const getSession = (accessToken: string, sessionId: number) => request<SessionDetail>(`/workouts/sessions/${sessionId}`, { accessToken });
export const getRecent = (accessToken: string) => request<RecentWorkouts>("/workouts/recent", { accessToken, query: { page: 1, pageSize: 10 } });
export const restartCycle = (accessToken: string) => request<RestartResponse>("/workouts/cycles/current/restart", { method: "POST", accessToken });
export const requestAiAnalysis = (accessToken: string) => request<void>("/workouts/cycles/current/ai-analysis", { method: "POST", accessToken });