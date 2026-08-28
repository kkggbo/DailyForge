package com.dailyforge.modules.stats.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.plan.application.service.PlanUserSupportService;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.BodyMetricLogMapper;
import com.dailyforge.modules.stats.application.service.StatsAggregationService.ExerciseAggregate;
import com.dailyforge.modules.stats.interfaces.vo.BodyMetricSeriesResponse;
import com.dailyforge.modules.stats.interfaces.vo.BodyMetricSeriesResponse.BodyMetricPoint;
import com.dailyforge.modules.stats.interfaces.vo.StatsExerciseAggregateResponse;
import com.dailyforge.modules.stats.interfaces.vo.StatsExerciseDetailResponse;
import com.dailyforge.modules.stats.interfaces.vo.StatsOverallResponse;
import com.dailyforge.modules.stats.interfaces.vo.StatsProgressionPointResponse;
import com.dailyforge.modules.stats.interfaces.vo.StatsSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatsQueryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(StatsQueryApplicationService.class);

    private static final String WEIGHT = "weight_kg";
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);

    private static final Map<String, String> BODY_METRIC_UNITS = Map.ofEntries(
            Map.entry("weight_kg", "kg"),
            Map.entry("body_fat_percent", "%"),
            Map.entry("bmi", ""),
            Map.entry("skeletal_muscle_percent", "%"),
            Map.entry("body_water_percent", "%"),
            Map.entry("basal_metabolic_rate_kcal", "kcal"),
            Map.entry("waist_cm", "cm"),
            Map.entry("hip_cm", "cm"),
            Map.entry("waist_hip_ratio", ""),
            Map.entry("body_age", "level"));

    private final PlanUserSupportService planUserSupportService;
    private final StatsAggregationService statsAggregationService;
    private final BodyMetricLogMapper bodyMetricLogMapper;
    private final SystemExerciseLookupService systemExerciseLookupService;

    public StatsQueryApplicationService(
            PlanUserSupportService planUserSupportService,
            StatsAggregationService statsAggregationService,
            BodyMetricLogMapper bodyMetricLogMapper,
            SystemExerciseLookupService systemExerciseLookupService) {
        this.planUserSupportService = planUserSupportService;
        this.statsAggregationService = statsAggregationService;
        this.bodyMetricLogMapper = bodyMetricLogMapper;
        this.systemExerciseLookupService = systemExerciseLookupService;
    }

    public StatsSummaryResponse getSummary(String from, String to) {
        Long userId = planUserSupportService.requireActiveUserId();
        LocalDateTime fromTime = parseDateTime(from, false);
        LocalDateTime toTime = parseDateTime(to, true);

        StatsAggregationService.AggregatedWorkout workout =
                statsAggregationService.aggregate(userId, fromTime, toTime);

        List<ExerciseAggregate> aggregates = new ArrayList<>(workout.exercises().values());
        Map<Long, SystemExerciseLookupResult> lookups =
                systemExerciseLookupService.loadActiveSystemExercisesByIds(
                        aggregates.stream().map(a -> a.exerciseId).toList());

        List<StatsExerciseAggregateResponse> exercises = aggregates.stream()
                .filter(a -> a.getAppearanceCount() > 0)
                .map(a -> toAggregateResponse(a, lookupName(lookups, a), lookupExerciseType(lookups, a)))
                .sorted(Comparator.comparingInt(StatsExerciseAggregateResponse::appearanceCount).reversed()
                        .thenComparing(StatsExerciseAggregateResponse::exerciseId))
                .toList();

        StatsOverallResponse overall = buildOverall(workout.sessionCountWithData(), exercises);
        log.debug("Stats summary computed. userId={}, sessionCount={}, exerciseCount={}",
                userId, overall.sessionCount(), exercises.size());
        return new StatsSummaryResponse(overall, exercises);
    }

    public StatsExerciseDetailResponse getExerciseDetail(Long exerciseId, String from, String to) {
        Long userId = planUserSupportService.requireActiveUserId();
        if (exerciseId == null || exerciseId < 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
        LocalDateTime fromTime = parseDateTime(from, false);
        LocalDateTime toTime = parseDateTime(to, true);

        StatsAggregationService.AggregatedWorkout workout =
                statsAggregationService.aggregate(userId, fromTime, toTime);
        ExerciseAggregate agg = workout.exercises().get(exerciseId);

        Map<Long, SystemExerciseLookupResult> lookups =
                systemExerciseLookupService.loadActiveSystemExercisesByIds(List.of(exerciseId));
        SystemExerciseLookupResult lookup = lookups.get(exerciseId);
        if (agg == null && lookup == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        String name = agg == null ? null : agg.nameSnapshot;
        if (!StringUtils.hasText(name) && lookup != null) {
            name = lookup.name();
        }
        String exerciseType = lookup == null ? null : lookup.exerciseType();
        boolean strength = agg != null && "set_based".equals(agg.structureType);

        List<StatsProgressionPointResponse> progression = buildProgression(agg);

        StatsExerciseAggregateResponse base = agg == null
                ? emptyAggregate(exerciseId, name, exerciseType)
                : toAggregateResponse(agg, name, exerciseType);

        log.debug("Stats exercise detail computed. userId={}, exerciseId={}, appearanceCount={}",
                userId, exerciseId, base.appearanceCount());
        return new StatsExerciseDetailResponse(
                base.exerciseId(),
                base.name(),
                base.exerciseType(),
                base.structureType(),
                base.appearanceCount(),
                base.setCount(),
                base.repCount(),
                base.totalVolumeKg(),
                base.avgWeightKg(),
                base.maxWeightKg(),
                base.avgReps(),
                base.totalDurationSeconds(),
                base.totalDistanceKm(),
                base.avgSpeedKmh(),
                base.funCopy(),
                progression);
    }

    public BodyMetricSeriesResponse getBodyMetrics(String metric, String from, String to) {
        Long userId = planUserSupportService.requireActiveUserId();
        if (!BODY_METRIC_UNITS.containsKey(metric)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);

        List<BodyMetricLogEntity> rows =
                bodyMetricLogMapper.selectActiveRecordsForStats(userId, fromDate, toDate);

        List<BodyMetricPoint> points = new ArrayList<>();
        LocalDate lastDate = null;
        for (BodyMetricLogEntity row : rows) {
            BigDecimal value = readMetric(row, metric);
            if (row.getRecordDate() == null) {
                continue;
            }
            // Rows are ordered record_date ASC, id DESC; first row per date is the newest.
            if (lastDate != null && row.getRecordDate().equals(lastDate)) {
                continue;
            }
            lastDate = row.getRecordDate();
            if (value == null) {
                continue;
            }
            points.add(new BodyMetricPoint(row.getRecordDate().toString(), value));
        }

        log.debug("Stats body metrics computed. userId={}, metric={}, pointCount={}", userId, metric, points.size());
        return new BodyMetricSeriesResponse(metric, BODY_METRIC_UNITS.get(metric), points);
    }

    private StatsOverallResponse buildOverall(int sessionCount, List<StatsExerciseAggregateResponse> exercises) {
        int totalSets = 0;
        int totalReps = 0;
        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal totalDurationSeconds = BigDecimal.ZERO;
        for (StatsExerciseAggregateResponse ex : exercises) {
            if (ex.setCount() != null) {
                totalSets += ex.setCount();
            }
            if (ex.repCount() != null) {
                totalReps += ex.repCount();
            }
            if (ex.totalVolumeKg() != null) {
                totalVolume = totalVolume.add(ex.totalVolumeKg());
            }
            if (ex.totalDistanceKm() != null) {
                totalDistance = totalDistance.add(ex.totalDistanceKm());
            }
            if (ex.totalDurationSeconds() != null) {
                totalDurationSeconds = totalDurationSeconds.add(ex.totalDurationSeconds());
            }
        }
        BigDecimal totalDurationMinutes = totalDurationSeconds.divide(SIXTY, 2, java.math.RoundingMode.HALF_UP);
        String overviewCopy = StatsFunCopyGenerator.buildOverviewCopy(
                sessionCount, totalVolume, totalDistance);
        return new StatsOverallResponse(
                sessionCount,
                totalSets,
                totalReps,
                totalVolume,
                totalDistance,
                totalDurationMinutes,
                overviewCopy);
    }

    private StatsExerciseAggregateResponse toAggregateResponse(
            ExerciseAggregate agg,
            String name,
            String exerciseType) {
        boolean strength = "set_based".equals(agg.structureType);
        Integer setCount = strength ? agg.setCount : null;
        Integer repCount = strength ? (agg.repsSum == null ? 0 : agg.repsSum.intValue()) : null;
        BigDecimal volume = strength ? agg.totalVolumeKg : null;
        BigDecimal avgWeight = strength ? agg.getAvgWeightKg() : null;
        BigDecimal maxWeight = strength ? agg.maxWeightKg : null;
        BigDecimal avgReps = strength ? agg.getAvgReps() : null;
        // Duration applies to both strength and cardio when the exercise has an actual duration.
        BigDecimal duration = agg.totalDurationSeconds;
        BigDecimal distance = strength ? null : agg.totalDistanceKm;
        BigDecimal speed = strength ? null : agg.getAvgSpeedKmh();

        String funCopy = StatsFunCopyGenerator.buildExerciseFunCopy(
                name,
                strength,
                strength ? (agg.repsSum == null ? 0 : agg.repsSum.intValue()) : 0,
                strength ? agg.totalVolumeKg : null,
                strength ? null : agg.totalDistanceKm);

        return new StatsExerciseAggregateResponse(
                agg.exerciseId,
                name,
                exerciseType,
                agg.structureType,
                agg.getAppearanceCount(),
                setCount,
                repCount,
                volume,
                avgWeight,
                maxWeight,
                avgReps,
                duration,
                distance,
                speed,
                funCopy);
    }

    private StatsExerciseAggregateResponse emptyAggregate(
            Long exerciseId,
            String name,
            String exerciseType) {
        return new StatsExerciseAggregateResponse(
                exerciseId,
                name,
                exerciseType,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                StatsFunCopyGenerator.buildExerciseFunCopy(name, true, 0, null, null));
    }

    private List<StatsProgressionPointResponse> buildProgression(ExerciseAggregate agg) {
        if (agg == null || agg.daily.isEmpty()) {
            return List.of();
        }
        boolean strength = "set_based".equals(agg.structureType);
        List<StatsProgressionPointResponse> points = new ArrayList<>();
        agg.daily.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    StatsAggregationService.DailyAggregate d = e.getValue();
                    points.add(new StatsProgressionPointResponse(
                            e.getKey().toString(),
                            strength ? d.maxWeightKg : null,
                            strength ? d.maxReps : null,
                            strength ? d.totalVolumeKg : null,
                            strength ? null : d.totalDurationSeconds,
                            strength ? null : d.totalDistanceKm));
                });
        return points;
    }

    private String lookupName(Map<Long, SystemExerciseLookupResult> lookups, ExerciseAggregate agg) {
        if (StringUtils.hasText(agg.nameSnapshot)) {
            return agg.nameSnapshot;
        }
        SystemExerciseLookupResult lookup = lookups.get(agg.exerciseId);
        return lookup == null ? null : lookup.name();
    }

    private String lookupExerciseType(Map<Long, SystemExerciseLookupResult> lookups, ExerciseAggregate agg) {
        SystemExerciseLookupResult lookup = lookups.get(agg.exerciseId);
        return lookup == null ? null : lookup.exerciseType();
    }

    private BigDecimal readMetric(BodyMetricLogEntity row, String metric) {
        return switch (metric) {
            case WEIGHT -> row.getWeightKg();
            case "body_fat_percent" -> row.getBodyFatPercent();
            case "bmi" -> row.getBmi();
            case "skeletal_muscle_percent" -> row.getSkeletalMusclePercent();
            case "body_water_percent" -> row.getBodyWaterPercent();
            case "basal_metabolic_rate_kcal" -> row.getBasalMetabolicRateKcal();
            case "waist_cm" -> row.getWaistCm();
            case "hip_cm" -> row.getHipCm();
            case "waist_hip_ratio" -> row.getWaistHipRatio();
            case "body_age" -> row.getBodyAge() == null ? null : BigDecimal.valueOf(row.getBodyAge());
            default -> null;
        };
    }

    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // fall through to date-only
        }
        try {
            LocalDate date = LocalDate.parse(trimmed);
            return endOfDay ? date.atTime(23, 59, 59, 999_999_000) : date.atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "invalid from/to date");
        }
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "invalid from/to date");
        }
    }
}
