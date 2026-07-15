-- cycle_template v2 动作参数模型重构
-- 说明：
-- 1. 本次迁移采用破坏式方案，先清空 cycle_template 相关业务数据
-- 2. 不兼容旧的固定动作参数列
-- 3. metricUnit 不落库，由后端按 metricKey 推导

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS cycle_day_exercise_item_metrics;
DROP TABLE IF EXISTS cycle_day_exercise_items;

DELETE FROM training_session_sets;
DELETE FROM training_session_exercises;
DELETE FROM training_sessions;
DELETE FROM user_active_cycles;
DELETE FROM cycle_runs;
DELETE FROM cycle_day_exercises;
DELETE FROM cycle_template_days;
DELETE FROM cycle_template_versions;
DELETE FROM cycle_templates;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE exercises
    ADD COLUMN default_structure_type VARCHAR(32) NULL COMMENT '默认动作结构类型' AFTER default_unit;

UPDATE exercises
SET default_structure_type = CASE
    WHEN exercise_type = 'cardio' THEN 'single_segment'
    ELSE 'set_based'
END
WHERE default_structure_type IS NULL;

ALTER TABLE exercises
    MODIFY COLUMN default_structure_type VARCHAR(32) NOT NULL COMMENT '默认动作结构类型';

ALTER TABLE cycle_day_exercises
    ADD COLUMN structure_type VARCHAR(32) NOT NULL COMMENT '动作结构类型' AFTER exercise_name_snapshot,
    CHANGE COLUMN notes note VARCHAR(500) NULL COMMENT '动作备注',
    DROP COLUMN target_sets,
    DROP COLUMN target_reps_min,
    DROP COLUMN target_reps_max,
    DROP COLUMN target_weight_kg,
    DROP COLUMN target_duration_seconds,
    DROP COLUMN target_rest_seconds,
    DROP COLUMN target_rpe,
    DROP COLUMN target_extra_json;

CREATE TABLE cycle_day_exercise_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    cycle_day_exercise_id BIGINT UNSIGNED NOT NULL COMMENT '模板动作ID',
    item_index SMALLINT UNSIGNED NOT NULL COMMENT '执行项序号',
    item_type VARCHAR(32) NOT NULL COMMENT '执行项类型',
    item_name VARCHAR(64) NULL COMMENT '执行项名称',
    note VARCHAR(500) NULL COMMENT '执行项备注',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_day_exercise_items_exercise_item (cycle_day_exercise_id, item_index),
    KEY idx_cycle_day_exercise_items_exercise_sort (cycle_day_exercise_id, sort_order),
    CONSTRAINT fk_cycle_day_exercise_items_exercise_id
        FOREIGN KEY (cycle_day_exercise_id) REFERENCES cycle_day_exercises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板动作执行项表';

CREATE TABLE cycle_day_exercise_item_metrics (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    exercise_item_id BIGINT UNSIGNED NOT NULL COMMENT '执行项ID',
    metric_key VARCHAR(64) NOT NULL COMMENT '参数键',
    metric_value_number DECIMAL(12,4) NOT NULL COMMENT '数值参数值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_day_exercise_item_metrics_item_key (exercise_item_id, metric_key),
    KEY idx_cycle_day_exercise_item_metrics_item_sort (exercise_item_id, sort_order),
    KEY idx_cycle_day_exercise_item_metrics_key (metric_key),
    CONSTRAINT fk_cycle_day_exercise_item_metrics_item_id
        FOREIGN KEY (exercise_item_id) REFERENCES cycle_day_exercise_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板动作执行项参数表';
