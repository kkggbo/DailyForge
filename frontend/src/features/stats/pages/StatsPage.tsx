import { useEffect, useState } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getBodyMetrics, getExerciseProgression, getStatsSummary } from "../api/stats";
import { BodyMetricsChart } from "../components/BodyMetricsChart";
import { ExerciseStatCard } from "../components/ExerciseStatCard";
import { StatsFilterBar } from "../components/StatsFilterBar";
import { StatsEmptyState } from "../components/StatsEmptyState";
import { SummaryHero } from "../components/SummaryHero";
import { getStatsErrorMessage } from "../lib/stats-formatters";
import { filterExercises } from "../lib/stats-mappers";
import {
  bodyMetricOptions,
  buildTimeRangeQuery,
  defaultBodyMetricRange,
  defaultTrainingRange,
  type TimeRangeSelection
} from "../lib/stats-options";
import type {
  BodyMetricKey,
  BodyMetricsSeries,
  ExerciseFilter,
  ExerciseProgression,
  StatsSummary
} from "../types/stats";

export function StatsPage() {
  const { accessToken } = useAuth();

  // 训练区
  const [summary, setSummary] = useState<StatsSummary | null>(null);
  const [isLoadingSummary, setIsLoadingSummary] = useState(true);
  const [summaryError, setSummaryError] = useState<string | null>(null);
  const [trainingRange, setTrainingRange] = useState<TimeRangeSelection>(
    defaultTrainingRange
  );
  const [filter, setFilter] = useState<ExerciseFilter>("all");
  const [search, setSearch] = useState("");

  // 动作进阶展开
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [progressionMap, setProgressionMap] = useState<
    Record<number, ExerciseProgression>
  >({});
  const [progressionLoadingId, setProgressionLoadingId] = useState<number | null>(
    null
  );
  const [progressionErrorMap, setProgressionErrorMap] = useState<
    Record<number, string>
  >({});

  // 身体指标区（时间范围独立，多指标）
  const [selectedMetrics, setSelectedMetrics] = useState<Set<BodyMetricKey>>(
    () => new Set<BodyMetricKey>(["weight_kg"])
  );
  const [metricRange, setMetricRange] = useState<TimeRangeSelection>(
    defaultBodyMetricRange
  );
  const [metricsSeriesMap, setMetricsSeriesMap] = useState<
    Partial<Record<BodyMetricKey, BodyMetricsSeries>>
  >({});
  const [isLoadingMetrics, setIsLoadingMetrics] = useState(true);
  const [metricsError, setMetricsError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    let cancelled = false;

    async function load() {
      setIsLoadingSummary(true);
      setSummaryError(null);
      setExpandedId(null);
      setProgressionMap({});
      setProgressionErrorMap({});

      try {
        const nextSummary = await getStatsSummary(
          token,
          buildTimeRangeQuery(trainingRange)
        );
        if (!cancelled) {
          setSummary(nextSummary);
        }
      } catch (error) {
        if (!cancelled) {
          setSummaryError(
            getStatsErrorMessage(error, "加载统计失败，请稍后再试。")
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoadingSummary(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken, trainingRange]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    let cancelled = false;

    async function load() {
      setIsLoadingMetrics(true);
      setMetricsError(null);

      const keys = Array.from(selectedMetrics);
      const settled = await Promise.allSettled(
        keys.map(async (key) => {
          const series = await getBodyMetrics(
            token,
            key,
            buildTimeRangeQuery(metricRange)
          );
          return { key, series } as const;
        })
      );

      if (cancelled) {
        return;
      }

      const nextMap: Partial<Record<BodyMetricKey, BodyMetricsSeries>> = {};
      let firstError: string | null = null;
      for (const result of settled) {
        if (result.status === "fulfilled") {
          nextMap[result.value.key] = result.value.series;
        } else if (firstError === null) {
          firstError = getStatsErrorMessage(
            result.reason,
            "加载身体指标失败，请稍后再试。"
          );
        }
      }

      setMetricsSeriesMap(nextMap);
      setMetricsError(firstError);
      setIsLoadingMetrics(false);
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken, metricRange, selectedMetrics]);

  function toggleMetric(metric: BodyMetricKey) {
    setSelectedMetrics((previous) => {
      const next = new Set(previous);
      if (next.has(metric)) {
        next.delete(metric);
      } else {
        next.add(metric);
      }
      return next;
    });
  }

  function selectAllMetrics() {
    setSelectedMetrics(new Set(bodyMetricOptions.map((option) => option.value)));
  }

  function clearAllMetrics() {
    setSelectedMetrics(new Set<BodyMetricKey>());
  }

  function loadProgression(exerciseId: number) {
    if (!accessToken) {
      return;
    }

    setProgressionLoadingId(exerciseId);
    setProgressionErrorMap((previous) => ({
      ...previous,
      [exerciseId]: ""
    }));

    getExerciseProgression(
      accessToken,
      exerciseId,
      buildTimeRangeQuery(trainingRange)
    )
      .then((progression) => {
        setProgressionMap((previous) => ({
          ...previous,
          [exerciseId]: progression
        }));
      })
      .catch((error) => {
        setProgressionErrorMap((previous) => ({
          ...previous,
          [exerciseId]: getStatsErrorMessage(error, "加载进阶数据失败。")
        }));
      })
      .finally(() => {
        setProgressionLoadingId(null);
      });
  }

  function toggleExpand(exerciseId: number) {
    if (expandedId === exerciseId) {
      setExpandedId(null);
      return;
    }

    setExpandedId(exerciseId);
    if (!progressionMap[exerciseId] && !progressionErrorMap[exerciseId]) {
      loadProgression(exerciseId);
    }
  }

  const visibleExercises = filterExercises(
    summary?.exercises ?? [],
    filter,
    search
  );

  return (
    <section className="space-y-8">
      <SummaryHero
        overall={summary?.overall ?? null}
        isLoading={isLoadingSummary}
        error={summaryError}
      />

      <BodyMetricsChart
        seriesMap={metricsSeriesMap}
        isLoading={isLoadingMetrics}
        error={metricsError}
        selectedMetrics={selectedMetrics}
        range={metricRange}
        onToggleMetric={toggleMetric}
        onSelectAll={selectAllMetrics}
        onClearAll={clearAllMetrics}
        onRangeChange={setMetricRange}
      />

      <StatsFilterBar
        range={trainingRange}
        onRangeChange={setTrainingRange}
        filter={filter}
        onFilterChange={setFilter}
        search={search}
        onSearchChange={setSearch}
        resultCount={visibleExercises.length}
      />

      <section>
        <h2 className="text-2xl font-semibold text-white">动作统计</h2>
        {isLoadingSummary ? (
          <div className="mt-4 flex items-center justify-center py-10">
            <span className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
              正在加载动作统计...
            </span>
          </div>
        ) : visibleExercises.length === 0 ? (
          <div className="mt-4">
            <StatsEmptyState
              title={search ? "没有匹配的动作" : "还没有动作统计"}
              description={
                search
                  ? "换个名称搜索试试。"
                  : "完成一次训练打卡后，这里会展示你的动作统计。"
              }
            />
          </div>
        ) : (
          <div className="mt-4 space-y-4">
            {visibleExercises.map((exercise) => (
              <ExerciseStatCard
                key={exercise.exerciseId}
                exercise={exercise}
                expanded={expandedId === exercise.exerciseId}
                onToggleExpand={() => toggleExpand(exercise.exerciseId)}
                progression={progressionMap[exercise.exerciseId] ?? null}
                isProgressionLoading={
                  progressionLoadingId === exercise.exerciseId
                }
                progressionError={
                  progressionErrorMap[exercise.exerciseId] || null
                }
                onRetryProgression={() => loadProgression(exercise.exerciseId)}
              />
            ))}
          </div>
        )}
      </section>
    </section>
  );
}
