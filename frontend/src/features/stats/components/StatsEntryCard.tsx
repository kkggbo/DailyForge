import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getStatsSummary } from "../api/stats";
import {
  formatDistanceKm,
  formatNumber,
  formatVolumeKg,
  getStatsErrorMessage
} from "../lib/stats-formatters";
import type { OverallStats } from "../types/stats";

export function StatsEntryCard() {
  const { accessToken } = useAuth();
  const [overall, setOverall] = useState<OverallStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) {
      setIsLoading(false);
      return;
    }

    const token = accessToken;
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      try {
        const summary = await getStatsSummary(token);
        if (!cancelled) {
          setOverall(summary.overall);
        }
      } catch {
        // 失败时折叠为纯入口，不阻塞首页
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  return (
    <Link
      to="/stats"
      className="block rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur transition hover:bg-white/8"
    >
      <div className="flex items-center justify-between">
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
          Statistics
        </p>
        <span className="text-sm text-amber-300">查看 →</span>
      </div>
      <h2 className="mt-3 text-xl font-semibold text-white">训练统计</h2>

      {isLoading ? (
        <p className="mt-4 text-sm text-stone-400">正在加载统计...</p>
      ) : overall ? (
        <div className="mt-4 space-y-1.5">
          <p className="text-sm text-stone-200">
            累计训练 <span className="font-semibold text-white">{formatNumber(overall.sessionCount)}</span> 场
          </p>
          {overall.totalVolumeKg > 0 ? (
            <p className="text-sm text-stone-300">
              总容量 {formatVolumeKg(overall.totalVolumeKg)}
            </p>
          ) : null}
          {overall.totalDistanceKm > 0 ? (
            <p className="text-sm text-stone-300">
              总里程 {formatDistanceKm(overall.totalDistanceKm)}
            </p>
          ) : null}
          {overall.overviewCopy ? (
            <p className="mt-2 text-xs leading-5 text-stone-400">
              {overall.overviewCopy}
            </p>
          ) : null}
        </div>
      ) : (
        <p className="mt-4 text-sm text-stone-400">点击查看训练统计</p>
      )}
    </Link>
  );
}
