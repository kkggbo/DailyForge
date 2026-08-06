import { useEffect, useState } from "react";
import { getGoalTypeLabel, getSceneTypeLabel } from "../lib/ai-coach-enums";
import { createDefaultTemplateGenerationForm } from "../lib/ai-coach-mappers";
import type {
  TemplateGenerationCapability,
  TemplateGenerationForm as TemplateGenerationFormValues
} from "../types/ai-coach";

type TemplateGenerationFormProps = {
  capability: TemplateGenerationCapability;
  isSubmitting: boolean;
  submitError: string | null;
  onSubmit: (form: TemplateGenerationFormValues) => void;
};

const MAX_ADDITIONAL_REQUIREMENTS = 500;

const inputClass =
  "w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60";

export function TemplateGenerationForm({
  capability,
  isSubmitting,
  submitError,
  onSubmit
}: TemplateGenerationFormProps) {
  const [form, setForm] = useState<TemplateGenerationFormValues | null>(() =>
    createDefaultTemplateGenerationForm(capability)
  );
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    setForm(createDefaultTemplateGenerationForm(capability));
    setValidationError(null);
  }, [
    capability.allowedGoalTypes,
    capability.allowedSceneTypes,
    capability.maxCycleLength,
    capability.minCycleLength
  ]);

  function handleSubmit() {
    if (!form) {
      setValidationError("当前生成条件不可用，请刷新后重试。");
      return;
    }

    const cycleLength = Number(form.cycleLengthText);
    const additionalRequirementsLength = form.additionalRequirements.trim().length;

    if (!Number.isInteger(cycleLength)) {
      setValidationError("周期天数必须是整数。");
      return;
    }

    if (
      cycleLength < capability.minCycleLength ||
      cycleLength > capability.maxCycleLength
    ) {
      setValidationError(
        `周期天数必须在 ${capability.minCycleLength} 到 ${capability.maxCycleLength} 之间。`
      );
      return;
    }

    if (additionalRequirementsLength > MAX_ADDITIONAL_REQUIREMENTS) {
      setValidationError("补充要求不能超过 500 个字符。");
      return;
    }

    setValidationError(null);
    onSubmit(form);
  }

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div>
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
          Template Generation
        </p>
        <h2 className="mt-3 text-3xl font-semibold text-white">
          设置本次生成条件
        </h2>
        <p className="mt-3 max-w-2xl leading-7 text-stone-300">
          结果会先落为可编辑草稿。你可以保留结构化选项，再用一段自由输入告诉 AI
          这次有哪些额外偏好、限制或提醒。
        </p>
      </div>

      {!form ? (
        <div className="mt-6 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          后端没有返回可用的场景或目标选项，当前无法安全发起模板生成。
        </div>
      ) : (
        <div className="mt-6 grid gap-5 lg:grid-cols-2">
          <label className="block">
            <span className="text-sm text-stone-300">训练场景</span>
            <select
              value={form.sceneType}
              onChange={(event) =>
                setForm((previous) =>
                  previous
                    ? {
                        ...previous,
                        sceneType: event.target.value as TemplateGenerationFormValues["sceneType"]
                      }
                    : previous
                )
              }
              className={`${inputClass} mt-2`}
            >
              {capability.allowedSceneTypes.map((sceneType) => (
                <option key={sceneType} value={sceneType}>
                  {getSceneTypeLabel(sceneType)}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="text-sm text-stone-300">目标方向</span>
            <select
              value={form.goalType}
              onChange={(event) =>
                setForm((previous) =>
                  previous
                    ? {
                        ...previous,
                        goalType: event.target.value as TemplateGenerationFormValues["goalType"]
                      }
                    : previous
                )
              }
              className={`${inputClass} mt-2`}
            >
              {capability.allowedGoalTypes.map((goalType) => (
                <option key={goalType} value={goalType}>
                  {getGoalTypeLabel(goalType)}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="text-sm text-stone-300">周期天数</span>
            <input
              type="number"
              min={capability.minCycleLength}
              max={capability.maxCycleLength}
              step={1}
              value={form.cycleLengthText}
              onChange={(event) =>
                setForm((previous) =>
                  previous
                    ? {
                        ...previous,
                        cycleLengthText: event.target.value
                      }
                    : previous
                )
              }
              className={`${inputClass} mt-2`}
            />
            <p className="mt-2 text-xs text-stone-500">
              当前允许范围：{capability.minCycleLength} -{" "}
              {capability.maxCycleLength} 天
            </p>
          </label>

          <div className="rounded-3xl border border-white/10 bg-black/20 p-4">
            <p className="text-sm font-medium text-white">附加偏好</p>
            <label className="mt-4 inline-flex items-center gap-3 text-sm text-stone-300">
              <input
                type="checkbox"
                checked={form.includeCardio}
                onChange={(event) =>
                  setForm((previous) =>
                    previous
                      ? {
                          ...previous,
                          includeCardio: event.target.checked
                        }
                      : previous
                  )
                }
                className="h-4 w-4 accent-amber-400"
              />
              接受系统安排有氧内容
            </label>
          </div>

          <label className="block lg:col-span-2">
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm text-stone-300">
                补充要求 <span className="text-stone-500">Optional</span>
              </span>
              <span className="text-xs text-stone-500">
                {form.additionalRequirements.length}/{MAX_ADDITIONAL_REQUIREMENTS}
              </span>
            </div>
            <textarea
              value={form.additionalRequirements}
              maxLength={MAX_ADDITIONAL_REQUIREMENTS}
              aria-label="Additional requirements"
              placeholder="例如：每周至少保留 1 天完整休息，避免高冲击跳跃，优先安排上肢推拉训练。"
              onChange={(event) =>
                setForm((previous) =>
                  previous
                    ? {
                        ...previous,
                        additionalRequirements: event.target.value
                      }
                    : previous
                )
              }
              className={`${inputClass} mt-2 min-h-32 resize-y`}
            />
            <p className="mt-2 text-xs leading-5 text-stone-500">
              这里的内容只作用于这一次生成，不会写回你的长期资料。
            </p>
          </label>
        </div>
      )}

      {validationError || submitError ? (
        <div className="mt-5 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {validationError ?? submitError}
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap justify-end gap-3">
        <button
          type="button"
          disabled={isSubmitting || !form}
          onClick={handleSubmit}
          className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
        >
          {isSubmitting ? "提交中..." : "生成草稿任务"}
        </button>
      </div>
    </section>
  );
}
