package com.dailyforge.modules.auth.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.InviteCodeEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class InviteCodeDomainService {

    public void validateAvailability(InviteCodeEntity inviteCode) {
        if (inviteCode == null) {
            throw new BusinessException(ErrorCode.INVITE_CODE_NOT_FOUND);
        }
        if (!"active".equals(inviteCode.getStatus())) {
            throw new BusinessException(ErrorCode.INVITE_CODE_DISABLED);
        }
        if (inviteCode.getExpiresAt() != null && inviteCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVITE_CODE_EXPIRED);
        }
        if (inviteCode.getUsedCount() >= inviteCode.getMaxUses()) {
            throw new BusinessException(ErrorCode.INVITE_CODE_EXHAUSTED);
        }
    }
}
