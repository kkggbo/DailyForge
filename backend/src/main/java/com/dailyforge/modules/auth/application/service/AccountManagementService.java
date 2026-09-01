package com.dailyforge.modules.auth.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.auth.application.assembler.AuthAssembler;
import com.dailyforge.modules.auth.domain.service.PasswordPolicyService;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.auth.interfaces.dto.ChangePasswordRequest;
import com.dailyforge.modules.auth.interfaces.dto.ForgotPasswordCodeRequest;
import com.dailyforge.modules.auth.interfaces.dto.ResetPasswordRequest;
import com.dailyforge.modules.auth.interfaces.dto.UpdateUserNameRequest;
import com.dailyforge.modules.auth.interfaces.vo.CurrentUserResponse;
import java.security.SecureRandom;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AccountManagementService {

    private static final Logger log = LoggerFactory.getLogger(AccountManagementService.class);

    private static final String FORGOT_PREFIX = "forgot:";
    private static final String COOLDOWN_SUFFIX = ":cooldown";
    private static final String ATTEMPTS_SUFFIX = ":attempts";
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_TTL_MINUTES = 10;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final StringRedisTemplate redisTemplate;
    private final EmailSendService emailSendService;
    private final AuthAssembler authAssembler;

    public AccountManagementService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            StringRedisTemplate redisTemplate,
            EmailSendService emailSendService,
            AuthAssembler authAssembler) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.redisTemplate = redisTemplate;
        this.emailSendService = emailSendService;
        this.authAssembler = authAssembler;
    }

    @Transactional
    public CurrentUserResponse updateUserName(UpdateUserNameRequest request) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        String userName = request.userName().trim();
        // Case-insensitive uniqueness check: compare ignoring case so behaviour does not depend on
        // the column collation alone. selectByUserName returns the first candidate; we re-confirm
        // with equalsIgnoreCase against the whole name.
        UserEntity existing = userMapper.selectByUserName(userName);
        if (isTakenByOther(existing, userId, userName)) {
            log.warn("Update username rejected because already taken. userId={}", userId);
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setUserName(userName);
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException exception) {
            log.warn("Update username rejected due to concurrent duplicate. userId={}", userId);
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        log.info("User name updated. userId={}", userId);
        return authAssembler.toCurrentUserResponse(user);
    }

    private boolean isTakenByOther(UserEntity candidate, Long userId, String userName) {
        if (candidate == null || candidate.getId().equals(userId)) {
            return false;
        }
        return candidate.getUserName() != null && candidate.getUserName().equalsIgnoreCase(userName);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
        passwordPolicyService.validateNewPassword(request.newPassword());
        passwordPolicyService.validatePasswordConfirmation(request.newPassword(), request.confirmPassword());
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        log.info("Password changed. userId={}", userId);
    }

    /**
     * Send a forgot-password verification code. Returns success even when the email does not exist
     * (anti-enumeration); for unknown emails a cooldown key is still set so the response behaviour is
     * indistinguishable from a known email.
     */
    public void sendForgotCode(ForgotPasswordCodeRequest request) {
        String email = request.email().trim().toLowerCase();
        String cooldownKey = FORGOT_PREFIX + email + COOLDOWN_SUFFIX;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException(ErrorCode.FORGOT_CODE_TOO_FREQUENT);
        }

        String codeKey = FORGOT_PREFIX + email;
        String attemptsKey = FORGOT_PREFIX + email + ATTEMPTS_SUFFIX;
        // Always set the cooldown regardless of whether the email exists, to avoid enumeration via
        // differing behaviour (unknown emails would otherwise not be rate-limited).
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        try {
            ops.set(cooldownKey, "1", COOLDOWN_TTL);
            redisTemplate.expire(cooldownKey, COOLDOWN_TTL);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist forgot-code cooldown to redis. email={}", mask(email));
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }

        UserEntity user = userMapper.selectByEmail(email);
        if (user == null) {
            log.info("Forgot-code requested for unknown email; returning success (anti-enumeration).");
            return;
        }

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        try {
            ops.set(codeKey, code, CODE_TTL);
            redisTemplate.expire(codeKey, CODE_TTL);
            redisTemplate.delete(attemptsKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist forgot-code to redis. email={}", mask(email));
            redisTemplate.delete(codeKey);
            redisTemplate.delete(cooldownKey);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
        try {
            emailSendService.sendForgotPasswordCode(email, code, CODE_TTL_MINUTES);
        } catch (BusinessException exception) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            redisTemplate.delete(cooldownKey);
            throw exception;
        }
        log.info("Forgot-code sent. email={}", mask(email));
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        String codeKey = FORGOT_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (!StringUtils.hasText(storedCode)) {
            throw new BusinessException(ErrorCode.FORGOT_CODE_EXPIRED);
        }

        if (!storedCode.equals(request.code().trim())) {
            handleWrongCode(email, codeKey);
            return;
        }

        passwordPolicyService.validateNewPassword(request.newPassword());
        passwordPolicyService.validatePasswordConfirmation(request.newPassword(), request.confirmPassword());

        UserEntity user = userMapper.selectByEmail(email);
        if (user == null) {
            // Defensive: user should exist because code was only issued to existing emails.
            redisTemplate.delete(codeKey);
            throw new BusinessException(ErrorCode.FORGOT_CODE_EXPIRED);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        deleteForgotKeys(email, codeKey);
        log.info("Password reset via forgot-code. userId={}", user.getId());
    }

    private void handleWrongCode(String email, String codeKey) {
        String attemptsKey = FORGOT_PREFIX + email + ATTEMPTS_SUFFIX;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == null) {
            attempts = 1L;
            redisTemplate.opsForValue().set(attemptsKey, "1", CODE_TTL);
        } else {
            redisTemplate.expire(attemptsKey, CODE_TTL);
        }
        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            throw new BusinessException(ErrorCode.FORGOT_CODE_ATTEMPTS_EXCEEDED);
        }
        throw new BusinessException(ErrorCode.FORGOT_CODE_INVALID);
    }

    private void deleteForgotKeys(String email, String codeKey) {
        redisTemplate.delete(codeKey);
        redisTemplate.delete(FORGOT_PREFIX + email + ATTEMPTS_SUFFIX);
        redisTemplate.delete(FORGOT_PREFIX + email + COOLDOWN_SUFFIX);
    }

    private String mask(String email) {
        if (email == null || email.length() < 3) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email.substring(0, 1) + "***" + (at > 0 ? email.substring(at) : "");
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}
