package com.dailyforge.modules.auth.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import org.junit.jupiter.api.Test;

class AccountTierPolicyServiceTest {

    private final AccountTierPolicyService accountTierPolicyService = new AccountTierPolicyService();

    @Test
    void shouldAllowUpgradeFromBasicToInvitedAi() {
        assertThat(accountTierPolicyService.resolveGrantedTier("basic", "account_tier", "invited_ai"))
                .isEqualTo("invited_ai");
    }

    @Test
    void shouldRejectSameTierGrant() {
        assertThatThrownBy(() -> accountTierPolicyService.resolveGrantedTier("invited_ai", "account_tier", "invited_ai"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVITE_CODE_GRANT_CONFLICT);
    }

    @Test
    void shouldRejectUnsupportedGrantType() {
        assertThatThrownBy(() -> accountTierPolicyService.resolveGrantedTier("basic", "platform_role", "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVITE_CODE_GRANT_CONFLICT);
    }
}
