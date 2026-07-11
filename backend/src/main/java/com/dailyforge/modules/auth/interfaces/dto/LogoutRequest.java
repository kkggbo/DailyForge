package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "退出登录请求")
public record LogoutRequest(
        @Schema(description = "可选的刷新令牌，第一版仅预留", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken) {
}
