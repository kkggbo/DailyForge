package com.dailyforge.modules.auth.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "认证令牌响应")
public record AuthTokenResponse(
        @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,

        @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken,

        @Schema(description = "访问令牌有效秒数", example = "7200",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long expiresIn,

        @Schema(description = "当前用户摘要信息", requiredMode = Schema.RequiredMode.REQUIRED)
        AuthUserSummary user) {
}
