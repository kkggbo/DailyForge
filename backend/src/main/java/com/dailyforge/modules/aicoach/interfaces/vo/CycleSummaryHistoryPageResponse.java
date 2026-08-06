package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI cycle summary history page response")
public record CycleSummaryHistoryPageResponse(
        @Schema(description = "Page number", example = "1") int page,
        @Schema(description = "Page size", example = "20") int pageSize,
        @Schema(description = "Total history count", example = "5") long total,
        @Schema(description = "History records") List<CycleSummaryHistoryItemResponse> records) {
}
