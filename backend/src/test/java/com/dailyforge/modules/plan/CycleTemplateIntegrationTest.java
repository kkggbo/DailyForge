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
        long versionId = insertVersion(templateId, 1, "ai_generated");
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
                .andExpect(jsonPath("$.data.sourceType").value("ai_generated"))
                .andExpect(jsonPath("$.data.days[0].exercises[0].structureType").value("set_based"))
                .andExpect(jsonPath("$.data.days[0].exercises[0].items[0].itemType").value("set"))
                .andExpect(jsonPath("$.data.days[0].exercises[0].items[0].metrics[0].metricUnit").value("kg"))
                .andExpect(jsonPath("$.data.days[0].exercises[0].items[0].metrics[1].metricUnit").value("count"));
    }

    @Test
    void templateListsShouldExposeCurrentVersionSourceType() throws Exception {
        long userId = insertUser("plan-source@example.com", "PlainTextPassword123");
        long formalTemplateId = insertTemplate(userId, "AI Formal", 4, "muscle_gain", "inactive");
        long formalVersionId = insertVersion(formalTemplateId, 1, "ai_generated");
        setTemplateCurrentVersion(formalTemplateId, formalVersionId);
        long draftTemplateId = insertTemplate(userId, "Manual Draft", 5, "fat_loss", "draft");
        long draftVersionId = insertVersion(draftTemplateId, 1, "manual");
        setTemplateCurrentVersion(draftTemplateId, draftVersionId);
        insertDay(formalVersionId, 1, "Push");
        insertDay(draftVersionId, 1, "Legs");
        String accessToken = loginAndGetAccessToken("plan-source@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/cycle-templates/formal")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].sourceType").value("ai_generated"));

        mockMvc.perform(apiGet("/cycle-templates/drafts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].sourceType").value("manual"));
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
                                  "confirmOverwriteCurrentSession": true,
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
    void updateActiveTemplateShouldRefreshCurrentSessionWithoutCreatingFutureSession() throws Exception {
        long userId = insertUser("plan-refresh@example.com", "PlainTextPassword123");
        long benchId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long runningId = insertSystemExercise("Treadmill Running", "cardio", "cardio", "minutes", "single_segment", 1);
        long templateId = insertTemplate(userId, "Active Plan", 4, "muscle_gain", "active");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        long currentDayId = insertDay(versionId, 2, "Push");
        long currentExerciseId = insertDayExercise(currentDayId, benchId, "Barbell Bench Press", "set_based", null, 1);
        long currentItemId = insertDayExerciseItem(currentExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(currentItemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(currentItemId, 2, "reps", new BigDecimal("8"));
        long futureDayId = insertDay(versionId, 3, "Cardio");
        long futureExerciseId = insertDayExercise(futureDayId, runningId, "Treadmill Running", "single_segment", null, 1);
        long futureItemId = insertDayExerciseItem(futureExerciseId, 1, "segment", "Main Segment", null);
        insertDayExerciseMetric(futureItemId, 1, "duration_seconds", new BigDecimal("1200"));
        long runId = insertRun(userId, templateId, versionId, 1, "active");
        insertUserActiveCycle(userId, templateId, versionId, runId, 2);
        String accessToken = loginAndGetAccessToken("plan-refresh@example.com", "PlainTextPassword123");

        MvcResult initializeResult = mockMvc.perform(apiPost("/workouts/current-day/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        long sessionId = responseData(initializeResult).path("day").path("session").path("sessionId").asLong();

        mockMvc.perform(apiPut("/cycle-templates/" + templateId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Active Plan v2",
                                  "goalType": "muscle_gain",
                                  "confirmOverwriteCurrentSession": true,
                                  "days": [
                                    {
                                      "dayIndex": 2,
                                      "dayName": "Cardio",
                                      "exercises": [
                                        {
                                          "sortOrder": 1,
                                          "exerciseId": %d,
                                          "structureType": "single_segment",
                                          "items": [
                                            {
                                              "itemIndex": 1,
                                              "itemType": "segment",
                                              "metrics": [
                                                {"sortOrder": 1, "metricKey": "duration_seconds", "metricValueNumber": 1800}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    },
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
                                                {"sortOrder": 1, "metricKey": "weight_kg", "metricValueNumber": 70}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(runningId, benchId)))
                .andExpect(status().isOk());

        long currentVersionId = jdbcTemplate.queryForObject(
                "SELECT current_version_id FROM cycle_templates WHERE id = ?", Long.class, templateId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT template_version_id FROM training_sessions WHERE id = ?", Long.class, sessionId))
                .isEqualTo(currentVersionId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM training_sessions WHERE cycle_run_id = ? AND day_index = 3", Integer.class, runId))
                .isZero();

        mockMvc.perform(apiGet("/workouts/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dayName").value("Cardio"))
                .andExpect(jsonPath("$.data.exercises.length()").value(1))
                .andExpect(jsonPath("$.data.exercises[0].exerciseName").value("Treadmill Running"))
                .andExpect(jsonPath("$.data.exercises[0].items[0].metrics[0].plannedValueNumber").value(1800));
    }

    @Test
    void updateActiveTemplateShouldOverwriteCurrentSessionWhenTrainingDataExists() throws Exception {
        long userId = insertUser("plan-refresh-filled@example.com", "PlainTextPassword123");
        long benchId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long runningId = insertSystemExercise("Treadmill Running", "cardio", "cardio", "minutes", "single_segment", 1);
        long templateId = insertTemplate(userId, "Active Plan", 3, "muscle_gain", "active");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        long currentDayId = insertDay(versionId, 2, "Push");
        long currentExerciseId = insertDayExercise(currentDayId, benchId, "Barbell Bench Press", "set_based", null, 1);
        long currentItemId = insertDayExerciseItem(currentExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(currentItemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(currentItemId, 2, "reps", new BigDecimal("8"));
        long runId = insertRun(userId, templateId, versionId, 1, "active");
        insertUserActiveCycle(userId, templateId, versionId, runId, 2);
        String accessToken = loginAndGetAccessToken("plan-refresh-filled@example.com", "PlainTextPassword123");

        MvcResult initializeResult = mockMvc.perform(apiPost("/workouts/current-day/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        long sessionId = responseData(initializeResult).path("day").path("session").path("sessionId").asLong();
        jdbcTemplate.update("UPDATE training_sessions SET overall_feeling = 'Low energy', notes = 'Keep the old note' WHERE id = ?", sessionId);
        jdbcTemplate.update("UPDATE training_session_exercises SET exercise_status = 'partial_completed', adjustment_note = 'Shoulder discomfort' WHERE session_id = ?", sessionId);
        jdbcTemplate.update("""
                UPDATE training_session_exercise_item_metrics
                SET actual_value_number = 55
                WHERE metric_key = 'weight_kg'
                  AND session_exercise_item_id IN (
                      SELECT item.id
                      FROM training_session_exercise_items item
                      JOIN training_session_exercises exercise ON exercise.id = item.session_exercise_id
                      WHERE exercise.session_id = ?
                  )
                """, sessionId);

        mockMvc.perform(apiPut("/cycle-templates/" + templateId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Active Plan v2",
                                  "goalType": "muscle_gain",
                                  "confirmOverwriteCurrentSession": true,
                                  "days": [
                                    {
                                      "dayIndex": 2,
                                      "dayName": "Cardio",
                                      "exercises": [
                                        {
                                          "sortOrder": 1,
                                          "exerciseId": %d,
                                          "structureType": "single_segment",
                                          "items": [
                                            {
                                              "itemIndex": 1,
                                              "itemType": "segment",
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
                                """.formatted(runningId)))
                .andExpect(status().isOk());

        long currentVersionId = jdbcTemplate.queryForObject(
                "SELECT current_version_id FROM cycle_templates WHERE id = ?", Long.class, templateId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT template_version_id FROM training_sessions WHERE id = ?", Long.class, sessionId))
                .isEqualTo(currentVersionId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT overall_feeling FROM training_sessions WHERE id = ?", String.class, sessionId)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT notes FROM training_sessions WHERE id = ?", String.class, sessionId)).isNull();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT actual_value_number
                FROM training_session_exercise_item_metrics metric
                JOIN training_session_exercise_items item ON item.id = metric.session_exercise_item_id
                JOIN training_session_exercises exercise ON exercise.id = item.session_exercise_id
                WHERE exercise.session_id = ? AND metric.metric_key = 'duration_seconds'
                """, BigDecimal.class, sessionId)).isNull();
        mockMvc.perform(apiGet("/workouts/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dayName").value("Cardio"))
                .andExpect(jsonPath("$.data.exercises.length()").value(1))
                .andExpect(jsonPath("$.data.exercises[0].exerciseName").value("Treadmill Running"))
                .andExpect(jsonPath("$.data.exercises[0].items[0].metrics[0].plannedValueNumber").value(1800));
    }
    @Test
    void updateActiveTemplateShouldRequireOverwriteConfirmation() throws Exception {
        long userId = insertUser("plan-confirm@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long templateId = insertTemplate(userId, "PPL", 5, "muscle_gain", "active");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        insertDay(versionId, 3, "Push");
        long activeRunId = insertRun(userId, templateId, versionId, 1, "active");
        insertUserActiveCycle(userId, templateId, versionId, activeRunId, 3);
        String accessToken = loginAndGetAccessToken("plan-confirm@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPut("/cycle-templates/" + templateId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "PPL v2",
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_OVERWRITE_CONFIRM_REQUIRED"));
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
                                  "confirmOverwriteCurrentSession": true,
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
    void updateActiveTemplateShouldRejectLockedPastDaySubmission() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long templateId = insertTemplate(userId, "PPL", 5, "muscle_gain", "active");
        long versionId = insertVersion(templateId, 1, "manual");
        setTemplateCurrentVersion(templateId, versionId);
        insertDay(versionId, 2, "Pull");
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
                                  "confirmOverwriteCurrentSession": true,
                                  "days": [
                                    {
                                      "dayIndex": 2,
                                      "dayName": "Pull",
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
                                                {"sortOrder": 1, "metricKey": "weight_kg", "metricValueNumber": 50}
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_EDIT_FORBIDDEN"));
    }

    @Test
    void createDraftShouldRejectDuplicateMetricKey() throws Exception {
        insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
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
                                                {"sortOrder": 1, "metricKey": "reps", "metricValueNumber": 8},
                                                {"sortOrder": 2, "metricKey": "reps", "metricValueNumber": 10}
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
                .andExpect(jsonPath("$.code").value("CYCLE_TEMPLATE_METRIC_DUPLICATE"));
    }

    @Test
    void activateTemplateShouldRequireConfirmationWhenSwitchingExistingActiveTemplate() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);

        long oldTemplateId = insertTemplate(userId, "Current Active", 4, "muscle_gain", "active");
        long oldVersionId = insertVersion(oldTemplateId, 1, "manual");
        setTemplateCurrentVersion(oldTemplateId, oldVersionId);
        insertDay(oldVersionId, 1, "Push");
        long oldRunId = insertRun(userId, oldTemplateId, oldVersionId, 1, "active");
        insertUserActiveCycle(userId, oldTemplateId, oldVersionId, oldRunId, 2);

        long newTemplateId = insertTemplate(userId, "Next Template", 4, "fat_loss", "inactive");
        long newVersionId = insertVersion(newTemplateId, 1, "manual");
        setTemplateCurrentVersion(newTemplateId, newVersionId);
        long newDayId = insertDay(newVersionId, 1, "Push");
        long newExerciseId = insertDayExercise(newDayId, exerciseId, "Barbell Bench Press", "set_based", null, 1);
        long newItemId = insertDayExerciseItem(newExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(newItemId, 1, "weight_kg", new BigDecimal("60"));

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
    void activateTemplateShouldCancelPreviousRunAndKeepCompletedSessions() throws Exception {
        long userId = insertUser("plan@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based", 1);

        long oldTemplateId = insertTemplate(userId, "Current Active", 4, "muscle_gain", "active");
        long oldVersionId = insertVersion(oldTemplateId, 1, "manual");
        setTemplateCurrentVersion(oldTemplateId, oldVersionId);
        long oldCompletedDayId = insertDay(oldVersionId, 1, "Push");
        long oldInProgressDayId = insertDay(oldVersionId, 2, "Pull");
        long oldRunId = insertRun(userId, oldTemplateId, oldVersionId, 1, "active");
        insertUserActiveCycle(userId, oldTemplateId, oldVersionId, oldRunId, 3);
        long completedSessionId = insertTrainingSession(
                userId, oldRunId, oldTemplateId, oldVersionId, oldCompletedDayId, 1, "completed", "workout");
        long inProgressSessionId = insertTrainingSession(
                userId, oldRunId, oldTemplateId, oldVersionId, oldInProgressDayId, 2, "in_progress", "workout");

        long targetTemplateId = insertTemplate(userId, "Next Template", 4, "fat_loss", "inactive");
        long targetVersionId = insertVersion(targetTemplateId, 1, "manual");
        setTemplateCurrentVersion(targetTemplateId, targetVersionId);
        long dayId = insertDay(targetVersionId, 1, "Push");
        long dayExerciseId = insertDayExercise(dayId, exerciseId, "Barbell Bench Press", "set_based", null, 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "set", "Set 1", null);
        insertDayExerciseMetric(itemId, 1, "weight_kg", new BigDecimal("70"));

        String accessToken = loginAndGetAccessToken("plan@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/cycle-templates/" + targetTemplateId + "/activate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmSwitch": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(targetTemplateId))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.currentDayIndex").value(1))
                .andExpect(jsonPath("$.data.previousActiveTemplateId").value(oldTemplateId));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM cycle_templates WHERE id = ?",
                String.class,
                oldTemplateId)).isEqualTo("inactive");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM cycle_runs WHERE id = ?",
                String.class,
                oldRunId)).isEqualTo("cancelled");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM training_sessions WHERE id = ?",
                String.class,
                inProgressSessionId)).isEqualTo("cancelled");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM training_sessions WHERE id = ?",
                String.class,
                completedSessionId)).isEqualTo("completed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT template_id FROM user_active_cycles WHERE user_id = ?",
                Long.class,
                userId)).isEqualTo(targetTemplateId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_day_index FROM user_active_cycles WHERE user_id = ?",
                Integer.class,
                userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cycle_runs WHERE user_id = ? AND template_id = ?",
                Integer.class,
                userId,
                targetTemplateId)).isEqualTo(1);
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

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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

    private long insertTrainingSession(
            Long userId,
            Long cycleRunId,
            Long templateId,
            Long templateVersionId,
            Long templateDayId,
            int dayIndex,
            String status,
            String sessionType) {
        jdbcTemplate.update("""
                INSERT INTO training_sessions(
                    user_id,
                    cycle_run_id,
                    template_id,
                    template_name_snapshot,
                    template_version_id,
                    template_day_id,
                    day_name_snapshot,
                    day_index,
                    status,
                    session_type,
                    started_at,
                    completed_at
                )
                VALUES (?, ?, ?, 'Current Active', ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                """,
                userId,
                cycleRunId,
                templateId,
                templateVersionId,
                templateDayId,
                "Day " + dayIndex,
                dayIndex,
                status,
                sessionType,
                "completed".equals(status) ? java.time.LocalDateTime.now() : null);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM training_sessions WHERE cycle_run_id = ? AND day_index = ?",
                Long.class,
                cycleRunId,
                dayIndex);
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
        jdbcTemplate.update("DELETE FROM training_session_exercise_item_metrics");
        jdbcTemplate.update("DELETE FROM training_session_exercise_items");
        jdbcTemplate.update("DELETE FROM training_session_exercises");
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
