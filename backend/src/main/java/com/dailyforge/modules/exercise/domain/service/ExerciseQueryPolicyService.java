package com.dailyforge.modules.exercise.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.interfaces.dto.ExerciseSystemListQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExerciseQueryPolicyService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseQueryPolicyService.class);

    /**
     * Normalize and validate list query parameters before hitting persistence.
     */
    public void normalizeListQuery(ExerciseSystemListQuery query) {
        query.setKeyword(normalizeText(query.getKeyword()));
        query.setExerciseType(normalizeText(query.getExerciseType()));
        query.setMovementType(normalizeText(query.getMovementType()));
        query.setStructureType(normalizeText(query.getStructureType()));
        query.setSceneType(normalizeText(query.getSceneType()));

        if (query.getPage() == null) {
            query.setPage(1);
        }
        if (query.getPageSize() == null) {
            query.setPageSize(20);
        }
        if (query.getPage() < 1 || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
        if (query.getMuscleId() != null && query.getMuscleId() < 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
        if (query.getStructureType() != null
                && !"set_based".equals(query.getStructureType())
                && !"single_segment".equals(query.getStructureType())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
        if (query.getSceneType() != null
                && !"home".equals(query.getSceneType())
                && !"gym".equals(query.getSceneType())
                && !"both".equals(query.getSceneType())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }

        log.debug("Exercise query normalized. page={}, pageSize={}, hasFilters={}",
                query.getPage(), query.getPageSize(), query.hasFilters());
    }

    /**
     * Validate one exercise id path parameter.
     */
    public void validateExerciseId(Long exerciseId) {
        if (exerciseId == null || exerciseId < 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
