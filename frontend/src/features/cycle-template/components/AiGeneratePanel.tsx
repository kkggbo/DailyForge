import { useState } from "react";
import { generateDraftTemplateByAi } from "../api/cycle-template";
import { getCycleTemplateErrorMessage, goalTypeOptions } from "../lib/cycle-template-enums";

type AiGeneratePanelProps = {
  accessToken: string;
  onGenerated: (templateId: number) => void;
};

export function AiGeneratePanel({ accessToken, onGenerated }: AiGeneratePanelProps) {
  const [goalType, setGoalType] = useState("muscle_gain");
  const [cycleLength, setCycleLength] = useState("5");
  const [prompt, setPrompt] = useState("");
  const [useProfileData, setUseProfileData] = useState(true);
  const [message, setMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit() {
    setIsSubmitting(true);
    setMessage(null);

    try {
      const response = await generateDraftTemplateByAi(accessToken, {
        goalType: goalType || null,
        cycleLength: cycleLength ? Number(cycleLength) : null,
        prompt,
        useProfileData
      });
      onGenerated(response.templateId);
    } catch (error) {
      setMessage(
        getCycleTemplateErrorMessage(error, "AI 生成模板失败，请稍后再试。")
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="rounded-[28px] border border-white/10 bg-white/8 p-5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            AI Draft
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-white">AI 生成草稿</h2>
          <p className="mt-2 text-sm leading-6 text-stone-300">
            当前后端是占位接口，提交后会给出明确提示，不会静默创建假草稿。
          </p>
        </div>
      </div>

      <div className="mt-5 grid gap-3 lg:grid-cols-[0.8fr_0.5fr_1.5fr]">
        <select
          value={goalType}
          onChange={(event) => setGoalType(event.target.value)}
          className={inputClass}
        >
          {goalTypeOptions
            .filter((option) => option.value)
            .map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
        </select>
        <input
          type="number"
          min={1}
          max={7}
          step={1}
          value={cycleLength}
          onChange={(event) => setCycleLength(event.target.value)}
          className={inputClass}
          placeholder="周期天数"
        />
        <input
          value={prompt}
          onChange={(event) => setPrompt(event.target.value)}
          className={inputClass}
          placeholder="例如：一周五练，重点提升胸背，膝盖不舒服"
        />
      </div>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <label className="inline-flex items-center gap-2 text-sm text-stone-300">
          <input
            type="checkbox"
            checked={useProfileData}
            onChange={(event) => setUseProfileData(event.target.checked)}
            className="h-4 w-4 accent-amber-400"
          />
          使用个人资料作为参考
        </label>
        <button
          type="button"
          disabled={isSubmitting}
          onClick={() => {
            void handleSubmit();
          }}
          className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
        >
          {isSubmitting ? "提交中..." : "生成草稿"}
        </button>
      </div>

      {message ? (
        <div className="mt-4 rounded-2xl border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-sm text-amber-100">
          {message}
        </div>
      ) : null}
    </section>
  );
}

const inputClass =
  "w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60";
