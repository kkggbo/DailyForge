package com.dailyforge.modules.plan.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class PlanUserSupportService {

    private final UserMapper userMapper;

    public PlanUserSupportService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * Return current authenticated user id and ensure the account is active.
     */
    public Long requireActiveUserId() {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return userId;
    }
}
