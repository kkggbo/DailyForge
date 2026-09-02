package com.dailyforge.modules.diet.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.FoodEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FoodMapper extends BaseMapper<FoodEntity> {

    @Select("""
            <script>
            SELECT * FROM foods
            WHERE is_active = 1
            <if test="keyword != null and keyword != ''">
              AND name LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY
              CASE WHEN source = 'system' THEN 0 ELSE 1 END,
              name ASC, id ASC
            </script>
            """)
    List<FoodEntity> searchActive(@Param("keyword") String keyword);

    @Select("""
            SELECT * FROM foods
            WHERE id = #{foodId} AND is_active = 1
            LIMIT 1
            """)
    FoodEntity selectActiveById(@Param("foodId") Long foodId);

    @Select("""
            <script>
            SELECT * FROM foods
            WHERE is_active = 1 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    List<FoodEntity> selectActiveByIds(@Param("ids") List<Long> ids);
}
