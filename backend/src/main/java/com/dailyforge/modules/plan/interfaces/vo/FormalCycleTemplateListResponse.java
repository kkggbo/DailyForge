package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Formal cycle template list response")
public record FormalCycleTemplateListResponse(
        @Schema(description = "Current active template id", example = "101") Long activeTemplateId,
        @Schema(description = "Formal template records") List<FormalCycleTemplateSummary> records) {
}
