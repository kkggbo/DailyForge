import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { ApiRequestError } from "../../../shared/api/http";
import {
  activateCycleTemplate,
  copyCycleTemplate,
  deleteCycleTemplate,
  getCurrentActiveTemplate,
  getDraftTemplates,
  getFormalTemplates
} from "../api/cycle-template";
import {
  ActiveTemplatePanel,
  TemplateList
} from "../components/CycleTemplateCards";
import { TemplateActionDialog } from "../components/CycleTemplateDialogs";
import { getCycleTemplateErrorMessage } from "../lib/cycle-template-enums";
import type {
  CurrentActiveTemplateResponse,
  CycleTemplateTab,
  DraftTemplateListResponse,
  FormalTemplateListResponse
} from "../types/cycle-template";

type PendingAction = {
  type: "activate" | "delete" | "copy";
  templateId: number;
  templateName: string;
} | null;

export function CycleTemplatePage() {
  const { accessToken } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<CycleTemplateTab>("formal");
  const [formalTemplates, setFormalTemplates] =
    useState<FormalTemplateListResponse | null>(null);
  const [draftTemplates, setDraftTemplates] =
    useState<DraftTemplateListResponse | null>(null);
  const [activeTemplate, setActiveTemplate] =
    useState<CurrentActiveTemplateResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [pageMessage, setPageMessage] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [dialogError, setDialogError] = useState<string | null>(null);
  const [isSubmittingAction, setIsSubmittingAction] = useState(false);
  const [requiresSwitchConfirm, setRequiresSwitchConfirm] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadPage(accessToken);
  }, [accessToken]);

  async function loadPage(token: string) {
    setIsLoading(true);
    setPageError(null);

    try {
      const [formal, drafts, active] = await Promise.all([
        getFormalTemplates(token),
        getDraftTemplates(token),
        loadActiveTemplate(token)
      ]);
      setFormalTemplates(formal);
      setDraftTemplates(drafts);
      setActiveTemplate(active);
    } catch (error) {
      setPageError(
        getCycleTemplateErrorMessage(error, "加载训练模板失败，请稍后再试。")
      );
    } finally {
      setIsLoading(false);
    }
  }

  async function loadActiveTemplate(token: string) {
    try {
      return await getCurrentActiveTemplate(token);
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        error.code === "CYCLE_TEMPLATE_ACTIVE_NOT_FOUND"
      ) {
        return null;
      }

      throw error;
    }
  }

  async function refreshPage() {
    if (!accessToken) {
      return;
    }

    await loadPage(accessToken);
  }

  async function handleConfirmAction() {
    if (!pendingAction || !accessToken) {
      return;
    }

    setIsSubmittingAction(true);
    setDialogError(null);

    try {
      if (pendingAction.type === "activate") {
        await activateCycleTemplate(accessToken, pendingAction.templateId, {
          confirmSwitch: requiresSwitchConfirm
        });
      }

      if (pendingAction.type === "delete") {
        await deleteCycleTemplate(accessToken, pendingAction.templateId);
      }

      if (pendingAction.type === "copy") {
        const response = await copyCycleTemplate(
          accessToken,
          pendingAction.templateId,
          `${pendingAction.templateName} - Copy`
        );
        navigate(`/cycle-templates/${response.templateId}/edit`);
        return;
      }

      setPendingAction(null);
      setRequiresSwitchConfirm(false);
      await refreshPage();
    } catch (error) {
      if (
        pendingAction.type === "activate" &&
        error instanceof ApiRequestError &&
        error.code === "CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED"
      ) {
        setRequiresSwitchConfirm(true);
        setDialogError(
          "当前已有启用模板，再次确认后会结束旧循环，并从新模板 Day 1 开始。"
        );
        return;
      }

      const message = getCycleTemplateErrorMessage(error, "操作失败，请稍后再试。");
      setDialogError(message);
      setPageError(message);
    } finally {
      setIsSubmittingAction(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在加载训练模板...
        </div>
      </div>
    );
  }

  return (
    <section className="space-y-8">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
            Cycle Templates
          </p>
          <h1 className="mt-3 text-4xl font-semibold text-white sm:text-5xl">
            训练模板
          </h1>
          <p className="mt-4 max-w-3xl leading-8 text-stone-300">
            把你的分化训练整理成 1 到 7 天的循环模板。启用后，系统会按训练日顺序推进。
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <Link
            to="/cycle-templates/create"
            className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
          >
            新建草稿
          </Link>
          <button
            type="button"
            onClick={() => {
              setPageMessage("AI 生成草稿功能暂未开放。");
            }}
            className="rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
          >
            AI 生成草稿
          </button>
        </div>
      </div>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {pageError}
        </div>
      ) : null}

      {pageMessage ? (
        <div className="rounded-2xl border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-sm text-amber-100">
          {pageMessage}
        </div>
      ) : null}

      <ActiveTemplatePanel activeTemplate={activeTemplate} />

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setActiveTab("formal")}
          className={tabClass(activeTab === "formal")}
        >
          正式模板
        </button>
        <button
          type="button"
          onClick={() => setActiveTab("drafts")}
          className={tabClass(activeTab === "drafts")}
        >
          草稿模板
        </button>
      </div>

      <TemplateList
        type={activeTab}
        formalTemplates={formalTemplates?.records}
        draftTemplates={draftTemplates?.records}
        onActivate={(templateId, templateName) => {
          setDialogError(null);
          setRequiresSwitchConfirm(false);
          setPendingAction({ type: "activate", templateId, templateName });
        }}
        onDelete={(templateId, templateName) => {
          setDialogError(null);
          setPendingAction({ type: "delete", templateId, templateName });
        }}
        onCopy={(templateId, templateName) => {
          setDialogError(null);
          setPendingAction({ type: "copy", templateId, templateName });
        }}
      />

      <TemplateActionDialog
        open={Boolean(pendingAction)}
        title={getDialogTitle(pendingAction)}
        description={getDialogDescription(pendingAction, requiresSwitchConfirm)}
        confirmLabel={getDialogConfirmLabel(pendingAction, requiresSwitchConfirm)}
        danger={pendingAction?.type === "delete"}
        isSubmitting={isSubmittingAction}
        errorMessage={dialogError}
        onClose={() => {
          setPendingAction(null);
          setDialogError(null);
          setRequiresSwitchConfirm(false);
        }}
        onConfirm={() => {
          void handleConfirmAction();
        }}
      />
    </section>
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

function getDialogTitle(action: PendingAction) {
  if (!action) {
    return "";
  }

  if (action.type === "activate") {
    return "启用训练模板";
  }

  if (action.type === "delete") {
    return "删除训练模板";
  }

  return "复制训练模板";
}

function getDialogDescription(action: PendingAction, requiresSwitchConfirm: boolean) {
  if (!action) {
    return "";
  }

  if (action.type === "activate") {
    return requiresSwitchConfirm
      ? `确认启用「${action.templateName}」？旧的启用模板会被结束，新模板会从 Day 1 开始。`
      : `确认启用「${action.templateName}」？如果当前已经有启用模板，后端会要求二次确认。`;
  }

  if (action.type === "delete") {
    return `确认删除「${action.templateName}」？删除后它不会再出现在模板列表中。`;
  }

  return `复制「${action.templateName}」为新的草稿模板，并进入编辑页。`;
}

function getDialogConfirmLabel(action: PendingAction, requiresSwitchConfirm: boolean) {
  if (!action) {
    return "确认";
  }

  if (action.type === "activate") {
    return requiresSwitchConfirm ? "确认切换" : "启用";
  }

  if (action.type === "delete") {
    return "删除";
  }

  return "复制";
}
