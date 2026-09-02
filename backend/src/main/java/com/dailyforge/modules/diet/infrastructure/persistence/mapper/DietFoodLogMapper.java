package com.dailyforge.modules.diet.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.DietFoodLogEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DietFoodLogMapper extends BaseMapper<DietFoodLogEntity> {

    @Select("""
            SELECT * FROM diet_food_logs
            WHERE user_id = #{userId} AND record_date = #{date}
            ORDER BY meal_type ASC, id ASC
            """)
    List<DietFoodLogEntity> selectByUserAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

    @Select("""
            <script>
            SELECT * FROM diet_food_logs
            WHERE user_id = #{userId}
            <if test="from != null"> AND record_date &gt;= #{from} </if>
            <if test="to != null"> AND record_date &lt;= #{to} </if>
            ORDER BY record_date ASC, id ASC
            </script>
            """)
    List<DietFoodLogEntity> selectByUserAndRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Select("""
            <script>
            SELECT food_id FROM diet_food_logs
            WHERE user_id = #{userId}
              AND food_id IN
              <foreach collection="foodIds" item="fid" open="(" separator="," close=")">
                #{fid}
              </foreach>
            GROUP BY food_id
            ORDER BY COUNT(*) DESC, MAX(id) DESC
            LIMIT #{limit}
            </script>
            """)
    List<Long> selectMostFrequentFoodIds(
            @Param("userId") Long userId,
            @Param("foodIds") List<Long> foodIds,
            @Param("limit") int limit);

    @Select("""
            SELECT food_id FROM diet_food_logs
            WHERE user_id = #{userId}
            GROUP BY food_id
            ORDER BY MAX(record_date) DESC, MAX(id) DESC
            LIMIT #{limit}
            """)
    List<Long> selectRecentFoodIds(@Param("userId") Long userId, @Param("limit") int limit);
}
