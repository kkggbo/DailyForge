# DailyForge MySQL 建表草案

## 1. 目标

本文档用于将当前 MVP 阶段的数据库设计落成可执行的 MySQL 8.0 DDL 草案。  
这份草案重点覆盖：

- 字段类型
- 主键与唯一约束
- 常用索引
- 主要外键关系
- JSON 字段落库方式

说明：

- 当前为 `DDL 草案`，目的是尽快把逻辑设计转成物理模型
- 部分状态枚举暂采用 `VARCHAR` 存储，不使用 MySQL `ENUM`
- 少量存在循环依赖的外键会放到最后通过 `ALTER TABLE` 补充

## 2. 约定

### 2.1 主键策略

- 所有主键统一使用 `BIGINT UNSIGNED`
- 当前草案采用 `AUTO_INCREMENT`
- 如果后续切换为雪花 ID，可在实现阶段调整

### 2.2 时间字段

- 创建时间、更新时间统一使用 `DATETIME(3)`
- 仅记录日期的字段使用 `DATE`

### 2.3 JSON 字段

以下字段采用 MySQL `JSON` 类型：

- `cycle_day_exercises.target_extra_json`
- `training_session_exercises.planned_snapshot_json`
- `training_session_exercises.actual_summary_json`
- `training_session_sets.actual_extra_json`
- `ai_generation_records.input_json`
- `ai_generation_records.output_json`

### 2.4 字符集

统一建议：

- `ENGINE=InnoDB`
- `DEFAULT CHARSET=utf8mb4`
- `COLLATE=utf8mb4_unicode_ci`

## 3. 建表顺序建议

建议按以下顺序执行：

1. 基础主表
2. 资源表
3. 模板表
4. 运行表
5. 记录表
6. 外键补充

这样可以降低循环依赖处理难度。

## 4. DDL 草案

### 4.1 用户与邀请码

```sql
CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    email VARCHAR(128) NOT NULL COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    user_name VARCHAR(64) NOT NULL COMMENT '用户名',
    platform_role VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '平台角色:user/admin',
    account_tier VARCHAR(32) NOT NULL DEFAULT 'basic' COMMENT '权益层级:basic/invited_ai/premium',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '账户状态',
    last_login_at DATETIME(3) NULL COMMENT '最后登录时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_platform_role (platform_role),
    KEY idx_users_account_tier (account_tier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE user_profiles (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    gender VARCHAR(16) NULL COMMENT '性别',
    birth_date DATE NULL COMMENT '出生日期',
    height_cm DECIMAL(5,2) NULL COMMENT '身高(cm)',
    training_level VARCHAR(32) NULL COMMENT '训练水平',
    goal_type VARCHAR(32) NULL COMMENT '目标类型',
    injury_notes VARCHAR(500) NULL COMMENT '伤病备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户档案表';

CREATE TABLE invite_codes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(64) NOT NULL COMMENT '邀请码',
    information VARCHAR(255) NULL COMMENT '邀请码说明信息',
    grant_type VARCHAR(32) NOT NULL COMMENT '授予类型',
    grant_value VARCHAR(64) NOT NULL COMMENT '授予值',
    max_uses INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '最大使用次数',
    used_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已使用次数',
    expires_at DATETIME(3) NULL COMMENT '过期时间',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invite_codes_code (code),
    KEY idx_invite_codes_status (status),
    KEY idx_invite_codes_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

CREATE TABLE user_invite_code_usages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    invite_code_id BIGINT UNSIGNED NOT NULL COMMENT '邀请码ID',
    used_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '使用时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_invite_code_usages_user_code (user_id, invite_code_id),
    KEY idx_user_invite_code_usages_code_id (invite_code_id),
    CONSTRAINT fk_user_invite_code_usages_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_invite_code_usages_invite_code_id FOREIGN KEY (invite_code_id) REFERENCES invite_codes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户邀请码使用记录表';
```

### 4.2 身体指标

```sql
CREATE TABLE body_metric_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    weight_kg DECIMAL(6,2) NULL COMMENT '体重(kg)',
    body_fat_percent DECIMAL(5,2) NULL COMMENT '体脂率(%)',
    bmi DECIMAL(5,2) NULL COMMENT 'BMI',
    skeletal_muscle_percent DECIMAL(5,2) NULL COMMENT '骨骼肌率(%)',
    body_water_percent DECIMAL(5,2) NULL COMMENT '身体水分率(%)',
    basal_metabolic_rate_kcal DECIMAL(8,2) NULL COMMENT '基础代谢(kcal)',
    waist_cm DECIMAL(6,2) NULL COMMENT '腰围(cm)',
    hip_cm DECIMAL(6,2) NULL COMMENT '臀围(cm)',
    waist_hip_ratio DECIMAL(5,2) NULL COMMENT '腰臀比',
    body_age SMALLINT UNSIGNED NULL COMMENT '身体年龄',
    body_type VARCHAR(32) NULL COMMENT '体型',
    data_source VARCHAR(32) NULL COMMENT '数据来源',
    note VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_body_metric_logs_user_date (user_id, record_date),
    CONSTRAINT fk_body_metric_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='身体指标记录表';
```

### 4.3 肌肉、设备、动作资源

```sql
CREATE TABLE muscles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '肌肉名称',
    code VARCHAR(64) NOT NULL COMMENT '肌肉编码',
    parent_id BIGINT UNSIGNED NULL COMMENT '父级肌肉ID',
    muscle_level VARCHAR(32) NOT NULL COMMENT '层级',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_muscles_code (code),
    UNIQUE KEY uk_muscles_parent_name (parent_id, name),
    KEY idx_muscles_parent_id (parent_id),
    CONSTRAINT fk_muscles_parent_id FOREIGN KEY (parent_id) REFERENCES muscles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='肌肉表';

CREATE TABLE equipments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '设备名称',
    scene_type VARCHAR(32) NOT NULL COMMENT '使用场景',
    description VARCHAR(500) NULL COMMENT '描述',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipments_name (name),
    KEY idx_equipments_scene_type (scene_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

CREATE TABLE exercises (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    owner_user_id BIGINT UNSIGNED NULL COMMENT '自定义动作所属用户ID',
    name VARCHAR(128) NOT NULL COMMENT '动作名称',
    exercise_type VARCHAR(32) NOT NULL COMMENT '动作类型',
    movement_type VARCHAR(32) NULL COMMENT '动作模式',
    video_url VARCHAR(500) NULL COMMENT '动作示范视频地址',
    default_unit VARCHAR(32) NOT NULL COMMENT '默认单位',
    calorie_burn_reference DECIMAL(8,2) NULL COMMENT '参考热量消耗值',
    calorie_reference_unit VARCHAR(32) NULL COMMENT '参考热量单位',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_exercises_owner_user_id (owner_user_id),
    KEY idx_exercises_type_name (exercise_type, name),
    CONSTRAINT fk_exercises_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作表';

CREATE TABLE exercise_muscles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    exercise_id BIGINT UNSIGNED NOT NULL COMMENT '动作ID',
    muscle_id BIGINT UNSIGNED NOT NULL COMMENT '肌肉ID',
    relation_type VARCHAR(32) NOT NULL COMMENT '关联类型',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_muscles_pair (exercise_id, muscle_id, relation_type),
    KEY idx_exercise_muscles_muscle_id (muscle_id),
    KEY idx_exercise_muscles_exercise_relation (exercise_id, relation_type),
    CONSTRAINT fk_exercise_muscles_exercise_id FOREIGN KEY (exercise_id) REFERENCES exercises (id),
    CONSTRAINT fk_exercise_muscles_muscle_id FOREIGN KEY (muscle_id) REFERENCES muscles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作肌肉关联表';

CREATE TABLE exercise_equipments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    exercise_id BIGINT UNSIGNED NOT NULL COMMENT '动作ID',
    equipment_id BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    requirement_type VARCHAR(32) NOT NULL COMMENT '需求类型',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_equipments_pair (exercise_id, equipment_id, requirement_type),
    KEY idx_exercise_equipments_equipment_id (equipment_id),
    KEY idx_exercise_equipments_exercise_req (exercise_id, requirement_type),
    CONSTRAINT fk_exercise_equipments_exercise_id FOREIGN KEY (exercise_id) REFERENCES exercises (id),
    CONSTRAINT fk_exercise_equipments_equipment_id FOREIGN KEY (equipment_id) REFERENCES equipments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动作设备关联表';
```

### 4.4 循环模板

```sql
CREATE TABLE cycle_templates (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    name VARCHAR(128) NOT NULL COMMENT '循环模板名称',
    cycle_length TINYINT UNSIGNED NOT NULL COMMENT '循环天数(1-7)',
    goal_type VARCHAR(32) NULL COMMENT '目标类型',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '模板状态',
    current_version_id BIGINT UNSIGNED NULL COMMENT '当前生效版本ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_cycle_templates_user_status (user_id, status),
    CONSTRAINT fk_cycle_templates_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='循环模板表';

CREATE TABLE cycle_template_versions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '循环模板ID',
    version_no INT UNSIGNED NOT NULL COMMENT '版本号',
    source_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型',
    change_note VARCHAR(500) NULL COMMENT '变更说明',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_template_versions_template_version (template_id, version_no),
    KEY idx_cycle_template_versions_template_id (template_id),
    CONSTRAINT fk_cycle_template_versions_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='循环模板版本表';

CREATE TABLE cycle_template_days (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_version_id BIGINT UNSIGNED NOT NULL COMMENT '模板版本ID',
    day_index TINYINT UNSIGNED NOT NULL COMMENT '周期中的第几天',
    day_name VARCHAR(64) NOT NULL COMMENT '训练日名称',
    focus VARCHAR(128) NULL COMMENT '训练重点',
    notes VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_template_days_version_day (template_version_id, day_index),
    KEY idx_cycle_template_days_version_id (template_version_id),
    CONSTRAINT fk_cycle_template_days_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='循环模板日表';

CREATE TABLE cycle_day_exercises (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_day_id BIGINT UNSIGNED NOT NULL COMMENT '模板日ID',
    exercise_id BIGINT UNSIGNED NOT NULL COMMENT '动作ID',
    exercise_name_snapshot VARCHAR(128) NOT NULL COMMENT '动作名称快照',
    target_sets SMALLINT UNSIGNED NULL COMMENT '目标组数',
    target_reps_min SMALLINT UNSIGNED NULL COMMENT '目标最小次数',
    target_reps_max SMALLINT UNSIGNED NULL COMMENT '目标最大次数',
    target_weight_kg DECIMAL(7,2) NULL COMMENT '目标重量(kg)',
    target_duration_seconds INT UNSIGNED NULL COMMENT '目标时长(秒)',
    target_rest_seconds INT UNSIGNED NULL COMMENT '目标休息时长(秒)',
    target_rpe DECIMAL(4,2) NULL COMMENT '目标RPE',
    target_extra_json JSON NULL COMMENT '扩展目标信息',
    notes VARCHAR(500) NULL COMMENT '备注',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '动作排序',
    PRIMARY KEY (id),
    KEY idx_cycle_day_exercises_exercise_id (exercise_id),
    KEY idx_cycle_day_exercises_day_sort (template_day_id, sort_order),
    CONSTRAINT fk_cycle_day_exercises_template_day_id FOREIGN KEY (template_day_id) REFERENCES cycle_template_days (id),
    CONSTRAINT fk_cycle_day_exercises_exercise_id FOREIGN KEY (exercise_id) REFERENCES exercises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='循环日动作表';
```

### 4.5 循环运行状态

```sql
CREATE TABLE cycle_runs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '循环模板ID',
    template_version_id BIGINT UNSIGNED NOT NULL COMMENT '模板版本ID',
    run_no INT UNSIGNED NOT NULL COMMENT '第几轮循环',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '运行状态',
    started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
    completed_at DATETIME(3) NULL COMMENT '完成时间',
    archived_at DATETIME(3) NULL COMMENT '归档时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_runs_user_template_run (user_id, template_id, run_no),
    KEY idx_cycle_runs_template_id (template_id),
    KEY idx_cycle_runs_version_id (template_version_id),
    KEY idx_cycle_runs_status (status),
    CONSTRAINT fk_cycle_runs_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cycle_runs_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates (id),
    CONSTRAINT fk_cycle_runs_template_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='循环运行表';

CREATE TABLE user_active_cycles (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '当前激活模板ID',
    template_version_id BIGINT UNSIGNED NOT NULL COMMENT '当前激活模板版本ID',
    current_run_id BIGINT UNSIGNED NOT NULL COMMENT '当前运行实例ID',
    current_day_index TINYINT UNSIGNED NOT NULL COMMENT '当前默认训练日',
    last_session_id BIGINT UNSIGNED NULL COMMENT '最近一次打卡ID',
    activated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '激活时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (user_id),
    KEY idx_user_active_cycles_template_id (template_id),
    KEY idx_user_active_cycles_version_id (template_version_id),
    KEY idx_user_active_cycles_run_id (current_run_id),
    CONSTRAINT fk_user_active_cycles_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_active_cycles_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates (id),
    CONSTRAINT fk_user_active_cycles_template_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions (id),
    CONSTRAINT fk_user_active_cycles_current_run_id FOREIGN KEY (current_run_id) REFERENCES cycle_runs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='当前激活循环表';
```

### 4.6 训练打卡与动作执行

```sql
CREATE TABLE training_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    cycle_run_id BIGINT UNSIGNED NOT NULL COMMENT '循环运行ID',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
    template_version_id BIGINT UNSIGNED NOT NULL COMMENT '模板版本ID',
    template_day_id BIGINT UNSIGNED NOT NULL COMMENT '模板日ID',
    day_index TINYINT UNSIGNED NOT NULL COMMENT '周期中的第几天',
    session_no INT UNSIGNED NOT NULL COMMENT '模板下训练序号',
    started_at DATETIME(3) NULL COMMENT '训练开始时间',
    completed_at DATETIME(3) NULL COMMENT '训练完成时间',
    overall_feeling VARCHAR(255) NULL COMMENT '整体感受',
    notes VARCHAR(1000) NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_training_sessions_user_created_at (user_id, created_at),
    KEY idx_training_sessions_cycle_run_day (cycle_run_id, day_index),
    KEY idx_training_sessions_template_session_no (template_id, session_no),
    CONSTRAINT fk_training_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_training_sessions_cycle_run_id FOREIGN KEY (cycle_run_id) REFERENCES cycle_runs (id),
    CONSTRAINT fk_training_sessions_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates (id),
    CONSTRAINT fk_training_sessions_template_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions (id),
    CONSTRAINT fk_training_sessions_template_day_id FOREIGN KEY (template_day_id) REFERENCES cycle_template_days (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练打卡表';

CREATE TABLE training_session_exercises (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '训练打卡ID',
    cycle_day_exercise_id BIGINT UNSIGNED NOT NULL COMMENT '循环日动作ID',
    exercise_id BIGINT UNSIGNED NOT NULL COMMENT '动作ID',
    exercise_name_snapshot VARCHAR(128) NOT NULL COMMENT '动作名称快照',
    exercise_status VARCHAR(32) NOT NULL COMMENT '动作状态',
    planned_snapshot_json JSON NOT NULL COMMENT '计划快照',
    actual_summary_json JSON NULL COMMENT '实际完成摘要',
    feeling VARCHAR(255) NULL COMMENT '动作感受',
    failure_reason VARCHAR(64) NULL COMMENT '未完成/跳过原因',
    adjustment_note VARCHAR(500) NULL COMMENT '调整备注',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (id),
    KEY idx_training_session_exercises_session_sort (session_id, sort_order),
    KEY idx_training_session_exercises_exercise_id (exercise_id),
    CONSTRAINT fk_training_session_exercises_session_id FOREIGN KEY (session_id) REFERENCES training_sessions (id),
    CONSTRAINT fk_training_session_exercises_cycle_day_exercise_id FOREIGN KEY (cycle_day_exercise_id) REFERENCES cycle_day_exercises (id),
    CONSTRAINT fk_training_session_exercises_exercise_id FOREIGN KEY (exercise_id) REFERENCES exercises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练动作记录表';

CREATE TABLE training_session_sets (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_exercise_id BIGINT UNSIGNED NOT NULL COMMENT '训练动作记录ID',
    set_no SMALLINT UNSIGNED NOT NULL COMMENT '第几组',
    planned_weight_kg DECIMAL(7,2) NULL COMMENT '计划重量(kg)',
    actual_weight_kg DECIMAL(7,2) NULL COMMENT '实际重量(kg)',
    planned_reps SMALLINT UNSIGNED NULL COMMENT '计划次数',
    actual_reps SMALLINT UNSIGNED NULL COMMENT '实际次数',
    planned_duration_seconds INT UNSIGNED NULL COMMENT '计划时长(秒)',
    actual_duration_seconds INT UNSIGNED NULL COMMENT '实际时长(秒)',
    actual_extra_json JSON NULL COMMENT '扩展实际数据',
    is_completed TINYINT(1) NOT NULL DEFAULT 1 COMMENT '该组是否完成',
    set_note VARCHAR(255) NULL COMMENT '该组备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_session_sets_exercise_set (session_exercise_id, set_no),
    CONSTRAINT fk_training_session_sets_session_exercise_id FOREIGN KEY (session_exercise_id) REFERENCES training_session_exercises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练组记录表';
```

### 4.7 AI 调用记录

```sql
CREATE TABLE ai_generation_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    scenario VARCHAR(64) NOT NULL COMMENT '场景',
    related_entity_type VARCHAR(64) NULL COMMENT '关联实体类型',
    related_entity_id BIGINT UNSIGNED NULL COMMENT '关联实体ID',
    provider VARCHAR(64) NOT NULL COMMENT '模型提供方',
    model VARCHAR(128) NOT NULL COMMENT '模型名称',
    prompt_version VARCHAR(64) NULL COMMENT 'Prompt版本',
    input_json JSON NULL COMMENT '输入内容',
    output_json JSON NULL COMMENT '输出内容',
    status VARCHAR(32) NOT NULL COMMENT '调用状态',
    latency_ms INT UNSIGNED NULL COMMENT '耗时毫秒',
    error_message VARCHAR(1000) NULL COMMENT '错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_generation_records_user_scenario_created (user_id, scenario, created_at),
    CONSTRAINT fk_ai_generation_records_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成记录表';
```

## 5. 循环依赖补充外键

以下字段存在循环依赖，建议在主表创建完成后再补：

- `cycle_templates.current_version_id -> cycle_template_versions.id`
- `user_active_cycles.last_session_id -> training_sessions.id`

```sql
ALTER TABLE cycle_templates
    ADD CONSTRAINT fk_cycle_templates_current_version_id
    FOREIGN KEY (current_version_id) REFERENCES cycle_template_versions (id);

ALTER TABLE user_active_cycles
    ADD CONSTRAINT fk_user_active_cycles_last_session_id
    FOREIGN KEY (last_session_id) REFERENCES training_sessions (id);
```

## 6. 当前草案中的实现取舍

### 6.1 为什么状态字段先用 `VARCHAR`

原因：

- 方便后续业务扩展
- 避免频繁修改 MySQL `ENUM`
- 更适合 Java 侧用枚举类统一收口

### 6.2 为什么保留 JSON 字段

以下场景天然适合 JSON：

- 有氧动作扩展指标
- 计划快照
- 实际完成摘要
- AI 输入输出

如果强行拆成固定字段，表会膨胀得很快，而且力量训练和有氧训练会互相污染。

### 6.3 为什么 `cycle_runs` 单独建表

原因：

- 需要表达“第几轮循环”
- 需要支持每轮归档
- 需要将训练打卡挂到某一轮具体运行实例
- 后续统计“某模板跑了多少轮”会更自然

## 7. 建议下一步

有了这份 DDL 草案后，建议按以下顺序继续推进：

1. 审查字段命名和类型
2. 确认是否要统一采用 `AUTO_INCREMENT` 或雪花 ID
3. 手动执行 `V1__init_schema.sql`
4. 再手动执行 `V2__seed_base_data.sql`
5. 如需继续演进，再按版本维护新的 SQL 脚本
