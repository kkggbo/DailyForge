import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  getCycleTemplateDetail,
  updateDraftTemplate,
  updateFormalTemplate
} from "../api/cycle-template";
import { CycleTemplateEditor } from "../components/CycleTemplateEditor";
import { useCycleTemplateEditor } from "../hooks/useCycleTemplateEditor";
import { getCycleTemplateErrorMessage } from "../lib/cycle-template-enums";
import {
  createEmptyEditorForm,
  detailToEditorForm,
  editorFormToPayload
} from "../lib/cycle-template-mappers";
import type { CycleTemplateDetailResponse } from "../types/cycle-template";

export function CycleTemplateEditPage() {
  const { accessToken } = useAuth();
  const navigate = useNavigate();
  const params = useParams();
  const templateId = Number(params.templateId);
  const [detail, setDetail] = useState<CycleTemplateDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken || !Number.isFinite(templateId)) {
      return;
    }

    void loadDetail(accessToken, templateId);
  }, [accessToken, templateId]);

  const initialForm = useMemo(
    () => (detail ? detailToEditorForm(detail) : createEmptyEditorForm()),
    [detail]
  );

  const editor = useCycleTemplateEditor(initialForm, {
    allowEmptyCycleLength: detail?.status === "draft"
  });

  async function loadDetail(token: string, id: number) {
    setIsLoading(true);
    setPageError(null);

    try {
      setDetail(await getCycleTemplateDetail(token, id));
    } catch (error) {
      setPageError(
        getCycleTemplateErrorMessage(error, "加载模板失败，请稍后再试。")
      );
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSubmit() {
    if (!accessToken || !detail) {
      return;
    }

    if (editor.hasErrors) {
      setPageError("请先修正表单中的错误，再保存模板。");
      return;
    }

    setIsSubmitting(true);
    setPageError(null);

    try {
      if (detail.status === "draft") {
        await updateDraftTemplate(
          accessToken,
          detail.templateId,
          editorFormToPayload(editor.form)
        );
      } else {
        await updateFormalTemplate(
          accessToken,
          detail.templateId,
          editorFormToPayload(editor.form, {
            includeOnlyEditableFromDay:
              detail.status === "active" ? detail.editableFromDayIndex : undefined
          })
        );
      }

      await loadDetail(accessToken, detail.templateId);
      editor.setBaselineToCurrent();
    } catch (error) {
      setPageError(
        getCycleTemplateErrorMessage(error, "保存训练模板失败，请稍后再试。")
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleBack() {
    if (editor.isDirty && !window.confirm("当前有未保存修改，确认离开吗？")) {
      return;
    }

    navigate(detail ? `/cycle-templates/${detail.templateId}` : "/cycle-templates");
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在加载模板编辑器...
        </div>
      </div>
    );
  }

  if (!accessToken || !detail) {
    return (
      <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
        {pageError ?? "模板不存在。"}
      </div>
    );
  }

  const lockedBeforeDayIndex =
    detail.status === "active" ? detail.editableFromDayIndex : 1;

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
            Template Editor
          </p>
          <h1 className="mt-3 text-4xl font-semibold text-white">编辑训练模板</h1>
          <p className="mt-4 max-w-3xl leading-8 text-stone-300">
            {detail.status === "active"
              ? `当前模板正在运行，只允许编辑 Day ${detail.editableFromDayIndex} 及之后的训练日。`
              : "草稿和未启用模板可以完整编辑。"}
          </p>
        </div>
        <div className="flex gap-2">
          <button type="button" onClick={handleBack} className={secondaryButtonClass}>
            返回
          </button>
          <Link
            to={`/cycle-templates/${detail.templateId}`}
            className={secondaryButtonClass}
          >
            查看详情
          </Link>
        </div>
      </div>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {pageError}
        </div>
      ) : null}

      <CycleTemplateEditor
        accessToken={accessToken}
        form={editor.form}
        fieldErrors={editor.fieldErrors}
        isDirty={editor.isDirty}
        canUndo={editor.canUndo}
        isSubmitting={isSubmitting}
        submitLabel={detail.status === "draft" ? "保存草稿" : "保存模板"}
        lockedBeforeDayIndex={lockedBeforeDayIndex}
        disableCycleLength={detail.status === "active"}
        cycleLengthMode="select"
        allowEmptyCycleLengthOption={detail.status === "draft"}
        onRootFieldChange={editor.updateRootField}
        onDayChange={editor.updateDay}
        onAddExercise={editor.addExercise}
        onUpdateExercise={editor.updateExercise}
        onRemoveExercise={editor.removeExercise}
        onMoveExercise={editor.moveExercise}
        onReorderExercise={editor.reorderExercise}
        onUndo={editor.undo}
        onReset={editor.resetToBaseline}
        onSubmit={() => {
          void handleSubmit();
        }}
      />
    </section>
  );
}

const secondaryButtonClass =
  "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";
