package com.dailyforge.modules.profile.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.interfaces.vo.BodyMetricSnapshotResponse;
import com.dailyforge.modules.profile.interfaces.vo.ProfileBasicResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ProfileAssemblerTest {

    private final ProfileAssembler profileAssembler = Mappers.getMapper(ProfileAssembler.class);

    @Test
    void toProfileBasicResponseShouldMergeProfileAndSnapshot() {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setGender("male");
        profile.setBirthDate(LocalDate.of(1998, 6, 15));
        profile.setHeightCm(new BigDecimal("178.00"));
        profile.setGoalType("fat_loss");
        profile.setTrainingLevel("beginner");
        profile.setInjuryNotes("Old knee injury");

        UserCurrentBodyMetricsEntity snapshot = new UserCurrentBodyMetricsEntity();
        snapshot.setCurrentWeightKg(new BigDecimal("76.50"));

        ProfileBasicResponse response =
                profileAssembler.toProfileBasicResponse(profile, snapshot, LocalDate.of(2026, 7, 12));

        assertThat(response.gender()).isEqualTo("male");
        assertThat(response.currentWeightKg()).isEqualByComparingTo("76.50");
        assertThat(response.latestBodyMetricRecordDate()).isEqualTo(LocalDate.of(2026, 7, 12));
    }

    @Test
    void toBodyMetricSnapshotResponseShouldReturnEmptyResponseWhenSnapshotMissing() {
        BodyMetricSnapshotResponse response = profileAssembler.toBodyMetricSnapshotResponse(null);

        assertThat(response.currentWeightKg()).isNull();
        assertThat(response.updatedAt()).isNull();
    }
}
