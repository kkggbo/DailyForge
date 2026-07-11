package com.dailyforge.modules.auth.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录态用户摘要信息")
public record AuthUserSummary(
        @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "邮箱", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "用户名", example = "daily_user", requiredMode = Schema.RequiredMode.REQUIRED)
        String userName,

        @Schema(description = "平台角色", example = "user", requiredMode = Schema.RequiredMode.REQUIRED)
        String platformRole,

        @Schema(description = "账户权益层级", example = "basic", requiredMode = Schema.RequiredMode.REQUIRED)
        String accountTier) {
}
