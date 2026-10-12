-- ============================================================
-- 迁移脚本：用户自定义食谱（组合食物）能力
-- ============================================================
-- 背景：
--   需求要求第三层「用户自定义食谱 = 若干基础食材组合、营养自动累加」。
--   本脚本为老库补齐两张结构（新库由 init.sql 或 ddl-auto=update 自动处理）：
--     1) user_custom_food 增加 is_recipe 列（标记是否为组合食谱）
--     2) 新增 user_custom_food_item 表（食谱组成明细）
--
-- 可重复执行：用 information_schema 判断列/表是否存在，避免重复报错。
-- ============================================================

USE diet_ai_planner;

-- 1) user_custom_food 增加 is_recipe 列 ------------------------
DROP PROCEDURE IF EXISTS add_is_recipe_column;
DELIMITER $$
CREATE PROCEDURE add_is_recipe_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_custom_food' AND COLUMN_NAME = 'is_recipe'
    ) THEN
        ALTER TABLE `user_custom_food`
            ADD COLUMN `is_recipe` TINYINT(1) NOT NULL DEFAULT 0
            COMMENT '是否为组合食谱（由食材组合累加营养）' AFTER `unit_weight`;
    END IF;
END$$
DELIMITER ;
CALL add_is_recipe_column();
DROP PROCEDURE add_is_recipe_column;

-- 2) 新增食谱组成明细表 ----------------------------------------
CREATE TABLE IF NOT EXISTS `user_custom_food_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `recipe_id`     BIGINT        NOT NULL COMMENT '所属食谱ID（user_custom_food.id，is_recipe=1）',
    `food_id`       BIGINT        NOT NULL COMMENT '基础食材ID（food.id）',
    `food_name`     VARCHAR(100)  NOT NULL COMMENT '食材名快照',
    `amount`        DECIMAL(8,2)  NOT NULL COMMENT '食材用量（克）',
    `calories`      DECIMAL(8,2)  NOT NULL COMMENT '该食材贡献热量快照(kcal)',
    `protein`       DECIMAL(8,2)  DEFAULT NULL COMMENT '该食材贡献蛋白质快照(g)',
    `carbohydrate`  DECIMAL(8,2)  DEFAULT NULL COMMENT '该食材贡献碳水快照(g)',
    `fat`           DECIMAL(8,2)  DEFAULT NULL COMMENT '该食材贡献脂肪快照(g)',
    `created_at`    DATETIME      NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_recipe` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义食谱组成明细表';
