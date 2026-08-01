import { Link } from "react-router-dom";
import type { AiCompletionScene } from "../../profile/types/profile";
import { getMissingFieldLabel } from "../lib/ai-coach-enums";
import { buildProfileAiCompletionPath } from "../lib/ai-coach-mappers";
import type { MissingFieldCode } from "../types/ai-coach";

type AiCoachMissingFieldsNoticeProps = {
  fields: MissingFieldCode[];
  scene: AiCompletionScene;
  redirectPath: string;
  title?: string;
  description?: string;
  actionLabel?: string;
};

export function AiCoachMissingFieldsNotice({
  fields,
  scene,
  redirectPath,
  title = "当前资料还不够完整",
  description = "补齐这些关键信息后，AI Coach 才能给出更稳定的建议。",
  actionLabel = "去补充资料"
}: AiCoachMissingFieldsNoticeProps) {
  if (fields.length === 0) {
    return null;
  }

  return (
    <section className="rounded-[28px] border border-amber-300/20 bg-amber-300/10 p-5">
      <h3 className="text-xl font-semibold text-white">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-stone-200">{description}</p>
      <div className="mt-4 flex flex-wrap gap-2">
        {fields.map((field) => (
          <span
            key={field}
            className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-100"
          >
            {getMissingFieldLabel(field)}
          </span>
        ))}
      </div>
      <Link
        to={buildProfileAiCompletionPath(scene, redirectPath)}
        className="mt-5 inline-flex rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
      >
        {actionLabel}
      </Link>
    </section>
  );
}
