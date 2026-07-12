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
