package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "修改密码请求")
public record ChangePasswordRequest(
        @Schema(description = "当前密码", example = "OldPass123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String oldPassword,

        @Schema(description = "新密码：6~18 位", example = "NewPass456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 6, max = 18) String newPassword,

        @Schema(description = "确认新密码", example = "NewPass456",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String confirmPassword) {
}
