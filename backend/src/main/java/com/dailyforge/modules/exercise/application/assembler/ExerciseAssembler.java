package com.dailyforge.modules.exercise.application.assembler;

import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseEquipmentRelationEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseMuscleNodeEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseMuscleRelationEntity;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseCategoryResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseEquipmentResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseFilterMuscleResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseFilterOptionsResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseListItemMuscleResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseMuscleResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemDetailResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemListItemResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemListResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExerciseAssembler {

    default ExerciseSystemListItemResponse toListItemResponse(
            ExerciseEntity entity,
            List<ExerciseListItemMuscleResponse> primaryMuscles,
            List<ExerciseListItemMuscleResponse> secondaryMuscles,
            List<String> equipmentNames) {
        return new ExerciseSystemListItemResponse(
                entity.getId(),
                entity.getName(),
                entity.getExerciseType(),
                entity.getMovementType(),
                entity.getDefaultUnit(),
                entity.getDefaultStructureType(),
                entity.getVideoUrl(),
                primaryMuscles,
                secondaryMuscles,
                equipmentNames);
    }

    default ExerciseListItemMuscleResponse toListItemMuscleResponse(ExerciseMuscleRelationEntity entity) {
        return new ExerciseListItemMuscleResponse(entity.getMuscleId(), entity.getMuscleName(), entity.getMuscleCode());
    }

    default ExerciseFilterMuscleResponse toFilterMuscleResponse(ExerciseMuscleNodeEntity entity) {
        return new ExerciseFilterMuscleResponse(
                entity.getMuscleId(),
                entity.getMuscleName(),
                entity.getMuscleCode(),
                entity.getParentMuscleId(),
                entity.getParentMuscleName(),
                entity.getSortOrder());
    }

    default ExerciseCategoryResponse toCategoryResponse(
            String categoryCode,
            String categoryName,
            Integer sortOrder,
            List<ExerciseFilterMuscleResponse> children) {
        return new ExerciseCategoryResponse(categoryCode, categoryName, sortOrder, children);
    }

    default ExerciseFilterOptionsResponse toFilterOptionsResponse(List<ExerciseCategoryResponse> categories) {
        return new ExerciseFilterOptionsResponse(categories);
    }

    default ExerciseSystemListResponse toListResponse(
            Integer page,
            Integer pageSize,
            long total,
            List<ExerciseSystemListItemResponse> records) {
        return new ExerciseSystemListResponse(page, pageSize, total, records);
    }

    default ExerciseMuscleResponse toMuscleResponse(ExerciseMuscleRelationEntity entity) {
        return new ExerciseMuscleResponse(
                entity.getMuscleId(),
                entity.getMuscleName(),
                entity.getMuscleCode(),
                entity.getRelationType());
    }

    default ExerciseEquipmentResponse toEquipmentResponse(ExerciseEquipmentRelationEntity entity) {
        return new ExerciseEquipmentResponse(entity.getEquipmentId(), entity.getEquipmentName(), entity.getSceneType());
    }

    default ExerciseSystemDetailResponse toDetailResponse(
            ExerciseEntity entity,
            List<ExerciseMuscleResponse> primaryMuscles,
            List<ExerciseMuscleResponse> secondaryMuscles,
            List<ExerciseEquipmentResponse> equipments) {
        return new ExerciseSystemDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getExerciseType(),
                entity.getMovementType(),
                entity.getDefaultUnit(),
                entity.getDefaultStructureType(),
                entity.getVideoUrl(),
                entity.getCalorieBurnReference(),
                entity.getCalorieReferenceUnit(),
                primaryMuscles,
                secondaryMuscles,
                equipments);
    }
}
