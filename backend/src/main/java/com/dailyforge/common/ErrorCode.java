package com.dailyforge.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS("SUCCESS", "ok", HttpStatus.OK),
    INVALID_ARGUMENT("INVALID_ARGUMENT", "request arguments are invalid", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "authentication is required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "permission denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "resource not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "email already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "account is disabled", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "email or password is incorrect", HttpStatus.UNAUTHORIZED),
    PASSWORD_CONFIRM_MISMATCH("PASSWORD_CONFIRM_MISMATCH", "password and confirmPassword do not match",
            HttpStatus.BAD_REQUEST),
    TOKEN_INVALID("TOKEN_INVALID", "token is invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "token is expired", HttpStatus.UNAUTHORIZED),
    TOKEN_TYPE_MISMATCH("TOKEN_TYPE_MISMATCH", "token type mismatch", HttpStatus.UNAUTHORIZED),
    INVITE_CODE_NOT_FOUND("INVITE_CODE_NOT_FOUND", "invite code not found", HttpStatus.NOT_FOUND),
    INVITE_CODE_DISABLED("INVITE_CODE_DISABLED", "invite code is disabled", HttpStatus.BAD_REQUEST),
    INVITE_CODE_EXPIRED("INVITE_CODE_EXPIRED", "invite code is expired", HttpStatus.BAD_REQUEST),
    INVITE_CODE_EXHAUSTED("INVITE_CODE_EXHAUSTED", "invite code is exhausted", HttpStatus.BAD_REQUEST),
    INVITE_CODE_ALREADY_USED("INVITE_CODE_ALREADY_USED", "invite code has already been used by current user",
            HttpStatus.CONFLICT),
    INVITE_CODE_GRANT_CONFLICT("INVITE_CODE_GRANT_CONFLICT",
            "invite code grant conflicts with current account tier", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
