package com.dailyforge.modules.profile.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Body metric history page query")
public class BodyMetricPageQuery {

    @Schema(description = "Page number", example = "1", defaultValue = "1")
    @Min(value = 1, message = "must be greater than or equal to 1")
    private int page = 1;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(value = 1, message = "must be greater than or equal to 1")
    @Max(value = 100, message = "must be less than or equal to 100")
    private int pageSize = 20;

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
}
