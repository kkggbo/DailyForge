package com.dailyforge.modules.diet.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.UserFoodFavoriteEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserFoodFavoriteMapper extends BaseMapper<UserFoodFavoriteEntity> {

    @Select("""
            SELECT food_id FROM user_food_favorites
            WHERE user_id = #{userId}
            """)
    List<Long> selectFoodIdsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM user_food_favorites
            WHERE user_id = #{userId} AND food_id = #{foodId}
            LIMIT 1
            """)
    UserFoodFavoriteEntity selectByUserAndFood(
            @Param("userId") Long userId,
            @Param("foodId") Long foodId);
}
