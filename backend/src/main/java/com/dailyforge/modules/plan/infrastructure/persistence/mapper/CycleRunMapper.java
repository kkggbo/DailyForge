package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CycleRunMapper extends BaseMapper<CycleRunEntity> {

    @Select("SELECT COALESCE(MAX(run_no), 0) FROM cycle_runs WHERE user_id = #{userId} AND template_id = #{templateId}")
    Integer selectMaxRunNo(@Param("userId") Long userId, @Param("templateId") Long templateId);

    @Select("SELECT * FROM cycle_runs WHERE user_id = #{userId} AND status = 'active' LIMIT 1")
    CycleRunEntity selectCurrentRunByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM cycle_runs
            WHERE id = #{cycleRunId} AND user_id = #{userId}
            LIMIT 1
            FOR UPDATE
            """)
    CycleRunEntity selectByIdAndUserIdForUpdate(
            @Param("cycleRunId") Long cycleRunId,
            @Param("userId") Long userId);

    @Update("""
            UPDATE cycle_runs
            SET status = 'cancelled', cancelled_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{cycleRunId}
              AND user_id = #{userId}
              AND status = 'active'
            """)
    int cancelActiveByIdAndUserId(
            @Param("cycleRunId") Long cycleRunId,
            @Param("userId") Long userId);
}
