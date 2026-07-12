package com.dailyforge.modules.profile.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BodyMetricLogMapper extends BaseMapper<BodyMetricLogEntity> {

    /**
     * Select the latest record for current user regardless of delete status.
     */
    @Select("""
            SELECT *
            FROM body_metric_logs
            WHERE user_id = #{userId}
            ORDER BY record_date DESC, id DESC
            LIMIT 1
            """)
    BodyMetricLogEntity selectLatestRecord(@Param("userId") Long userId);

    /**
     * Select the latest active record for current user.
     */
    @Select("""
            SELECT *
            FROM body_metric_logs
            WHERE user_id = #{userId} AND is_del = 0
            ORDER BY record_date DESC, id DESC
            LIMIT 1
            """)
    BodyMetricLogEntity selectLatestActiveRecord(@Param("userId") Long userId);

    /**
     * Count active history rows for current user.
     */
    @Select("""
            SELECT COUNT(1)
            FROM body_metric_logs
            WHERE user_id = #{userId} AND is_del = 0
            """)
    long countActiveRecords(@Param("userId") Long userId);

    /**
     * Select active history rows for current user ordered by latest first.
     */
    @Select("""
            SELECT *
            FROM body_metric_logs
            WHERE user_id = #{userId} AND is_del = 0
            ORDER BY record_date DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<BodyMetricLogEntity> selectActiveRecordsPage(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("limit") int limit);

    /**
     * Select all active history rows ordered for snapshot rebuild.
     */
    @Select("""
            SELECT *
            FROM body_metric_logs
            WHERE user_id = #{userId} AND is_del = 0
            ORDER BY record_date DESC, id DESC
            """)
    List<BodyMetricLogEntity> selectAllActiveRecords(@Param("userId") Long userId);
}
