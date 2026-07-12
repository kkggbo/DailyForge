package com.dailyforge.modules.profile.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Body metric history page response")
public record BodyMetricsPageResponse(
        @Schema(description = "Page number", example = "1") int page,
        @Schema(description = "Page size", example = "20") int pageSize,
        @Schema(description = "Total active history count", example = "2") long total,
        @Schema(description = "History records") List<BodyMetricLogItemResponse> records) {
}
