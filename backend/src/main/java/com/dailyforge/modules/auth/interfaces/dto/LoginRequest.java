package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户登录请求")
public record LoginRequest(
        @Schema(description = "用户邮箱", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "明文密码", example = "PlainTextPassword123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password) {
}
