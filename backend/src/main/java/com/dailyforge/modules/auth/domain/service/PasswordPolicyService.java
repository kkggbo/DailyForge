package com.dailyforge.modules.auth.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordPolicyService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 18;

    public void validatePasswordConfirmation(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
    }

    /**
     * Validate a new password: non-blank and 6~18 characters long.
     */
    public void validateNewPassword(String newPassword) {
        if (!StringUtils.hasText(newPassword)
                || newPassword.length() < MIN_PASSWORD_LENGTH
                || newPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
    }
}
