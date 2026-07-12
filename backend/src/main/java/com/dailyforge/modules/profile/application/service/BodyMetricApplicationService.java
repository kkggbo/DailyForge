package com.dailyforge.modules.profile.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.profile.application.assembler.ProfileAssembler;
import com.dailyforge.modules.profile.domain.service.BodyMetricDomainService;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.BodyMetricLogMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.interfaces.dto.BodyMetricPageQuery;
import com.dailyforge.modules.profile.interfaces.dto.CreateBodyMetricRequest;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricLogItemResponse;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricSnapshotResponse;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricsPageResponse;
import com.dailyforge.modules.profile.interfaces.vo.DeleteLatestBodyMetricResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BodyMetricApplicationService {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricApplicationService.class);

    private final UserMapper userMapper;
    private final BodyMetricLogMapper bodyMetricLogMapper;
    private final UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    private final ProfileAssembler profileAssembler;
    private final BodyMetricDomainService bodyMetricDomainService;

    public BodyMetricApplicationService(
            UserMapper userMapper,
            BodyMetricLogMapper bodyMetricLogMapper,
            UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper,
            ProfileAssembler profileAssembler,
            BodyMetricDomainService bodyMetricDomainService) {
        this.userMapper = userMapper;
        this.bodyMetricLogMapper = bodyMetricLogMapper;
        this.userCurrentBodyMetricsMapper = userCurrentBodyMetricsMapper;
        this.profileAssembler = profileAssembler;
        this.bodyMetricDomainService = bodyMetricDomainService;
    }

    /**
     * Read the current body metric snapshot for the logged-in user.
     */
    public BodyMetricSnapshotResponse getCurrentSnapshot() {
        Long userId = requireActiveUserId();
        UserCurrentBodyMetricsEntity snapshot = userCurrentBodyMetricsMapper.selectById(userId);
        log.debug("Body metric snapshot loaded. userId={}, hasSnapshot={}", userId, snapshot != null);
        return profileAssembler.toBodyMetricSnapshotResponse(snapshot);
    }

    /**
     * Read paged active body metric history for the logged-in user.
     */
    public BodyMetricsPageResponse getBodyMetrics(BodyMetricPageQuery query) {
        Long userId = requireActiveUserId();
        long total = bodyMetricLogMapper.countActiveRecords(userId);
        long offset = (long) (query.getPage() - 1) * query.getPageSize();
        List<BodyMetricLogEntity> records = bodyMetricLogMapper.selectActiveRecordsPage(userId, offset, query.getPageSize());
        List<BodyMetricLogItemResponse> responses = records.stream()
                .map(record -> profileAssembler.toBodyMetricLogItemResponse(record, false))
                .toList();
        if (!responses.isEmpty()) {
            BodyMetricLogItemResponse first = responses.get(0);
            responses = new java.util.ArrayList<>(responses);
            responses.set(0, new BodyMetricLogItemResponse(
                    first.id(),
                    first.recordDate(),
                    first.weightKg(),
                    first.bodyFatPercent(),
                    first.bmi(),
                    first.skeletalMusclePercent(),
                    first.bodyWaterPercent(),
                    first.basalMetabolicRateKcal(),
                    first.waistCm(),
                    first.hipCm(),
                    first.waistHipRatio(),
                    first.bodyAge(),
                    first.bodyType(),
                    first.note(),
                    true));
        }
        log.debug("Body metric history loaded. userId={}, page={}, pageSize={}, total={}",
                userId, query.getPage(), query.getPageSize(), total);
        return new BodyMetricsPageResponse(query.getPage(), query.getPageSize(), total, responses);
    }

    /**
     * Create one new body metric history row and incrementally update the snapshot.
     */
    @Transactional
    public BodyMetricLogItemResponse createBodyMetric(CreateBodyMetricRequest request) {
        Long userId = requireActiveUserId();
        List<String> effectiveFields = bodyMetricDomainService.collectEffectiveMetricFields(request);
        if (effectiveFields.isEmpty()) {
            log.warn("Body metric creation rejected because no effective field exists. userId={}", userId);
            throw new BusinessException(ErrorCode.BODY_METRIC_EMPTY_RECORD);
        }

        BodyMetricLogEntity entity = new BodyMetricLogEntity();
        entity.setUserId(userId);
        entity.setRecordDate(request.recordDate());
        entity.setWeightKg(request.weightKg());
        entity.setBodyFatPercent(request.bodyFatPercent());
        entity.setBmi(request.bmi());
        entity.setSkeletalMusclePercent(request.skeletalMusclePercent());
        entity.setBodyWaterPercent(request.bodyWaterPercent());
        entity.setBasalMetabolicRateKcal(request.basalMetabolicRateKcal());
        entity.setWaistCm(request.waistCm());
        entity.setHipCm(request.hipCm());
        entity.setWaistHipRatio(request.waistHipRatio());
        entity.setBodyAge(request.bodyAge());
        entity.setBodyType(request.bodyType());
        entity.setNote(request.note());
        entity.setIsDel(false);
        bodyMetricLogMapper.insert(entity);

        UserCurrentBodyMetricsEntity existingSnapshot = userCurrentBodyMetricsMapper.selectById(userId);
        UserCurrentBodyMetricsEntity snapshot =
                bodyMetricDomainService.mergeIntoSnapshot(userId, existingSnapshot, request);
        saveSnapshot(snapshot, existingSnapshot != null);

        log.debug("Body metric created. userId={}, recordId={}, effectiveFields={}",
                userId, entity.getId(), effectiveFields);
        return profileAssembler.toBodyMetricLogItemResponse(entity, true);
    }

    /**
     * Delete only the latest body metric row and rebuild the snapshot from remaining history.
     */
    @Transactional
    public DeleteLatestBodyMetricResponse deleteLatestBodyMetric() {
        Long userId = requireActiveUserId();
        BodyMetricLogEntity latestRecord = bodyMetricLogMapper.selectLatestRecord(userId);
        if (latestRecord == null) {
            throw new BusinessException(ErrorCode.BODY_METRIC_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(latestRecord.getIsDel())) {
            log.warn("Latest body metric delete rejected because latest row is already deleted. userId={}, recordId={}",
                    userId, latestRecord.getId());
            throw new BusinessException(ErrorCode.BODY_METRIC_LATEST_ALREADY_DELETED);
        }

        latestRecord.setIsDel(true);
        latestRecord.setDeletedAt(LocalDateTime.now());
        bodyMetricLogMapper.updateById(latestRecord);

        List<BodyMetricLogEntity> activeRecords = bodyMetricLogMapper.selectAllActiveRecords(userId);
        UserCurrentBodyMetricsEntity rebuiltSnapshot = bodyMetricDomainService.rebuildSnapshot(userId, activeRecords);
        if (rebuiltSnapshot == null) {
            userCurrentBodyMetricsMapper.deleteById(userId);
            log.debug("Body metric snapshot removed after delete. userId={}", userId);
        } else {
            userCurrentBodyMetricsMapper.deleteById(userId);
            userCurrentBodyMetricsMapper.insert(rebuiltSnapshot);
            log.debug("Body metric snapshot rebuilt after delete. userId={}, latestDeletedRecordId={}",
                    userId, latestRecord.getId());
        }

        return profileAssembler.toDeleteLatestBodyMetricResponse(latestRecord);
    }

    private void saveSnapshot(UserCurrentBodyMetricsEntity snapshot, boolean exists) {
        if (exists) {
            userCurrentBodyMetricsMapper.updateById(snapshot);
            return;
        }
        userCurrentBodyMetricsMapper.insert(snapshot);
    }

    private Long requireActiveUserId() {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return userId;
    }
}
