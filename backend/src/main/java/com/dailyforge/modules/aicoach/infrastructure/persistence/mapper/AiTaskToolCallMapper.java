package com.dailyforge.modules.aicoach.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskToolCallEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiTaskToolCallMapper extends BaseMapper<AiTaskToolCallEntity> {

    @Select("""
            SELECT * FROM ai_task_tool_calls
            WHERE task_id = #{taskId}
            ORDER BY round_no DESC, id DESC
            LIMIT 1
            """)
    AiTaskToolCallEntity selectLatestByTaskId(@Param("taskId") Long taskId);
}
