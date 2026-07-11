package com.dailyforge.modules.auth.interfaces.rest;

import com.dailyforge.common.ApiResponse;
import com.dailyforge.modules.auth.application.service.AuthApplicationService;
import com.dailyforge.modules.auth.interfaces.dto.LoginRequest;
import com.dailyforge.modules.auth.interfaces.dto.LogoutRequest;
import com.dailyforge.modules.auth.interfaces.dto.RedeemInviteCodeRequest;
import com.dailyforge.modules.auth.interfaces.dto.RefreshTokenRequest;
import com.dailyforge.modules.auth.interfaces.dto.RegisterRequest;
import com.dailyforge.modules.auth.interfaces.vo.AuthTokenResponse;
import com.dailyforge.modules.auth.interfaces.vo.CurrentUserResponse;
import com.dailyforge.modules.auth.interfaces.vo.RedeemInviteCodeResponse;
import com.dailyforge.modules.auth.interfaces.vo.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账号，并初始化用户档案。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或邀请码不可用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "邮箱已存在")
    })
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("AuthController register entered. email={}", request.email());
        return ApiResponse.success(authApplicationService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验邮箱密码并返回访问令牌与刷新令牌。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "密码错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "账号已禁用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "账号不存在")
    })
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("AuthController login entered. email={}", request.email());
        return ApiResponse.success(authApplicationService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户", description = "返回当前登录用户的最新基础身份信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = CurrentUserResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或令牌无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ApiResponse<CurrentUserResponse> me() {
        log.debug("AuthController me entered");
        return ApiResponse.success(authApplicationService.me());
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新访问令牌", description = "使用刷新令牌换取新的令牌对。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刷新成功",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "刷新令牌无效或过期"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "账号已禁用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ApiResponse<AuthTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("AuthController refreshToken entered");
        return ApiResponse.success(authApplicationService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "第一版为空操作，由前端清理本地 token。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "退出成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或令牌无效")
    })
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        log.debug("AuthController logout entered, hasRefreshToken={}",
                request != null && request.refreshToken() != null);
        authApplicationService.logout();
        return ApiResponse.success();
    }

    @PostMapping("/redeem-invite-code")
    @Operation(summary = "兑换邀请码", description = "当前登录用户输入邀请码后提升账户权益。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "兑换成功",
                    content = @Content(schema = @Schema(implementation = RedeemInviteCodeResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "邀请码不可用或权益冲突"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或令牌无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "邀请码或用户不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "邀请码已被当前用户使用")
    })
    public ApiResponse<RedeemInviteCodeResponse> redeemInviteCode(
            @Valid @RequestBody RedeemInviteCodeRequest request) {
        log.debug("AuthController redeemInviteCode entered");
        return ApiResponse.success(authApplicationService.redeemInviteCode(request.code()));
    }
}
