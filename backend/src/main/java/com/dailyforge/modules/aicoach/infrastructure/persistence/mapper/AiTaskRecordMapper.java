package com.dailyforge.modules.aicoach.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            SELECT * FROM ai_task_records
            WHERE id = #{taskId}
            LIMIT 1
            FOR UPDATE
            """)
    AiTaskRecordEntity selectByIdForUpdate(@Param("taskId") Long taskId);
}