package com.dailyforge.modules.auth.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "注册响应")
public record RegisterResponse(
        @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "邮箱", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "用户名", example = "daily_user", requiredMode = Schema.RequiredMode.REQUIRED)
        String userName,

        @Schema(description = "平台角色", example = "user", requiredMode = Schema.RequiredMode.REQUIRED)
        String platformRole,

        @Schema(description = "账户权益层级", example = "invited_ai", requiredMode = Schema.RequiredMode.REQUIRED)
        String accountTier,

        @Schema(description = "是否成功应用邀请码", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean inviteCodeApplied) {
}
