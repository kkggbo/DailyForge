package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.dailyforge.modules.plan.infrastructure.persistence.entity.ExerciseReadEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExerciseReadMapper {

    @Select({
            "<script>",
            "SELECT id, owner_user_id, name, is_active",
            "FROM exercises",
            "WHERE id IN",
            "<foreach collection='exerciseIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<ExerciseReadEntity> selectByIds(@Param("exerciseIds") List<Long> exerciseIds);
}
