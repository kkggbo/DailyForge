package com.dailyforge.modules.exercise;

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
class ExerciseIntegrationTest {

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
    void getSystemExercisesShouldReturnOnlyActiveSystemExercisesWithFullFields() throws Exception {
        long userId = insertUser("exercise@example.com", "PlainTextPassword123");
        long chestId = insertMuscle("Chest Middle", "pectoralis_major_middle", null, "subgroup", 12);
        long tricepsId = insertMuscle("Triceps", "triceps_brachii", null, "group", 50);
        long barbellId = insertEquipment("Barbell", "gym", 1);
        long benchId = insertEquipment("Bench", "gym", 1);
        long systemExerciseId = insertExercise(null, "Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        long customExerciseId = insertExercise(userId, "Custom Push-Up", "strength", "push", "reps", "set_based", 1);
        insertExercise(null, "Disabled Bench Press", "strength", "push", "kg", "set_based", 0);
        insertExerciseMuscle(systemExerciseId, chestId, "primary", 1);
        insertExerciseMuscle(systemExerciseId, tricepsId, "secondary", 2);
        insertExerciseEquipment(systemExerciseId, barbellId, "required", 1);
        insertExerciseEquipment(systemExerciseId, benchId, "required", 2);
        insertExerciseMuscle(customExerciseId, chestId, "primary", 1);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseId").value(systemExerciseId))
                .andExpect(jsonPath("$.data.records[0].exerciseName").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.data.records[0].defaultStructureType").value("set_based"))
                .andExpect(jsonPath("$.data.records[0].primaryMuscles[0].muscleName").value("Chest Middle"))
                .andExpect(jsonPath("$.data.records[0].primaryMuscles[0].muscleCode").value("pectoralis_major_middle"))
                .andExpect(jsonPath("$.data.records[0].secondaryMuscles[0].muscleName").value("Triceps"))
                .andExpect(jsonPath("$.data.records[0].equipmentNames[0]").value("Barbell"));
    }

    @Test
    void getSystemExercisesShouldFilterByKeywordTypeMovementAndStructure() throws Exception {
        insertUser("exercise@example.com", "PlainTextPassword123");
        insertExercise(null, "Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        insertExercise(null, "Running", "cardio", "cardio", "minutes", "single_segment", 1);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system?keyword=Bench")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseName").value("Barbell Bench Press"));

        mockMvc.perform(apiGet("/exercises/system?exerciseType=cardio&movementType=cardio&structureType=single_segment")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseName").value("Running"));
    }

    @Test
    void getSystemExerciseFilterOptionsShouldReturnFixedCategoriesAndResolvedChildren() throws Exception {
        long userId = insertUser("exercise@example.com", "PlainTextPassword123");
        long chestGroupId = insertMuscle("Chest", "pectoralis_major", null, "group", 10);
        insertMuscle("Chest Upper", "pectoralis_major_upper", chestGroupId, "subgroup", 11);
        insertMuscle("Chest Middle", "pectoralis_major_middle", chestGroupId, "subgroup", 12);
        insertMuscle("Chest Lower", "pectoralis_major_lower", chestGroupId, "subgroup", 13);
        long backGroupId = insertMuscle("Back", "back", null, "group", 20);
        insertMuscle("Latissimus Dorsi", "latissimus_dorsi", backGroupId, "subgroup", 21);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system/filter-options").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories.length()").value(7))
                .andExpect(jsonPath("$.data.categories[0].categoryCode").value("chest"))
                .andExpect(jsonPath("$.data.categories[0].children[0].muscleCode").value("pectoralis_major_upper"))
                .andExpect(jsonPath("$.data.categories[0].children[0].parentMuscleName").value("Chest"))
                .andExpect(jsonPath("$.data.categories[6].categoryCode").value("cardio"))
                .andExpect(jsonPath("$.data.categories[6].children.length()").value(0));
    }

    @Test
    void getSystemExercisesShouldFilterByCategoryKeywordAndExactMuscleTogether() throws Exception {
        insertUser("exercise@example.com", "PlainTextPassword123");
        long chestGroupId = insertMuscle("Chest", "pectoralis_major", null, "group", 10);
        long chestUpperId = insertMuscle("Chest Upper", "pectoralis_major_upper", chestGroupId, "subgroup", 11);
        long legGroupId = insertMuscle("Legs", "legs", null, "group", 90);
        long quadricepsId = insertMuscle("Quadriceps", "quadriceps", legGroupId, "subgroup", 91);
        long chestExerciseId = insertExercise(null, "Incline Dumbbell Press", "strength", "push", "kg", "set_based", 1);
        long legExerciseId = insertExercise(null, "Leg Press", "strength", "legs", "kg", "set_based", 1);
        insertExerciseMuscle(chestExerciseId, chestUpperId, "primary", 1);
        insertExerciseMuscle(legExerciseId, quadricepsId, "primary", 1);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system?categoryCode=chest")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseId").value(chestExerciseId));

        mockMvc.perform(apiGet("/exercises/system?categoryCode=chest&keyword=Incline")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseName").value("Incline Dumbbell Press"));

        mockMvc.perform(apiGet("/exercises/system?categoryCode=chest&muscleId=" + chestUpperId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseId").value(chestExerciseId));

        mockMvc.perform(apiGet("/exercises/system?categoryCode=chest&muscleId=" + quadricepsId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void getSystemExercisesShouldFilterBySceneTypeIncludingBoth() throws Exception {
        insertUser("exercise@example.com", "PlainTextPassword123");
        long bothEquipmentId = insertEquipment("Bodyweight", "both", 1);
        long homeEquipmentId = insertEquipment("Resistance Band", "home", 1);
        long gymEquipmentId = insertEquipment("Cable Machine", "gym", 1);
        long pushUpId = insertExercise(null, "Push-Up", "strength", "push", "reps", "set_based", 1);
        long bandPullId = insertExercise(null, "Band Pull-Apart", "mobility", "mobility", "reps", "set_based", 1);
        long latPullId = insertExercise(null, "Lat Pulldown", "strength", "pull", "kg", "set_based", 1);
        insertExerciseEquipment(pushUpId, bothEquipmentId, "required", 1);
        insertExerciseEquipment(bandPullId, homeEquipmentId, "required", 1);
        insertExerciseEquipment(latPullId, gymEquipmentId, "required", 1);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system?sceneType=home")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(apiGet("/exercises/system?sceneType=gym")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(apiGet("/exercises/system?sceneType=both")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseName").value("Push-Up"));
    }

    @Test
    void getSystemExercisesShouldFilterByMuscleIdExactly() throws Exception {
        insertUser("exercise@example.com", "PlainTextPassword123");
        long chestGroupId = insertMuscle("Chest Group", "chest_group", null, "group", 10);
        long chestUpperId = insertMuscle("Chest Upper", "chest_upper", chestGroupId, "subgroup", 11);
        long backId = insertMuscle("Back", "back", null, "group", 20);
        long chestExerciseId = insertExercise(null, "Incline Dumbbell Press", "strength", "push", "kg", "set_based", 1);
        long backExerciseId = insertExercise(null, "Lat Pulldown", "strength", "pull", "kg", "set_based", 1);
        insertExerciseMuscle(chestExerciseId, chestUpperId, "primary", 1);
        insertExerciseMuscle(backExerciseId, backId, "primary", 1);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system?muscleId=" + chestUpperId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exerciseId").value(chestExerciseId));

        mockMvc.perform(apiGet("/exercises/system?muscleId=" + chestGroupId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void getSystemExerciseDetailShouldReturnFullStructuredFields() throws Exception {
        insertUser("exercise@example.com", "PlainTextPassword123");
        long chestId = insertMuscle("Chest", "pectoralis_major_middle", null, "subgroup", 12);
        long frontDeltId = insertMuscle("Front Delt", "deltoid_front", null, "subgroup", 31);
        long benchId = insertEquipment("Bench", "gym", 1);
        long barbellId = insertEquipment("Barbell", "gym", 1);
        long exerciseId = insertExercise(null, "Barbell Bench Press", "strength", "push", "kg", "set_based", 1);
        updateExerciseDetail(exerciseId, "https://example.com/bench", new BigDecimal("6.50"), "kcal/min");
        insertExerciseMuscle(exerciseId, chestId, "primary", 1);
        insertExerciseMuscle(exerciseId, frontDeltId, "secondary", 2);
        insertExerciseEquipment(exerciseId, benchId, "required", 1);
        insertExerciseEquipment(exerciseId, barbellId, "required", 2);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system/" + exerciseId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exerciseId").value(exerciseId))
                .andExpect(jsonPath("$.data.defaultStructureType").value("set_based"))
                .andExpect(jsonPath("$.data.calorieBurnReference").value(6.50))
                .andExpect(jsonPath("$.data.primaryMuscles[0].relationType").value("primary"))
                .andExpect(jsonPath("$.data.secondaryMuscles[0].muscleName").value("Front Delt"))
                .andExpect(jsonPath("$.data.equipments[0].equipmentName").value("Bench"));
    }

    @Test
    void getSystemExerciseDetailShouldReturnNotFoundForInaccessibleExercise() throws Exception {
        long userId = insertUser("exercise@example.com", "PlainTextPassword123");
        long customExerciseId = insertExercise(userId, "Custom Deadlift", "strength", "hinge", "kg", "set_based", 1);
        long disabledExerciseId = insertExercise(null, "Disabled Running", "cardio", "cardio", "minutes", "single_segment", 0);
        String accessToken = loginAndGetAccessToken("exercise@example.com", "PlainTextPassword123");

        mockMvc.perform(apiGet("/exercises/system/" + customExerciseId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_FOUND"));

        mockMvc.perform(apiGet("/exercises/system/" + disabledExerciseId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_FOUND"));

        mockMvc.perform(apiGet("/exercises/system/999999").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_FOUND"));
    }

    @Test
    void protectedExerciseEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(apiGet("/exercises/system/filter-options"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(apiGet("/exercises/system"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(apiGet("/exercises/system/1"))
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

    private long insertMuscle(String name, String code, Long parentId, String muscleLevel, int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO muscles(name, code, parent_id, muscle_level, sort_order, is_active)
                VALUES (?, ?, ?, ?, ?, 1)
                """, name, code, parentId, muscleLevel, sortOrder);
        return jdbcTemplate.queryForObject("SELECT id FROM muscles WHERE code = ?", Long.class, code);
    }

    private long insertEquipment(String name, String sceneType, int isActive) {
        jdbcTemplate.update("""
                INSERT INTO equipments(name, scene_type, description, is_active)
                VALUES (?, ?, ?, ?)
                """, name, sceneType, name + " desc", isActive);
        return jdbcTemplate.queryForObject("SELECT id FROM equipments WHERE name = ?", Long.class, name);
    }

    private long insertExercise(
            Long ownerUserId,
            String name,
            String exerciseType,
            String movementType,
            String defaultUnit,
            String defaultStructureType,
            int isActive) {
        jdbcTemplate.update("""
                INSERT INTO exercises(
                    owner_user_id,
                    name,
                    exercise_type,
                    movement_type,
                    video_url,
                    default_unit,
                    default_structure_type,
                    calorie_burn_reference,
                    calorie_reference_unit,
                    is_active
                )
                VALUES (?, ?, ?, ?, NULL, ?, ?, NULL, NULL, ?)
                """, ownerUserId, name, exerciseType, movementType, defaultUnit, defaultStructureType, isActive);
        return jdbcTemplate.queryForObject("SELECT id FROM exercises WHERE name = ?", Long.class, name);
    }

    private void updateExerciseDetail(
            long exerciseId,
            String videoUrl,
            BigDecimal calorieBurnReference,
            String calorieReferenceUnit) {
        jdbcTemplate.update("""
                UPDATE exercises
                SET video_url = ?, calorie_burn_reference = ?, calorie_reference_unit = ?
                WHERE id = ?
                """, videoUrl, calorieBurnReference, calorieReferenceUnit, exerciseId);
    }

    private void insertExerciseMuscle(long exerciseId, long muscleId, String relationType, int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO exercise_muscles(exercise_id, muscle_id, relation_type, sort_order)
                VALUES (?, ?, ?, ?)
                """, exerciseId, muscleId, relationType, sortOrder);
    }

    private void insertExerciseEquipment(long exerciseId, long equipmentId, String requirementType, int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO exercise_equipments(exercise_id, equipment_id, requirement_type, sort_order)
                VALUES (?, ?, ?, ?)
                """, exerciseId, equipmentId, requirementType, sortOrder);
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
}
