package com.dailyforge.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailyforge.modules.auth.application.service.InviteCodeApplicationService;
import com.dailyforge.modules.auth.interfaces.dto.LoginRequest;
import com.dailyforge.modules.auth.interfaces.dto.RedeemInviteCodeRequest;
import com.dailyforge.modules.auth.interfaces.dto.RefreshTokenRequest;
import com.dailyforge.modules.auth.interfaces.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InviteCodeApplicationService inviteCodeApplicationService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_invite_code_usages");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM invite_codes");
        jdbcTemplate.update("DELETE FROM users");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM user_invite_code_usages");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM invite_codes");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void registerShouldCreateUserAndProfile() throws Exception {
        insertInviteCode("DAILYFORGE-AI-001", "account_tier", "invited_ai", 3, 0, "active", LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "user@example.com",
                                "PlainTextPassword123",
                                "PlainTextPassword123",
                                "daily_user",
                                "DAILYFORGE-AI-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountTier").value("invited_ai"))
                .andExpect(jsonPath("$.data.inviteCodeApplied").value(true));

        Integer usersCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users", Integer.class);
        Integer profilesCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_profiles", Integer.class);
        Integer usagesCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_invite_code_usages", Integer.class);

        assertThat(usersCount).isEqualTo(1);
        assertThat(profilesCount).isEqualTo(1);
        assertThat(usagesCount).isEqualTo(1);
    }

    @Test
    void registerShouldRollbackWhenInviteCodeInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "user@example.com",
                                "PlainTextPassword123",
                                "PlainTextPassword123",
                                "daily_user",
                                "NOT-EXISTS"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITE_CODE_NOT_FOUND"));

        Integer usersCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users", Integer.class);
        assertThat(usersCount).isEqualTo(0);
    }

    @Test
    void loginShouldReturnTokens() throws Exception {
        insertUser("user@example.com", "PlainTextPassword123", "daily_user", "basic", "active");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", "PlainTextPassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"));
    }

    @Test
    void loginShouldReturnInvalidCredentialsWhenPasswordMismatch() throws Exception {
        insertUser("user@example.com", "PlainTextPassword123", "daily_user", "basic", "active");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", "WrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void meShouldReturnCurrentUserInfo() throws Exception {
        insertUser("user@example.com", "PlainTextPassword123", "daily_user", "basic", "active");
        String accessToken = loginAndGetToken("user@example.com", "PlainTextPassword123", "accessToken");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.accountTier").value("basic"));
    }

    @Test
    void refreshTokenShouldReturnNewTokenPair() throws Exception {
        insertUser("user@example.com", "PlainTextPassword123", "daily_user", "basic", "active");
        String refreshToken = loginAndGetToken("user@example.com", "PlainTextPassword123", "refreshToken");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"));
    }

    @Test
    void refreshTokenShouldRejectAccessToken() throws Exception {
        insertUser("user@example.com", "PlainTextPassword123", "daily_user", "basic", "active");
        String accessToken = loginAndGetToken("user@example.com", "PlainTextPassword123", "accessToken");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(accessToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_TYPE_MISMATCH"));
    }

    @Test
    void redeemInviteCodeShouldRejectRepeatUsage() throws Exception {
        insertUser("user@example.com", "PlainTextPassword123", "daily_user", "basic", "active");
        insertInviteCode("DAILYFORGE-AI-001", "account_tier", "invited_ai", 3, 0, "active", LocalDateTime.now().plusDays(1));
        String accessToken = loginAndGetToken("user@example.com", "PlainTextPassword123", "accessToken");

        mockMvc.perform(post("/api/auth/redeem-invite-code")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemInviteCodeRequest("DAILYFORGE-AI-001"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/redeem-invite-code")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemInviteCodeRequest("DAILYFORGE-AI-001"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITE_CODE_ALREADY_USED"));
    }

    @Test
    void redeemInviteCodeShouldRejectExhaustedInviteCode() throws Exception {
        insertUser("user@example.com", "PlainTextPassword123", "daily_user", "basic", "active");
        insertInviteCode("DAILYFORGE-AI-001", "account_tier", "invited_ai", 1, 1, "active", LocalDateTime.now().plusDays(1));
        String accessToken = loginAndGetToken("user@example.com", "PlainTextPassword123", "accessToken");

        mockMvc.perform(post("/api/auth/redeem-invite-code")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemInviteCodeRequest("DAILYFORGE-AI-001"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_CODE_EXHAUSTED"));
    }

    @Test
    void protectedEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/redeem-invite-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemInviteCodeRequest("DAILYFORGE-AI-001"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void concurrentRedeemShouldNotOverIssueInviteCode() throws Exception {
        long userId1 = insertUser("user1@example.com", "PlainTextPassword123", "daily_user_1", "basic", "active");
        long userId2 = insertUser("user2@example.com", "PlainTextPassword123", "daily_user_2", "basic", "active");
        insertInviteCode("DAILYFORGE-AI-001", "account_tier", "invited_ai", 1, 0, "active", LocalDateTime.now().plusDays(1));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        List<Future<Boolean>> futures = new ArrayList<>();
        futures.add(executorService.submit(() -> redeemInThread(userId1, ready, start)));
        futures.add(executorService.submit(() -> redeemInThread(userId2, ready, start)));

        ready.await();
        start.countDown();

        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }
        executorService.shutdown();

        Integer usedCount = jdbcTemplate.queryForObject(
                "SELECT used_count FROM invite_codes WHERE code = 'DAILYFORGE-AI-001'", Integer.class);
        Integer usageRows = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_invite_code_usages", Integer.class);

        assertThat(successCount).isEqualTo(1);
        assertThat(usedCount).isEqualTo(1);
        assertThat(usageRows).isEqualTo(1);
    }

    private boolean redeemInThread(Long userId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            inviteCodeApplicationService.redeemInviteCode(userId, "DAILYFORGE-AI-001");
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private String loginAndGetToken(String email, String password, String fieldName) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return jsonNode.path("data").path(fieldName).asText();
    }

    private long insertUser(String email, String rawPassword, String userName, String accountTier, String status) {
        jdbcTemplate.update("""
                INSERT INTO users(email, password_hash, user_name, platform_role, account_tier, status)
                VALUES (?, ?, ?, 'user', ?, ?)
                """, email, passwordEncoder.encode(rawPassword), userName, accountTier, status);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        jdbcTemplate.update("INSERT INTO user_profiles(user_id) VALUES (?)", userId);
        return userId;
    }

    private void insertInviteCode(
            String code,
            String grantType,
            String grantValue,
            int maxUses,
            int usedCount,
            String status,
            LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO invite_codes(code, information, grant_type, grant_value, max_uses, used_count, expires_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                code,
                "test invite code",
                grantType,
                grantValue,
                maxUses,
                usedCount,
                Timestamp.valueOf(expiresAt),
                status);
    }
}
