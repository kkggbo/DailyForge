package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CycleDayExerciseItemMapper extends BaseMapper<CycleDayExerciseItemEntity> {

    /**
     * Return items for one template exercise sorted by sort order.
     */
    @Select("""
            SELECT * FROM cycle_day_exercise_items
            WHERE cycle_day_exercise_id = #{cycleDayExerciseId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<CycleDayExerciseItemEntity> selectByCycleDayExerciseId(Long cycleDayExerciseId);

    /**
     * Delete all items belonging to one template exercise.
     */
    @Delete("DELETE FROM cycle_day_exercise_items WHERE cycle_day_exercise_id = #{cycleDayExerciseId}")
    int deleteByCycleDayExerciseId(Long cycleDayExerciseId);
}
