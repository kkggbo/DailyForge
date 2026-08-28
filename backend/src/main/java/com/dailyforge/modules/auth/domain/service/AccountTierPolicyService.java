package com.dailyforge.modules.auth.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AccountTierPolicyService {

    private static final String GRANT_TYPE_ACCOUNT_TIER = "account_tier";
    public static final String TIER_BASIC = "basic";
    private static final Map<String, Integer> TIER_ORDER = Map.of(
            TIER_BASIC, 1,
            "invited_ai", 2,
            "premium", 3);

    public String resolveGrantedTier(String currentTier, String grantType, String grantValue) {
        if (!GRANT_TYPE_ACCOUNT_TIER.equals(grantType)) {
            throw new BusinessException(ErrorCode.INVITE_CODE_GRANT_CONFLICT);
        }
        Integer currentOrder = TIER_ORDER.get(currentTier);
        Integer grantedOrder = TIER_ORDER.get(grantValue);
        if (currentOrder == null || grantedOrder == null || grantedOrder <= currentOrder) {
            throw new BusinessException(ErrorCode.INVITE_CODE_GRANT_CONFLICT);
        }
        return grantValue;
    }

    /**
     * Whether a granted (non-basic) tier has already passed its expiry time.
     */
    public boolean isExpired(String tier, LocalDateTime expiresAt, LocalDateTime now) {
        if (TIER_BASIC.equals(tier) || expiresAt == null) {
            return false;
        }
        return !expiresAt.isAfter(now);
    }

    /**
     * The effective tier for the user: falls back to {@code basic} once a granted tier expired.
     */
    public String resolveEffectiveTier(String tier, LocalDateTime expiresAt) {
        return isExpired(tier, expiresAt, LocalDateTime.now()) ? TIER_BASIC : tier;
    }
}
