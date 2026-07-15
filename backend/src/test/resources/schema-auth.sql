DROP TABLE IF EXISTS user_active_cycles;
DROP TABLE IF EXISTS training_sessions;
DROP TABLE IF EXISTS cycle_runs;
DROP TABLE IF EXISTS cycle_day_exercise_item_metrics;
DROP TABLE IF EXISTS cycle_day_exercise_items;
DROP TABLE IF EXISTS cycle_day_exercises;
DROP TABLE IF EXISTS cycle_template_days;
DROP TABLE IF EXISTS cycle_template_versions;
DROP TABLE IF EXISTS cycle_templates;
DROP TABLE IF EXISTS exercise_equipments;
DROP TABLE IF EXISTS exercise_muscles;
DROP TABLE IF EXISTS exercises;
DROP TABLE IF EXISTS equipments;
DROP TABLE IF EXISTS muscles;
DROP TABLE IF EXISTS user_current_body_metrics;
DROP TABLE IF EXISTS body_metric_logs;
DROP TABLE IF EXISTS user_invite_code_usages;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS invite_codes;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_name VARCHAR(64) NOT NULL,
    platform_role VARCHAR(32) NOT NULL DEFAULT 'user',
    account_tier VARCHAR(32) NOT NULL DEFAULT 'basic',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY,
    gender VARCHAR(16) NULL,
    birth_date DATE NULL,
    height_cm DECIMAL(5, 2) NULL,
    training_level VARCHAR(32) NULL,
    goal_type VARCHAR(32) NULL,
    injury_notes VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE body_metric_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    weight_kg DECIMAL(6, 2) NULL,
    body_fat_percent DECIMAL(5, 2) NULL,
    bmi DECIMAL(5, 2) NULL,
    skeletal_muscle_percent DECIMAL(5, 2) NULL,
    body_water_percent DECIMAL(5, 2) NULL,
    basal_metabolic_rate_kcal DECIMAL(8, 2) NULL,
    waist_cm DECIMAL(6, 2) NULL,
    hip_cm DECIMAL(6, 2) NULL,
    waist_hip_ratio DECIMAL(5, 2) NULL,
    body_age SMALLINT NULL,
    body_type VARCHAR(32) NULL,
    data_source VARCHAR(32) NULL,
    note VARCHAR(1000) NULL,
    is_del TINYINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_body_metric_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_body_metric_logs_user_del_date
    ON body_metric_logs(user_id, is_del, record_date, id);

CREATE TABLE user_current_body_metrics (
    user_id BIGINT PRIMARY KEY,
    current_weight_kg DECIMAL(6, 2) NULL,
    current_body_fat_percent DECIMAL(5, 2) NULL,
    current_bmi DECIMAL(5, 2) NULL,
    current_skeletal_muscle_percent DECIMAL(5, 2) NULL,
    current_body_water_percent DECIMAL(5, 2) NULL,
    current_basal_metabolic_rate_kcal DECIMAL(8, 2) NULL,
    current_waist_cm DECIMAL(6, 2) NULL,
    current_hip_cm DECIMAL(6, 2) NULL,
    current_waist_hip_ratio DECIMAL(5, 2) NULL,
    current_body_age SMALLINT NULL,
    current_body_type VARCHAR(32) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_current_body_metrics_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE invite_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    information VARCHAR(255) NULL,
    grant_type VARCHAR(32) NOT NULL,
    grant_value VARCHAR(64) NOT NULL,
    max_uses INT NOT NULL DEFAULT 1,
    used_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_invite_codes_code UNIQUE (code)
);

CREATE TABLE user_invite_code_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    invite_code_id BIGINT NOT NULL,
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_invite_code_usages_user_code UNIQUE (user_id, invite_code_id),
    CONSTRAINT fk_user_invite_code_usages_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_invite_code_usages_invite_code_id FOREIGN KEY (invite_code_id) REFERENCES invite_codes(id)
);

CREATE TABLE exercises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    exercise_type VARCHAR(32) NOT NULL,
    movement_type VARCHAR(32) NULL,
    video_url VARCHAR(500) NULL,
    default_unit VARCHAR(32) NOT NULL,
    default_structure_type VARCHAR(32) NOT NULL,
    calorie_burn_reference DECIMAL(8, 2) NULL,
    calorie_reference_unit VARCHAR(32) NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exercises_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE TABLE muscles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    parent_id BIGINT NULL,
    muscle_level VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_muscles_code UNIQUE (code),
    CONSTRAINT uk_muscles_parent_name UNIQUE (parent_id, name),
    CONSTRAINT fk_muscles_parent_id FOREIGN KEY (parent_id) REFERENCES muscles(id)
);

CREATE TABLE equipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    scene_type VARCHAR(32) NOT NULL,
    description VARCHAR(500) NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_equipments_name UNIQUE (name)
);

CREATE TABLE exercise_muscles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercise_id BIGINT NOT NULL,
    muscle_id BIGINT NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_exercise_muscles_pair UNIQUE (exercise_id, muscle_id, relation_type),
    CONSTRAINT fk_exercise_muscles_exercise_id FOREIGN KEY (exercise_id) REFERENCES exercises(id),
    CONSTRAINT fk_exercise_muscles_muscle_id FOREIGN KEY (muscle_id) REFERENCES muscles(id)
);

CREATE TABLE exercise_equipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercise_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    requirement_type VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_exercise_equipments_pair UNIQUE (exercise_id, equipment_id, requirement_type),
    CONSTRAINT fk_exercise_equipments_exercise_id FOREIGN KEY (exercise_id) REFERENCES exercises(id),
    CONSTRAINT fk_exercise_equipments_equipment_id FOREIGN KEY (equipment_id) REFERENCES equipments(id)
);

CREATE TABLE cycle_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    cycle_length TINYINT NULL,
    goal_type VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    current_version_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cycle_templates_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE cycle_template_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'manual',
    change_note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cycle_template_versions_template_version UNIQUE (template_id, version_no),
    CONSTRAINT fk_cycle_template_versions_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates(id)
);

CREATE TABLE cycle_template_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_version_id BIGINT NOT NULL,
    day_index TINYINT NOT NULL,
    day_name VARCHAR(64) NOT NULL,
    focus VARCHAR(128) NULL,
    notes VARCHAR(500) NULL,
    CONSTRAINT uk_cycle_template_days_version_day UNIQUE (template_version_id, day_index),
    CONSTRAINT fk_cycle_template_days_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions(id)
);

CREATE TABLE cycle_day_exercises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_day_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    exercise_name_snapshot VARCHAR(128) NOT NULL,
    structure_type VARCHAR(32) NOT NULL,
    note VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cycle_day_exercises_template_day_id FOREIGN KEY (template_day_id) REFERENCES cycle_template_days(id),
    CONSTRAINT fk_cycle_day_exercises_exercise_id FOREIGN KEY (exercise_id) REFERENCES exercises(id)
);

CREATE INDEX idx_cycle_day_exercises_day_sort
    ON cycle_day_exercises(template_day_id, sort_order);

CREATE TABLE cycle_day_exercise_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cycle_day_exercise_id BIGINT NOT NULL,
    item_index SMALLINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    item_name VARCHAR(64) NULL,
    note VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cycle_day_exercise_items_exercise_item UNIQUE (cycle_day_exercise_id, item_index),
    CONSTRAINT fk_cycle_day_exercise_items_exercise_id
        FOREIGN KEY (cycle_day_exercise_id) REFERENCES cycle_day_exercises(id)
);

CREATE INDEX idx_cycle_day_exercise_items_exercise_sort
    ON cycle_day_exercise_items(cycle_day_exercise_id, sort_order);

CREATE TABLE cycle_day_exercise_item_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercise_item_id BIGINT NOT NULL,
    metric_key VARCHAR(64) NOT NULL,
    metric_value_number DECIMAL(12, 4) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cycle_day_exercise_item_metrics_item_key UNIQUE (exercise_item_id, metric_key),
    CONSTRAINT fk_cycle_day_exercise_item_metrics_item_id
        FOREIGN KEY (exercise_item_id) REFERENCES cycle_day_exercise_items(id)
);

CREATE INDEX idx_cycle_day_exercise_item_metrics_item_sort
    ON cycle_day_exercise_item_metrics(exercise_item_id, sort_order);

CREATE TABLE cycle_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    template_version_id BIGINT NOT NULL,
    run_no INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    archived_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cycle_runs_user_template_run UNIQUE (user_id, template_id, run_no),
    CONSTRAINT fk_cycle_runs_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cycle_runs_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates(id),
    CONSTRAINT fk_cycle_runs_template_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions(id)
);

CREATE TABLE user_active_cycles (
    user_id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    template_version_id BIGINT NOT NULL,
    current_run_id BIGINT NOT NULL,
    current_day_index TINYINT NOT NULL,
    last_session_id BIGINT NULL,
    activated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_active_cycles_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_active_cycles_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates(id),
    CONSTRAINT fk_user_active_cycles_template_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions(id),
    CONSTRAINT fk_user_active_cycles_current_run_id FOREIGN KEY (current_run_id) REFERENCES cycle_runs(id)
);

CREATE TABLE training_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cycle_run_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    template_version_id BIGINT NOT NULL,
    template_day_id BIGINT NOT NULL,
    day_index TINYINT NOT NULL,
    session_no INT NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    overall_feeling VARCHAR(255) NULL,
    notes VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_training_sessions_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_training_sessions_cycle_run_id FOREIGN KEY (cycle_run_id) REFERENCES cycle_runs(id),
    CONSTRAINT fk_training_sessions_template_id FOREIGN KEY (template_id) REFERENCES cycle_templates(id),
    CONSTRAINT fk_training_sessions_template_version_id FOREIGN KEY (template_version_id) REFERENCES cycle_template_versions(id),
    CONSTRAINT fk_training_sessions_template_day_id FOREIGN KEY (template_day_id) REFERENCES cycle_template_days(id)
);

ALTER TABLE cycle_templates
    ADD CONSTRAINT fk_cycle_templates_current_version_id
    FOREIGN KEY (current_version_id) REFERENCES cycle_template_versions(id);

ALTER TABLE user_active_cycles
    ADD CONSTRAINT fk_user_active_cycles_last_session_id
    FOREIGN KEY (last_session_id) REFERENCES training_sessions(id);
