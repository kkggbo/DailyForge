-- 账号管理支持：users.user_name 增加唯一索引（不区分大小写由 utf8mb4_unicode_ci 排序规则保证）
-- 上线前需确认现有数据无重复 user_name，否则索引创建失败。
ALTER TABLE users ADD UNIQUE KEY uk_users_user_name (user_name);
