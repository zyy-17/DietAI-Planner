-- ============================================================
-- 迁移：登录密码改为明文存储（教学 / 演示用途）
--
-- 背景：项目早期用 BCrypt 存密码，库里是 $2a$10$... 的 60 位散列。
-- 现按需求改为「直接存明文」，Java 侧 SecurityConfig#passwordEncoder
-- 已改为 encode 原样返回、matches 优先明文比较（并对历史 $2a$ 数据保留
-- BCrypt 兜底校验，避免老账号被锁死）。
--
-- 本脚本只做两件 ddl-auto 做不到的事：
--   1) 修正列注释，让读库的人知道这里不再是散列
--   2) 可重复执行，重复跑不会报错
--
-- ⚠️ 已存在的 BCrypt 散列**无法还原成明文**，脚本不做任何改写；
--    需要把某个账号密码重置为已知明文时，请用：
--    UPDATE `user` SET password_hash = '新密码' WHERE username = 'xxx';
-- ============================================================

-- 显式声明字符集：Windows 下 mysql 客户端默认 gbk，直接比较中文注释会报
-- ERROR 1267 Illegal mix of collations (utf8mb3_bin / gbk_chinese_ci)
SET NAMES utf8mb4;

-- 修正 password_hash 列注释（列名保持不变，避免改实体 / 改代码的连带风险）
SET @col_comment := (
    SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'password_hash'
);

SET @sql := IF(@col_comment = '登录密码（明文存储，教学演示用）',
    'SELECT ''password_hash 列注释已是目标值，跳过'' AS result',
    'ALTER TABLE `user` MODIFY COLUMN `password_hash` VARCHAR(100) NOT NULL COMMENT ''登录密码（明文存储，教学演示用）'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
