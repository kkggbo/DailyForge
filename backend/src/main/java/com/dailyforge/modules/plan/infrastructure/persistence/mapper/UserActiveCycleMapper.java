package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserActiveCycleMapper extends BaseMapper<UserActiveCycleEntity> {

    @Select("SELECT * FROM user_active_cycles WHERE user_id = #{userId} LIMIT 1 FOR UPDATE")
    UserActiveCycleEntity selectByUserIdForUpdate(Long userId);
}
