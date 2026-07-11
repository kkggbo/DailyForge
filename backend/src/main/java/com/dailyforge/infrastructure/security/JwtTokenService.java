package com.dailyforge.infrastructure.security;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PLATFORM_ROLE = "platformRole";
    private static final String CLAIM_ACCOUNT_TIER = "accountTier";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        if (!StringUtils.hasText(jwtProperties.getSecret()) || jwtProperties.getSecret().length() < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair generateTokenPair(UserEntity user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user.getId());
        long expiresIn = jwtProperties.getAccessTokenTtl().toSeconds();
        log.debug("Generated token pair for userId={}, platformRole={}, accountTier={}",
                user.getId(), user.getPlatformRole(), user.getAccountTier());
        return new TokenPair(accessToken, refreshToken, expiresIn);
    }

    public AuthUserPrincipal parseAccessToken(String token) {
        Claims claims = parseAndValidate(token);
        assertTokenType(claims, TYPE_ACCESS);
        return new AuthUserPrincipal(
                Long.parseLong(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                claims.get(CLAIM_PLATFORM_ROLE, String.class),
                claims.get(CLAIM_ACCOUNT_TIER, String.class));
    }

    public Long parseRefreshToken(String token) {
        Claims claims = parseAndValidate(token);
        assertTokenType(claims, TYPE_REFRESH);
        return Long.parseLong(claims.getSubject());
    }

    private String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getAccessTokenTtl());
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_PLATFORM_ROLE, user.getPlatformRole())
                .claim(CLAIM_ACCOUNT_TIER, user.getAccountTier())
                .signWith(signingKey)
                .compact();
    }

    private String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getRefreshTokenTtl());
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .signWith(signingKey)
                .compact();
    }

    private Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            log.warn("Token parsing failed because token is expired");
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("Token parsing failed because token is invalid");
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    private void assertTokenType(Claims claims, String expectedType) {
        String actualType = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(actualType)) {
            log.warn("Token type mismatch. expectedType={}, actualType={}", expectedType, actualType);
            throw new BusinessException(ErrorCode.TOKEN_TYPE_MISMATCH);
        }
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }
}
