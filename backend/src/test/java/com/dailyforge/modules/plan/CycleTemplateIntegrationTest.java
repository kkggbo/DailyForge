package com.dailyforge.modules.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailyforge.modules.auth.interfaces.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
    void createDraftShouldPersistThreeLayerStructure() throws Exception {
        insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/drafts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Push Pull Legs",
                                  "cycleLength": 6,
                                  "goalType": "muscle_gain",
                                  "days": [
                                    {
                                      "dayIndex": 1,
                                      "dayName": "Push",
                                      "exercises": [
                                        {
                                          "sortOrder": 1,
                                          "exerciseId": %d,
                                          "structureType": "set_based",
                                          "note": "main lift",
                                          "items": [
                                            {
                                              "itemIndex": 1,
                                              "itemType": "set",
                                              "itemName": "Set 1",
                                              "metrics": [
                                                {"sortOrder": 1, "metricKey": "weight_kg", "metricValueNumber": 60},
                                                {"sortOrder": 2, "metricKey": "reps", "metricValueNumber": 8}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_day_exercises", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_day_exercise_items", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_day_exercise_item_metrics", Integer.class)).isEqualTo(2);
    }

    @Test
    void getTemplateDetailShouldReturnThreeLayerStructureAndDerivedMetricUnit() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long templateId = insertTemplate(userId, "PPL", 6, "muscle_gain", "inactive");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        long dayId = insertDay(versionId, 1, "Push");
        long dayExerciseId = insertDayExercise(dayId, exerciseId, "Barbell Bench Press", "set_based", "main lift", 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(itemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(itemId, 2, "reps", new BigDecimal("8"));
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/cycle-templates/" + templateId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].exercises[0].structureType").value("set_based"))
                .andExpect(jsonPath("$.data.days[0].exercises[0].items[0].itemType").value("set"))
                .andExpect(jsonPath("$.data.days[0].exercises[0].items[0].metrics[0].metricUnit").value("kg"))
                .andExpect(jsonPath("$.data.days[0].exercises[0].items[0].metrics[1].metricUnit").value("count"));
    }

    @Test
    void updateDraftShouldCreateNewVersionAndReplaceStructure() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
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
                                          "structureType": "set_based",
                                          "items": [
                                            {
                                              "itemIndex": 1,
                                              "itemType": "set",
                                              "metrics": [
                                                {"sortOrder": 1, "metricKey": "weight_kg", "metricValueNumber": 65},
                                                {"sortOrder": 2, "metricKey": "reps", "metricValueNumber": 6}
                                              ]
                                            },
                                            {
                                              "itemIndex": 2,
                                              "itemType": "set",
                                              "metrics": [
                                                {"sortOrder": 1, "metricKey": "weight_kg", "metricValueNumber": 60},
                                                {"sortOrder": 2, "metricKey": "reps", "metricValueNumber": 8}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(templateId));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cycle_template_versions WHERE template_id = ?", Integer.class, templateId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_day_exercise_items", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_day_exercise_item_metrics", Integer.class))
                .isEqualTo(4);
    }

    @Test
    void copyTemplateShouldCloneItemsAndMetrics() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long templateId = insertTemplate(userId, "Source", 4, "muscle_gain", "inactive");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        long dayId = insertDay(versionId, 1, "Push");
        long dayExerciseId = insertDayExercise(dayId, exerciseId, "Barbell Bench Press", "set_based", null, 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(itemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(itemId, 2, "reps", new BigDecimal("8"));
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/" + templateId + "/copy")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Copied Draft"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_templates", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_day_exercise_items", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cycle_day_exercise_item_metrics", Integer.class)).isEqualTo(4);
    }

    @Test
    void updateActiveTemplateShouldPreserveUnsubmittedFutureDays() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long benchId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long runId = insertSystemExercise("Running", "cardio", "cardio", "minutes", "single_segment", 1);
        long templateId = insertTemplate(userId, "Active Plan", 5, "muscle_gain", "active");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);

        long day3Id = insertDay(versionId, 3, "Push");
        long day3ExerciseId = insertDayExercise(day3Id, benchId, "Barbell Bench Press", "set_based", null, 1);
        long day3ItemId = insertDayExerciseItem(day3ExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(day3ItemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(day3ItemId, 2, "reps", new BigDecimal("8"));

        long day4Id = insertDay(versionId, 4, "Cardio");
        long day4ExerciseId = insertDayExercise(day4Id, runId, "Running", "single_segment", null, 1);
        long day4ItemId = insertDayExerciseItem(day4ExerciseId, 1, "segment", "Main Segment", null);
        insertDayExerciseMetric(day4ItemId, 1, "duration_seconds", new BigDecimal("1800"));

        long activeRunId = insertRun(userId, templateId, versionId, 1, "active");
        insertUserActiveCycle(userId, templateId, versionId, activeRunId, 3);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPut("/cycle-templates/" + templateId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Active Plan v2",
                                  "goalType": "muscle_gain",
                                  "days": [
                                    {
                                      "dayIndex": 3,
                                      "dayName": "Push",
                                      "exercises": [
                                        {
                                          "sortOrder": 1,
                                          "exerciseId": %d,
                                          "structureType": "set_based",
                                          "items": [
                                            {
                                              "itemIndex": 1,
                                              "itemType": "set",
                                              "metrics": [
                                                {"sortOrder": 1, "metricKey": "weight_kg", "metricValueNumber": 65},
                                                {"sortOrder": 2, "metricKey": "reps", "metricValueNumber": 6}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(benchId)))
                .andExpect(status().isOk());

        mockMvc.perform(apiGet("/cycle-templates/" + templateId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(2))
                .andExpect(jsonPath("$.data.days[0].dayIndex").value(3))
                .andExpect(jsonPath("$.data.days[0].exercises[0].items[0].metrics[0].metricValueNumber").value(65))
                .andExpect(jsonPath("$.data.days[1].dayIndex").value(4))
                .andExpect(jsonPath("$.data.days[1].exercises[0].structureType").value("single_segment"));
    }

    @Test
    void updateActiveTemplateShouldRejectCycleLengthChange() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long templateId = insertTemplate(userId, "PPL", 5, "muscle_gain", "active");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        long dayId = insertDay(versionId, 3, "Legs");
        long dayExerciseId = insertDayExercise(dayId, exerciseId, "Barbell Bench Press", "set_based", null, 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(itemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(itemId, 2, "reps", new BigDecimal("8"));
        long activeRunId = insertRun(userId, templateId, versionId, 1, "active");
        insertUserActiveCycle(userId, templateId, versionId, activeRunId, 3);
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
    void createDraftShouldRejectStructureTypeMismatch() throws Exception {
        insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Running", "cardio", "cardio", "minutes", "single_segment", 1);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/drafts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Bad Draft",
                                  "cycleLength": 3,
                                  "days": [
                                    {
                                      "dayIndex": 1,
                                      "dayName": "Cardio",
                                      "exercises": [
                                        {
                                          "sortOrder": 1,
                                          "exerciseId": %d,
                                          "structureType": "set_based",
                                          "items": [
                                            {
                                              "itemIndex": 1,
                                              "itemType": "set",
                                              "metrics": [
                                                {"sortOrder": 1, "metricKey": "duration_seconds", "metricValueNumber": 1800}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID"));
    }

    @Test
    void createDraftShouldRejectInactiveSystemExercise() throws Exception {
        insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Disabled Exercise", "strength", "push", "kg", "set_based", 0);
        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/drafts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Bad Draft",
                                  "cycleLength": 3,
                                  "days": [
                                    {
                                      "dayIndex": 1,
                                      "dayName": "Push",
                                      "exercises": [
                                        {
                                          "sortOrder": 1,
                                          "exerciseId": %d,
                                          "structureType": "set_based",
                                          "items": [
                                            {
                                              "itemIndex": 1,
                                              "itemType": "set",
                                              "metrics": [
                                                {"sortOrder": 1, "metricKey": "weight_kg", "metricValueNumber": 60}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED"));
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

    private long insertSystemExercise(
            String name,
            String exerciseType,
            String movementType,
            String defaultUnit,
            String defaultStructureType,
            int isActive) {
        jdbcTemplate.update("""
                INSERT INTO exercises(owner_user_id, name, exercise_type, movement_type, default_unit, default_structure_type, is_active)
                VALUES (NULL, ?, ?, ?, ?, ?, ?)
                """, name, exerciseType, movementType, defaultUnit, defaultStructureType, isActive);
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

    private long insertDayExercise(
            Long dayId,
            Long exerciseId,
            String exerciseName,
            String structureType,
            String note,
            int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO cycle_day_exercises(template_day_id, exercise_id, exercise_name_snapshot, structure_type, note, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """, dayId, exerciseId, exerciseName, structureType, note, sortOrder);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_day_exercises WHERE template_day_id = ? AND exercise_id = ? AND sort_order = ?",
                Long.class,
                dayId,
                exerciseId,
                sortOrder);
    }

    private long insertDayExerciseItem(
            Long cycleDayExerciseId,
            int itemIndex,
            String itemType,
            String itemName,
            String note) {
        jdbcTemplate.update("""
                INSERT INTO cycle_day_exercise_items(cycle_day_exercise_id, item_index, item_type, item_name, note, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """, cycleDayExerciseId, itemIndex, itemType, itemName, note, itemIndex);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_day_exercise_items WHERE cycle_day_exercise_id = ? AND item_index = ?",
                Long.class,
                cycleDayExerciseId,
                itemIndex);
    }

    private void insertDayExerciseMetric(Long itemId, int sortOrder, String metricKey, BigDecimal metricValueNumber) {
        jdbcTemplate.update("""
                INSERT INTO cycle_day_exercise_item_metrics(exercise_item_id, metric_key, metric_value_number, sort_order)
                VALUES (?, ?, ?, ?)
                """, itemId, metricKey, metricValueNumber, sortOrder);
    }

    private void cleanTables() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM user_active_cycles");
        jdbcTemplate.update("DELETE FROM training_sessions");
        jdbcTemplate.update("DELETE FROM cycle_runs");
        jdbcTemplate.update("DELETE FROM cycle_day_exercise_item_metrics");
        jdbcTemplate.update("DELETE FROM cycle_day_exercise_items");
        jdbcTemplate.update("DELETE FROM cycle_day_exercises");
        jdbcTemplate.update("DELETE FROM cycle_template_days");
        jdbcTemplate.update("DELETE FROM cycle_template_versions");
        jdbcTemplate.update("DELETE FROM cycle_templates");
        jdbcTemplate.update("DELETE FROM exercise_equipments");
        jdbcTemplate.update("DELETE FROM exercise_muscles");
        jdbcTemplate.update("DELETE FROM exercises");
        jdbcTemplate.update("DELETE FROM equipments");
        jdbcTemplate.update("DELETE FROM muscles");
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
}
