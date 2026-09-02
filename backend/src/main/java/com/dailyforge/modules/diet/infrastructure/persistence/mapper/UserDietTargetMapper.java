package com.dailyforge.modules.diet.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.UserDietTargetEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDietTargetMapper extends BaseMapper<UserDietTargetEntity> {
}
