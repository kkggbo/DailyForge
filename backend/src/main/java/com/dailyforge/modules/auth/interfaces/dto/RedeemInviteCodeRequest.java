package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "兑换邀请码请求")
public record RedeemInviteCodeRequest(
        @Schema(description = "邀请码", example = "DAILYFORGE-AI-001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 64) String code) {
}
