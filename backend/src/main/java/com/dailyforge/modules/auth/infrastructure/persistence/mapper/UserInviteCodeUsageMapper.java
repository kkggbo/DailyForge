package com.dailyforge.modules.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserInviteCodeUsageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserInviteCodeUsageMapper extends BaseMapper<UserInviteCodeUsageEntity> {

    @Select("""
            SELECT COUNT(1)
            FROM user_invite_code_usages
            WHERE user_id = #{userId}
              AND invite_code_id = #{inviteCodeId}
            """)
    long countByUserIdAndInviteCodeId(Long userId, Long inviteCodeId);
}
