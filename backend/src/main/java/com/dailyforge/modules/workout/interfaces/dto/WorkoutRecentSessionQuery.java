package com.dailyforge.modules.workout.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Recent workout session page query")
public class WorkoutRecentSessionQuery {

    @Schema(description = "One-based page number", example = "1", defaultValue = "1")
    @Min(1)
    private int page = 1;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(1) @Max(50)
    private int pageSize = 20;

    @Schema(description = "Optional session status filter",
            allowableValues = {"completed", "cancelled", "in_progress"})
    @Pattern(regexp = "completed|cancelled|in_progress")
    private String sessionStatus;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }
}
