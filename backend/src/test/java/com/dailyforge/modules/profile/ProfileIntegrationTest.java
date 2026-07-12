package com.dailyforge.modules.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailyforge.modules.auth.interfaces.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        cleanTables();
    }

    @AfterEach
    void tearDown() {
        cleanTables();
    }

    @Test
    void getBasicProfileShouldReturnCurrentProfileAndWeight() throws Exception {
        long userId = insertUser("profile@example.com", "PlainTextPassword123");
        updateProfile(userId, "male", LocalDate.of(1998, 6, 15), new BigDecimal("178.00"), "fat_loss", "beginner", "Old knee injury");
        insertCurrentSnapshot(userId, new BigDecimal("76.50"), null, new BigDecimal("82.00"));
        insertBodyMetricLog(userId, LocalDate.of(2026, 7, 12), new BigDecimal("76.50"), null, null, false);
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/profile/basic").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("male"))
                .andExpect(jsonPath("$.data.currentWeightKg").value(76.50))
                .andExpect(jsonPath("$.data.latestBodyMetricRecordDate").value("2026-07-12"));
    }

    @Test
    void updateBasicProfileShouldOnlyUpdateProvidedFields() throws Exception {
        long userId = insertUser("profile@example.com", "PlainTextPassword123");
        updateProfile(userId, "male", LocalDate.of(1998, 6, 15), new BigDecimal("178.00"), "fat_loss", "beginner", null);
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPut("/profile/basic")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalType": "muscle_gain",
                                  "injuryNotes": "Shoulder feels tight"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("male"))
                .andExpect(jsonPath("$.data.goalType").value("muscle_gain"))
                .andExpect(jsonPath("$.data.trainingLevel").value("beginner"))
                .andExpect(jsonPath("$.data.injuryNotes").value("Shoulder feels tight"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT goal_type FROM user_profiles WHERE user_id = ?",
                String.class,
                userId)).isEqualTo("muscle_gain");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT training_level FROM user_profiles WHERE user_id = ?",
                String.class,
                userId)).isEqualTo("beginner");
    }

    @Test
    void updateBasicProfileShouldRejectEmptyPayload() throws Exception {
        insertUser("profile@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPut("/profile/basic")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROFILE_UPDATE_EMPTY"));
    }

    @Test
    void getCurrentSnapshotShouldReturnSnapshot() throws Exception {
        long userId = insertUser("profile@example.com", "PlainTextPassword123");
        insertCurrentSnapshot(userId, new BigDecimal("76.50"), new BigDecimal("18.20"), new BigDecimal("82.00"));
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/profile/body-metrics/current").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentWeightKg").value(76.50))
                .andExpect(jsonPath("$.data.currentBodyFatPercent").value(18.20))
                .andExpect(jsonPath("$.data.currentWaistCm").value(82.00));
    }

    @Test
    void createBodyMetricShouldRejectNoteOnlyPayload() throws Exception {
        insertUser("profile@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/profile/body-metrics")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-07-12",
                                  "note": "note only"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BODY_METRIC_EMPTY_RECORD"));
    }

    @Test
    void createBodyMetricShouldPersistHistoryAndSnapshot() throws Exception {
        long userId = insertUser("profile@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/profile/body-metrics")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-07-12",
                                  "weightKg": 76.50,
                                  "bodyFatPercent": 18.20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLatest").value(true))
                .andExpect(jsonPath("$.data.weightKg").value(76.50));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM body_metric_logs", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_weight_kg FROM user_current_body_metrics WHERE user_id = ?",
                BigDecimal.class,
                userId)).isEqualByComparingTo("76.50");
    }

    @Test
    void createBodyMetricShouldMergeSnapshotIncrementally() throws Exception {
        long userId = insertUser("profile@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        createMetric(accessToken, """
                {
                  "recordDate": "2026-07-11",
                  "weightKg": 80.00
                }
                """);
        createMetric(accessToken, """
                {
                  "recordDate": "2026-07-12",
                  "waistCm": 82.00
                }
                """);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_weight_kg FROM user_current_body_metrics WHERE user_id = ?",
                BigDecimal.class,
                userId)).isEqualByComparingTo("80.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_waist_cm FROM user_current_body_metrics WHERE user_id = ?",
                BigDecimal.class,
                userId)).isEqualByComparingTo("82.00");
    }

    @Test
    void deleteLatestBodyMetricShouldSoftDeleteAndRebuildSnapshot() throws Exception {
        long userId = insertUser("profile@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        createMetric(accessToken, """
                {
                  "recordDate": "2026-07-11",
                  "weightKg": 80.00,
                  "waistCm": 83.00
                }
                """);
        createMetric(accessToken, """
                {
                  "recordDate": "2026-07-12",
                  "weightKg": 79.00,
                  "bodyFatPercent": 18.20
                }
                """);

        mockMvc.perform(apiDelete("/profile/body-metrics/latest").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedRecordDate").value("2026-07-12"))
                .andExpect(jsonPath("$.data.deletedWeightKg").value(79.00));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_del FROM body_metric_logs WHERE user_id = ? AND record_date = ?",
                Integer.class,
                userId,
                Date.valueOf(LocalDate.of(2026, 7, 12)))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_weight_kg FROM user_current_body_metrics WHERE user_id = ?",
                BigDecimal.class,
                userId)).isEqualByComparingTo("80.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_body_fat_percent FROM user_current_body_metrics WHERE user_id = ?",
                BigDecimal.class,
                userId)).isNull();
    }

    @Test
    void deleteLatestBodyMetricShouldRejectWhenLatestAlreadyDeleted() throws Exception {
        insertUser("profile@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        createMetric(accessToken, """
                {
                  "recordDate": "2026-07-12",
                  "weightKg": 79.00
                }
                """);

        mockMvc.perform(apiDelete("/profile/body-metrics/latest").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(apiDelete("/profile/body-metrics/latest").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BODY_METRIC_LATEST_ALREADY_DELETED"));
    }

    @Test
    void completionSummaryShouldReflectAiReadiness() throws Exception {
        long userId = insertUser("profile@example.com", "PlainTextPassword123");
        updateProfile(userId, "male", LocalDate.of(1998, 6, 15), new BigDecimal("178.00"), "fat_loss", "beginner", null);
        insertCurrentSnapshot(userId, new BigDecimal("76.50"), null, null);
        String accessToken = loginAndGetAccessToken("profile@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/profile/completion-summary").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basicProfileReady").value(true))
                .andExpect(jsonPath("$.data.hasWeightRecord").value(true))
                .andExpect(jsonPath("$.data.aiPlanReady").value(true))
                .andExpect(jsonPath("$.data.aiNutritionReady").value(true))
                .andExpect(jsonPath("$.data.aiSummaryReady").value(true));
    }

    @Test
    void protectedProfileEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(apiGet("/profile/basic"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(apiPost("/profile/body-metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-07-12",
                                  "weightKg": 80.00
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void createMetric(String accessToken, String payload) throws Exception {
        mockMvc.perform(apiPost("/profile/body-metrics")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(apiPost("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return jsonNode.path("data").path("accessToken").asText();
    }

    private long insertUser(String email, String rawPassword) {
        jdbcTemplate.update("""
                INSERT INTO users(email, password_hash, user_name, platform_role, account_tier, status)
                VALUES (?, ?, ?, 'user', 'basic', 'active')
                """, email, passwordEncoder.encode(rawPassword), "daily_user");
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        jdbcTemplate.update("INSERT INTO user_profiles(user_id) VALUES (?)", userId);
        return userId;
    }

    private void updateProfile(
            long userId,
            String gender,
            LocalDate birthDate,
            BigDecimal heightCm,
            String goalType,
            String trainingLevel,
            String injuryNotes) {
        jdbcTemplate.update("""
                UPDATE user_profiles
                SET gender = ?, birth_date = ?, height_cm = ?, goal_type = ?, training_level = ?, injury_notes = ?
                WHERE user_id = ?
                """, gender, birthDate == null ? null : Date.valueOf(birthDate), heightCm, goalType, trainingLevel, injuryNotes, userId);
    }

    private void insertCurrentSnapshot(Long userId, BigDecimal weightKg, BigDecimal bodyFatPercent, BigDecimal waistCm) {
        jdbcTemplate.update("""
                INSERT INTO user_current_body_metrics(
                    user_id,
                    current_weight_kg,
                    current_body_fat_percent,
                    current_waist_cm
                )
                VALUES (?, ?, ?, ?)
                """, userId, weightKg, bodyFatPercent, waistCm);
    }

    private void insertBodyMetricLog(
            Long userId,
            LocalDate recordDate,
            BigDecimal weightKg,
            BigDecimal bodyFatPercent,
            BigDecimal waistCm,
            boolean isDel) {
        jdbcTemplate.update("""
                INSERT INTO body_metric_logs(
                    user_id,
                    record_date,
                    weight_kg,
                    body_fat_percent,
                    waist_cm,
                    is_del
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, Date.valueOf(recordDate), weightKg, bodyFatPercent, waistCm, isDel ? 1 : 0);
    }

    private void cleanTables() {
        jdbcTemplate.update("DELETE FROM user_current_body_metrics");
        jdbcTemplate.update("DELETE FROM body_metric_logs");
        jdbcTemplate.update("DELETE FROM user_invite_code_usages");
        jdbcTemplate.update("DELETE FROM invite_codes");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM users");
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api" + path).contextPath("/api");
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api" + path).contextPath("/api");
    }

    private MockHttpServletRequestBuilder apiPut(String path) {
        return put("/api" + path).contextPath("/api");
    }

    private MockHttpServletRequestBuilder apiDelete(String path) {
        return delete("/api" + path).contextPath("/api");
    }
}
