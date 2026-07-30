package com.dailyforge.modules.workout.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrainingSessionExerciseMapper extends BaseMapper<TrainingSessionExerciseEntity> {
    @Select("""
            SELECT * FROM training_session_exercises
            WHERE session_id = #{sessionId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<TrainingSessionExerciseEntity> selectBySessionId(@Param("sessionId") Long sessionId);

    @Select("""
            <script>
            SELECT * FROM training_session_exercises
            WHERE session_id IN
            <foreach collection="sessionIds" item="sessionId" open="(" separator="," close=")">
              #{sessionId}
            </foreach>
            ORDER BY session_id ASC, sort_order ASC, id ASC
            </script>
            """)
    List<TrainingSessionExerciseEntity> selectBySessionIds(@Param("sessionIds") List<Long> sessionIds);
}
