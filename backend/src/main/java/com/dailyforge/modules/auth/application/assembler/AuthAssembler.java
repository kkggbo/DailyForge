package com.dailyforge.modules.auth.application.assembler;

import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.interfaces.vo.AuthTokenResponse;
import com.dailyforge.modules.auth.interfaces.vo.AuthUserSummary;
import com.dailyforge.modules.auth.interfaces.vo.CurrentUserResponse;
import com.dailyforge.modules.auth.interfaces.vo.RedeemInviteCodeResponse;
import com.dailyforge.modules.auth.interfaces.vo.RegisterResponse;
import com.dailyforge.infrastructure.security.JwtTokenService.TokenPair;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthAssembler {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "inviteCodeApplied", source = "inviteCodeApplied")
    RegisterResponse toRegisterResponse(UserEntity user, boolean inviteCodeApplied);

    @Mapping(target = "userId", source = "id")
    AuthUserSummary toAuthUserSummary(UserEntity user);

    @Mapping(target = "userId", source = "id")
    CurrentUserResponse toCurrentUserResponse(UserEntity user);

    default AuthTokenResponse toAuthTokenResponse(TokenPair tokenPair, UserEntity user) {
        return new AuthTokenResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresIn(),
                toAuthUserSummary(user));
    }

    default RedeemInviteCodeResponse toRedeemInviteCodeResponse(UserEntity user, String inviteCode) {
        return new RedeemInviteCodeResponse(user.getId(), user.getAccountTier(), inviteCode);
    }
}
