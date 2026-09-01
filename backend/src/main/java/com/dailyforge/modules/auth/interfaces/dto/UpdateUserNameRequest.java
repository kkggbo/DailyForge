package com.dailyforge.modules.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "修改用户名请求")
public record UpdateUserNameRequest(
        @Schema(description = "新用户名：2~20 位，中文/字母/数字/下划线", example = "张三",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 2, max = 20)
        @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$", message = "用户名格式非法") String userName) {
}
