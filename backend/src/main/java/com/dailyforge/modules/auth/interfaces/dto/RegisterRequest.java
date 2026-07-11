package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "用户注册请求")
public record RegisterRequest(
        @Schema(description = "用户邮箱", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "明文密码", example = "PlainTextPassword123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password,

        @Schema(description = "确认密码", example = "PlainTextPassword123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String confirmPassword,

        @Schema(description = "用户名", example = "daily_user", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 64) String userName,

        @Schema(description = "邀请码", example = "DAILYFORGE-AI-001")
        @Size(max = 64) String inviteCode) {
}
