package com.dailyforge.modules.exercise.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.modules.exercise.interfaces.dto.ExerciseSystemListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExerciseQueryPolicyServiceTest {

    private ExerciseQueryPolicyService exerciseQueryPolicyService;

    @BeforeEach
    void setUp() {
        exerciseQueryPolicyService = new ExerciseQueryPolicyService();
    }

    @Test
    void normalizeListQueryShouldApplyDefaultsAndTrimText() {
        ExerciseSystemListQuery query = new ExerciseSystemListQuery();
        query.setKeyword("  bench  ");
        query.setExerciseType("  strength ");
        query.setMovementType(" ");

        exerciseQueryPolicyService.normalizeListQuery(query);

        assertThat(query.getPage()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(20);
        assertThat(query.getKeyword()).isEqualTo("bench");
        assertThat(query.getExerciseType()).isEqualTo("strength");
        assertThat(query.getMovementType()).isNull();
    }

    @Test
    void normalizeListQueryShouldRejectInvalidPage() {
        ExerciseSystemListQuery query = new ExerciseSystemListQuery();
        query.setPage(0);

        assertThatThrownBy(() -> exerciseQueryPolicyService.normalizeListQuery(query))
                .isInstanceOf(BusinessException.class)
                .hasMessage("request arguments are invalid");
    }

    @Test
    void normalizeListQueryShouldRejectInvalidPageSize() {
        ExerciseSystemListQuery query = new ExerciseSystemListQuery();
        query.setPageSize(101);

        assertThatThrownBy(() -> exerciseQueryPolicyService.normalizeListQuery(query))
                .isInstanceOf(BusinessException.class)
                .hasMessage("request arguments are invalid");
    }

    @Test
    void normalizeListQueryShouldRejectInvalidStructureType() {
        ExerciseSystemListQuery query = new ExerciseSystemListQuery();
        query.setStructureType("unknown");

        assertThatThrownBy(() -> exerciseQueryPolicyService.normalizeListQuery(query))
                .isInstanceOf(BusinessException.class)
                .hasMessage("request arguments are invalid");
    }

    @Test
    void normalizeListQueryShouldRejectInvalidSceneType() {
        ExerciseSystemListQuery query = new ExerciseSystemListQuery();
        query.setSceneType("office");

        assertThatThrownBy(() -> exerciseQueryPolicyService.normalizeListQuery(query))
                .isInstanceOf(BusinessException.class)
                .hasMessage("request arguments are invalid");
    }

    @Test
    void validateExerciseIdShouldRejectInvalidValue() {
        assertThatThrownBy(() -> exerciseQueryPolicyService.validateExerciseId(0L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("request arguments are invalid");
    }
}
