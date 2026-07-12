package com.dailyforge.modules.auth.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.infrastructure.security.JwtTokenService;
import com.dailyforge.infrastructure.security.JwtTokenService.TokenPair;
import com.dailyforge.modules.auth.application.assembler.AuthAssembler;
import com.dailyforge.modules.auth.domain.service.PasswordPolicyService;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import com.dailyforge.modules.auth.interfaces.dto.LoginRequest;
import com.dailyforge.modules.auth.interfaces.dto.RefreshTokenRequest;
import com.dailyforge.modules.auth.interfaces.dto.RegisterRequest;
import com.dailyforge.modules.auth.interfaces.vo.AuthTokenResponse;
import com.dailyforge.modules.auth.interfaces.vo.CurrentUserResponse;
import com.dailyforge.modules.auth.interfaces.vo.RedeemInviteCodeResponse;
import com.dailyforge.modules.auth.interfaces.vo.RegisterResponse;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final InviteCodeApplicationService inviteCodeApplicationService;
    private final JwtTokenService jwtTokenService;
    private final AuthAssembler authAssembler;

    public AuthApplicationService(
            UserMapper userMapper,
            UserProfileMapper userProfileMapper,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            InviteCodeApplicationService inviteCodeApplicationService,
            JwtTokenService jwtTokenService,
            AuthAssembler authAssembler) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.inviteCodeApplicationService = inviteCodeApplicationService;
        this.jwtTokenService = jwtTokenService;
        this.authAssembler = authAssembler;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.debug("Register started. email={}, hasInviteCode={}", request.email(), StringUtils.hasText(request.inviteCode()));
        passwordPolicyService.validatePasswordConfirmation(request.password(), request.confirmPassword());

        if (userMapper.selectByEmail(request.email()) != null) {
            log.warn("Register failed because email already exists. email={}", request.email());
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (StringUtils.hasText(request.inviteCode())) {
            inviteCodeApplicationService.validateInviteCode(request.inviteCode());
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserName(request.userName());
        user.setPlatformRole("user");
        user.setAccountTier("basic");
        user.setStatus("active");
        userMapper.insert(user);

        UserProfileEntity profileEntity = new UserProfileEntity();
        profileEntity.setUserId(user.getId());
        userProfileMapper.insert(profileEntity);

        boolean inviteCodeApplied = false;
        if (StringUtils.hasText(request.inviteCode())) {
            InviteCodeApplicationService.InviteCodeRedemptionResult result =
                    inviteCodeApplicationService.redeemInviteCode(user.getId(), request.inviteCode());
            user.setAccountTier(result.accountTier());
            inviteCodeApplied = true;
        }

        log.info("Register succeeded. userId={}, email={}, inviteCodeApplied={}",
                user.getId(), user.getEmail(), inviteCodeApplied);
        return authAssembler.toRegisterResponse(user, inviteCodeApplied);
    }

    public AuthTokenResponse login(LoginRequest request) {
        UserEntity user = userMapper.selectByEmail(request.email());
        if (user == null) {
            log.warn("Login failed because user does not exist. email={}", request.email());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        ensureUserIsActive(user);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed because password mismatch. email={}", request.email());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        TokenPair tokenPair = jwtTokenService.generateTokenPair(user);
        log.info("Login succeeded. userId={}, platformRole={}, accountTier={}",
                user.getId(), user.getPlatformRole(), user.getAccountTier());
        return authAssembler.toAuthTokenResponse(tokenPair, user);
    }

    public CurrentUserResponse me() {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        ensureUserIsActive(user);
        return authAssembler.toCurrentUserResponse(user);
    }

    public AuthTokenResponse refreshToken(RefreshTokenRequest request) {
        Long userId = jwtTokenService.parseRefreshToken(request.refreshToken());
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        ensureUserIsActive(user);
        TokenPair tokenPair = jwtTokenService.generateTokenPair(user);
        return authAssembler.toAuthTokenResponse(tokenPair, user);
    }

    public void logout() {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        log.info("Logout accepted. userId={}", userId);
    }

    @Transactional
    public RedeemInviteCodeResponse redeemInviteCode(String code) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        InviteCodeApplicationService.InviteCodeRedemptionResult result =
                inviteCodeApplicationService.redeemInviteCode(userId, code);
        UserEntity user = userMapper.selectById(userId);
        return authAssembler.toRedeemInviteCodeResponse(user, result.inviteCode());
    }

    private void ensureUserIsActive(UserEntity user) {
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }
}
