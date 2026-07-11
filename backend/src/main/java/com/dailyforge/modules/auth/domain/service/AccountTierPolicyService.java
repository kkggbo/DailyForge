package com.dailyforge.modules.auth.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AccountTierPolicyService {

    private static final String GRANT_TYPE_ACCOUNT_TIER = "account_tier";
    private static final Map<String, Integer> TIER_ORDER = Map.of(
            "basic", 1,
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
}
