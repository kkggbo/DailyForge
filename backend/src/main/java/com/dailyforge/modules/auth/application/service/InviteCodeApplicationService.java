package com.dailyforge.modules.auth.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.auth.domain.service.AccountTierPolicyService;
import com.dailyforge.modules.auth.domain.service.InviteCodeDomainService;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.InviteCodeEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserInviteCodeUsageEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.InviteCodeMapper;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserInviteCodeUsageMapper;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InviteCodeApplicationService {

    private static final Logger log = LoggerFactory.getLogger(InviteCodeApplicationService.class);

    private final InviteCodeMapper inviteCodeMapper;
    private final UserInviteCodeUsageMapper userInviteCodeUsageMapper;
    private final UserMapper userMapper;
    private final InviteCodeDomainService inviteCodeDomainService;
    private final AccountTierPolicyService accountTierPolicyService;

    public InviteCodeApplicationService(
            InviteCodeMapper inviteCodeMapper,
            UserInviteCodeUsageMapper userInviteCodeUsageMapper,
            UserMapper userMapper,
            InviteCodeDomainService inviteCodeDomainService,
            AccountTierPolicyService accountTierPolicyService) {
        this.inviteCodeMapper = inviteCodeMapper;
        this.userInviteCodeUsageMapper = userInviteCodeUsageMapper;
        this.userMapper = userMapper;
        this.inviteCodeDomainService = inviteCodeDomainService;
        this.accountTierPolicyService = accountTierPolicyService;
    }

    public void validateInviteCode(String code) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        inviteCodeDomainService.validateAvailability(inviteCodeMapper.selectByCode(code));
    }

    @Transactional
    public InviteCodeRedemptionResult redeemInviteCode(Long userId, String code) {
        InviteCodeEntity inviteCode = inviteCodeMapper.selectByCodeForUpdate(code);
        inviteCodeDomainService.validateAvailability(inviteCode);

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (userInviteCodeUsageMapper.countByUserIdAndInviteCodeId(userId, inviteCode.getId()) > 0) {
            throw new BusinessException(ErrorCode.INVITE_CODE_ALREADY_USED);
        }

        String nextTier = accountTierPolicyService.resolveGrantedTier(
                user.getAccountTier(), inviteCode.getGrantType(), inviteCode.getGrantValue());

        user.setAccountTier(nextTier);
        userMapper.updateById(user);

        UserInviteCodeUsageEntity usageEntity = new UserInviteCodeUsageEntity();
        usageEntity.setUserId(userId);
        usageEntity.setInviteCodeId(inviteCode.getId());
        usageEntity.setUsedAt(LocalDateTime.now());

        try {
            userInviteCodeUsageMapper.insert(usageEntity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.INVITE_CODE_ALREADY_USED);
        }

        inviteCode.setUsedCount(inviteCode.getUsedCount() + 1);
        inviteCodeMapper.updateById(inviteCode);

        log.info("Invite code redeemed successfully. userId={}, inviteCode={}, grantType={}, grantValue={}",
                userId, maskInviteCode(inviteCode.getCode()), inviteCode.getGrantType(), inviteCode.getGrantValue());
        return new InviteCodeRedemptionResult(nextTier, inviteCode.getCode());
    }

    private String maskInviteCode(String code) {
        if (!StringUtils.hasText(code) || code.length() <= 8) {
            return "******";
        }
        return code.substring(0, 6) + "..." + code.substring(code.length() - 2);
    }

    public record InviteCodeRedemptionResult(String accountTier, String inviteCode) {
    }
}
