-- V6__workout_schema_upgrade.sql 部分执行恢复脚本
-- 适用范围：仅用于首次执行 V6 时，training_sessions ALTER TABLE 因
-- idx_training_sessions_template_session_no 被外键依赖而失败的当前数据库。
-- 前置状态：cycle_runs、training_session_exercises、两张新的执行项表已按 V6 成功变更；
-- training_sessions 保持 V6 执行前的旧结构。
-- 禁止用于全新数据库或已完整执行 V6 的数据库。

-- 先补 template_id 单列索引，作为 fk_training_sessions_template_id 的替代支撑。
ALTER TABLE training_sessions
    ADD KEY idx_training_sessions_template_id (template_id);

-- 再移除旧 session_no 结构，并补齐 workout 新字段与索引。
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