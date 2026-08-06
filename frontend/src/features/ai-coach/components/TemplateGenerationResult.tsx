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
      <CycleTemplateReadOnly
        detail={mapGeneratedDraftTemplateToDetail(result.draftTemplate)}
      />

      <GenerationRationalePanel rationale={result.generationRationale} />
    </div>
  );
}
