package com.dailyforge.infrastructure.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AuthUserPrincipal {

    private final Long userId;
    private final String email;
    private final String platformRole;
    private final String accountTier;
    private final List<GrantedAuthority> authorities;

    public AuthUserPrincipal(Long userId, String email, String platformRole, String accountTier) {
        this.userId = userId;
        this.email = email;
        this.platformRole = platformRole;
        this.accountTier = accountTier;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + platformRole.toUpperCase()));
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPlatformRole() {
        return platformRole;
    }

    public String getAccountTier() {
        return accountTier;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
