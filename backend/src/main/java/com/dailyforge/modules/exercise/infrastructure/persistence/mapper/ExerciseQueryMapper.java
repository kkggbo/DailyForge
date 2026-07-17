package com.dailyforge.modules.exercise.infrastructure.persistence.mapper;

import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.SystemExerciseLookupEntity;
import com.dailyforge.modules.exercise.interfaces.dto.ExerciseSystemListQuery;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

@Mapper
public interface ExerciseQueryMapper {

    /**
     * Count system exercises matching current filters.
     */
    @SelectProvider(type = SqlProvider.class, method = "buildCountSystemExercises")
    long countSystemExercises(ExerciseSystemListQuery query);

    /**
     * Fetch one page of system exercise ids using the same filter set as count.
     */
    @SelectProvider(type = SqlProvider.class, method = "buildSelectSystemExercisePageIds")
    List<Long> selectSystemExercisePageIds(ExerciseSystemListQuery query);

    /**
     * Batch load system exercise base fields by ids.
     */
    @Select({
            "<script>",
            "SELECT id, owner_user_id, name, exercise_type, movement_type, video_url,",
            "default_unit, default_structure_type, calorie_burn_reference, calorie_reference_unit, is_active",
            "FROM exercises",
            "WHERE owner_user_id IS NULL AND is_active = 1",
            "AND id IN",
            "<foreach collection='exerciseIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<ExerciseEntity> selectSystemExercisesByIds(@Param("exerciseIds") List<Long> exerciseIds);

    /**
     * Load one active system exercise detail row.
     */
    @Select("""
            SELECT id, owner_user_id, name, exercise_type, movement_type, video_url,
                   default_unit, default_structure_type, calorie_burn_reference, calorie_reference_unit, is_active
            FROM exercises
            WHERE id = #{exerciseId}
              AND owner_user_id IS NULL
              AND is_active = 1
            LIMIT 1
            """)
    ExerciseEntity selectSystemExerciseDetailById(Long exerciseId);

    /**
     * Batch load exercise lookup fields without applying active-system visibility.
     */
    @Select({
            "<script>",
            "SELECT id, owner_user_id, name, exercise_type, movement_type, default_unit, default_structure_type, is_active",
            "FROM exercises",
            "WHERE id IN",
            "<foreach collection='exerciseIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<SystemExerciseLookupEntity> selectLookupByIds(@Param("exerciseIds") List<Long> exerciseIds);

    /**
     * Batch load active system exercise lookup fields for internal modules.
     */
    @Select({
            "<script>",
            "SELECT id, owner_user_id, name, exercise_type, movement_type, default_unit, default_structure_type, is_active",
            "FROM exercises",
            "WHERE owner_user_id IS NULL AND is_active = 1",
            "AND id IN",
            "<foreach collection='exerciseIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<SystemExerciseLookupEntity> selectActiveSystemLookupByIds(@Param("exerciseIds") List<Long> exerciseIds);

    class SqlProvider {

        public String buildCountSystemExercises(ExerciseSystemListQuery query) {
            return buildBaseListSql(query, false);
        }

        public String buildSelectSystemExercisePageIds(ExerciseSystemListQuery query) {
            return buildBaseListSql(query, true);
        }

        private String buildBaseListSql(ExerciseSystemListQuery query, boolean paged) {
            StringBuilder sql = new StringBuilder("<script>");
            sql.append("SELECT ");
            sql.append(paged ? "e.id " : "COUNT(1) ");
            sql.append("FROM exercises e ");
            sql.append("WHERE e.owner_user_id IS NULL AND e.is_active = 1 ");
            if (query.getKeyword() != null) {
                sql.append("AND e.name LIKE CONCAT('%', #{keyword}, '%') ");
            }
            if (query.getExerciseType() != null) {
                sql.append("AND e.exercise_type = #{exerciseType} ");
            }
            if (query.getMovementType() != null) {
                sql.append("AND e.movement_type = #{movementType} ");
            }
            if (query.getStructureType() != null) {
                sql.append("AND e.default_structure_type = #{structureType} ");
            }
            if (query.getMuscleId() != null) {
                sql.append("""
                        AND EXISTS (
                            SELECT 1
                            FROM exercise_muscles em
                            JOIN muscles m ON m.id = em.muscle_id
                            WHERE em.exercise_id = e.id
                              AND em.muscle_id = #{muscleId}
                              AND m.is_active = 1
                        )
                        """);
            }
            if (query.getCategoryMuscleIds() != null && !query.getCategoryMuscleIds().isEmpty()) {
                sql.append("""
                        AND EXISTS (
                            SELECT 1
                            FROM exercise_muscles em
                            JOIN muscles m ON m.id = em.muscle_id
                            WHERE em.exercise_id = e.id
                              AND em.muscle_id IN
                        """);
                appendInClause(sql, "categoryMuscleIds");
                sql.append("""
                              AND m.is_active = 1
                        )
                        """);
            }
            if (query.getSceneType() != null) {
                sql.append("""
                        AND EXISTS (
                            SELECT 1
                            FROM exercise_equipments ee
                            JOIN equipments eq ON eq.id = ee.equipment_id
                            WHERE ee.exercise_id = e.id
                              AND eq.is_active = 1
                        """);
                if ("both".equals(query.getSceneType())) {
                    sql.append(" AND eq.scene_type = 'both'");
                } else {
                    sql.append(" AND (eq.scene_type = #{sceneType} OR eq.scene_type = 'both')");
                }
                sql.append(" ) ");
            }
            if (paged) {
                sql.append("ORDER BY e.name ASC, e.id ASC ");
                sql.append("LIMIT #{offset}, #{pageSize}");
            }
            sql.append("</script>");
            return sql.toString();
        }

        private void appendInClause(StringBuilder sql, String collectionName) {
            sql.append("<foreach collection='").append(collectionName)
                    .append("' item='id' open='(' separator=',' close=')'>");
            sql.append("#{id}");
            sql.append("</foreach> ");
        }
    }
}
