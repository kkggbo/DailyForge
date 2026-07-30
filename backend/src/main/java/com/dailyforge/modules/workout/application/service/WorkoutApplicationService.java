package com.dailyforge.modules.workout.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.plan.application.service.PlanUserSupportService;
import com.dailyforge.modules.plan.domain.service.CycleActivationDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.DaySnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ExerciseSnapshot;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateDayEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateDayMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.UserActiveCycleMapper;
import com.dailyforge.modules.workout.application.assembler.WorkoutAssembler;
import com.dailyforge.modules.workout.application.assembler.WorkoutAssemblySource;
import com.dailyforge.modules.workout.domain.service.WorkoutSessionPolicyDomainService;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMetricMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutRecentSessionQuery;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseItemSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseMetricSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionSaveRequest;
import com.dailyforge.modules.workout.interfaces.vo.CompleteWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.InitializeCurrentWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.RestartWorkoutCycleResponse;
import com.dailyforge.modules.workout.interfaces.vo.SaveWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutDayDetailResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutRecentSessionPageResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutSessionDetailResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutWorkspaceContextResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutApplicationService.class);

    private final PlanUserSupportService planUserSupportService;
    private final CycleTemplateMapper cycleTemplateMapper;
    private final CycleTemplateDayMapper cycleTemplateDayMapper;
    private final UserActiveCycleMapper userActiveCycleMapper;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    private final CycleActivationDomainService cycleActivationDomainService;
    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingSessionExerciseMapper trainingSessionExerciseMapper;
    private final TrainingSessionExerciseItemMapper trainingSessionExerciseItemMapper;
    private final TrainingSessionExerciseItemMetricMapper trainingSessionExerciseItemMetricMapper;
    private final WorkoutSessionSnapshotApplicationService workoutSessionSnapshotApplicationService;
    private final WorkoutSessionPolicyDomainService workoutSessionPolicyDomainService;
    private final WorkoutAssembler workoutAssembler;

    public WorkoutApplicationService(
            PlanUserSupportService planUserSupportService,
            CycleTemplateMapper cycleTemplateMapper,
            CycleTemplateDayMapper cycleTemplateDayMapper,
            UserActiveCycleMapper userActiveCycleMapper,
            CycleRunMapper cycleRunMapper,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService,
            CycleActivationDomainService cycleActivationDomainService,
            TrainingSessionMapper trainingSessionMapper,
            TrainingSessionExerciseMapper trainingSessionExerciseMapper,
            TrainingSessionExerciseItemMapper trainingSessionExerciseItemMapper,
            TrainingSessionExerciseItemMetricMapper trainingSessionExerciseItemMetricMapper,
            WorkoutSessionSnapshotApplicationService workoutSessionSnapshotApplicationService,
            WorkoutSessionPolicyDomainService workoutSessionPolicyDomainService,
            WorkoutAssembler workoutAssembler) {
        this.planUserSupportService = planUserSupportService;
        this.cycleTemplateMapper = cycleTemplateMapper;
        this.cycleTemplateDayMapper = cycleTemplateDayMapper;
        this.userActiveCycleMapper = userActiveCycleMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
        this.cycleActivationDomainService = cycleActivationDomainService;
        this.trainingSessionMapper = trainingSessionMapper;
        this.trainingSessionExerciseMapper = trainingSessionExerciseMapper;
        this.trainingSessionExerciseItemMapper = trainingSessionExerciseItemMapper;
        this.trainingSessionExerciseItemMetricMapper = trainingSessionExerciseItemMetricMapper;
        this.workoutSessionSnapshotApplicationService = workoutSessionSnapshotApplicationService;
        this.workoutSessionPolicyDomainService = workoutSessionPolicyDomainService;
        this.workoutAssembler = workoutAssembler;
    }

    public WorkoutWorkspaceContextResponse getWorkspaceContext() {
        Long userId = planUserSupportService.requireActiveUserId();
        UserActiveCycleEntity activeCycle = userActiveCycleMapper.selectById(userId);
        if (activeCycle == null) {
            return emptyWorkspaceContext();
        }
        CycleTemplateEntity template = cycleTemplateMapper.selectByIdAndUserId(activeCycle.getTemplateId(), userId);
        CycleRunEntity run = cycleRunMapper.selectById(activeCycle.getCurrentRunId());
        if (template == null || !"active".equals(template.getStatus()) || run == null) {
            return emptyWorkspaceContext();
        }
        if ("completed".equals(run.getStatus())) {
            return workoutAssembler.toWorkspaceContextResponse(new WorkoutAssemblySource.WorkspaceContext(
                    "cycle_completed", template.getId(), template.getName(), run.getId(), run.getRunNo(),
                    template.getCycleLength(), null, null, List.of()));
        }
        if (!"active".equals(run.getStatus())) {
            return emptyWorkspaceContext();
        }
        int cycleLength = requireCycleLength(template);
        int currentDayIndex = requireCurrentDayIndex(activeCycle, cycleLength);
        Map<Integer, DaySnapshot> daysByIndex = loadDaySnapshotMap(activeCycle.getTemplateVersionId());
        Map<Integer, TrainingSessionEntity> sessionsByDayIndex = new HashMap<>();
        for (TrainingSessionEntity session : trainingSessionMapper.selectByCycleRunIdAndUserId(run.getId(), userId)) {
            sessionsByDayIndex.put(session.getDayIndex(), session);
        }
        List<WorkoutAssemblySource.DayNavigation> days = new ArrayList<>();
        for (int dayIndex = 1; dayIndex <= cycleLength; dayIndex++) {
            DaySnapshot day = daysByIndex.get(dayIndex);
            TrainingSessionEntity session = sessionsByDayIndex.get(dayIndex);
            days.add(new WorkoutAssemblySource.DayNavigation(
                    dayIndex, resolveDayName(day, dayIndex), isRestDay(day),
                    resolveDayState(dayIndex, currentDayIndex),
                    session == null ? null : session.getId(), session == null ? null : session.getStatus()));
        }
        return workoutAssembler.toWorkspaceContextResponse(new WorkoutAssemblySource.WorkspaceContext(
                "active", template.getId(), template.getName(), run.getId(), run.getRunNo(), cycleLength,
                currentDayIndex, currentDayIndex, days));
    }

    @Transactional
    public InitializeCurrentWorkoutSessionResponse initializeCurrentDaySession() {
        Long userId = planUserSupportService.requireActiveUserId();
        ActiveContext context = requireActiveContext(userId, true);
        assertRunActive(context.run());
        int currentDayIndex = requireCurrentDayIndex(context.activeCycle(), requireCycleLength(context.template()));
        TrainingSessionEntity existing =
                trainingSessionMapper.selectByCycleRunIdAndDayIndexForUpdate(context.run().getId(), currentDayIndex);
        if (existing != null) {
            if (!"in_progress".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.WORKOUT_CURRENT_DAY_SESSION_CONFLICT);
            }
            return workoutAssembler.toInitializeCurrentSessionResponse(
                    false, buildSessionDayDetail(context, currentDayIndex, existing, "current", "editable", true));
        }
        DaySnapshot daySnapshot = requireDaySnapshot(context.activeCycle().getTemplateVersionId(), currentDayIndex);
        CycleTemplateDayEntity templateDay = requireTemplateDay(context.activeCycle().getTemplateVersionId(), currentDayIndex);
        TrainingSessionEntity created = createSession(context, templateDay, daySnapshot);
        try {
            trainingSessionMapper.insert(created);
            workoutSessionSnapshotApplicationService.copyTemplateDaySnapshot(
                    created.getId(), templateDay.getId(), daySnapshot.exercises());
        } catch (DuplicateKeyException exception) {
            TrainingSessionEntity concurrent =
                    trainingSessionMapper.selectByCycleRunIdAndDayIndexForUpdate(context.run().getId(), currentDayIndex);
            if (concurrent != null && "in_progress".equals(concurrent.getStatus())) {
                return workoutAssembler.toInitializeCurrentSessionResponse(
                        false, buildSessionDayDetail(context, currentDayIndex, concurrent, "current", "editable", true));
            }
            throw new BusinessException(ErrorCode.WORKOUT_CURRENT_DAY_SESSION_CONFLICT);
        }
        log.debug("Workout current-day session initialized. userId={}, runId={}, dayIndex={}, sessionId={}",
                userId, context.run().getId(), currentDayIndex, created.getId());
        return workoutAssembler.toInitializeCurrentSessionResponse(
                true, buildSessionDayDetail(context, currentDayIndex, created, "current", "editable", true));
    }

    public WorkoutDayDetailResponse getDayDetail(Integer dayIndex) {
        Long userId = planUserSupportService.requireActiveUserId();
        ActiveContext context = requireActiveContext(userId, false);
        assertRunActive(context.run());
        int cycleLength = requireCycleLength(context.template());
        if (dayIndex == null || dayIndex < 1 || dayIndex > cycleLength) {
            throw new BusinessException(ErrorCode.WORKOUT_DAY_OUT_OF_RANGE);
        }
        int currentDayIndex = requireCurrentDayIndex(context.activeCycle(), cycleLength);
        DaySnapshot snapshot = requireDaySnapshot(context.activeCycle().getTemplateVersionId(), dayIndex);
        TrainingSessionEntity session =
                trainingSessionMapper.selectByCycleRunIdAndDayIndex(context.run().getId(), dayIndex);
        if (dayIndex < currentDayIndex) {
            if (session == null) {
                throw new BusinessException(ErrorCode.WORKOUT_SESSION_NOT_FOUND);
            }
            return workoutAssembler.toDayDetailResponse(buildSessionDayDetail(
                    context, dayIndex, session, "completed", "readonly", false));
        }
        if (dayIndex == currentDayIndex) {
            if (session == null) {
                return workoutAssembler.toDayDetailResponse(buildEmptyCurrentDayDetail(context, dayIndex, snapshot));
            }
            if (!"in_progress".equals(session.getStatus())) {
                throw new BusinessException(ErrorCode.WORKOUT_SESSION_STATUS_INVALID);
            }
            return workoutAssembler.toDayDetailResponse(buildSessionDayDetail(
                    context, dayIndex, session, "current", "editable", true));
        }
        return workoutAssembler.toDayDetailResponse(new WorkoutAssemblySource.DayDetail(
                context.run().getId(), context.run().getRunNo(), context.template().getId(), context.template().getName(),
                dayIndex, resolveDayName(snapshot, dayIndex), isRestDay(snapshot), "upcoming", "preview", false,
                null, toPreviewExerciseSources(snapshot.exercises())));
    }

    @Transactional
    public SaveWorkoutSessionResponse saveSession(Long sessionId, WorkoutSessionSaveRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        TrainingSessionEntity session = requireSessionForUpdate(sessionId, userId);
        if (!"in_progress".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_EDIT_FORBIDDEN);
        }
        SessionStructure structure = loadSessionStructure(session.getId());
        workoutSessionPolicyDomainService.validateFullSave(
                request, session.getSessionType(), structure.exercises(), structure.itemsByExerciseId(),
                structure.metricsByItemId(), false);
        applyFullSave(session, request, structure, false);
        LocalDateTime savedAt = LocalDateTime.now();
        trainingSessionMapper.updateById(session);
        log.debug("Workout session saved. userId={}, sessionId={}", userId, sessionId);
        return workoutAssembler.toSaveWorkoutSessionResponse(
                new WorkoutAssemblySource.SaveResult(session.getId(), session.getStatus(), savedAt));
    }

    @Transactional
    public CompleteWorkoutSessionResponse completeSession(Long sessionId, WorkoutSessionSaveRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        ActiveContext context = requireActiveContext(userId, true);
        TrainingSessionEntity session = requireSessionForUpdate(sessionId, userId);
        if (!"in_progress".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_STATUS_INVALID);
        }
        int cycleLength = requireCycleLength(context.template());
        if (!context.run().getId().equals(session.getCycleRunId())
                || !context.activeCycle().getCurrentDayIndex().equals(session.getDayIndex())
                || !"active".equals(context.run().getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_COMPLETE_FORBIDDEN);
        }
        SessionStructure structure = loadSessionStructure(session.getId());
        workoutSessionPolicyDomainService.validateFullSave(
                request, session.getSessionType(), structure.exercises(), structure.itemsByExerciseId(),
                structure.metricsByItemId(), true);
        applyFullSave(session, request, structure, true);

        LocalDateTime completedAt = LocalDateTime.now();
        session.setStatus("completed");
        session.setCompletedAt(completedAt);
        trainingSessionMapper.updateById(session);
        context.activeCycle().setLastSessionId(session.getId());

        Integer nextDayIndex = null;
        WorkoutAssemblySource.DaySummary nextDay = null;
        if (session.getDayIndex() < cycleLength) {
            nextDayIndex = session.getDayIndex() + 1;
            context.activeCycle().setCurrentDayIndex(nextDayIndex);
            DaySnapshot nextSnapshot = requireDaySnapshot(context.activeCycle().getTemplateVersionId(), nextDayIndex);
            nextDay = new WorkoutAssemblySource.DaySummary(
                    nextDayIndex, resolveDayName(nextSnapshot, nextDayIndex), isRestDay(nextSnapshot));
        } else {
            context.run().setStatus("completed");
            context.run().setCompletedAt(completedAt);
            cycleRunMapper.updateById(context.run());
        }
        userActiveCycleMapper.updateById(context.activeCycle());

        WorkoutAssemblySource.DayDetail completedDay = buildSessionDayDetail(
                context, session.getDayIndex(), session, "completed", "readonly", false);
        log.debug("Workout session completed. userId={}, sessionId={}, runId={}, nextDayIndex={}, runStatus={}",
                userId, sessionId, context.run().getId(), nextDayIndex, context.run().getStatus());
        return workoutAssembler.toCompleteWorkoutSessionResponse(new WorkoutAssemblySource.CompletionResult(
                session.getId(), session.getStatus(), completedAt, session.getDayIndex(), context.run().getId(),
                context.run().getStatus(), nextDayIndex, nextDay, completedDay));
    }

    public WorkoutSessionDetailResponse getSessionDetail(Long sessionId) {
        Long userId = planUserSupportService.requireActiveUserId();
        TrainingSessionEntity session = trainingSessionMapper.selectByIdAndUserId(sessionId, userId);
        if (session == null) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_NOT_FOUND);
        }
        CycleRunEntity run = cycleRunMapper.selectById(session.getCycleRunId());
        return workoutAssembler.toSessionDetailResponse(new WorkoutAssemblySource.SessionDetail(
                session.getId(), session.getSessionType(), session.getStatus(), session.getCycleRunId(),
                run == null ? null : run.getRunNo(), session.getTemplateId(), session.getTemplateNameSnapshot(),
                session.getDayIndex(), session.getDayNameSnapshot(), session.getStartedAt(), session.getCompletedAt(),
                mergeLegacyText(session.getOverallFeeling(), session.getNotes()), loadSessionSource(session.getId()).exercises()));
    }

    public WorkoutRecentSessionPageResponse getRecentSessions(WorkoutRecentSessionQuery query) {
        Long userId = planUserSupportService.requireActiveUserId();
        long offset = (long) (query.getPage() - 1) * query.getPageSize();
        long total = trainingSessionMapper.countRecentByUserId(userId, query.getSessionStatus());
        List<TrainingSessionEntity> sessions =
                trainingSessionMapper.selectRecentByUserId(userId, query.getSessionStatus(), offset, query.getPageSize());
        Map<Long, Integer> runNosById = new HashMap<>();
        List<Long> runIds = sessions.stream()
                .map(TrainingSessionEntity::getCycleRunId)
                .distinct()
                .toList();
        if (!runIds.isEmpty()) {
            for (CycleRunEntity run : cycleRunMapper.selectBatchIds(runIds)) {
                runNosById.put(run.getId(), run.getRunNo());
            }
        }
        List<WorkoutAssemblySource.RecentSession> records = new ArrayList<>();
        for (TrainingSessionEntity session : sessions) {
            records.add(new WorkoutAssemblySource.RecentSession(
                    session.getId(), session.getSessionType(), session.getStatus(), session.getTemplateId(),
                    session.getTemplateNameSnapshot(), session.getCycleRunId(), runNosById.get(session.getCycleRunId()),
                    session.getDayIndex(), session.getDayNameSnapshot(), session.getStartedAt(), session.getCompletedAt()));
        }
        return workoutAssembler.toRecentSessionPageResponse(query.getPage(), query.getPageSize(), total, records);
    }

    @Transactional
    public RestartWorkoutCycleResponse restartCurrentCycle() {
        Long userId = planUserSupportService.requireActiveUserId();
        ActiveContext context = requireActiveContext(userId, true);
        if (!"completed".equals(context.run().getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_CYCLE_RESTART_FORBIDDEN);
        }
        CycleRunEntity newRun = cycleActivationDomainService.createNewRun(userId, context.template());
        UserActiveCycleEntity nextActiveCycle =
                cycleActivationDomainService.buildActiveCycleContext(context.activeCycle(), userId, context.template(), newRun);
        nextActiveCycle.setLastSessionId(null);
        userActiveCycleMapper.updateById(nextActiveCycle);
        log.debug("Workout cycle restarted. userId={}, templateId={}, newRunId={}",
                userId, context.template().getId(), newRun.getId());
        return workoutAssembler.toRestartWorkoutCycleResponse(new WorkoutAssemblySource.RestartResult(
                context.template().getId(), context.template().getName(), newRun.getId(), newRun.getRunNo(),
                newRun.getStatus(), 1));
    }

    public void requestCurrentCycleAiAnalysis() {
        Long userId = planUserSupportService.requireActiveUserId();
        ActiveContext context = requireActiveContext(userId, false);
        if (!"completed".equals(context.run().getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_AI_ANALYSIS_COMPLETED_CYCLE_REQUIRED);
        }
        throw new BusinessException(ErrorCode.WORKOUT_AI_NOT_IMPLEMENTED);
    }

    private WorkoutWorkspaceContextResponse emptyWorkspaceContext() {
        return workoutAssembler.toWorkspaceContextResponse(new WorkoutAssemblySource.WorkspaceContext(
                "no_active_template", null, null, null, null, null, null, null, List.of()));
    }

    private ActiveContext requireActiveContext(Long userId, boolean forUpdate) {
        UserActiveCycleEntity activeCycle = forUpdate
                ? userActiveCycleMapper.selectByUserIdForUpdate(userId)
                : userActiveCycleMapper.selectById(userId);
        if (activeCycle == null) {
            throw new BusinessException(ErrorCode.WORKOUT_ACTIVE_CYCLE_NOT_FOUND);
        }
        // 锁序固定为活动循环、cycle run、模板、session，避免打卡与模板切换交叉等待。
        CycleRunEntity run = forUpdate
                ? cycleRunMapper.selectByIdAndUserIdForUpdate(activeCycle.getCurrentRunId(), userId)
                : cycleRunMapper.selectById(activeCycle.getCurrentRunId());
        if (run == null || !userId.equals(run.getUserId())) {
            throw new BusinessException(ErrorCode.WORKOUT_ACTIVE_CYCLE_NOT_FOUND);
        }
        CycleTemplateEntity template = forUpdate
                ? cycleTemplateMapper.selectByIdAndUserIdForUpdate(activeCycle.getTemplateId(), userId)
                : cycleTemplateMapper.selectByIdAndUserId(activeCycle.getTemplateId(), userId);
        if (template == null || !"active".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_ACTIVE_CYCLE_NOT_FOUND);
        }
        return new ActiveContext(activeCycle, template, run);
    }

    private void assertRunActive(CycleRunEntity run) {
        if ("completed".equals(run.getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_CYCLE_COMPLETED);
        }
        if (!"active".equals(run.getStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_ACTIVE_CYCLE_NOT_FOUND);
        }
    }

    private int requireCycleLength(CycleTemplateEntity template) {
        if (template.getCycleLength() == null || template.getCycleLength() < 1) {
            throw new BusinessException(ErrorCode.WORKOUT_ACTIVE_CYCLE_NOT_FOUND);
        }
        return template.getCycleLength();
    }

    private int requireCurrentDayIndex(UserActiveCycleEntity activeCycle, int cycleLength) {
        Integer currentDayIndex = activeCycle.getCurrentDayIndex();
        if (currentDayIndex == null || currentDayIndex < 1 || currentDayIndex > cycleLength) {
            throw new BusinessException(ErrorCode.WORKOUT_ACTIVE_CYCLE_NOT_FOUND);
        }
        return currentDayIndex;
    }

    private Map<Integer, DaySnapshot> loadDaySnapshotMap(Long versionId) {
        return cycleTemplateVersionDomainService.loadVersionSnapshot(versionId).toDayIndexMap();
    }

    private DaySnapshot requireDaySnapshot(Long versionId, int dayIndex) {
        DaySnapshot snapshot = loadDaySnapshotMap(versionId).get(dayIndex);
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.WORKOUT_DAY_OUT_OF_RANGE);
        }
        return snapshot;
    }

    private CycleTemplateDayEntity requireTemplateDay(Long versionId, int dayIndex) {
        for (CycleTemplateDayEntity day : cycleTemplateDayMapper.selectByVersionId(versionId)) {
            if (day.getDayIndex().equals(dayIndex)) {
                return day;
            }
        }
        throw new BusinessException(ErrorCode.WORKOUT_DAY_OUT_OF_RANGE);
    }

    private TrainingSessionEntity createSession(
            ActiveContext context,
            CycleTemplateDayEntity templateDay,
            DaySnapshot snapshot) {
        TrainingSessionEntity session = new TrainingSessionEntity();
        session.setUserId(context.activeCycle().getUserId());
        session.setCycleRunId(context.run().getId());
        session.setTemplateId(context.template().getId());
        session.setTemplateVersionId(context.activeCycle().getTemplateVersionId());
        session.setTemplateDayId(templateDay.getId());
        session.setDayIndex(templateDay.getDayIndex());
        session.setStatus("in_progress");
        session.setSessionType(isRestDay(snapshot) ? "rest_day" : "workout");
        session.setTemplateNameSnapshot(context.template().getName());
        session.setDayNameSnapshot(resolveDayName(snapshot, templateDay.getDayIndex()));
        session.setStartedAt(LocalDateTime.now());
        return session;
    }

    private SessionStructure loadSessionStructure(Long sessionId) {
        List<TrainingSessionExerciseEntity> exercises =
                trainingSessionExerciseMapper.selectBySessionIds(List.of(sessionId));
        Map<Long, List<TrainingSessionExerciseItemEntity>> itemsByExerciseId = new LinkedHashMap<>();
        Map<Long, List<TrainingSessionExerciseItemMetricEntity>> metricsByItemId = new LinkedHashMap<>();
        List<Long> exerciseIds = exercises.stream()
                .map(TrainingSessionExerciseEntity::getId)
                .toList();
        List<TrainingSessionExerciseItemEntity> items = exerciseIds.isEmpty()
                ? List.of()
                : trainingSessionExerciseItemMapper.selectBySessionExerciseIds(exerciseIds);
        for (TrainingSessionExerciseEntity exercise : exercises) {
            itemsByExerciseId.put(exercise.getId(), new ArrayList<>());
        }
        for (TrainingSessionExerciseItemEntity item : items) {
            itemsByExerciseId.computeIfAbsent(item.getSessionExerciseId(), ignored -> new ArrayList<>()).add(item);
            metricsByItemId.put(item.getId(), new ArrayList<>());
        }
        List<Long> itemIds = items.stream()
                .map(TrainingSessionExerciseItemEntity::getId)
                .toList();
        if (!itemIds.isEmpty()) {
            for (TrainingSessionExerciseItemMetricEntity metric :
                    trainingSessionExerciseItemMetricMapper.selectBySessionExerciseItemIds(itemIds)) {
                metricsByItemId.computeIfAbsent(metric.getSessionExerciseItemId(), ignored -> new ArrayList<>()).add(metric);
            }
        }
        return new SessionStructure(exercises, itemsByExerciseId, metricsByItemId);
    }

    private void applyFullSave(
            TrainingSessionEntity session,
            WorkoutSessionSaveRequest request,
            SessionStructure structure,
            boolean completing) {
        session.setOverallFeeling(request.overallFeeling());
        session.setNotes(request.notes());
        if ("rest_day".equals(session.getSessionType())) {
            return;
        }
        Map<Long, WorkoutSessionExerciseSaveRequest> exerciseRequests = new LinkedHashMap<>();
        for (WorkoutSessionExerciseSaveRequest requestExercise : request.exercises()) {
            exerciseRequests.put(requestExercise.sessionExerciseId(), requestExercise);
        }
        for (TrainingSessionExerciseEntity exercise : structure.exercises()) {
            WorkoutSessionExerciseSaveRequest requestExercise = exerciseRequests.get(exercise.getId());
            exercise.setExerciseStatus(requestExercise.exerciseStatus());
            exercise.setFailureReason(requestExercise.failureReason());
            exercise.setFeeling(requestExercise.feeling());
            exercise.setAdjustmentNote(requestExercise.feedback() == null
                    ? requestExercise.adjustmentNote()
                    : requestExercise.feedback());
            trainingSessionExerciseMapper.updateById(exercise);
            Map<Integer, WorkoutSessionExerciseItemSaveRequest> itemRequests = new LinkedHashMap<>();
            for (WorkoutSessionExerciseItemSaveRequest itemRequest : requestExercise.items()) {
                itemRequests.put(itemRequest.itemIndex(), itemRequest);
            }
            for (TrainingSessionExerciseItemEntity item :
                    structure.itemsByExerciseId().getOrDefault(exercise.getId(), List.of())) {
                Map<String, WorkoutSessionExerciseMetricSaveRequest> metricRequests = new LinkedHashMap<>();
                for (WorkoutSessionExerciseMetricSaveRequest metricRequest :
                        itemRequests.get(item.getItemIndex()).metrics()) {
                    metricRequests.put(metricRequest.metricKey(), metricRequest);
                }
                for (TrainingSessionExerciseItemMetricEntity metric :
                        structure.metricsByItemId().getOrDefault(item.getId(), List.of())) {
                    WorkoutSessionExerciseMetricSaveRequest metricRequest = metricRequests.get(metric.getMetricKey());
                    BigDecimal actualValue = metricRequest.actualValueNumber();
                    if (completing && "completed".equals(requestExercise.exerciseStatus()) && actualValue == null) {
                        actualValue = metric.getPlannedValueNumber();
                    }
                    metric.setActualValueNumber(actualValue);
                    trainingSessionExerciseItemMetricMapper.updateById(metric);
                }
            }
        }
    }

    private WorkoutAssemblySource.Session loadSessionSource(Long sessionId) {
        TrainingSessionEntity session = trainingSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_NOT_FOUND);
        }
        SessionStructure structure = loadSessionStructure(sessionId);
        List<WorkoutAssemblySource.Exercise> exercises = new ArrayList<>();
        for (TrainingSessionExerciseEntity exercise : structure.exercises()) {
            List<WorkoutAssemblySource.ExerciseItem> items = new ArrayList<>();
            for (TrainingSessionExerciseItemEntity item :
                    structure.itemsByExerciseId().getOrDefault(exercise.getId(), List.of())) {
                List<WorkoutAssemblySource.ExerciseMetric> metrics =
                        structure.metricsByItemId().getOrDefault(item.getId(), List.of()).stream()
                                .map(metric -> new WorkoutAssemblySource.ExerciseMetric(
                                        metric.getSortOrder(),
                                        metric.getMetricKey(),
                                        metric.getPlannedValueNumber(),
                                        metric.getActualValueNumber()))
                                .toList();
                items.add(new WorkoutAssemblySource.ExerciseItem(
                        item.getItemIndex(), item.getItemType(), item.getItemNameSnapshot(),
                        item.getNoteSnapshot(), metrics));
            }
            exercises.add(new WorkoutAssemblySource.Exercise(
                    exercise.getId(), exercise.getSortOrder(), exercise.getExerciseId(),
                    exercise.getExerciseNameSnapshot(), exercise.getStructureType(), exercise.getExerciseStatus(),
                    exercise.getFailureReason(), mergeLegacyText(exercise.getFeeling(), exercise.getAdjustmentNote()), items));
        }
        return new WorkoutAssemblySource.Session(
                session.getId(), session.getSessionType(), session.getStatus(), session.getStartedAt(),
                session.getCompletedAt(), mergeLegacyText(session.getOverallFeeling(), session.getNotes()), exercises);
    }

    private WorkoutAssemblySource.DayDetail buildSessionDayDetail(
            ActiveContext context,
            int dayIndex,
            TrainingSessionEntity session,
            String dayState,
            String viewMode,
            boolean canInitializeSession) {
        return new WorkoutAssemblySource.DayDetail(
                context.run().getId(), context.run().getRunNo(), session.getTemplateId(),
                session.getTemplateNameSnapshot(),
                dayIndex, session.getDayNameSnapshot(), "rest_day".equals(session.getSessionType()), dayState, viewMode,
                canInitializeSession, loadSessionSource(session.getId()), null);
    }

    private WorkoutAssemblySource.DayDetail buildEmptyCurrentDayDetail(
            ActiveContext context,
            int dayIndex,
            DaySnapshot snapshot) {
        return new WorkoutAssemblySource.DayDetail(
                context.run().getId(), context.run().getRunNo(), context.template().getId(), context.template().getName(),
                dayIndex, resolveDayName(snapshot, dayIndex), isRestDay(snapshot), "current", "editable", true,
                null, List.of());
    }

    private TrainingSessionEntity requireSessionForUpdate(Long sessionId, Long userId) {
        TrainingSessionEntity session = trainingSessionMapper.selectByIdAndUserIdForUpdate(sessionId, userId);
        if (session == null) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_NOT_FOUND);
        }
        return session;
    }

    private String mergeLegacyText(String first, String second) {
        String firstValue = normalizeOptionalText(first);
        String secondValue = normalizeOptionalText(second);
        if (firstValue == null) {
            return secondValue;
        }
        if (secondValue == null || firstValue.equals(secondValue)) {
            return firstValue;
        }
        // 保留旧字段的原始语义，避免历史记录在契约收口后丢失任一段文本。
        return firstValue + "\n" + secondValue;
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String resolveDayName(DaySnapshot day, int dayIndex) {
        return day == null || day.dayName() == null || day.dayName().isBlank() ? "Day " + dayIndex : day.dayName();
    }

    private boolean isRestDay(DaySnapshot day) {
        return day == null || day.exercises() == null || day.exercises().isEmpty();
    }

    private String resolveDayState(int dayIndex, int currentDayIndex) {
        if (dayIndex < currentDayIndex) {
            return "completed";
        }
        return dayIndex == currentDayIndex ? "current" : "upcoming";
    }

    private record ActiveContext(
            UserActiveCycleEntity activeCycle,
            CycleTemplateEntity template,
            CycleRunEntity run) {
    }

    private record SessionStructure(
            List<TrainingSessionExerciseEntity> exercises,
            Map<Long, List<TrainingSessionExerciseItemEntity>> itemsByExerciseId,
            Map<Long, List<TrainingSessionExerciseItemMetricEntity>> metricsByItemId) {
    }
    private List<WorkoutAssemblySource.Exercise> toPreviewExerciseSources(List<ExerciseSnapshot> exercises) {
        return exercises.stream()
                .map(exercise -> new WorkoutAssemblySource.Exercise(
                        null,
                        exercise.sortOrder(),
                        exercise.exerciseId(),
                        exercise.exerciseNameSnapshot(),
                        exercise.structureType(),
                        null,
                        null,
                        null,
                        exercise.items().stream()
                                .map(item -> new WorkoutAssemblySource.ExerciseItem(
                                        item.itemIndex(),
                                        item.itemType(),
                                        item.itemName(),
                                        item.note(),
                                        item.metrics().stream()
                                                .map(metric -> new WorkoutAssemblySource.ExerciseMetric(
                                                        metric.sortOrder(),
                                                        metric.metricKey(),
                                                        metric.metricValueNumber(),
                                                        null))
                                                .toList()))
                                .toList()))
                .toList();
    }
}

