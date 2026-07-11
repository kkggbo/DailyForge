package com.dailyforge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties(Duration.ofHours(2), Duration.ofDays(14)));
        UserEntity user = user();

        String accessToken = jwtTokenService.generateTokenPair(user).accessToken();
        AuthUserPrincipal principal = jwtTokenService.parseAccessToken(accessToken);

        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getEmail()).isEqualTo("user@example.com");
        assertThat(principal.getPlatformRole()).isEqualTo("user");
        assertThat(principal.getAccountTier()).isEqualTo("basic");
    }

    @Test
    void shouldGenerateAndParseRefreshToken() {
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties(Duration.ofHours(2), Duration.ofDays(14)));
        UserEntity user = user();

        String refreshToken = jwtTokenService.generateTokenPair(user).refreshToken();

        assertThat(jwtTokenService.parseRefreshToken(refreshToken)).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenTokenTypeMismatch() {
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties(Duration.ofHours(2), Duration.ofDays(14)));
        UserEntity user = user();

        String accessToken = jwtTokenService.generateTokenPair(user).accessToken();

        assertThatThrownBy(() -> jwtTokenService.parseRefreshToken(accessToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_TYPE_MISMATCH);
    }

    @Test
    void shouldThrowWhenTokenExpired() {
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties(Duration.ofSeconds(-1), Duration.ofDays(14)));
        UserEntity user = user();

        String accessToken = jwtTokenService.generateTokenPair(user).accessToken();

        assertThatThrownBy(() -> jwtTokenService.parseAccessToken(accessToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    private JwtProperties jwtProperties(Duration accessTokenTtl, Duration refreshTokenTtl) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("dailyforge-test");
        jwtProperties.setSecret("dailyforge-test-jwt-secret-key-change-me-1234567890");
        jwtProperties.setAccessTokenTtl(accessTokenTtl);
        jwtProperties.setRefreshTokenTtl(refreshTokenTtl);
        return jwtProperties;
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPlatformRole("user");
        user.setAccountTier("basic");
        return user;
    }
}
