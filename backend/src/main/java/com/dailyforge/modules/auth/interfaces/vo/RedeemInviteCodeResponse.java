package com.dailyforge.modules.auth.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "兑换邀请码响应")
public record RedeemInviteCodeResponse(
        @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "账户权益层级", example = "invited_ai", requiredMode = Schema.RequiredMode.REQUIRED)
        String accountTier,

        @Schema(description = "本次兑换的邀请码", example = "DAILYFORGE-AI-001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String inviteCode) {
}
