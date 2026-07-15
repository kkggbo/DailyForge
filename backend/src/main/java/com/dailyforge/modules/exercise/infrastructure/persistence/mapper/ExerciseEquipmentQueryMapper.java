package com.dailyforge.modules.exercise.infrastructure.persistence.mapper;

import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseEquipmentRelationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExerciseEquipmentQueryMapper {

    /**
     * Batch load exercise equipment relations ordered by relation sort.
     */
    @Select({
            "<script>",
            "SELECT ee.exercise_id, ee.equipment_id, eq.name AS equipment_name, eq.scene_type, ee.sort_order",
            "FROM exercise_equipments ee",
            "JOIN equipments eq ON eq.id = ee.equipment_id",
            "WHERE eq.is_active = 1",
            "AND ee.exercise_id IN",
            "<foreach collection='exerciseIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "ORDER BY ee.exercise_id ASC, ee.sort_order ASC, ee.id ASC",
            "</script>"
    })
    List<ExerciseEquipmentRelationEntity> selectByExerciseIds(@Param("exerciseIds") List<Long> exerciseIds);

    /**
     * Load equipment relations for one exercise.
     */
    default List<ExerciseEquipmentRelationEntity> selectByExerciseId(Long exerciseId) {
        return selectByExerciseIds(List.of(exerciseId));
    }
}
