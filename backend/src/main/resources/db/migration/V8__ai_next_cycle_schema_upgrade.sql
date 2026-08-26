-- ai_coach 下一周期模板生成（next_cycle_generation）支持
-- 方案 A：将 AI 生成时的场景/有氧偏好持久化到 cycle_templates，
-- 供「根据上一周期表现生成下一周期模板」精确预填 sceneType / includeCardio。
--
-- 说明：
-- 1. scene_type 允许 NULL：手动模板或历史行无此概念，AI 生成模板会写入。
-- 2. include_cardio 默认 1（允许有氧），与现有前端默认一致。
-- 3. 非破坏式，仅新增两列，不影响既有数据。

ALTER TABLE cycle_templates
    ADD COLUMN scene_type VARCHAR(32) NULL COMMENT '生成场景类型(gym/home)' AFTER goal_type,
    ADD COLUMN include_cardio TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许有氧(1=是 0=否)' AFTER scene_type;
