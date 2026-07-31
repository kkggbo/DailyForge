-- ai_coach 模块数据结构升级
-- 前置条件：已按顺序执行 V1__init_schema.sql 至 V6__workout_schema_upgrade.sql。
-- 说明：
-- 1. 将旧 ai_generation_records 升级为通用 AI 异步任务表 ai_task_records。
-- 2. 新增 AI 工具调用明细表，支撑 tool calling 追溯与排查。
-- 3. 为 cycle_template_versions 增加 AI 来源任务追溯字段。

-- ================================================================
-- 1. AI 任务主表升级：从 generation 语义升级为通用 task 语义
-- ================================================================
RENAME TABLE ai_generation_records TO ai_task_records;

ALTER TABLE ai_task_records
    DROP FOREIGN KEY fk_ai_generation_records_user_id,
    DROP INDEX idx_ai_generation_records_user_scenario_created,
    CHANGE COLUMN scenario task_type VARCHAR(64) NOT NULL COMMENT '任务类型:template_generation/cycle_summary',
    CHANGE COLUMN input_json input_summary_json JSON NULL COMMENT '输入摘要JSON',
    CHANGE COLUMN output_json result_json JSON NULL COMMENT '最终结构化结果JSON',
    MODIFY COLUMN related_entity_type VARCHAR(64) NULL COMMENT '关联实体类型:cycle_template_version/cycle_run',
    MODIFY COLUMN provider VARCHAR(64) NOT NULL COMMENT '模型提供方',
    MODIFY COLUMN model VARCHAR(128) NOT NULL COMMENT '模型名称',
    MODIFY COLUMN prompt_version VARCHAR(64) NULL COMMENT '提示词版本',
    MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '任务状态:pending/running/succeeded/failed',
    MODIFY COLUMN error_message VARCHAR(1000) NULL COMMENT '错误信息',
    ADD COLUMN client_request_id VARCHAR(64) NULL COMMENT '客户端请求ID' AFTER task_type,
    ADD COLUMN request_payload_json JSON NULL COMMENT '请求载荷JSON' AFTER prompt_version,
    ADD COLUMN output_preview VARCHAR(1000) NULL COMMENT '输出预览' AFTER result_json,
    ADD COLUMN tool_call_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '工具调用轮次' AFTER status,
    ADD COLUMN repair_attempt_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '结果修复次数' AFTER tool_call_count,
    ADD COLUMN error_code VARCHAR(64) NULL COMMENT '错误码' AFTER latency_ms,
    ADD COLUMN started_at DATETIME(3) NULL COMMENT '开始时间' AFTER created_at,
    ADD COLUMN completed_at DATETIME(3) NULL COMMENT '完成时间' AFTER started_at,
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间' AFTER completed_at,
    ADD KEY idx_ai_task_records_user_task_created (user_id, task_type, created_at),
    ADD KEY idx_ai_task_records_user_status_created (user_id, status, created_at),
    ADD KEY idx_ai_task_records_related_entity (related_entity_type, related_entity_id),
    ADD UNIQUE KEY uk_ai_task_records_user_task_request (user_id, task_type, client_request_id),
    ADD CONSTRAINT fk_ai_task_records_user_id
        FOREIGN KEY (user_id) REFERENCES users (id);

-- ================================================================
-- 2. AI 工具调用明细表：记录每轮 tool calling 结果
-- ================================================================
CREATE TABLE ai_task_tool_calls (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT UNSIGNED NOT NULL COMMENT 'AI任务ID',
    round_no SMALLINT UNSIGNED NOT NULL COMMENT '第几轮工具调用',
    tool_name VARCHAR(64) NOT NULL COMMENT '工具名称',
    request_summary_json JSON NULL COMMENT '工具请求摘要JSON',
    response_summary_json JSON NULL COMMENT '工具响应摘要JSON',
    status VARCHAR(32) NOT NULL COMMENT '调用状态:succeeded/failed',
    latency_ms INT UNSIGNED NULL COMMENT '耗时毫秒',
    error_message VARCHAR(1000) NULL COMMENT '错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_task_tool_calls_task_round (task_id, round_no),
    KEY idx_ai_task_tool_calls_tool_created (tool_name, created_at),
    CONSTRAINT fk_ai_task_tool_calls_task_id
        FOREIGN KEY (task_id) REFERENCES ai_task_records (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI任务工具调用记录表';

-- ================================================================
-- 3. 模板版本来源追溯：记录 AI 生成模板对应的任务来源
-- ================================================================
ALTER TABLE cycle_template_versions
    MODIFY COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'manual'
        COMMENT '来源类型:manual/ai_generated',
    ADD COLUMN source_task_id BIGINT UNSIGNED NULL COMMENT '来源AI任务ID' AFTER source_type,
    ADD KEY idx_cycle_template_versions_source_task_id (source_task_id),
    ADD CONSTRAINT fk_cycle_template_versions_source_task_id
        FOREIGN KEY (source_task_id) REFERENCES ai_task_records (id);
