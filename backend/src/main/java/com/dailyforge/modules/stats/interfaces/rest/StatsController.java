package com.dailyforge.modules.stats.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.modules.stats.application.service.StatsQueryApplicationService;
import com.dailyforge.modules.stats.interfaces.vo.BodyMetricSeriesResponse;
import com.dailyforge.modules.stats.interfaces.vo.StatsExerciseDetailResponse;
import com.dailyforge.modules.stats.interfaces.vo.StatsSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/stats")
@Tag(name = "Stats")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsQueryApplicationService statsQueryApplicationService;

    public StatsController(StatsQueryApplicationService statsQueryApplicationService) {
        this.statsQueryApplicationService = statsQueryApplicationService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get overall training summary and per-exercise aggregates")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Stats summary loaded",
                    content = @Content(schema = @Schema(implementation = StatsSummaryResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<StatsSummaryResponse> getSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.success(statsQueryApplicationService.getSummary(from, to));
    }

    @GetMapping("/exercise/{exerciseId}")
    @Operation(summary = "Get one exercise aggregate detail with progression")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Exercise detail loaded",
                    content = @Content(schema = @Schema(implementation = StatsExerciseDetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Exercise not found")
    })
    public ApiResponse<StatsExerciseDetailResponse> getExerciseDetail(
            @PathVariable @Min(1) Long exerciseId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.success(statsQueryApplicationService.getExerciseDetail(exerciseId, from, to));
    }

    @GetMapping("/body-metrics")
    @Operation(summary = "Get body metric time series")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Body metric series loaded",
                    content = @Content(schema = @Schema(implementation = BodyMetricSeriesResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid metric"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<BodyMetricSeriesResponse> getBodyMetrics(
            @RequestParam String metric,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.success(statsQueryApplicationService.getBodyMetrics(metric, from, to));
    }
}
