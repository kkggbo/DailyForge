package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseItemMetricEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CycleDayExerciseItemMetricMapper extends BaseMapper<CycleDayExerciseItemMetricEntity> {

    /**
     * Return metrics for one template exercise item sorted by sort order.
     */
    @Select("""
            SELECT * FROM cycle_day_exercise_item_metrics
            WHERE exercise_item_id = #{exerciseItemId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<CycleDayExerciseItemMetricEntity> selectByExerciseItemId(Long exerciseItemId);

    /**
     * Delete all metrics belonging to one template exercise item.
     */
    @Delete("DELETE FROM cycle_day_exercise_item_metrics WHERE exercise_item_id = #{exerciseItemId}")
    int deleteByExerciseItemId(Long exerciseItemId);
}
