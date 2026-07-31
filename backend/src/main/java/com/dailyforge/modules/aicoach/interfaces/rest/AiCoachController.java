package com.dailyforge.modules.aicoach.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.modules.aicoach.application.service.AiCoachApplicationService;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.AiAsyncTaskAcceptedResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiCoachCapabilitiesResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiTaskDetailResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryTaskResultResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/ai-coach")
@Tag(name = "AI Coach")
@SecurityRequirement(name = "bearerAuth")
public class AiCoachController {

    private final AiCoachApplicationService aiCoachApplicationService;

    public AiCoachController(AiCoachApplicationService aiCoachApplicationService) {
        this.aiCoachApplicationService = aiCoachApplicationService;
    }

    @GetMapping("/capabilities")
    @Operation(summary = "Get AI coach capabilities")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Capabilities loaded",
                    content = @Content(schema = @Schema(implementation = AiCoachCapabilitiesResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<AiCoachCapabilitiesResponse> getCapabilities() {
        return ApiResponse.success(aiCoachApplicationService.getCapabilities());
    }

    @PostMapping("/template-generations")
    @Operation(summary = "Submit one AI template generation task")
    public ApiResponse<AiAsyncTaskAcceptedResponse> submitTemplateGeneration(
            @Valid @RequestBody TemplateGenerationRequest request) {
        return ApiResponse.success(aiCoachApplicationService.submitTemplateGeneration(request));
    }

    @GetMapping("/template-generations/{taskId}")
    @Operation(summary = "Query one AI template generation task")
    public ApiResponse<AiTaskDetailResponse<TemplateGenerationTaskResultResponse>> getTemplateGeneration(
            @PathVariable @Min(1) Long taskId) {
        return ApiResponse.success(aiCoachApplicationService.getTemplateGeneration(taskId));
    }

    @PostMapping("/cycle-summaries")
    @Operation(summary = "Submit one AI cycle summary task")
    public ApiResponse<AiAsyncTaskAcceptedResponse> submitCycleSummary(
            @Valid @RequestBody CycleSummaryRequest request) {
        return ApiResponse.success(aiCoachApplicationService.submitCycleSummary(request));
    }

    @GetMapping("/cycle-summaries/{taskId}")
    @Operation(summary = "Query one AI cycle summary task")
    public ApiResponse<AiTaskDetailResponse<CycleSummaryTaskResultResponse>> getCycleSummary(
            @PathVariable @Min(1) Long taskId) {
        return ApiResponse.success(aiCoachApplicationService.getCycleSummary(taskId));
    }
}