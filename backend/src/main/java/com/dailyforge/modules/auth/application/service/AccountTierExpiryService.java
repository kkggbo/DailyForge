package com.dailyforge.modules.auth.application.service;

import com.dailyforge.modules.auth.domain.service.AccountTierPolicyService;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Detects and persists the downgrade of an expired granted account tier.
 *
 * <p>When {@code users.account_tier_expires_at} has passed and the tier is not {@code basic},
 * the user is reverted to {@code basic} and the expiry is cleared. This keeps the DB (and
 * therefore all tier-gated reads and freshly issued JWT claims) consistent with the effective tier.
 */
@Service
public class AccountTierExpiryService {

    private final UserMapper userMapper;
    private final AccountTierPolicyService accountTierPolicyService;

    public AccountTierExpiryService(
            UserMapper userMapper,
            AccountTierPolicyService accountTierPolicyService) {
        this.userMapper = userMapper;
        this.accountTierPolicyService = accountTierPolicyService;
    }

    /**
     * If the user's granted tier has expired, downgrade to {@code basic} and persist. The user
     * entity is mutated in place so callers can read the effective tier afterwards.
     */
    public UserEntity applyExpiryIfNeeded(UserEntity user) {
        if (user == null
                || !accountTierPolicyService.isExpired(
                        user.getAccountTier(), user.getAccountTierExpiresAt(), LocalDateTime.now())) {
            return user;
        }
        user.setAccountTier(AccountTierPolicyService.TIER_BASIC);
        user.setAccountTierExpiresAt(null);
        userMapper.updateById(user);
        return user;
    }
}
