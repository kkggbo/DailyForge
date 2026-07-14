import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { createDraftTemplate } from "../api/cycle-template";
import { CycleTemplateEditor } from "../components/CycleTemplateEditor";
import { useCycleTemplateEditor } from "../hooks/useCycleTemplateEditor";
import { getCycleTemplateErrorMessage } from "../lib/cycle-template-enums";
import {
  createEmptyEditorForm,
  editorFormToPayload
} from "../lib/cycle-template-mappers";
import { getCycleTemplateFieldErrorSummaries } from "../lib/cycle-template-validators";

export function CycleTemplateCreatePage() {
  const { accessToken } = useAuth();
  const navigate = useNavigate();
  const initialForm = useMemo(() => createEmptyEditorForm(), []);
  const editor = useCycleTemplateEditor(initialForm, {
    allowEmptyCycleLength: false
  });
  const [pageError, setPageError] = useState<string | null>(null);
  const [showValidationSummary, setShowValidationSummary] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fieldErrorSummaries = getCycleTemplateFieldErrorSummaries(editor.fieldErrors);

  async function handleSubmit() {
    if (!accessToken) {
      return;
    }

    if (editor.hasErrors) {
      setShowValidationSummary(true);
      setPageError("请先修正表单中的错误，再保存草稿。");
      return;
    }

    setIsSubmitting(true);
    setPageError(null);
    setShowValidationSummary(false);

    try {
      const response = await createDraftTemplate(
        accessToken,
        editorFormToPayload(editor.form)
      );
      editor.setBaselineToCurrent();
      navigate(`/cycle-templates/${response.templateId}/edit`);
    } catch (error) {
      setPageError(
        getCycleTemplateErrorMessage(error, "创建训练模板失败，请稍后再试。")
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleBack() {
    if (editor.isDirty && !window.confirm("当前有未保存修改，确认离开吗？")) {
      return;
    }

    navigate("/cycle-templates");
  }

  if (!accessToken) {
    return null;
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
            New Draft
          </p>
          <h1 className="mt-3 text-4xl font-semibold text-white">新建训练模板</h1>
          <p className="mt-4 max-w-3xl leading-8 text-stone-300">
            先搭出你的循环分化结构，再逐天补动作内容。没有动作的训练日会按休息日保存。
          </p>
        </div>
        <div className="flex gap-2">
          <button type="button" onClick={handleBack} className={secondaryButtonClass}>
            返回列表
          </button>
          <Link to="/cycle-templates" className={secondaryButtonClass}>
            模板首页
          </Link>
        </div>
      </div>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {pageError}
        </div>
      ) : null}

      {showValidationSummary && fieldErrorSummaries.length > 0 ? (
        <div className="rounded-[24px] border border-rose-300/20 bg-rose-300/10 p-5">
          <p className="text-sm font-semibold text-rose-100">还需要修正这些问题：</p>
          <ul className="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-rose-50">
            {fieldErrorSummaries.map((summary) => (
              <li key={summary}>{summary}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <CycleTemplateEditor
        accessToken={accessToken}
        form={editor.form}
        fieldErrors={editor.fieldErrors}
        isDirty={editor.isDirty}
        canUndo={editor.canUndo}
        isSubmitting={isSubmitting}
        submitLabel="保存草稿"
        cycleLengthMode="select"
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
