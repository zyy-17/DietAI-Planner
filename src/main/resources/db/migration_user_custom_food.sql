-- ============================================================
-- 迁移脚本：把用户自定义食物从公共食物库（food 表）搬到独立表
-- ============================================================
-- 背景：
--   旧实现把「用户自定义食物」也写进 food 表（source='user'），
--   导致食物库/食物管理里混入用户自建的食物。
--   新实现新增 user_custom_food 表做物理隔离，此脚本负责搬运历史数据。
--
-- 使用场景：数据库里已存在 source='user' 的历史数据时执行一次。
-- 全新初始化的库不需要执行（init.sql 已是新结构）。
--
-- 执行前请先备份：mysqldump -uroot -p diet_ai_planner > backup.sql
-- ============================================================

USE diet_ai_planner;

-- 1) 新增用户自定义食物表 ------------------------------------
CREATE TABLE IF NOT EXISTS `user_custom_food` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT        NOT NULL COMMENT '所属用户ID',
    `name`          VARCHAR(100)  NOT NULL COMMENT '食物名称',
    `category_id`   BIGINT        NOT NULL COMMENT '关联分类ID',
    `calories`      DECIMAL(8,2)  NOT NULL COMMENT '热量(kcal/100g)',
    `protein`       DECIMAL(8,2)  DEFAULT NULL COMMENT '蛋白质(g/100g)',
    `carbohydrate`  DECIMAL(8,2)  DEFAULT NULL COMMENT '碳水(g/100g)',
    `fat`           DECIMAL(8,2)  DEFAULT NULL COMMENT '脂肪(g/100g)',
    `fiber`         DECIMAL(8,2)  DEFAULT NULL COMMENT '膳食纤维(g/100g)',
    `created_at`    DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_user_name` (`user_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义食物表（私有，不进公共食物库）';

-- 2) 饮食记录表增加来源字段 ----------------------------------
--    不存在时才添加（MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，用存储过程判断）
DROP PROCEDURE IF EXISTS add_food_source_column;
DELIMITER $$
CREATE PROCEDURE add_food_source_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'diet_record' AND COLUMN_NAME = 'food_source'
    ) THEN
        ALTER TABLE `diet_record`
            ADD COLUMN `food_source` VARCHAR(20) NOT NULL DEFAULT 'system'
            COMMENT '食物来源：system公共食物库/user用户自定义食物' AFTER `food_id`;
    END IF;
END$$
DELIMITER ;
CALL add_food_source_column();
DROP PROCEDURE add_food_source_column;

-- 3) 先把引用了用户自定义食物的饮食记录标记为 user 来源 -------
UPDATE `diet_record` dr
JOIN `food` f ON dr.food_id = f.id AND f.source = 'user'
SET dr.food_source = 'user';

-- 4) 搬运食物数据 --------------------------------------------
--    先用临时表记录「旧 food.id -> 新 user_custom_food.id」的映射，
--    以便把饮食记录里的 food_id 换成新表主键。
DROP TEMPORARY TABLE IF EXISTS tmp_food_map;
CREATE TEMPORARY TABLE tmp_food_map (
    old_food_id BIGINT PRIMARY KEY,
    new_id      BIGINT NOT NULL
);

INSERT INTO `user_custom_food`
    (`user_id`, `name`, `category_id`, `calories`, `protein`, `carbohydrate`, `fat`, `fiber`, `created_at`, `updated_at`)
SELECT `created_by`, `name`, `category_id`, `calories`, `protein`, `carbohydrate`, `fat`, `fiber`, `created_at`, `updated_at`
FROM `food`
WHERE `source` = 'user' AND `created_by` IS NOT NULL;

-- 同一用户同名食物只保留一条，因此可用 (user_id, name) 建立映射
-- （若 user_custom_food 里已存在同名记录，取最小的那条，保证映射唯一）
INSERT INTO tmp_food_map (old_food_id, new_id)
SELECT f.id, MIN(u.id)
FROM `food` f
JOIN `user_custom_food` u ON u.user_id = f.created_by AND u.name = f.name
WHERE f.source = 'user' AND f.created_by IS NOT NULL
GROUP BY f.id;

-- 5) 回填饮食记录：food_id 换到新表主键 -----------------------
UPDATE `diet_record` dr
JOIN tmp_food_map m ON dr.food_id = m.old_food_id
SET dr.food_id = m.new_id
WHERE dr.food_source = 'user';

-- 6) 食物库回归纯净：删除 food 表中的用户自定义食物 -----------
DELETE FROM `food` WHERE `source` = 'user';

DROP TEMPORARY TABLE IF EXISTS tmp_food_map;

-- 7) 校验 ----------------------------------------------------
SELECT 'food 表剩余来源分布' AS step;
SELECT `source`, COUNT(*) AS cnt FROM `food` GROUP BY `source`;
SELECT 'user_custom_food 数据' AS step;
SELECT u.id, u.user_id, u.name, u.calories FROM `user_custom_food` u ORDER BY u.id;
SELECT '饮食记录来源分布' AS step;
SELECT `food_source`, COUNT(*) AS cnt FROM `diet_record` GROUP BY `food_source`;
