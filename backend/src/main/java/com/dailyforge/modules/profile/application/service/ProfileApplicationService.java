package com.dailyforge.modules.profile.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.profile.application.assembler.ProfileAssembler;
import com.dailyforge.modules.profile.domain.service.ProfileCompletionDomainService;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.BodyMetricLogMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import com.dailyforge.modules.profile.interfaces.dto.UpdateProfileBasicRequest;
import com.dailyforge.modules.profile.interfaces.vo.ProfileBasicResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileBasicUpdateResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileCompletionSummaryResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ProfileApplicationService.class);

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    private final BodyMetricLogMapper bodyMetricLogMapper;
    private final ProfileAssembler profileAssembler;
    private final ProfileCompletionDomainService profileCompletionDomainService;

    public ProfileApplicationService(
            UserMapper userMapper,
            UserProfileMapper userProfileMapper,
            UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper,
            BodyMetricLogMapper bodyMetricLogMapper,
            ProfileAssembler profileAssembler,
            ProfileCompletionDomainService profileCompletionDomainService) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userCurrentBodyMetricsMapper = userCurrentBodyMetricsMapper;
        this.bodyMetricLogMapper = bodyMetricLogMapper;
        this.profileAssembler = profileAssembler;
        this.profileCompletionDomainService = profileCompletionDomainService;
    }

    /**
     * Read the current user's basic profile and body weight summary.
     */
    public ProfileBasicResponse getBasicProfile() {
        Long userId = requireActiveUserId();
        UserProfileEntity profile = loadOrCreateProfile(userId);
        UserCurrentBodyMetricsEntity snapshot = userCurrentBodyMetricsMapper.selectById(userId);
        BodyMetricLogEntity latestActiveRecord = bodyMetricLogMapper.selectLatestActiveRecord(userId);
        log.debug("Profile basic loaded. userId={}, hasSnapshot={}, latestRecordId={}",
                userId, snapshot != null, latestActiveRecord == null ? null : latestActiveRecord.getId());
        return profileAssembler.toProfileBasicResponse(
                profile,
                snapshot,
                latestActiveRecord == null ? null : latestActiveRecord.getRecordDate());
    }

    /**
     * Update the current user's basic profile with partial update semantics.
     */
    @Transactional
    public ProfileBasicUpdateResponse updateBasicProfile(UpdateProfileBasicRequest request) {
        Long userId = requireActiveUserId();
        UserProfileEntity profile = loadOrCreateProfile(userId);
        List<String> updatedFields = new ArrayList<>();

        if (request.gender() != null) {
            profile.setGender(request.gender());
            updatedFields.add("gender");
        }
        if (request.birthDate() != null) {
            profile.setBirthDate(request.birthDate());
            updatedFields.add("birthDate");
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
            updatedFields.add("heightCm");
        }
        if (request.goalType() != null) {
            profile.setGoalType(request.goalType());
            updatedFields.add("goalType");
        }
        if (request.trainingLevel() != null) {
            profile.setTrainingLevel(request.trainingLevel());
            updatedFields.add("trainingLevel");
        }
        if (request.injuryNotes() != null) {
            profile.setInjuryNotes(request.injuryNotes());
            updatedFields.add("injuryNotes");
        }

        if (updatedFields.isEmpty()) {
            log.warn("Profile update rejected because request is empty. userId={}", userId);
            throw new BusinessException(ErrorCode.PROFILE_UPDATE_EMPTY);
        }

        userProfileMapper.updateById(profile);
        UserCurrentBodyMetricsEntity snapshot = userCurrentBodyMetricsMapper.selectById(userId);
        BodyMetricLogEntity latestActiveRecord = bodyMetricLogMapper.selectLatestActiveRecord(userId);
        log.debug("Profile basic updated. userId={}, updatedFields={}", userId, updatedFields);
        return profileAssembler.toProfileBasicUpdateResponse(
                profile,
                snapshot,
                latestActiveRecord == null ? null : latestActiveRecord.getRecordDate());
    }

    /**
     * Build the current user's completion summary used by profile and AI prompts.
     */
    public ProfileCompletionSummaryResponse getCompletionSummary() {
        Long userId = requireActiveUserId();
        UserProfileEntity profile = loadOrCreateProfile(userId);
        UserCurrentBodyMetricsEntity snapshot = userCurrentBodyMetricsMapper.selectById(userId);
        ProfileCompletionDomainService.CompletionSummary summary =
                profileCompletionDomainService.summarize(profile, snapshot);
        log.debug("Profile completion summary calculated. userId={}, basicReady={}, aiPlanReady={}",
                userId, summary.basicProfileReady(), summary.aiPlanReady());
        return new ProfileCompletionSummaryResponse(
                summary.basicProfileReady(),
                summary.hasWeightRecord(),
                summary.currentWeightKg(),
                summary.missingBasicProfileFields(),
                summary.aiPlanReady(),
                summary.aiPlanMissingFields(),
                summary.aiNutritionReady(),
                summary.aiNutritionMissingFields(),
                summary.aiSummaryReady(),
                summary.aiSummaryMissingFields());
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

    private UserProfileEntity loadOrCreateProfile(Long userId) {
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        if (profile != null) {
            return profile;
        }
        profile = new UserProfileEntity();
        profile.setUserId(userId);
        userProfileMapper.insert(profile);
        log.debug("Profile row auto-created for user. userId={}", userId);
        return profile;
    }
}
