import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { ApiRequestError } from "../../../shared/api/http";
import {
  activateCycleTemplate,
  copyCycleTemplate,
  deleteCycleTemplate,
  getCycleTemplateDetail
} from "../api/cycle-template";
import { TemplateActionDialog } from "../components/CycleTemplateDialogs";
import { CycleTemplateReadOnly } from "../components/CycleTemplateReadOnly";
import { getCycleTemplateErrorMessage } from "../lib/cycle-template-enums";
import type { CycleTemplateDetailResponse } from "../types/cycle-template";

type DetailAction = "activate" | "delete" | "copy" | null;

export function CycleTemplateDetailPage() {
  const { accessToken } = useAuth();
  const navigate = useNavigate();
  const params = useParams();
  const templateId = Number(params.templateId);
  const [detail, setDetail] = useState<CycleTemplateDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [action, setAction] = useState<DetailAction>(null);
  const [dialogError, setDialogError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [requiresSwitchConfirm, setRequiresSwitchConfirm] = useState(false);

  useEffect(() => {
    if (!accessToken || !Number.isFinite(templateId)) {
      return;
    }

    void loadDetail(accessToken, templateId);
  }, [accessToken, templateId]);

  async function loadDetail(token: string, id: number) {
    setIsLoading(true);
    setPageError(null);

    try {
      setDetail(await getCycleTemplateDetail(token, id));
    } catch (error) {
      setPageError(
        getCycleTemplateErrorMessage(error, "加载模板详情失败，请稍后再试。")
      );
    } finally {
      setIsLoading(false);
    }
  }

  async function handleConfirmAction() {
    if (!accessToken || !detail || !action) {
      return;
    }

    setIsSubmitting(true);
    setDialogError(null);

    try {
      if (action === "activate") {
        await activateCycleTemplate(accessToken, detail.templateId, {
          confirmSwitch: requiresSwitchConfirm
        });
        await loadDetail(accessToken, detail.templateId);
      }

      if (action === "delete") {
        await deleteCycleTemplate(accessToken, detail.templateId);
        navigate("/cycle-templates");
        return;
      }

      if (action === "copy") {
        const response = await copyCycleTemplate(
          accessToken,
          detail.templateId,
          `${detail.templateName} - Copy`
        );
        navigate(`/cycle-templates/${response.templateId}/edit`);
        return;
      }

      setAction(null);
      setRequiresSwitchConfirm(false);
    } catch (error) {
      if (
        action === "activate" &&
        error instanceof ApiRequestError &&
        error.code === "CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED"
      ) {
        setRequiresSwitchConfirm(true);
        setDialogError("当前已有启用模板，再次确认后会结束旧循环，并从新模板 Day 1 开始。");
        return;
      }

      setDialogError(
        getCycleTemplateErrorMessage(error, "操作失败，请稍后再试。")
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在加载模板详情...
        </div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
        {pageError ?? "模板不存在。"}
      </div>
    );
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link to="/cycle-templates" className={secondaryButtonClass}>
          返回列表
        </Link>
        <div className="flex flex-wrap gap-2">
          <Link
            to={`/cycle-templates/${detail.templateId}/edit`}
            className={secondaryButtonClass}
          >
            编辑
          </Link>
          <button
            type="button"
            onClick={() => setAction("copy")}
            className={secondaryButtonClass}
          >
            复制
          </button>
          {detail.canActivate ? (
            <button
              type="button"
              onClick={() => {
                setRequiresSwitchConfirm(false);
                setAction("activate");
              }}
              className={primaryButtonClass}
            >
              启用
            </button>
          ) : null}
          {detail.canDelete ? (
            <button
              type="button"
              onClick={() => setAction("delete")}
              className="rounded-full border border-rose-300/25 px-4 py-2 text-sm text-rose-100 transition hover:bg-rose-400/10"
            >
              删除
            </button>
          ) : null}
        </div>
      </div>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {pageError}
        </div>
      ) : null}

      <CycleTemplateReadOnly detail={detail} />

      <TemplateActionDialog
        open={Boolean(action)}
        title={getTitle(action)}
        description={getDescription(action, detail.templateName, requiresSwitchConfirm)}
        confirmLabel={getConfirmLabel(action, requiresSwitchConfirm)}
        danger={action === "delete"}
        isSubmitting={isSubmitting}
        errorMessage={dialogError}
        onClose={() => {
          setAction(null);
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

function getTitle(action: DetailAction) {
  if (action === "activate") return "启用训练模板";
  if (action === "delete") return "删除训练模板";
  if (action === "copy") return "复制训练模板";
  return "";
}

function getDescription(
  action: DetailAction,
  templateName: string,
  requiresSwitchConfirm: boolean
) {
  if (action === "activate") {
    return requiresSwitchConfirm
      ? `确认启用「${templateName}」？旧的启用模板会被结束，新模板会从 Day 1 开始。`
      : `确认启用「${templateName}」？`;
  }
  if (action === "delete") return `确认删除「${templateName}」？`;
  if (action === "copy") return `复制「${templateName}」为新的草稿模板。`;
  return "";
}

function getConfirmLabel(action: DetailAction, requiresSwitchConfirm: boolean) {
  if (action === "activate") return requiresSwitchConfirm ? "确认切换" : "启用";
  if (action === "delete") return "删除";
  if (action === "copy") return "复制";
  return "确认";
}

const primaryButtonClass =
  "rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300";

const secondaryButtonClass =
  "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";
