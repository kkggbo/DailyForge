package com.dailyforge.modules.workout.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrainingSessionMapper extends BaseMapper<TrainingSessionEntity> {
    @Select("""
            SELECT * FROM training_sessions
            WHERE cycle_run_id = #{cycleRunId} AND day_index = #{dayIndex}
            LIMIT 1
            """)
    TrainingSessionEntity selectByCycleRunIdAndDayIndex(
            @Param("cycleRunId") Long cycleRunId,
            @Param("dayIndex") Integer dayIndex);

    @Select("""
            SELECT * FROM training_sessions
            WHERE cycle_run_id = #{cycleRunId} AND day_index = #{dayIndex}
            LIMIT 1
            FOR UPDATE
            """)
    TrainingSessionEntity selectByCycleRunIdAndDayIndexForUpdate(
            @Param("cycleRunId") Long cycleRunId,
            @Param("dayIndex") Integer dayIndex);

    @Select("""
            SELECT * FROM training_sessions
            WHERE id = #{sessionId} AND user_id = #{userId}
            LIMIT 1
            """)
    TrainingSessionEntity selectByIdAndUserId(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId);

    @Select("""
            SELECT * FROM training_sessions
            WHERE id = #{sessionId} AND user_id = #{userId}
            LIMIT 1
            FOR UPDATE
            """)
    TrainingSessionEntity selectByIdAndUserIdForUpdate(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId);

    @Select("""
            SELECT * FROM training_sessions
            WHERE cycle_run_id = #{cycleRunId} AND user_id = #{userId}
            ORDER BY day_index ASC
            """)
    List<TrainingSessionEntity> selectByCycleRunIdAndUserId(
            @Param("cycleRunId") Long cycleRunId,
            @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT * FROM training_sessions
            WHERE user_id = #{userId}
            <if test="sessionStatus != null">
              AND status = #{sessionStatus}
            </if>
            ORDER BY
              completed_at DESC,
              started_at DESC,
              id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<TrainingSessionEntity> selectRecentByUserId(
            @Param("userId") Long userId,
            @Param("sessionStatus") String sessionStatus,
            @Param("offset") long offset,
            @Param("limit") long limit);

    @Select("""
            <script>
            SELECT COUNT(1) FROM training_sessions
            WHERE user_id = #{userId}
            <if test="sessionStatus != null">
              AND status = #{sessionStatus}
            </if>
            </script>
            """)
    long countRecentByUserId(
            @Param("userId") Long userId,
            @Param("sessionStatus") String sessionStatus);

    @Update("""
            UPDATE training_sessions
            SET template_id = #{templateId},
                template_version_id = #{templateVersionId},
                template_day_id = #{templateDayId},
                template_name_snapshot = #{templateName},
                day_name_snapshot = #{dayName},
                session_type = #{sessionType},
                overall_feeling = NULL,
                notes = NULL
            WHERE id = #{sessionId}
              AND user_id = #{userId}
              AND status = 'in_progress'
            """)
    int refreshInProgressSnapshot(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("templateId") Long templateId,
            @Param("templateVersionId") Long templateVersionId,
            @Param("templateDayId") Long templateDayId,
            @Param("templateName") String templateName,
            @Param("dayName") String dayName,
            @Param("sessionType") String sessionType);
    @Update("""
            UPDATE training_sessions
            SET status = 'cancelled'
            WHERE cycle_run_id = #{cycleRunId}
              AND user_id = #{userId}
              AND status = 'in_progress'
            """)
    int cancelInProgressByCycleRunIdAndUserId(
            @Param("cycleRunId") Long cycleRunId,
            @Param("userId") Long userId);
}
