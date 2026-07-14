package com.dailyforge.modules.plan.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.modules.plan.application.service.CycleTemplateActivationApplicationService;
import com.dailyforge.modules.plan.application.service.CycleTemplateAiApplicationService;
import com.dailyforge.modules.plan.application.service.CycleTemplateCommandApplicationService;
import com.dailyforge.modules.plan.application.service.CycleTemplateQueryApplicationService;
import com.dailyforge.modules.plan.interfaces.dto.ActivateCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.AiGenerateDraftCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.CopyCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.CreateDraftCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.UpdateCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.UpdateDraftCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.vo.ActivateCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CopyCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CreateDraftCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CurrentActiveCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CycleTemplateDetailResponse;
import com.dailyforge.modules.plan.interfaces.vo.DeleteCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.DraftCycleTemplateListResponse;
import com.dailyforge.modules.plan.interfaces.vo.FormalCycleTemplateListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/cycle-templates")
@Tag(name = "Cycle Template")
@SecurityRequirement(name = "bearerAuth")
public class CycleTemplateController {

    private static final Logger log = LoggerFactory.getLogger(CycleTemplateController.class);

    private final CycleTemplateQueryApplicationService cycleTemplateQueryApplicationService;
    private final CycleTemplateCommandApplicationService cycleTemplateCommandApplicationService;
    private final CycleTemplateActivationApplicationService cycleTemplateActivationApplicationService;
    private final CycleTemplateAiApplicationService cycleTemplateAiApplicationService;

    public CycleTemplateController(
            CycleTemplateQueryApplicationService cycleTemplateQueryApplicationService,
            CycleTemplateCommandApplicationService cycleTemplateCommandApplicationService,
            CycleTemplateActivationApplicationService cycleTemplateActivationApplicationService,
            CycleTemplateAiApplicationService cycleTemplateAiApplicationService) {
        this.cycleTemplateQueryApplicationService = cycleTemplateQueryApplicationService;
        this.cycleTemplateCommandApplicationService = cycleTemplateCommandApplicationService;
        this.cycleTemplateActivationApplicationService = cycleTemplateActivationApplicationService;
        this.cycleTemplateAiApplicationService = cycleTemplateAiApplicationService;
    }

    /**
     * Get active and inactive templates.
     */
    @GetMapping("/formal")
    @Operation(summary = "Get formal cycle templates")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Formal templates loaded",
                    content = @Content(schema = @Schema(implementation = FormalCycleTemplateListResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<FormalCycleTemplateListResponse> getFormalTemplates() {
        log.debug("CycleTemplateController getFormalTemplates entered");
        return ApiResponse.success(cycleTemplateQueryApplicationService.getFormalTemplates());
    }

    /**
     * Get draft templates.
     */
    @GetMapping("/drafts")
    @Operation(summary = "Get draft cycle templates")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Draft templates loaded",
                    content = @Content(schema = @Schema(implementation = DraftCycleTemplateListResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<DraftCycleTemplateListResponse> getDraftTemplates() {
        log.debug("CycleTemplateController getDraftTemplates entered");
        return ApiResponse.success(cycleTemplateQueryApplicationService.getDraftTemplates());
    }

    /**
     * Get one template detail.
     */
    @GetMapping("/{templateId}")
    @Operation(summary = "Get cycle template detail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Template detail loaded",
                    content = @Content(schema = @Schema(implementation = CycleTemplateDetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ApiResponse<CycleTemplateDetailResponse> getTemplateDetail(@PathVariable Long templateId) {
        log.debug("CycleTemplateController getTemplateDetail entered. templateId={}", templateId);
        return ApiResponse.success(cycleTemplateQueryApplicationService.getTemplateDetail(templateId));
    }

    /**
     * Create a new draft template.
     */
    @PostMapping("/drafts")
    @Operation(summary = "Create manual draft cycle template")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Draft created",
                    content = @Content(schema = @Schema(implementation = CreateDraftCycleTemplateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<CreateDraftCycleTemplateResponse> createDraft(
            @Valid @RequestBody CreateDraftCycleTemplateRequest request) {
        log.debug("CycleTemplateController createDraft entered. templateName={}", request.templateName());
        return ApiResponse.success(cycleTemplateCommandApplicationService.createDraft(request));
    }

    /**
     * Placeholder AI draft generation endpoint.
     */
    @PostMapping("/drafts/ai-generate")
    @Operation(summary = "Generate draft cycle template by AI")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "Not implemented"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<Void> generateDraftByAi(@Valid @RequestBody AiGenerateDraftCycleTemplateRequest request) {
        log.debug("CycleTemplateController generateDraftByAi entered");
        cycleTemplateAiApplicationService.generateDraft(request);
        return ApiResponse.success();
    }

    /**
     * Update a draft template.
     */
    @PutMapping("/drafts/{templateId}")
    @Operation(summary = "Update draft cycle template")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Draft updated",
                    content = @Content(schema = @Schema(implementation = CreateDraftCycleTemplateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ApiResponse<CreateDraftCycleTemplateResponse> updateDraft(
            @PathVariable Long templateId,
            @Valid @RequestBody UpdateDraftCycleTemplateRequest request) {
        log.debug("CycleTemplateController updateDraft entered. templateId={}", templateId);
        return ApiResponse.success(cycleTemplateCommandApplicationService.updateDraft(templateId, request));
    }

    /**
     * Update a formal template.
     */
    @PutMapping("/{templateId}")
    @Operation(summary = "Update formal cycle template")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Formal template updated",
                    content = @Content(schema = @Schema(implementation = CreateDraftCycleTemplateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Edit forbidden")
    })
    public ApiResponse<CreateDraftCycleTemplateResponse> updateFormal(
            @PathVariable Long templateId,
            @Valid @RequestBody UpdateCycleTemplateRequest request) {
        log.debug("CycleTemplateController updateFormal entered. templateId={}", templateId);
        return ApiResponse.success(cycleTemplateCommandApplicationService.updateFormal(templateId, request));
    }

    /**
     * Copy a template into a new draft.
     */
    @PostMapping("/{templateId}/copy")
    @Operation(summary = "Copy cycle template to draft")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Template copied",
                    content = @Content(schema = @Schema(implementation = CopyCycleTemplateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ApiResponse<CopyCycleTemplateResponse> copyTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CopyCycleTemplateRequest request) {
        log.debug("CycleTemplateController copyTemplate entered. templateId={}", templateId);
        return ApiResponse.success(cycleTemplateCommandApplicationService.copyTemplate(templateId, request));
    }

    /**
     * Activate one draft or inactive template.
     */
    @PostMapping("/{templateId}/activate")
    @Operation(summary = "Activate cycle template")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Template activated",
                    content = @Content(schema = @Schema(implementation = ActivateCycleTemplateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Activation invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Switch confirm required")
    })
    public ApiResponse<ActivateCycleTemplateResponse> activateTemplate(
            @PathVariable Long templateId,
            @RequestBody(required = false) ActivateCycleTemplateRequest request) {
        log.debug("CycleTemplateController activateTemplate entered. templateId={}", templateId);
        ActivateCycleTemplateRequest effectiveRequest =
                request == null ? new ActivateCycleTemplateRequest(false) : request;
        return ApiResponse.success(cycleTemplateActivationApplicationService.activateTemplate(templateId, effectiveRequest));
    }

    /**
     * Return current active template summary.
     */
    @GetMapping("/active/current")
    @Operation(summary = "Get current active cycle template")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Current active template loaded",
                    content = @Content(schema = @Schema(implementation = CurrentActiveCycleTemplateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Active template not found")
    })
    public ApiResponse<CurrentActiveCycleTemplateResponse> getCurrentActiveTemplate() {
        log.debug("CycleTemplateController getCurrentActiveTemplate entered");
        return ApiResponse.success(cycleTemplateQueryApplicationService.getCurrentActiveTemplate());
    }

    /**
     * Soft delete one draft or inactive template.
     */
    @DeleteMapping("/{templateId}")
    @Operation(summary = "Delete cycle template")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Template deleted",
                    content = @Content(schema = @Schema(implementation = DeleteCycleTemplateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Delete forbidden")
    })
    public ApiResponse<DeleteCycleTemplateResponse> deleteTemplate(@PathVariable Long templateId) {
        log.debug("CycleTemplateController deleteTemplate entered. templateId={}", templateId);
        return ApiResponse.success(cycleTemplateCommandApplicationService.deleteTemplate(templateId));
    }
}
