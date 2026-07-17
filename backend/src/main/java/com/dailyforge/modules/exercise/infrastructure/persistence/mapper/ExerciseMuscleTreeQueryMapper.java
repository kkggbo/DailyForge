package com.dailyforge.modules.exercise.infrastructure.persistence.mapper;

import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseMuscleNodeEntity;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExerciseMuscleTreeQueryMapper {

    /**
     * Load active muscle nodes by muscle code set with parent display fields.
     */
    @Select({
            "<script>",
            "SELECT m.id AS muscle_id, m.name AS muscle_name, m.code AS muscle_code,",
            "m.parent_id AS parent_muscle_id, pm.name AS parent_muscle_name, m.sort_order",
            "FROM muscles m",
            "LEFT JOIN muscles pm ON pm.id = m.parent_id",
            "WHERE m.is_active = 1",
            "AND m.code IN",
            "<foreach collection='muscleCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "ORDER BY m.sort_order ASC, m.id ASC",
            "</script>"
    })
    List<ExerciseMuscleNodeEntity> selectActiveMusclesByCodes(@Param("muscleCodes") Collection<String> muscleCodes);
}
