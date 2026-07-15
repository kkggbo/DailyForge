package com.dailyforge.modules.exercise.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.exercise.application.service.ExerciseQueryApplicationService;
import com.dailyforge.modules.exercise.interfaces.dto.ExerciseSystemListQuery;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemDetailResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemListResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/exercises")
@Tag(name = "Exercise")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseController {

    private static final Logger log = LoggerFactory.getLogger(ExerciseController.class);

    private final ExerciseQueryApplicationService exerciseQueryApplicationService;

    public ExerciseController(ExerciseQueryApplicationService exerciseQueryApplicationService) {
        this.exerciseQueryApplicationService = exerciseQueryApplicationService;
    }

    /**
     * Query one page of active system exercises for template selection and browsing.
     */
    @GetMapping("/system")
    @Operation(summary = "Get system exercise list")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "System exercises loaded",
                    content = @Content(schema = @Schema(implementation = ExerciseSystemListResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<ExerciseSystemListResponse> getSystemExercises(@Valid @ModelAttribute ExerciseSystemListQuery query) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        log.debug("ExerciseController getSystemExercises entered. userId={}, page={}, pageSize={}, hasFilters={}",
                userId, query.getPage(), query.getPageSize(), query.hasFilters());
        return ApiResponse.success(exerciseQueryApplicationService.getSystemExercises(query, userId));
    }

    /**
     * Query one active system exercise detail for template display and training guidance.
     */
    @GetMapping("/system/{exerciseId}")
    @Operation(summary = "Get system exercise detail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "System exercise detail loaded",
                    content = @Content(schema = @Schema(implementation = ExerciseSystemDetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    public ApiResponse<ExerciseSystemDetailResponse> getSystemExerciseDetail(@PathVariable Long exerciseId) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        log.debug("ExerciseController getSystemExerciseDetail entered. userId={}, exerciseId={}", userId, exerciseId);
        return ApiResponse.success(exerciseQueryApplicationService.getSystemExerciseDetail(exerciseId, userId));
    }
}
