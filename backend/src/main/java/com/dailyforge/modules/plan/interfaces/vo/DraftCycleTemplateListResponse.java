package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Draft cycle template list response")
public record DraftCycleTemplateListResponse(
        @Schema(description = "Draft template records") List<DraftCycleTemplateSummary> records) {
}
