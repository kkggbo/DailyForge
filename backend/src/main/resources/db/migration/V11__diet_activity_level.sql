-- diet 模块 V11：user_profiles 增 activity_level + 新增 4 张表
-- 业务上 activity_level 是每日目标计算的必需项（缺失时 missingFields 含 activityLevel），列允许 NULL。

ALTER TABLE user_profiles
    ADD COLUMN activity_level VARCHAR(32) NULL
        COMMENT '日常活动量(sedentary/light/moderate/high/very_high)'
        AFTER goal_type;

CREATE TABLE foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '食物名称',
    category VARCHAR(32) NULL COMMENT '分类(staple/meat_egg/vegetable/fruit/dairy/nut_bean/drink/other)',
    calories_kcal DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g热量(kcal)',
    protein_g DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g蛋白质(g)',
    carbs_g DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g碳水(g)',
    fat_g DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g脂肪(g)',
    source VARCHAR(16) NOT NULL DEFAULT 'system' COMMENT 'system/user',
    owner_user_id BIGINT NULL COMMENT '用户上传时的归属用户(system为NULL)',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可用(1=是 0=否)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_foods_name (name),
    KEY idx_foods_source_active (source, is_active)
) COMMENT='食物库：每100g可食部分营养';

CREATE TABLE user_food_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_food (user_id, food_id),
    KEY idx_user_food_favorites_user (user_id)
) COMMENT='用户食物收藏';

CREATE TABLE diet_food_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL COMMENT '来源食物id',
    food_name_snapshot VARCHAR(64) NOT NULL COMMENT '食物名快照',
    meal_type VARCHAR(16) NOT NULL COMMENT 'breakfast/lunch/dinner/snack',
    record_date DATE NOT NULL COMMENT '记录日期',
    quantity_grams DECIMAL(10,2) NOT NULL COMMENT '克数',
    calories_kcal DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '热量快照(kcal)',
    protein_g DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '蛋白质快照(g)',
    carbs_g DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '碳水快照(g)',
    fat_g DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '脂肪快照(g)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_diet_logs_user_date (user_id, record_date),
    KEY idx_diet_logs_user_food (user_id, food_id),
    KEY idx_diet_logs_user_meal_date (user_id, meal_type, record_date)
) COMMENT='饮食记录：营养素按添加时快照';

CREATE TABLE user_diet_targets (
    user_id BIGINT PRIMARY KEY COMMENT '用户id(1对1)',
    calories_kcal DECIMAL(10,2) NOT NULL,
    protein_g DECIMAL(10,2) NOT NULL,
    carbs_g DECIMAL(10,2) NOT NULL,
    fat_g DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='用户自定义每日目标覆盖';
