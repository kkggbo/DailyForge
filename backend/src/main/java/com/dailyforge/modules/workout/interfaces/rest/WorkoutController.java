package com.dailyforge.modules.workout.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.modules.workout.application.service.WorkoutApplicationService;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutRecentSessionQuery;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionSaveRequest;
import com.dailyforge.modules.workout.interfaces.vo.CompleteWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.InitializeCurrentWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.RestartWorkoutCycleResponse;
import com.dailyforge.modules.workout.interfaces.vo.SaveWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutDayDetailResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutRecentSessionPageResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutSessionDetailResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutWorkspaceContextResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/workouts")
@Tag(name = "Workout")
@SecurityRequirement(name = "bearerAuth")
public class WorkoutController {

    private static final Logger log = LoggerFactory.getLogger(WorkoutController.class);

    private final WorkoutApplicationService workoutApplicationService;

    public WorkoutController(WorkoutApplicationService workoutApplicationService) {
        this.workoutApplicationService = workoutApplicationService;
    }

    @GetMapping("/context")
    @Operation(summary = "Get workout workspace context")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Workout context loaded",
                    content = @Content(schema = @Schema(implementation = WorkoutWorkspaceContextResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<WorkoutWorkspaceContextResponse> getContext() {
        return ApiResponse.success(workoutApplicationService.getWorkspaceContext());
    }

    @PostMapping("/current-day/session")
    @Operation(summary = "Initialize current workout day session")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Current session initialized or restored",
                    content = @Content(schema = @Schema(implementation = InitializeCurrentWorkoutSessionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cycle unavailable")
    })
    public ApiResponse<InitializeCurrentWorkoutSessionResponse> initializeCurrentDaySession() {
        return ApiResponse.success(workoutApplicationService.initializeCurrentDaySession());
    }

    @GetMapping("/days/{dayIndex}")
    @Operation(summary = "Get one day in the active workout cycle")
    public ApiResponse<WorkoutDayDetailResponse> getDay(@PathVariable @Min(1) Integer dayIndex) {
        return ApiResponse.success(workoutApplicationService.getDayDetail(dayIndex));
    }

    @PutMapping("/sessions/{sessionId}")
    @Operation(summary = "Save an in-progress workout session")
    public ApiResponse<SaveWorkoutSessionResponse> saveSession(
            @PathVariable @Min(1) Long sessionId,
            @Valid @RequestBody WorkoutSessionSaveRequest request) {
        log.debug("WorkoutController saveSession entered. sessionId={}", sessionId);
        return ApiResponse.success(workoutApplicationService.saveSession(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @Operation(summary = "Complete a workout session and advance the cycle")
    public ApiResponse<CompleteWorkoutSessionResponse> completeSession(
            @PathVariable @Min(1) Long sessionId,
            @Valid @RequestBody WorkoutSessionSaveRequest request) {
        log.debug("WorkoutController completeSession entered. sessionId={}", sessionId);
        return ApiResponse.success(workoutApplicationService.completeSession(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get workout session detail")
    public ApiResponse<WorkoutSessionDetailResponse> getSessionDetail(@PathVariable @Min(1) Long sessionId) {
        return ApiResponse.success(workoutApplicationService.getSessionDetail(sessionId));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent workout sessions")
    public ApiResponse<WorkoutRecentSessionPageResponse> getRecentSessions(
            @Valid @ModelAttribute WorkoutRecentSessionQuery query) {
        return ApiResponse.success(workoutApplicationService.getRecentSessions(query));
    }

    @PostMapping("/cycles/current/restart")
    @Operation(summary = "Restart the completed current workout cycle")
    public ApiResponse<RestartWorkoutCycleResponse> restartCurrentCycle() {
        return ApiResponse.success(workoutApplicationService.restartCurrentCycle());
    }

    @PostMapping("/cycles/current/ai-analysis")
    @Operation(summary = "Request current cycle AI analysis")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Current cycle must be completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "501", description = "AI analysis not implemented")
    })
    public ApiResponse<Void> requestCurrentCycleAiAnalysis() {
        workoutApplicationService.requestCurrentCycleAiAnalysis();
        return ApiResponse.success();
    }
}
