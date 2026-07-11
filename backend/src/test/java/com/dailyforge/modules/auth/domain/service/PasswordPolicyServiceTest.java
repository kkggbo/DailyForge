package com.dailyforge.modules.auth.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import org.junit.jupiter.api.Test;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService passwordPolicyService = new PasswordPolicyService();

    @Test
    void shouldPassWhenPasswordAndConfirmPasswordMatch() {
        assertThatCode(() -> passwordPolicyService.validatePasswordConfirmation("abc123", "abc123"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenPasswordAndConfirmPasswordDoNotMatch() {
        assertThatThrownBy(() -> passwordPolicyService.validatePasswordConfirmation("abc123", "xyz789"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
    }
}
