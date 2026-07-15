package com.dailyforge.modules.exercise.infrastructure.persistence.mapper;

import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseMuscleRelationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExerciseMuscleQueryMapper {

    /**
     * Batch load exercise muscle relations ordered by relation sort.
     */
    @Select({
            "<script>",
            "SELECT em.exercise_id, em.muscle_id, m.name AS muscle_name, m.code AS muscle_code,",
            "em.relation_type, em.sort_order",
            "FROM exercise_muscles em",
            "JOIN muscles m ON m.id = em.muscle_id",
            "WHERE m.is_active = 1",
            "AND em.exercise_id IN",
            "<foreach collection='exerciseIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "ORDER BY em.exercise_id ASC, em.sort_order ASC, em.id ASC",
            "</script>"
    })
    List<ExerciseMuscleRelationEntity> selectByExerciseIds(@Param("exerciseIds") List<Long> exerciseIds);

    /**
     * Load muscle relations for one exercise.
     */
    default List<ExerciseMuscleRelationEntity> selectByExerciseId(Long exerciseId) {
        return selectByExerciseIds(List.of(exerciseId));
    }
}
