import { CycleTemplateReadOnly } from "../../cycle-template/components/CycleTemplateReadOnly";
import { mapGeneratedDraftTemplateToDetail } from "../lib/ai-coach-mappers";
import type { TemplateGenerationTaskResult } from "../types/ai-coach";
import { GenerationRationalePanel } from "./GenerationRationalePanel";

type TemplateGenerationResultProps = {
  result: TemplateGenerationTaskResult;
};

export function TemplateGenerationResult({
  result
}: TemplateGenerationResultProps) {
  return (
    <div className="space-y-6">
      <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
          Draft Preview
        </p>
        <h2 className="mt-3 text-3xl font-semibold text-white">草稿模板预览</h2>
        <p className="mt-3 max-w-2xl leading-7 text-stone-300">
          这份草稿已经由后端完成结构校验，但仍建议你进入模板模块做最后调整后再启用。
        </p>
      </section>

      <CycleTemplateReadOnly
        detail={mapGeneratedDraftTemplateToDetail(result.draftTemplate)}
      />

      <GenerationRationalePanel rationale={result.generationRationale} />
    </div>
  );
}
