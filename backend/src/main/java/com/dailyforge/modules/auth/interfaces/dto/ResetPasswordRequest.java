package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "重置密码请求")
public record ResetPasswordRequest(
        @Schema(description = "注册邮箱", example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "6 位验证码", example = "123456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 6, max = 6) String code,

        @Schema(description = "新密码：6~18 位", example = "NewPass456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 6, max = 18) String newPassword,

        @Schema(description = "确认新密码", example = "NewPass456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String confirmPassword) {
}
