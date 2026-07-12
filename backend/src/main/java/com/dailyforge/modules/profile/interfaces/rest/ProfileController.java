package com.dailyforge.modules.profile.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.modules.profile.application.service.BodyMetricApplicationService;
import com.dailyforge.modules.profile.application.service.ProfileApplicationService;
import com.dailyforge.modules.profile.interfaces.dto.BodyMetricPageQuery;
import com.dailyforge.modules.profile.interfaces.dto.CreateBodyMetricRequest;
import com.dailyforge.modules.profile.interfaces.dto.UpdateProfileBasicRequest;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricLogItemResponse;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricSnapshotResponse;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricsPageResponse;
import com.dailyforge.modules.profile.interfaces.vo.DeleteLatestBodyMetricResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileBasicResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileBasicUpdateResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileCompletionSummaryResponse;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/profile")
@Tag(name = "Profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileApplicationService profileApplicationService;
    private final BodyMetricApplicationService bodyMetricApplicationService;

    public ProfileController(
            ProfileApplicationService profileApplicationService,
            BodyMetricApplicationService bodyMetricApplicationService) {
        this.profileApplicationService = profileApplicationService;
        this.bodyMetricApplicationService = bodyMetricApplicationService;
    }

    /**
     * Get the current user's basic profile summary.
     */
    @GetMapping("/basic")
    @Operation(summary = "Get current user basic profile")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile loaded",
                    content = @Content(schema = @Schema(implementation = ProfileBasicResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<ProfileBasicResponse> getBasicProfile() {
        log.debug("ProfileController getBasicProfile entered");
        return ApiResponse.success(profileApplicationService.getBasicProfile());
    }

    /**
     * Partially update the current user's basic profile.
     */
    @PutMapping("/basic")
    @Operation(summary = "Update current user basic profile")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = ProfileBasicUpdateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<ProfileBasicUpdateResponse> updateBasicProfile(
            @Valid @RequestBody UpdateProfileBasicRequest request) {
        log.debug("ProfileController updateBasicProfile entered");
        return ApiResponse.success(profileApplicationService.updateBasicProfile(request));
    }

    /**
     * Get the current user's body metric snapshot.
     */
    @GetMapping("/body-metrics/current")
    @Operation(summary = "Get current body metric snapshot")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Snapshot loaded",
                    content = @Content(schema = @Schema(implementation = BodyMetricSnapshotResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<BodyMetricSnapshotResponse> getCurrentSnapshot() {
        log.debug("ProfileController getCurrentSnapshot entered");
        return ApiResponse.success(bodyMetricApplicationService.getCurrentSnapshot());
    }

    /**
     * Get paged body metric history for the current user.
     */
    @GetMapping("/body-metrics")
    @Operation(summary = "Get body metric history")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "History loaded",
                    content = @Content(schema = @Schema(implementation = BodyMetricsPageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<BodyMetricsPageResponse> getBodyMetrics(@Valid @ModelAttribute BodyMetricPageQuery query) {
        log.debug("ProfileController getBodyMetrics entered. page={}, pageSize={}", query.getPage(), query.getPageSize());
        return ApiResponse.success(bodyMetricApplicationService.getBodyMetrics(query));
    }

    /**
     * Create one body metric history row for the current user.
     */
    @PostMapping("/body-metrics")
    @Operation(summary = "Create body metric history record")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Record created",
                    content = @Content(schema = @Schema(implementation = BodyMetricLogItemResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<BodyMetricLogItemResponse> createBodyMetric(
            @Valid @RequestBody CreateBodyMetricRequest request) {
        log.debug("ProfileController createBodyMetric entered. recordDate={}", request.recordDate());
        return ApiResponse.success(bodyMetricApplicationService.createBodyMetric(request));
    }

    /**
     * Delete only the latest body metric record for the current user.
     */
    @DeleteMapping("/body-metrics/latest")
    @Operation(summary = "Delete latest body metric history record")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Record deleted",
                    content = @Content(schema = @Schema(implementation = DeleteLatestBodyMetricResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Record not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Latest record already deleted")
    })
    public ApiResponse<DeleteLatestBodyMetricResponse> deleteLatestBodyMetric() {
        log.debug("ProfileController deleteLatestBodyMetric entered");
        return ApiResponse.success(bodyMetricApplicationService.deleteLatestBodyMetric());
    }

    /**
     * Get profile completion and AI readiness summary for the current user.
     */
    @GetMapping("/completion-summary")
    @Operation(summary = "Get profile completion summary")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Summary loaded",
                    content = @Content(schema = @Schema(implementation = ProfileCompletionSummaryResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<ProfileCompletionSummaryResponse> getCompletionSummary() {
        log.debug("ProfileController getCompletionSummary entered");
        return ApiResponse.success(profileApplicationService.getCompletionSummary());
    }
}
