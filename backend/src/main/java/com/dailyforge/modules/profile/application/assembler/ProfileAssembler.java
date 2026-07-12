package com.dailyforge.modules.profile.application.assembler;

import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricLogItemResponse;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricSnapshotResponse;
import com.dailyforge.modules.profile.interfaces.vo.DeleteLatestBodyMetricResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileBasicResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileBasicUpdateResponse;
import java.time.LocalDate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileAssembler {

    default ProfileBasicResponse toProfileBasicResponse(
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity snapshot,
            LocalDate latestBodyMetricRecordDate) {
        return new ProfileBasicResponse(
                profile.getGender(),
                profile.getBirthDate(),
                profile.getHeightCm(),
                profile.getGoalType(),
                profile.getTrainingLevel(),
                profile.getInjuryNotes(),
                snapshot == null ? null : snapshot.getCurrentWeightKg(),
                latestBodyMetricRecordDate);
    }

    default ProfileBasicUpdateResponse toProfileBasicUpdateResponse(
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity snapshot,
            LocalDate latestBodyMetricRecordDate) {
        return new ProfileBasicUpdateResponse(
                profile.getGender(),
                profile.getBirthDate(),
                profile.getHeightCm(),
                profile.getGoalType(),
                profile.getTrainingLevel(),
                profile.getInjuryNotes(),
                snapshot == null ? null : snapshot.getCurrentWeightKg(),
                latestBodyMetricRecordDate);
    }

    default BodyMetricSnapshotResponse toBodyMetricSnapshotResponse(UserCurrentBodyMetricsEntity snapshot) {
        if (snapshot == null) {
            return new BodyMetricSnapshotResponse(null, null, null, null, null, null, null, null, null, null, null, null);
        }
        return new BodyMetricSnapshotResponse(
                snapshot.getCurrentWeightKg(),
                snapshot.getCurrentBodyFatPercent(),
                snapshot.getCurrentBmi(),
                snapshot.getCurrentSkeletalMusclePercent(),
                snapshot.getCurrentBodyWaterPercent(),
                snapshot.getCurrentBasalMetabolicRateKcal(),
                snapshot.getCurrentWaistCm(),
                snapshot.getCurrentHipCm(),
                snapshot.getCurrentWaistHipRatio(),
                snapshot.getCurrentBodyAge(),
                snapshot.getCurrentBodyType(),
                snapshot.getUpdatedAt());
    }

    default BodyMetricLogItemResponse toBodyMetricLogItemResponse(BodyMetricLogEntity entity, boolean isLatest) {
        return new BodyMetricLogItemResponse(
                entity.getId(),
                entity.getRecordDate(),
                entity.getWeightKg(),
                entity.getBodyFatPercent(),
                entity.getBmi(),
                entity.getSkeletalMusclePercent(),
                entity.getBodyWaterPercent(),
                entity.getBasalMetabolicRateKcal(),
                entity.getWaistCm(),
                entity.getHipCm(),
                entity.getWaistHipRatio(),
                entity.getBodyAge(),
                entity.getBodyType(),
                entity.getNote(),
                isLatest);
    }

    default DeleteLatestBodyMetricResponse toDeleteLatestBodyMetricResponse(BodyMetricLogEntity entity) {
        return new DeleteLatestBodyMetricResponse(entity.getId(), entity.getRecordDate(), entity.getWeightKg());
    }
}
