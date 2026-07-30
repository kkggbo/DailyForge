package com.dailyforge.modules.workout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class WorkoutIntegrationTest {

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
    void getContextShouldReturnNoActiveTemplateWhenUserHasNoActiveCycle() throws Exception {
        insertUser("workout@example.com", "PlainTextPassword123");
        String accessToken = loginAndGetAccessToken("workout@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/workouts/context").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workspaceState").value("no_active_template"))
                .andExpect(jsonPath("$.data.templateId").isEmpty())
                .andExpect(jsonPath("$.data.days").isEmpty());
    }

    @Test
    void initializeCurrentDaySessionShouldBeIdempotentAndCopyPlanSnapshot() throws Exception {
        long userId = insertUser("workout@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based");
        ActiveCycleFixture fixture = createActiveCycle(userId, 2, 1);
        long dayId = insertDay(fixture.versionId(), 1, "Push");
        long dayExerciseId = insertDayExercise(dayId, exerciseId, "Barbell Bench Press", "set_based", 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "set", "Set 1");
        insertDayExerciseMetric(itemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(itemId, 2, "reps", new BigDecimal("8"));
        insertDay(fixture.versionId(), 2, "Rest");
        String accessToken = loginAndGetAccessToken("workout@example.com", "PlainTextPassword123");

        MvcResult firstResult = mockMvc.perform(apiPost("/workouts/current-day/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionCreated").value(true))
                .andExpect(jsonPath("$.data.day.session.sessionStatus").value("in_progress"))
                .andExpect(jsonPath("$.data.day.session.exercises[0].exerciseName").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.data.day.session.exercises[0].items[0].metrics[0].plannedValueNumber").value(60))
                .andReturn();
        long sessionId = responseData(firstResult).path("day").path("session").path("sessionId").asLong();

        mockMvc.perform(apiPost("/workouts/current-day/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionCreated").value(false))
                .andExpect(jsonPath("$.data.day.session.sessionId").value(sessionId));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM training_sessions", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM training_session_exercises WHERE session_id = ?", Integer.class, sessionId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM training_session_exercise_item_metrics metric
                JOIN training_session_exercise_items item ON item.id = metric.session_exercise_item_id
                JOIN training_session_exercises exercise ON exercise.id = item.session_exercise_id
                WHERE exercise.session_id = ?
                """, Integer.class, sessionId)).isEqualTo(2);
    }

    @Test
    void getFutureDayShouldPreviewPlanWithoutCreatingSession() throws Exception {
        long userId = insertUser("workout@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Running", "cardio", "cardio", "minutes", "single_segment");
        ActiveCycleFixture fixture = createActiveCycle(userId, 2, 1);
        insertDay(fixture.versionId(), 1, "Push");
        long futureDayId = insertDay(fixture.versionId(), 2, "Cardio");
        long dayExerciseId = insertDayExercise(futureDayId, exerciseId, "Running", "single_segment", 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "segment", "Main Segment");
        insertDayExerciseMetric(itemId, 1, "duration_seconds", new BigDecimal("1800"));
        String accessToken = loginAndGetAccessToken("workout@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/workouts/days/2").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dayState").value("upcoming"))
                .andExpect(jsonPath("$.data.viewMode").value("preview"))
                .andExpect(jsonPath("$.data.canInitializeSession").value(false))
                .andExpect(jsonPath("$.data.session").isEmpty())
                .andExpect(jsonPath("$.data.exercises[0].exerciseName").value("Running"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM training_sessions WHERE cycle_run_id = ?", Integer.class, fixture.runId()))
                .isZero();
    }

    @Test
    void completeWorkoutDayShouldCompleteSessionAndAdvanceCycle() throws Exception {
        long userId = insertUser("workout@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Barbell Bench Press", "strength", "push", "kg", "set_based");
        ActiveCycleFixture fixture = createActiveCycle(userId, 2, 1);
        long dayId = insertDay(fixture.versionId(), 1, "Push");
        long dayExerciseId = insertDayExercise(dayId, exerciseId, "Barbell Bench Press", "set_based", 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "set", "Set 1");
        insertDayExerciseMetric(itemId, 1, "weight_kg", new BigDecimal("60"));
        insertDayExerciseMetric(itemId, 2, "reps", new BigDecimal("8"));
        insertDay(fixture.versionId(), 2, "Rest");
        String accessToken = loginAndGetAccessToken("workout@example.com", "PlainTextPassword123");

        MvcResult initializeResult = mockMvc.perform(apiPost("/workouts/current-day/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode session = responseData(initializeResult).path("day").path("session");
        long sessionId = session.path("sessionId").asLong();
        long sessionExerciseId = session.path("exercises").get(0).path("sessionExerciseId").asLong();

        mockMvc.perform(apiPost("/workouts/sessions/" + sessionId + "/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "overallFeeling": "Strong session",
                                  "notes": "Increase weight next cycle",
                                  "exercises": [
                                    {
                                      "sessionExerciseId": %d,
                                      "exerciseStatus": "completed",
                                      "failureReason": null,
                                      "feeling": "Good",
                                      "adjustmentNote": "Reduce warm-up push-ups",
                                      "items": [
                                        {
                                          "itemIndex": 1,
                                          "metrics": [
                                            {"metricKey": "weight_kg", "actualValueNumber": 60},
                                            {"metricKey": "reps", "actualValueNumber": 8}
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(sessionExerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("completed"))
                .andExpect(jsonPath("$.data.completedDayIndex").value(1))
                .andExpect(jsonPath("$.data.cycleRunStatus").value("active"))
                .andExpect(jsonPath("$.data.nextCurrentDayIndex").value(2))
                .andExpect(jsonPath("$.data.nextDay.isRestDay").value(true))
                .andExpect(jsonPath("$.data.completedDay.viewMode").value("readonly"))
                .andExpect(jsonPath("$.data.completedDay.session.notes")
                        .value("Strong session\nIncrease weight next cycle"))
                .andExpect(jsonPath("$.data.completedDay.session.overallFeeling").doesNotExist())
                .andExpect(jsonPath("$.data.completedDay.session.exercises[0].feedback")
                        .value("Good\nReduce warm-up push-ups"))
                .andExpect(jsonPath("$.data.completedDay.session.exercises[0].feeling").doesNotExist())
                .andExpect(jsonPath("$.data.completedDay.session.exercises[0].adjustmentNote").doesNotExist());

        mockMvc.perform(apiGet("/workouts/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes").value("Strong session\nIncrease weight next cycle"))
                .andExpect(jsonPath("$.data.overallFeeling").doesNotExist())
                .andExpect(jsonPath("$.data.exercises[0].feedback").value("Good\nReduce warm-up push-ups"))
                .andExpect(jsonPath("$.data.exercises[0].feeling").doesNotExist())
                .andExpect(jsonPath("$.data.exercises[0].adjustmentNote").doesNotExist());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM training_sessions WHERE id = ?", String.class, sessionId)).isEqualTo("completed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_day_index FROM user_active_cycles WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT actual_value_number FROM training_session_exercise_item_metrics "
                        + "WHERE metric_key = 'weight_kg'",
                BigDecimal.class)).isEqualByComparingTo("60");
    }

    @Test
    void completeCompletedExerciseShouldFillMissingActualValuesFromPlan() throws Exception {
        long userId = insertUser("workout-autofill@example.com", "PlainTextPassword123");
        long exerciseId = insertSystemExercise("Dumbbell Row", "strength", "pull", "kg", "set_based");
        ActiveCycleFixture fixture = createActiveCycle(userId, 1, 1);
        long dayId = insertDay(fixture.versionId(), 1, "Pull");
        long dayExerciseId = insertDayExercise(dayId, exerciseId, "Dumbbell Row", "set_based", 1);
        long itemId = insertDayExerciseItem(dayExerciseId, 1, "set", "Set 1");
        insertDayExerciseMetric(itemId, 1, "weight_kg", new BigDecimal("32.50"));
        insertDayExerciseMetric(itemId, 2, "reps", new BigDecimal("10"));
        String accessToken = loginAndGetAccessToken("workout-autofill@example.com", "PlainTextPassword123");

        MvcResult initializeResult = mockMvc.perform(apiPost("/workouts/current-day/session")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode session = responseData(initializeResult).path("day").path("session");
        long sessionId = session.path("sessionId").asLong();
        long sessionExerciseId = session.path("exercises").get(0).path("sessionExerciseId").asLong();

        mockMvc.perform(apiPost("/workouts/sessions/" + sessionId + "/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "overallFeeling": null,
                                  "notes": "Everything matched the plan",
                                  "exercises": [
                                    {
                                      "sessionExerciseId": %d,
                                      "exerciseStatus": "completed",
                                      "failureReason": null,
                                      "feeling": null,
                                      "adjustmentNote": null,
                                      "feedback": "No deviation from plan",
                                      "items": [
                                        {
                                          "itemIndex": 1,
                                          "metrics": [
                                            {"metricKey": "weight_kg", "actualValueNumber": null},
                                            {"metricKey": "reps", "actualValueNumber": null}
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(sessionExerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("completed"))
                .andExpect(jsonPath("$.data.cycleRunStatus").value("completed"))
                .andExpect(jsonPath("$.data.completedDay.session.notes").value("Everything matched the plan"))
                .andExpect(jsonPath("$.data.completedDay.session.overallFeeling").doesNotExist())
                .andExpect(jsonPath("$.data.completedDay.session.exercises[0].feedback")
                        .value("No deviation from plan"))
                .andExpect(jsonPath("$.data.completedDay.session.exercises[0].feeling").doesNotExist())
                .andExpect(jsonPath("$.data.completedDay.session.exercises[0].adjustmentNote").doesNotExist());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT actual_value_number FROM training_session_exercise_item_metrics WHERE metric_key = 'weight_kg'",
                BigDecimal.class)).isEqualByComparingTo("32.50");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT actual_value_number FROM training_session_exercise_item_metrics WHERE metric_key = 'reps'",
                BigDecimal.class)).isEqualByComparingTo("10");
    }

    @Test
    void restartCompletedCycleShouldCreateNextRunAndResetCurrentDay() throws Exception {
        long userId = insertUser("workout@example.com", "PlainTextPassword123");
        long templateId = insertTemplate(userId, "Completed Plan", 1, "active");
        long versionId = insertVersion(templateId, 1);
        setTemplateCurrentVersion(templateId, versionId);
        insertDay(versionId, 1, "Push");
        long completedRunId = insertRun(userId, templateId, versionId, 1, "completed");
        insertUserActiveCycle(userId, templateId, versionId, completedRunId, 1);
        String accessToken = loginAndGetAccessToken("workout@example.com", "PlainTextPassword123");

        mockMvc.perform(apiPost("/workouts/cycles/current/restart")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(templateId))
                .andExpect(jsonPath("$.data.runNo").value(2))
                .andExpect(jsonPath("$.data.cycleRunStatus").value("active"))
                .andExpect(jsonPath("$.data.currentDayIndex").value(1));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cycle_runs WHERE user_id = ? AND template_id = ?", Integer.class, userId, templateId))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM cycle_runs WHERE user_id = ? AND template_id = ? AND run_no = 2",
                String.class,
                userId,
                templateId)).isEqualTo("active");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_day_index FROM user_active_cycles WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(1);
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
        return responseData(mvcResult).path("accessToken").asText();
    }

    private long insertUser(String email, String rawPassword) {
        jdbcTemplate.update("""
                INSERT INTO users(email, password_hash, user_name, platform_role, account_tier, status)
                VALUES (?, ?, 'daily_user', 'user', 'basic', 'active')
                """, email, passwordEncoder.encode(rawPassword));
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        jdbcTemplate.update("INSERT INTO user_profiles(user_id) VALUES (?)", userId);
        return userId;
    }

    private ActiveCycleFixture createActiveCycle(long userId, int cycleLength, int currentDayIndex) {
        long templateId = insertTemplate(userId, "Workout Plan", cycleLength, "active");
        long versionId = insertVersion(templateId, 1);
        setTemplateCurrentVersion(templateId, versionId);
        long runId = insertRun(userId, templateId, versionId, 1, "active");
        insertUserActiveCycle(userId, templateId, versionId, runId, currentDayIndex);
        return new ActiveCycleFixture(templateId, versionId, runId);
    }

    private long insertSystemExercise(
            String name,
            String exerciseType,
            String movementType,
            String defaultUnit,
            String defaultStructureType) {
        jdbcTemplate.update("""
                INSERT INTO exercises(owner_user_id, name, exercise_type, movement_type, default_unit, default_structure_type, is_active)
                VALUES (NULL, ?, ?, ?, ?, ?, 1)
                """, name, exerciseType, movementType, defaultUnit, defaultStructureType);
        return jdbcTemplate.queryForObject("SELECT id FROM exercises WHERE name = ?", Long.class, name);
    }

    private long insertTemplate(long userId, String name, int cycleLength, String status) {
        jdbcTemplate.update("""
                INSERT INTO cycle_templates(user_id, name, cycle_length, goal_type, status)
                VALUES (?, ?, ?, 'muscle_gain', ?)
                """, userId, name, cycleLength, status);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_templates WHERE user_id = ? AND name = ?", Long.class, userId, name);
    }

    private long insertVersion(long templateId, int versionNo) {
        jdbcTemplate.update("""
                INSERT INTO cycle_template_versions(template_id, version_no, source_type)
                VALUES (?, ?, 'manual')
                """, templateId, versionNo);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cycle_template_versions WHERE template_id = ? AND version_no = ?",
                Long.class,
                templateId,
                versionNo);
    }

    private void setTemplateCurrentVersion(long templateId, long versionId) {
        jdbcTemplate.update("UPDATE cycle_templates SET current_version_id = ? WHERE id = ?", versionId, templateId);
    }

    private long insertRun(long userId, long templateId, long versionId, int runNo, String status) {
        jdbcTemplate.update("""
                INSERT INTO cycle_runs(user_id, template_id, template_version_id, run_no, status)
                VALUES (?, ?, ?, ?, ?)
                """, userId, templateId, versionId, runNo, status);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM cycle_runs
                WHERE user_id = ? AND template_id = ? AND run_no = ?
                """, Long.class, userId, templateId, runNo);
    }

    private void insertUserActiveCycle(long userId, long templateId, long versionId, long runId, int currentDayIndex) {
        jdbcTemplate.update("""
                INSERT INTO user_active_cycles(user_id, template_id, template_version_id, current_run_id, current_day_index)
                VALUES (?, ?, ?, ?, ?)
                """, userId, templateId, versionId, runId, currentDayIndex);
    }

    private long insertDay(long versionId, int dayIndex, String dayName) {
        jdbcTemplate.update("""
                INSERT INTO cycle_template_days(template_version_id, day_index, day_name)
                VALUES (?, ?, ?)
                """, versionId, dayIndex, dayName);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM cycle_template_days
                WHERE template_version_id = ? AND day_index = ?
                """, Long.class, versionId, dayIndex);
    }

    private long insertDayExercise(
            long dayId,
            long exerciseId,
            String exerciseName,
            String structureType,
            int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO cycle_day_exercises(
                    template_day_id, exercise_id, exercise_name_snapshot, structure_type, sort_order)
                VALUES (?, ?, ?, ?, ?)
                """, dayId, exerciseId, exerciseName, structureType, sortOrder);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM cycle_day_exercises
                WHERE template_day_id = ? AND exercise_id = ? AND sort_order = ?
                """, Long.class, dayId, exerciseId, sortOrder);
    }

    private long insertDayExerciseItem(long dayExerciseId, int itemIndex, String itemType, String itemName) {
        jdbcTemplate.update("""
                INSERT INTO cycle_day_exercise_items(
                    cycle_day_exercise_id, item_index, item_type, item_name, sort_order)
                VALUES (?, ?, ?, ?, ?)
                """, dayExerciseId, itemIndex, itemType, itemName, itemIndex);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM cycle_day_exercise_items
                WHERE cycle_day_exercise_id = ? AND item_index = ?
                """, Long.class, dayExerciseId, itemIndex);
    }

    private void insertDayExerciseMetric(long itemId, int sortOrder, String metricKey, BigDecimal value) {
        jdbcTemplate.update("""
                INSERT INTO cycle_day_exercise_item_metrics(exercise_item_id, metric_key, metric_value_number, sort_order)
                VALUES (?, ?, ?, ?)
                """, itemId, metricKey, value, sortOrder);
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

    private record ActiveCycleFixture(long templateId, long versionId, long runId) {
    }
}
