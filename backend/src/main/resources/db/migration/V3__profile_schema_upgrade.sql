ALTER TABLE user_profiles
    MODIFY COLUMN injury_notes VARCHAR(1000) NULL COMMENT '伤病备注';

ALTER TABLE body_metric_logs
    MODIFY COLUMN note VARCHAR(1000) NULL COMMENT '备注',
    ADD COLUMN is_del TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除' AFTER note,
    ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '逻辑删除时间' AFTER is_del;

ALTER TABLE body_metric_logs
    DROP INDEX idx_body_metric_logs_user_date,
    ADD INDEX idx_body_metric_logs_user_del_date (user_id, is_del, record_date, id);

CREATE TABLE user_current_body_metrics (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    current_weight_kg DECIMAL(6,2) NULL COMMENT '当前体重(kg)',
    current_body_fat_percent DECIMAL(5,2) NULL COMMENT '当前体脂率(%)',
    current_bmi DECIMAL(5,2) NULL COMMENT '当前BMI',
    current_skeletal_muscle_percent DECIMAL(5,2) NULL COMMENT '当前骨骼肌率(%)',
    current_body_water_percent DECIMAL(5,2) NULL COMMENT '当前身体水分率(%)',
    current_basal_metabolic_rate_kcal DECIMAL(8,2) NULL COMMENT '当前基础代谢(kcal)',
    current_waist_cm DECIMAL(6,2) NULL COMMENT '当前腰围(cm)',
    current_hip_cm DECIMAL(6,2) NULL COMMENT '当前臀围(cm)',
    current_waist_hip_ratio DECIMAL(5,2) NULL COMMENT '当前腰臀比',
    current_body_age SMALLINT UNSIGNED NULL COMMENT '当前身体年龄',
    current_body_type VARCHAR(32) NULL COMMENT '当前体型',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '快照更新时间',
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_current_body_metrics_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户当前身体状态快照表';
