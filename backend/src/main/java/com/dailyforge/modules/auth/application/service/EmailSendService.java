package com.dailyforge.modules.auth.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends verification-code emails via QQ SMTP. When mail is not configured (empty credentials),
 * sending fails with {@code EMAIL_SEND_FAILED}; tests mock {@link JavaMailSender}.
 */
@Service
public class EmailSendService {

    private static final Logger log = LoggerFactory.getLogger(EmailSendService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailSendService(
            JavaMailSender mailSender,
            @Value("${spring.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Send a password-reset verification email. Throws {@code EMAIL_SEND_FAILED} when mail is not
     * configured or the underlying sender fails.
     */
    public void sendForgotPasswordCode(String toEmail, String code, int ttlMinutes) {
        if (!StringUtils.hasText(from)) {
            log.warn("Mail not configured, refusing to send forgot-code email. to={}", mask(toEmail));
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("DailyForge 密码找回验证码");
        message.setText("您的密码找回验证码是：" + code + "，有效期 " + ttlMinutes + " 分钟。请勿泄露给他人。");
        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.warn("Failed to send forgot-code email. to={}, cause={}", mask(toEmail), exception.getMessage());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String mask(String email) {
        if (email == null || email.length() < 3) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email.substring(0, 1) + "***" + (at > 0 ? email.substring(at) : "");
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}
