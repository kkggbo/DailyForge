package com.dailyforge.modules.profile.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Delete latest body metric response")
public record DeleteLatestBodyMetricResponse(
        @Schema(description = "Deleted record id", example = "12") Long deletedId,
        @Schema(description = "Deleted record date", example = "2026-07-12") LocalDate deletedRecordDate,
        @Schema(description = "Deleted record weight", example = "76.50") BigDecimal deletedWeightKg) {
}
