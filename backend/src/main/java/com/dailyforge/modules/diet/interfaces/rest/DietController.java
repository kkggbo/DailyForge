package com.dailyforge.modules.diet.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.modules.diet.application.service.DietFoodService;
import com.dailyforge.modules.diet.application.service.DietLogService;
import com.dailyforge.modules.diet.application.service.DietQueryService;
import com.dailyforge.modules.diet.application.service.DietStatsService;
import com.dailyforge.modules.diet.application.service.DietTargetService;
import com.dailyforge.modules.diet.interfaces.dto.CreateDietLogRequest;
import com.dailyforge.modules.diet.interfaces.dto.OverrideTargetRequest;
import com.dailyforge.modules.diet.interfaces.dto.UploadFoodRequest;
import com.dailyforge.modules.diet.interfaces.vo.DietLogItemVO;
import com.dailyforge.modules.diet.interfaces.vo.DietStatsVO;
import com.dailyforge.modules.diet.interfaces.vo.DietSummaryVO;
import com.dailyforge.modules.diet.interfaces.vo.DietTargetVO;
import com.dailyforge.modules.diet.interfaces.vo.FoodItemVO;
import com.dailyforge.modules.diet.interfaces.vo.FoodSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/diet")
@Tag(name = "Diet")
@SecurityRequirement(name = "bearerAuth")
public class DietController {

    private final DietQueryService dietQueryService;
    private final DietLogService dietLogService;
    private final DietFoodService dietFoodService;
    private final DietTargetService dietTargetService;
    private final DietStatsService dietStatsService;

    public DietController(
            DietQueryService dietQueryService,
            DietLogService dietLogService,
            DietFoodService dietFoodService,
            DietTargetService dietTargetService,
            DietStatsService dietStatsService) {
        this.dietQueryService = dietQueryService;
        this.dietLogService = dietLogService;
        this.dietFoodService = dietFoodService;
        this.dietTargetService = dietTargetService;
        this.dietStatsService = dietStatsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "每日饮食总结")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = DietSummaryVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<DietSummaryVO> getDailySummary(@RequestParam(required = false) String date) {
        LocalDate day = DietQueryService.resolveDate(date);
        return ApiResponse.success(dietQueryService.getDailySummary(day));
    }

    @PostMapping("/logs")
    @Operation(summary = "添加饮食记录")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = DietLogItemVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数非法"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "食物不存在")
    })
    public ApiResponse<DietLogItemVO> addLog(@Valid @RequestBody CreateDietLogRequest request) {
        return ApiResponse.success(dietLogService.addLog(request));
    }

    @PutMapping("/logs/{logId}")
    @Operation(summary = "修改饮食记录")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = DietLogItemVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数非法"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "记录或食物不存在")
    })
    public ApiResponse<DietLogItemVO> updateLog(
            @PathVariable @Min(1) Long logId,
            @Valid @RequestBody CreateDietLogRequest request) {
        return ApiResponse.success(dietLogService.updateLog(logId, request));
    }

    @DeleteMapping("/logs/{logId}")
    @Operation(summary = "删除饮食记录")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "记录不存在")
    })
    public ApiResponse<Void> deleteLog(@PathVariable @Min(1) Long logId) {
        dietLogService.deleteLog(logId);
        return ApiResponse.success();
    }

    @GetMapping("/foods")
    @Operation(summary = "搜索食物")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = FoodSearchVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<FoodSearchVO> searchFoods(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer pageSize) {
        return ApiResponse.success(dietFoodService.searchFoods(keyword, filter, page, pageSize));
    }

    @GetMapping("/foods/{foodId}")
    @Operation(summary = "食物详情")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = FoodItemVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "食物不存在")
    })
    public ApiResponse<FoodItemVO> getFoodDetail(@PathVariable @Min(1) Long foodId) {
        return ApiResponse.success(dietFoodService.getFoodDetail(foodId));
    }

    @PostMapping("/foods")
    @Operation(summary = "上传食物（全局共享）")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = FoodItemVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "上传非法"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<FoodItemVO> uploadFood(@Valid @RequestBody UploadFoodRequest request) {
        return ApiResponse.success(dietFoodService.uploadFood(request));
    }

    @PostMapping("/favorites/{foodId}")
    @Operation(summary = "收藏食物")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "食物不存在")
    })
    public ApiResponse<Void> addFavorite(@PathVariable @Min(1) Long foodId) {
        dietFoodService.addFavorite(foodId);
        return ApiResponse.success();
    }

    @DeleteMapping("/favorites/{foodId}")
    @Operation(summary = "取消收藏食物")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<Void> removeFavorite(@PathVariable @Min(1) Long foodId) {
        dietFoodService.removeFavorite(foodId);
        return ApiResponse.success();
    }

    @GetMapping("/targets")
    @Operation(summary = "查询每日目标")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = DietTargetVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<DietTargetVO> getTarget() {
        return ApiResponse.success(dietTargetService.getTarget());
    }

    @PutMapping("/targets")
    @Operation(summary = "覆盖/清除每日目标")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = DietTargetVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "目标值非法"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<DietTargetVO> overrideTarget(@Valid @RequestBody OverrideTargetRequest request) {
        return ApiResponse.success(dietTargetService.overrideTarget(request));
    }

    @GetMapping("/stats")
    @Operation(summary = "摄入统计")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "ok",
                    content = @Content(schema = @Schema(implementation = DietStatsVO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<DietStatsVO> getStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.success(dietStatsService.getStats(parse(from), parse(to)));
    }

    private LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }
}
