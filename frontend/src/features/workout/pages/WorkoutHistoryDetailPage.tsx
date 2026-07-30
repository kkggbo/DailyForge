import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getSession } from "../api/workout";
import { SessionReadOnly } from "../components/WorkoutPanel";
import { errorMessage, formatTime, sessionLabel } from "../lib/workout";
import type { SessionDetail } from "../types/workout";

const backClass = "inline-flex rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12";
export function WorkoutHistoryDetailPage() {
  const { accessToken } = useAuth();
  const { sessionId: rawSessionId } = useParams();
  const sessionId = Number(rawSessionId);
  const [detail, setDetail] = useState<SessionDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => { if (!accessToken || !Number.isSafeInteger(sessionId) || sessionId <= 0) { setError("训练记录 ID 无效。"); setIsLoading(false); return; } const token = accessToken; let cancelled = false; async function load() { setIsLoading(true); setError(null); try { const response = await getSession(token, sessionId); if (!cancelled) setDetail(response); } catch (loadError) { if (!cancelled) setError(errorMessage(loadError, "加载训练记录失败，请稍后重试。")); } finally { if (!cancelled) setIsLoading(false); } } void load(); return () => { cancelled = true; }; }, [accessToken, sessionId]);
  if (isLoading) return <div className="flex min-h-[40vh] items-center justify-center"><div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">正在加载训练记录...</div></div>;
  if (!detail) return <section className="space-y-5"><div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">{error ?? "训练记录不存在。"}</div><Link to="/workout" className={backClass}>返回训练工作台</Link></section>;
  return <section className="space-y-6"><Link to="/workout" className={backClass}>返回训练工作台</Link><header className="rounded-[32px] border border-white/10 bg-white/6 p-6"><p className="text-sm uppercase tracking-[0.24em] text-amber-300">Workout History</p><div className="mt-3 flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between"><div><h1 className="text-3xl font-semibold text-white">{detail.templateName} · Day {detail.dayIndex}</h1><p className="mt-2 text-stone-300">{detail.dayName}</p></div><div className="text-sm text-stone-300"><p>{sessionLabel(detail.sessionStatus)}</p><p className="mt-1">{formatTime(detail.completedAt ?? detail.startedAt)}</p></div></div></header><SessionReadOnly session={detail} /></section>;
}