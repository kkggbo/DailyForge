package com.dailyforge.modules.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.InviteCodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InviteCodeMapper extends BaseMapper<InviteCodeEntity> {

    @Select("SELECT * FROM invite_codes WHERE code = #{code} LIMIT 1")
    InviteCodeEntity selectByCode(String code);

    @Select("SELECT * FROM invite_codes WHERE code = #{code} LIMIT 1 FOR UPDATE")
    InviteCodeEntity selectByCodeForUpdate(String code);
}
