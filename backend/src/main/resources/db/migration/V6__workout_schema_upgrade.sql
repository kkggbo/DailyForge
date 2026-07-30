-- workout 模块数据结构升级
-- 前置条件：已按顺序执行 V1__init_schema.sql 至 V5__cycle_template_structure_v2.sql。
-- 适用条件：当前数据库没有训练、循环或模板执行记录。
-- 本迁移是干净迁移：会删除 training_session_sets 和旧 JSON 执行快照字段。
-- 若数据库后续已有训练记录，禁止直接执行本文件，必须改用兼容迁移方案。

-- ================================================================
-- 1. 循环运行状态：支持中途取消
-- ================================================================
ALTER TABLE cycle_runs
    ADD COLUMN cancelled_at DATETIME(3) NULL COMMENT '取消时间' AFTER completed_at,
    DROP COLUMN archived_at,
    MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'active'
        COMMENT '运行状态:active/completed/cancelled',
    ADD KEY idx_cycle_runs_user_status (user_id, status);

-- ================================================================
-- 2. 旧固定组模型已不适用，当前无数据可直接删除
-- ================================================================
DROP TABLE training_session_sets;

-- ================================================================
-- 3. 训练会话主表：状态、类型、快照、幂等约束与查询索引
-- 先补 template_id 单列索引，避免删除旧复合索引时破坏外键依赖。
-- ================================================================
ALTER TABLE training_sessions
    ADD KEY idx_training_sessions_template_id (template_id);

ALTER TABLE training_sessions
    DROP INDEX idx_training_sessions_cycle_run_day,
    DROP INDEX idx_training_sessions_template_session_no,
    DROP COLUMN session_no,
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'in_progress'
        COMMENT '会话状态:in_progress/completed/cancelled' AFTER day_index,
    ADD COLUMN session_type VARCHAR(32) NOT NULL DEFAULT 'workout'
        COMMENT '会话类型:workout/rest_day' AFTER status,
    ADD COLUMN template_name_snapshot VARCHAR(128) NOT NULL
        COMMENT '模板名称快照' AFTER template_id,
    ADD COLUMN day_name_snapshot VARCHAR(64) NOT NULL
        COMMENT '训练日名称快照' AFTER template_day_id,
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间' AFTER created_at,
    ADD UNIQUE KEY uk_training_sessions_cycle_run_day (cycle_run_id, day_index),
    ADD KEY idx_training_sessions_cycle_run_status (cycle_run_id, status),
    ADD KEY idx_training_sessions_user_completed_at (user_id, completed_at, started_at);

-- ================================================================
-- 4. 训练动作记录：删除旧 JSON，补充结构类型
-- ================================================================
ALTER TABLE training_session_exercises
    DROP COLUMN planned_snapshot_json,
    DROP COLUMN actual_summary_json,
    ADD COLUMN structure_type VARCHAR(32) NOT NULL COMMENT '动作结构类型' AFTER exercise_name_snapshot,
    MODIFY COLUMN exercise_status VARCHAR(32) NULL COMMENT '动作状态';

-- ================================================================
-- 5. 训练执行项：与 cycle_template v2 的动作 -> 执行项 -> 参数模型对齐
-- ================================================================
CREATE TABLE training_session_exercise_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_exercise_id BIGINT UNSIGNED NOT NULL COMMENT '训练动作记录ID',
    item_index SMALLINT UNSIGNED NOT NULL COMMENT '执行项序号',
    item_type VARCHAR(32) NOT NULL COMMENT '执行项类型:set/segment',
    item_name_snapshot VARCHAR(64) NULL COMMENT '执行项名称快照',
    note_snapshot VARCHAR(500) NULL COMMENT '执行项备注快照',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_session_exercise_items_exercise_item (session_exercise_id, item_index),
    KEY idx_training_session_exercise_items_exercise_sort (session_exercise_id, sort_order),
    CONSTRAINT fk_training_session_exercise_items_session_exercise_id
        FOREIGN KEY (session_exercise_id) REFERENCES training_session_exercises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练动作执行项记录表';

CREATE TABLE training_session_exercise_item_metrics (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_exercise_item_id BIGINT UNSIGNED NOT NULL COMMENT '训练动作执行项记录ID',
    metric_key VARCHAR(64) NOT NULL COMMENT '参数键',
    planned_value_number DECIMAL(12,4) NULL COMMENT '计划参数值',
    actual_value_number DECIMAL(12,4) NULL COMMENT '实际参数值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_session_exercise_item_metrics_item_key (session_exercise_item_id, metric_key),
    KEY idx_training_session_exercise_item_metrics_item_sort (session_exercise_item_id, sort_order),
    CONSTRAINT fk_training_session_exercise_item_metrics_item_id
        FOREIGN KEY (session_exercise_item_id) REFERENCES training_session_exercise_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练动作执行项参数记录表';
