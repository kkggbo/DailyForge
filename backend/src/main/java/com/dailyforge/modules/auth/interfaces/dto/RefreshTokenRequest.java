package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "刷新令牌请求")
public record RefreshTokenRequest(
        @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String refreshToken) {
}
