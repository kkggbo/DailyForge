package com.dailyforge.modules.workout.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrainingSessionExerciseItemMapper extends BaseMapper<TrainingSessionExerciseItemEntity> {
    @Select("""
            SELECT * FROM training_session_exercise_items
            WHERE session_exercise_id = #{sessionExerciseId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<TrainingSessionExerciseItemEntity> selectBySessionExerciseId(
            @Param("sessionExerciseId") Long sessionExerciseId);

    @Select("""
            <script>
            SELECT * FROM training_session_exercise_items
            WHERE session_exercise_id IN
            <foreach collection="sessionExerciseIds" item="sessionExerciseId" open="(" separator="," close=")">
              #{sessionExerciseId}
            </foreach>
            ORDER BY session_exercise_id ASC, sort_order ASC, id ASC
            </script>
            """)
    List<TrainingSessionExerciseItemEntity> selectBySessionExerciseIds(
            @Param("sessionExerciseIds") List<Long> sessionExerciseIds);
}
