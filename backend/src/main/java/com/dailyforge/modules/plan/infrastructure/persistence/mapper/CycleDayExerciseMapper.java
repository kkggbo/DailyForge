package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CycleDayExerciseMapper extends BaseMapper<CycleDayExerciseEntity> {

    /**
     * Return exercises for one template day sorted by sort order.
     */
    @Select("SELECT * FROM cycle_day_exercises WHERE template_day_id = #{templateDayId} ORDER BY sort_order ASC, id ASC")
    List<CycleDayExerciseEntity> selectByTemplateDayId(Long templateDayId);

    /**
     * Delete all exercises belonging to one template day.
     */
    @Delete("DELETE FROM cycle_day_exercises WHERE template_day_id = #{templateDayId}")
    int deleteByTemplateDayId(Long templateDayId);
}
