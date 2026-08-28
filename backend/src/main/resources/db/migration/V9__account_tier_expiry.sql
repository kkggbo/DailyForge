-- 邀请码账户层级加时间限制（到期自动降级）
-- 说明：
-- 1. users.account_tier_expires_at：当前层级到期时间，NULL=永久。到期后用户回落 basic。
-- 2. invite_codes.grant_duration_days：授予层级的持续天数，NULL=永久。
--    兑换时按 now + grant_duration_days 写入 users.account_tier_expires_at。
-- 3. 非破坏式，仅新增列；既有数据 account_tier_expires_at 为 NULL（永久），不受影响。

ALTER TABLE users
    ADD COLUMN account_tier_expires_at DATETIME(3) NULL COMMENT '当前权益层级到期时间(NULL=永久)' AFTER account_tier;

ALTER TABLE invite_codes
    ADD COLUMN grant_duration_days INT UNSIGNED NULL COMMENT '授予层级持续天数(NULL=永久)' AFTER grant_value;
