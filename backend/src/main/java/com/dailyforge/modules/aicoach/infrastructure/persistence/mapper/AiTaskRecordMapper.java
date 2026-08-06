package com.dailyforge.modules.aicoach.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiTaskRecordMapper extends BaseMapper<AiTaskRecordEntity> {

    @Select("""
            SELECT * FROM ai_task_records
            WHERE user_id = #{userId}
              AND task_type = #{taskType}
              AND client_request_id = #{clientRequestId}
            LIMIT 1
            """)
    AiTaskRecordEntity selectByUserTaskAndClientRequestId(
            @Param("userId") Long userId,
            @Param("taskType") String taskType,
            @Param("clientRequestId") String clientRequestId);

    @Select("""
            SELECT * FROM ai_task_records
            WHERE id = #{taskId}
              AND user_id = #{userId}
              AND task_type = #{taskType}
            LIMIT 1
            """)
    AiTaskRecordEntity selectByIdAndUserIdAndTaskType(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId,
            @Param("taskType") String taskType);

    @Select("""
            SELECT COUNT(*)
            FROM ai_task_records
            WHERE user_id = #{userId}
              AND task_type = #{taskType}
            """)
    long countByUserIdAndTaskType(
            @Param("userId") Long userId,
            @Param("taskType") String taskType);

    @Select("""
            SELECT * FROM ai_task_records
            WHERE user_id = #{userId}
              AND task_type = #{taskType}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AiTaskRecordEntity> selectHistoryPageByUserIdAndTaskType(
            @Param("userId") Long userId,
            @Param("taskType") String taskType,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            SELECT * FROM ai_task_records
            WHERE user_id = #{userId}
              AND task_type = #{taskType}
              AND related_entity_type = #{relatedEntityType}
              AND related_entity_id = #{relatedEntityId}
              AND status = 'succeeded'
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    AiTaskRecordEntity selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
            @Param("userId") Long userId,
            @Param("taskType") String taskType,
            @Param("relatedEntityType") String relatedEntityType,
            @Param("relatedEntityId") Long relatedEntityId);

    @Select("""
            SELECT * FROM ai_task_records
            WHERE id = #{taskId}
            LIMIT 1
            FOR UPDATE
            """)
    AiTaskRecordEntity selectByIdForUpdate(@Param("taskId") Long taskId);

    @Update("""
            UPDATE ai_task_records
            SET tool_call_count = tool_call_count + 1
            WHERE id = #{taskId}
            """)
    int incrementToolCallCount(@Param("taskId") Long taskId);

    @Update("""
            UPDATE ai_task_records
            SET repair_attempt_count = repair_attempt_count + 1
            WHERE id = #{taskId}
            """)
    int incrementRepairAttemptCount(@Param("taskId") Long taskId);
}
