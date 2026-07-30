package com.dailyforge.modules.workout.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrainingSessionExerciseItemMetricMapper
        extends BaseMapper<TrainingSessionExerciseItemMetricEntity> {
    @Select("""
            SELECT * FROM training_session_exercise_item_metrics
            WHERE session_exercise_item_id = #{sessionExerciseItemId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<TrainingSessionExerciseItemMetricEntity> selectBySessionExerciseItemId(
            @Param("sessionExerciseItemId") Long sessionExerciseItemId);

    @Select("""
            <script>
            SELECT * FROM training_session_exercise_item_metrics
            WHERE session_exercise_item_id IN
            <foreach collection="sessionExerciseItemIds" item="sessionExerciseItemId" open="(" separator="," close=")">
              #{sessionExerciseItemId}
            </foreach>
            ORDER BY session_exercise_item_id ASC, sort_order ASC, id ASC
            </script>
            """)
    List<TrainingSessionExerciseItemMetricEntity> selectBySessionExerciseItemIds(
            @Param("sessionExerciseItemIds") List<Long> sessionExerciseItemIds);
}
