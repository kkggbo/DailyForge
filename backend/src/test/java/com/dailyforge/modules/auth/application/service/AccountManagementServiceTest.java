package com.dailyforge.modules.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthUserPrincipal;
import com.dailyforge.modules.auth.application.assembler.AuthAssembler;
import com.dailyforge.modules.auth.domain.service.PasswordPolicyService;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.auth.interfaces.dto.ChangePasswordRequest;
import com.dailyforge.modules.auth.interfaces.dto.ForgotPasswordCodeRequest;
import com.dailyforge.modules.auth.interfaces.dto.ResetPasswordRequest;
import com.dailyforge.modules.auth.interfaces.dto.UpdateUserNameRequest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyService passwordPolicyService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private EmailSendService emailSendService;
    @Mock
    private AuthAssembler authAssembler;

    private AccountManagementService service;

    @BeforeEach
    void setUp() {
        service = new AccountManagementService(
                userMapper, passwordEncoder, passwordPolicyService, redisTemplate, emailSendService, authAssembler);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Authentication authentication = new Authentication() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getName() {
                return String.valueOf(USER_ID);
            }

            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return java.util.List.of();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return new AuthUserPrincipal(USER_ID, "u@example.com", "user", "basic");
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // --- updateUserName ---

    @Test
    void updateUserNameShouldRejectWhenTakenByOtherUser() {
        UserEntity other = user(200L, "张三");
        when(userMapper.selectByUserName("张三")).thenReturn(other);

        assertThatThrownBy(() -> service.updateUserName(new UpdateUserNameRequest("张三")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    @Test
    void updateUserNameShouldAllowWhenNameBelongsToSelf() {
        UserEntity current = user(USER_ID, "old");
        when(userMapper.selectByUserName("newName")).thenReturn(current);
        when(userMapper.selectById(USER_ID)).thenReturn(current);
        when(authAssembler.toCurrentUserResponse(any())).thenReturn(null);

        assertThatCode(() -> service.updateUserName(new UpdateUserNameRequest("newName")))
                .doesNotThrowAnyException();
        verify(userMapper).updateById(any());
    }

    @Test
    void updateUserNameShouldSucceedWhenNameIsFree() {
        UserEntity current = user(USER_ID, "old");
        when(userMapper.selectByUserName("freeName")).thenReturn(null);
        when(userMapper.selectById(USER_ID)).thenReturn(current);
        when(authAssembler.toCurrentUserResponse(any())).thenReturn(null);

        assertThatCode(() -> service.updateUserName(new UpdateUserNameRequest("freeName")))
                .doesNotThrowAnyException();
        assertThat(current.getUserName()).isEqualTo("freeName");
        verify(userMapper).updateById(current);
    }

    @Test
    void updateUserNameShouldMapConcurrentDuplicateToConflict() {
        UserEntity current = user(USER_ID, "old");
        when(userMapper.selectByUserName("raceName")).thenReturn(null);
        when(userMapper.selectById(USER_ID)).thenReturn(current);
        doThrow(new org.springframework.dao.DuplicateKeyException("dup"))
                .when(userMapper).updateById(any());

        assertThatThrownBy(() -> service.updateUserName(new UpdateUserNameRequest("raceName")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    @Test
    void updateUserNameShouldTreatCaseOnlyDifferenceAsDuplicate() {
        // Existing user "Zhang San"; requesting "zhang san" must be rejected (case-insensitive).
        UserEntity other = user(200L, "Zhang San");
        when(userMapper.selectByUserName("zhang san")).thenReturn(other);

        assertThatThrownBy(() -> service.updateUserName(new UpdateUserNameRequest("zhang san")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    // --- changePassword ---

    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        UserEntity user = user(USER_ID, "x");
        user.setPasswordHash("hash");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("wrongOld", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(
                new ChangePasswordRequest("wrongOld", "NewPass456", "NewPass456")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_INCORRECT);
    }

    @Test
    void changePasswordShouldRejectSameAsOld() {
        UserEntity user = user(USER_ID, "x");
        user.setPasswordHash("hash");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("samePass", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(
                new ChangePasswordRequest("samePass", "samePass", "samePass")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_SAME_AS_OLD);
    }

    @Test
    void changePasswordShouldSucceed() {
        UserEntity user = user(USER_ID, "x");
        user.setPasswordHash("oldHash");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        assertThatCode(() -> service.changePassword(
                new ChangePasswordRequest("oldPass", "newPass", "newPass")))
                .doesNotThrowAnyException();
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        verify(userMapper).updateById(user);
    }

    // --- sendForgotCode ---

    @Test
    void sendForgotCodeShouldSetCooldownForUnknownEmailButNotSendCode() {
        when(userMapper.selectByEmail("ghost@example.com")).thenReturn(null);
        when(redisTemplate.hasKey("forgot:ghost@example.com:cooldown")).thenReturn(false);

        assertThatCode(() -> service.sendForgotCode(
                new ForgotPasswordCodeRequest("ghost@example.com")))
                .doesNotThrowAnyException();
        // No email sent and no code stored for unknown email.
        verify(emailSendService, never()).sendForgotPasswordCode(anyString(), anyString(), any(int.class));
        verify(valueOperations, never()).set(eq("forgot:ghost@example.com"), anyString(), any(Duration.class));
        // But cooldown is set so a second attempt is rate-limited (anti-enumeration).
        verify(valueOperations).set(eq("forgot:ghost@example.com:cooldown"), eq("1"), any(Duration.class));
    }

    @Test
    void forgotCodeShouldBeRateLimitedIndistinguishablyForKnownAndUnknownEmail() {
        // Known email: first request sends, second within cooldown -> TOO_FREQUENT.
        UserEntity known = user(USER_ID, "x");
        known.setEmail("known@example.com");
        when(userMapper.selectByEmail("known@example.com")).thenReturn(known);
        when(redisTemplate.hasKey("forgot:known@example.com:cooldown"))
                .thenReturn(false, true);
        service.sendForgotCode(new ForgotPasswordCodeRequest("known@example.com"));
        assertThatThrownBy(() -> service.sendForgotCode(new ForgotPasswordCodeRequest("known@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORGOT_CODE_TOO_FREQUENT);

        // Unknown email: first request returns success (sets cooldown), second -> TOO_FREQUENT.
        when(userMapper.selectByEmail("unknown@example.com")).thenReturn(null);
        when(redisTemplate.hasKey("forgot:unknown@example.com:cooldown"))
                .thenReturn(false, true);
        service.sendForgotCode(new ForgotPasswordCodeRequest("unknown@example.com"));
        assertThatThrownBy(() -> service.sendForgotCode(new ForgotPasswordCodeRequest("unknown@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORGOT_CODE_TOO_FREQUENT);
    }

    @Test
    void sendForgotCodeShouldRollbackWhenEmailSendFails() {
        UserEntity user = user(USER_ID, "x");
        user.setEmail("u@example.com");
        when(userMapper.selectByEmail("u@example.com")).thenReturn(user);
        when(redisTemplate.hasKey("forgot:u@example.com:cooldown")).thenReturn(false);
        doThrow(new BusinessException(ErrorCode.EMAIL_SEND_FAILED))
                .when(emailSendService).sendForgotPasswordCode(eq("u@example.com"), anyString(), any(int.class));

        assertThatThrownBy(() -> service.sendForgotCode(new ForgotPasswordCodeRequest("u@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_SEND_FAILED);

        // Code and attempts are rolled back so no stale state remains.
        verify(redisTemplate).delete("forgot:u@example.com");
        verify(redisTemplate).delete("forgot:u@example.com:cooldown");
    }

    @Test
    void sendForgotCodeShouldResetAttemptsCounterForNewCode() {
        UserEntity user = user(USER_ID, "x");
        user.setEmail("u@example.com");
        when(userMapper.selectByEmail("u@example.com")).thenReturn(user);
        when(redisTemplate.hasKey("forgot:u@example.com:cooldown")).thenReturn(false);

        service.sendForgotCode(new ForgotPasswordCodeRequest("u@example.com"));

        // A fresh code must clear any previous attempt counter.
        verify(redisTemplate).delete("forgot:u@example.com:attempts");
    }

    @Test
    void sendForgotCodeShouldRejectWhenCooldownActive() {
        when(redisTemplate.hasKey("forgot:u@example.com:cooldown")).thenReturn(true);

        assertThatThrownBy(() -> service.sendForgotCode(new ForgotPasswordCodeRequest("u@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORGOT_CODE_TOO_FREQUENT);
    }

    @Test
    void sendForgotCodeShouldSendForExistingEmail() {
        UserEntity user = user(USER_ID, "x");
        user.setEmail("u@example.com");
        when(userMapper.selectByEmail("u@example.com")).thenReturn(user);
        when(redisTemplate.hasKey("forgot:u@example.com:cooldown")).thenReturn(false);

        assertThatCode(() -> service.sendForgotCode(new ForgotPasswordCodeRequest("u@example.com")))
                .doesNotThrowAnyException();
        verify(emailSendService).sendForgotPasswordCode(eq("u@example.com"), anyString(), any(int.class));
    }

    // --- resetPassword ---

    @Test
    void resetPasswordShouldRejectExpiredCode() {
        when(valueOperations.get("forgot:u@example.com")).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(
                "u@example.com", "123456", "NewPass456", "NewPass456")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORGOT_CODE_EXPIRED);
    }

    @Test
    void resetPasswordShouldRejectWrongCode() {
        when(valueOperations.get("forgot:u@example.com")).thenReturn("111111");
        when(valueOperations.increment("forgot:u@example.com:attempts")).thenReturn(1L);

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(
                "u@example.com", "000000", "NewPass456", "NewPass456")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORGOT_CODE_INVALID);
    }

    @Test
    void resetPasswordShouldRejectWhenAttemptsExceeded() {
        when(valueOperations.get("forgot:u@example.com")).thenReturn("111111");
        when(valueOperations.increment("forgot:u@example.com:attempts")).thenReturn(5L);

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(
                "u@example.com", "000000", "NewPass456", "NewPass456")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORGOT_CODE_ATTEMPTS_EXCEEDED);
    }

    @Test
    void resetPasswordShouldSucceedWithCorrectCode() {
        UserEntity user = user(USER_ID, "x");
        user.setEmail("u@example.com");
        user.setPasswordHash("oldHash");
        when(valueOperations.get("forgot:u@example.com")).thenReturn("123456");
        when(userMapper.selectByEmail("u@example.com")).thenReturn(user);
        when(passwordEncoder.encode("NewPass456")).thenReturn("newHash");

        assertThatCode(() -> service.resetPassword(new ResetPasswordRequest(
                "u@example.com", "123456", "NewPass456", "NewPass456")))
                .doesNotThrowAnyException();
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        verify(userMapper).updateById(user);
    }

    private UserEntity user(Long id, String name) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUserName(name);
        user.setEmail("u@example.com");
        return user;
    }
}
