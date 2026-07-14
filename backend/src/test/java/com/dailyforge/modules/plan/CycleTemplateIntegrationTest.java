package com.dailyforge.modules.plan;

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
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
class CycleTemplateIntegrationTest {

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
    void getFormalTemplatesShouldReturnOnlyActiveAndInactive() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        insertSystemExercise("Barbell Bench Press");
        long activeTemplateId = insertTemplate(userId, "PPL", 6, "muscle_gain", "active");
        long inactiveTemplateId = insertTemplate(userId, "Home Cut", 4, "fat_loss", "inactive");
        long draftTemplateId = insertTemplate(userId, "Draft", null, null, "draft");
        long activeVersionId = insertVersion(activeTemplateId, 1, "manual");
        long inactiveVersionId = insertVersion(inactiveTemplateId, 1, "manual");
        long draftVersionId = insertVersion(draftTemplateId, 1, "manual");
        setTemplateCurrentVersion(activeTemplateId, activeVersionId);
        setTemplateCurrentVersion(inactiveTemplateId, inactiveVersionId);
        setTemplateCurrentVersion(draftTemplateId, draftVersionId);
        long activeRunId = insertRun(userId, activeTemplateId, activeVersionId, 1, "active");
        insertUserActiveCycle(userId, activeTemplateId, activeVersionId, activeRunId, 3);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/cycle-templates/formal").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeTemplateId").value(activeTemplateId))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].status").exists());
    }

    @Test
    void createDraftShouldSupportNullCycleLength() throws Exception {
        insertUser("plan@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/drafts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "New Draft",
                                  "cycleLength": null,
                                  "goalType": "fat_loss"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"));

        Integer templateCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_templates", Integer.class);
        Integer versionCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_template_versions", Integer.class);
        assertThat(templateCount).isEqualTo(1);
        assertThat(versionCount).isEqualTo(1);
    }

    @Test
    void updateDraftShouldCreateNewVersion() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press");
        long templateId = insertTemplate(userId, "Draft", null, null, "draft");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPut("/cycle-templates/drafts/" + templateId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Draft v2",
                                  "cycleLength": 5,
                                  "goalType": "muscle_gain",
                                  "days": [
                                    {
                                      "dayIndex": 1,
                                      "dayName": "Push",
                                      "exercises": [
                                        {
                                          "sortOrder": 1,
                                          "exerciseId": %d,
                                          "targetSets": 4,
                                          "targetRepsMin": 6,
                                          "targetRepsMax": 8,
                                          "restSeconds": 180
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(templateId));

        Integer versionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cycle_template_versions WHERE template_id = ?", Integer.class, templateId);
        Integer currentVersionNo = jdbcTemplate.queryForObject(
                "SELECT v.version_no FROM cycle_templates t JOIN cycle_template_versions v ON t.current_version_id = v.id WHERE t.id = ?",
                Integer.class,
                templateId);
        assertThat(versionCount).isEqualTo(2);
        assertThat(currentVersionNo).isEqualTo(2);
    }

    @Test
    void activateTemplateShouldRequireConfirmWhenAnotherTemplateIsActive() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long activeTemplateId = insertTemplate(userId, "Old Active", 5, "muscle_gain", "active");
        long newTemplateId = insertTemplate(userId, "New Template", 4, "fat_loss", "inactive");
        long activeVersionId = insertVersion(activeTemplateId, 1, "manual");
        long newVersionId = insertVersion(newTemplateId, 1, "manual");
        setTemplateCurrentVersion(activeTemplateId, activeVersionId);
        setTemplateCurrentVersion(newTemplateId, newVersionId);
        long activeRunId = insertRun(userId, activeTemplateId, activeVersionId, 1, "active");
        insertUserActiveCycle(userId, activeTemplateId, activeVersionId, activeRunId, 2);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/" + newTemplateId + "/activate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmSwitch": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED"));
    }

    @Test
    void activateTemplateShouldCreateNewRunAndOverwriteActiveContext() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long oldTemplateId = insertTemplate(userId, "Old Active", 5, "muscle_gain", "active");
        long newTemplateId = insertTemplate(userId, "New Template", 4, "fat_loss", "inactive");
        long oldVersionId = insertVersion(oldTemplateId, 1, "manual");
        long newVersionId = insertVersion(newTemplateId, 1, "manual");
        setTemplateCurrentVersion(oldTemplateId, oldVersionId);
        setTemplateCurrentVersion(newTemplateId, newVersionId);
        long oldRunId = insertRun(userId, oldTemplateId, oldVersionId, 1, "active");
        insertUserActiveCycle(userId, oldTemplateId, oldVersionId, oldRunId, 2);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/" + newTemplateId + "/activate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmSwitch": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(newTemplateId))
                .andExpect(jsonPath("$.data.currentDayIndex").value(1))
                .andExpect(jsonPath("$.data.previousActiveTemplateId").value(oldTemplateId));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM cycle_templates WHERE id = ?", String.class, oldTemplateId)).isEqualTo("inactive");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM cycle_templates WHERE id = ?", String.class, newTemplateId)).isEqualTo("active");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_day_index FROM user_active_cycles WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cycle_runs WHERE template_id = ?", Integer.class, newTemplateId)).isEqualTo(1);
    }

    @Test
    void updateActiveTemplateShouldRejectCycleLengthChange() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press");
        long templateId = insertTemplate(userId, "PPL", 5, "muscle_gain", "active");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        long dayId = insertDay(versionId, 3, "Legs");
        insertDayExercise(dayId, exerciseId, "Barbell Bench Press", 1);
        long runId = insertRun(userId, templateId, versionId, 1, "active");
        insertUserActiveCycle(userId, templateId, versionId, runId, 3);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPut("/cycle-templates/" + templateId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "PPL v2",
                                  "goalType": "muscle_gain",
                                  "cycleLength": 6,
                                  "days": [
                                    {
                                      "dayIndex": 3,
                                      "dayName": "Legs",
                                      "exercises": []
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_EDIT_FORBIDDEN"));
    }

    @Test
    void aiGenerateShouldReturnNotImplemented() throws Exception {
        insertUser("plan@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/drafts/ai-generate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalType": "muscle_gain",
                                  "cycleLength": 5,
                                  "prompt": "Create a 5-day split",
                                  "useProfileData": true
                                }
                                """))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED"));
    }

    @Test
    void protectedPlanEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(apiGet("/cycle-templates/formal"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(apiPost("/cycle-templates/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "New Draft"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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

    private long insertSystemExercise(String name) {
        jdbcTemplate.update("""
                INSERT INTO exercises(owner_user_id, name, exercise_type, movement_type, default_unit, is_active)
                VALUES (NULL, ?, 'strength', 'compound', 'reps', 1)
                """, name);
        return jdbcTemplate.queryForObject("SELECT id FROM exercises WHERE name = ?", Long.class, name);
    }

    private long insertTemplate(Long userId, String name, Integer cycleLength, String goalType, String status) {
        jdbcTemplate.update("""
                INSERT INTO cycle_templates(user_id, name, cycle_length, goal_type, status)
                VALUES (?, ?, ?, ?, ?)
                """, userId, name, cycleLength, goalType, status);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_templates WHERE user_id = ? AND name = ?",
                Long.class,
                userId,
                name);
    }

    private long insertVersion(Long templateId, int versionNo, String sourceType) {
        jdbcTemplate.update("""
                INSERT INTO cycle_template_versions(template_id, version_no, source_type)
                VALUES (?, ?, ?)
                """, templateId, versionNo, sourceType);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_template_versions WHERE template_id = ? AND version_no = ?",
                Long.class,
                templateId,
                versionNo);
    }

    private void setTemplateCurrentVersion(Long templateId, Long versionId) {
        jdbcTemplate.update("UPDATE cycle_templates SET current_version_id = ? WHERE id = ?", versionId, templateId);
    }

    private long insertRun(Long userId, Long templateId, Long versionId, int runNo, String status) {
        jdbcTemplate.update("""
                INSERT INTO cycle_runs(user_id, template_id, template_version_id, run_no, status)
                VALUES (?, ?, ?, ?, ?)
                """, userId, templateId, versionId, runNo, status);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_runs WHERE user_id = ? AND template_id = ? AND run_no = ?",
                Long.class,
                userId,
                templateId,
                runNo);
    }

    private void insertUserActiveCycle(Long userId, Long templateId, Long versionId, Long runId, int currentDayIndex) {
        jdbcTemplate.update("""
                INSERT INTO user_active_cycles(user_id, template_id, template_version_id, current_run_id, current_day_index)
                VALUES (?, ?, ?, ?, ?)
                """, userId, templateId, versionId, runId, currentDayIndex);
    }

    private long insertDay(Long versionId, int dayIndex, String dayName) {
        jdbcTemplate.update("""
                INSERT INTO cycle_template_days(template_version_id, day_index, day_name)
                VALUES (?, ?, ?)
                """, versionId, dayIndex, dayName);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_template_days WHERE template_version_id = ? AND day_index = ?",
                Long.class,
                versionId,
                dayIndex);
    }

    private void insertDayExercise(Long dayId, Long exerciseId, String exerciseName, int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO cycle_day_exercises(template_day_id, exercise_id, exercise_name_snapshot, sort_order)
                VALUES (?, ?, ?, ?)
                """, dayId, exerciseId, exerciseName, sortOrder);
    }

    private void cleanTables() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM user_active_cycles");
        jdbcTemplate.update("DELETE FROM training_sessions");
        jdbcTemplate.update("DELETE FROM cycle_runs");
        jdbcTemplate.update("DELETE FROM cycle_day_exercises");
        jdbcTemplate.update("DELETE FROM cycle_template_days");
        jdbcTemplate.update("DELETE FROM cycle_template_versions");
        jdbcTemplate.update("DELETE FROM cycle_templates");
        jdbcTemplate.update("DELETE FROM exercises");
        jdbcTemplate.update("DELETE FROM user_current_body_metrics");
        jdbcTemplate.update("DELETE FROM body_metric_logs");
        jdbcTemplate.update("DELETE FROM user_invite_code_usages");
        jdbcTemplate.update("DELETE FROM invite_codes");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
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

    @SuppressWarnings("unused")
    private MockHttpServletRequestBuilder apiDelete(String path) {
        return delete("/api" + path).contextPath("/api");
    }
}
