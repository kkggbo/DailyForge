package com.dailyforge.modules.auth.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    public void validatePasswordConfirmation(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
    }
}
