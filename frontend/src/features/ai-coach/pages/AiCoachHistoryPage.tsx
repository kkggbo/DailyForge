import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  getCycleSummaryHistory,
  getTemplateGenerationHistory
} from "../api/ai-coach";
import { AiTaskHistoryList } from "../components/AiTaskHistoryList";
import { getAiCoachErrorMessage } from "../lib/ai-coach-enums";
import {
  formatGoalType,
  formatSceneType
} from "../lib/ai-coach-formatters";
import type {
  CycleSummaryHistoryItem,
  CycleSummaryHistoryPage,
  TemplateGenerationHistoryItem,
  TemplateGenerationHistoryPage
} from "../types/ai-coach";

type HistoryTab = "template-generations" | "cycle-summaries";

const DEFAULT_PAGE_SIZE = 10;
const backLinkClass =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";

export function AiCoachHistoryPage() {
  const { accessToken } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = useMemo<HistoryTab>(() => {
    const rawTab = searchParams.get("tab");
    return rawTab === "cycle-summaries" ? "cycle-summaries" : "template-generations";
  }, [searchParams]);

  const [templatePage, setTemplatePage] = useState(1);
  const [cyclePage, setCyclePage] = useState(1);
  const [templateHistory, setTemplateHistory] =
    useState<TemplateGenerationHistoryPage | null>(null);
  const [cycleHistory, setCycleHistory] =
    useState<CycleSummaryHistoryPage | null>(null);
  const [isLoadingTemplateHistory, setIsLoadingTemplateHistory] = useState(false);
  const [isLoadingCycleHistory, setIsLoadingCycleHistory] = useState(false);
  const [templateHistoryError, setTemplateHistoryError] = useState<string | null>(
    null
  );
  const [cycleHistoryError, setCycleHistoryError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    if (activeTab === "template-generations") {
      void loadTemplateHistory(accessToken, templatePage);
      return;
    }

    void loadCycleHistory(accessToken, cyclePage);
  }, [accessToken, activeTab, templatePage, cyclePage]);

  async function loadTemplateHistory(token: string, page: number) {
    setIsLoadingTemplateHistory(true);
    setTemplateHistoryError(null);

    try {
      const response = await getTemplateGenerationHistory(token, {
        page,
        pageSize: DEFAULT_PAGE_SIZE
      });
      setTemplateHistory(response);
    } catch (error) {
      setTemplateHistoryError(
        getAiCoachErrorMessage(error, "加载模板生成历史失败，请稍后再试。")
      );
    } finally {
      setIsLoadingTemplateHistory(false);
    }
  }

  async function loadCycleHistory(token: string, page: number) {
    setIsLoadingCycleHistory(true);
    setCycleHistoryError(null);

    try {
      const response = await getCycleSummaryHistory(token, {
        page,
        pageSize: DEFAULT_PAGE_SIZE
      });
      setCycleHistory(response);
    } catch (error) {
      setCycleHistoryError(
        getAiCoachErrorMessage(error, "加载周期总结历史失败，请稍后再试。")
      );
    } finally {
      setIsLoadingCycleHistory(false);
    }
  }

  function switchTab(nextTab: HistoryTab) {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set("tab", nextTab);
    setSearchParams(nextSearchParams, { replace: true });
  }

  return (
    <section className="space-y-8">
      <div className="flex flex-wrap gap-3">
        <Link to="/app" className={backLinkClass}>
          返回控制台
        </Link>
        <Link to="/ai-coach/template-generation" className={backLinkClass}>
          进入模板生成
        </Link>
        <Link to="/ai-coach/cycle-summary" className={backLinkClass}>
          进入周期总结
        </Link>
      </div>

      <header className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          History
        </p>
        <h1 className="mt-4 text-4xl font-semibold text-white sm:text-5xl">
          AI 任务历史
        </h1>
        <p className="mt-4 max-w-3xl leading-8 text-stone-300">
          这里集中回看模板生成和周期总结任务。仍在运行中的任务也会显示在历史里，
          方便继续追踪。
        </p>
      </header>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => switchTab("template-generations")}
          className={tabClass(activeTab === "template-generations")}
        >
          模板生成历史
        </button>
        <button
          type="button"
          onClick={() => switchTab("cycle-summaries")}
          className={tabClass(activeTab === "cycle-summaries")}
        >
          周期总结历史
        </button>
      </div>

      {activeTab === "template-generations" ? (
        <AiTaskHistoryList
          title="模板生成历史"
          description="回看你提交过的 AI 模板生成任务，进入详情页后可以继续等待、查看结果或跳转到对应草稿。"
          emptyMessage="还没有模板生成历史。发起第一条 AI 模板生成任务后会显示在这里。"
          taskLinkLabel="查看模板任务"
          isLoading={isLoadingTemplateHistory}
          error={templateHistoryError}
          data={templateHistory}
          getTaskLink={(record) =>
            `/ai-coach/template-generation/tasks/${record.taskId}`
          }
          renderHeading={renderTemplateHistoryHeading}
          renderMeta={renderTemplateHistoryMeta}
          onPageChange={setTemplatePage}
        />
      ) : (
        <AiTaskHistoryList
          title="周期总结历史"
          description="回看已生成的周期总结，或继续追踪还在处理中和等待中的总结任务。"
          emptyMessage="还没有周期总结历史。完成一次 AI 周期总结后会显示在这里。"
          taskLinkLabel="查看总结任务"
          isLoading={isLoadingCycleHistory}
          error={cycleHistoryError}
          data={cycleHistory}
          getTaskLink={(record) => `/ai-coach/cycle-summary/tasks/${record.taskId}`}
          renderHeading={renderCycleHistoryHeading}
          renderMeta={renderCycleHistoryMeta}
          onPageChange={setCyclePage}
        />
      )}
    </section>
  );
}

function renderTemplateHistoryHeading(record: TemplateGenerationHistoryItem) {
  return record.templateName ?? `模板生成任务 #${record.taskId}`;
}

function renderTemplateHistoryMeta(record: TemplateGenerationHistoryItem) {
  return (
    <>
      <Tag>{formatSceneType(record.sceneType)}</Tag>
      <Tag>{formatGoalType(record.goalType)}</Tag>
      <Tag>{record.cycleLength} 天</Tag>
      <Tag>{record.includeCardio ? "允许有氧" : "不含有氧"}</Tag>
      {record.additionalRequirements ? (
        <Tag title={record.additionalRequirements}>含补充要求</Tag>
      ) : null}
    </>
  );
}

function renderCycleHistoryHeading(record: CycleSummaryHistoryItem) {
  return record.templateName
    ? `${record.templateName} · 第 ${record.runNo ?? "--"} 轮`
    : `周期总结任务 #${record.taskId}`;
}

function renderCycleHistoryMeta(record: CycleSummaryHistoryItem) {
  return (
    <>
      <Tag>Cycle Run #{record.cycleRunId}</Tag>
      <Tag>
        {record.cycleLength ? `${record.cycleLength} 天` : "周期长度待定"}
      </Tag>
      {record.templateId ? <Tag>模板 #{record.templateId}</Tag> : null}
    </>
  );
}

function tabClass(active: boolean) {
  return [
    "rounded-full px-4 py-2 text-sm transition",
    active
      ? "bg-amber-400 text-stone-950"
      : "bg-white/8 text-stone-200 hover:bg-white/12"
  ].join(" ");
}

function Tag({
  children,
  title
}: {
  children: ReactNode;
  title?: string;
}) {
  return (
    <span
      title={title}
      className="rounded-full border border-white/10 bg-white/8 px-3 py-1"
    >
      {children}
    </span>
  );
}
