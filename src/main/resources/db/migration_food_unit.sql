-- ============================================================
-- 迁移脚本：食物计量单位（按个数 / 按克数记录）
-- 适用：已经存在的库（新库直接跑 init.sql 即可，无需本脚本）
--
-- 作用：
--   1. 给 food、user_custom_food 两表补上 unit_name / unit_weight 两列
--      （unit_name = 常用单位名，如 个/份/盒；unit_weight = 每个/每份约多少克）
--   2. 给常见食物预置参考单位，让「鸡蛋」这类食物可以直接按个数记录
--
-- 特性：可重复执行。列已存在时跳过，已配置过单位的食物不会被覆盖。
-- 执行：mysql -uroot -p diet_ai_planner < migration_food_unit.sql
-- ============================================================

-- ---------- 1. 补列（已存在则跳过） ----------

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `food` ADD COLUMN `unit_name` VARCHAR(20) DEFAULT NULL COMMENT ''常用计量单位名称，如 个/份/盒'' AFTER `image_url`',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food' AND COLUMN_NAME = 'unit_name');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `food` ADD COLUMN `unit_weight` DECIMAL(8,2) DEFAULT NULL COMMENT ''每个/每份约多少克'' AFTER `unit_name`',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food' AND COLUMN_NAME = 'unit_weight');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `user_custom_food` ADD COLUMN `unit_name` VARCHAR(20) DEFAULT NULL COMMENT ''常用计量单位名称'' AFTER `fiber`',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_custom_food' AND COLUMN_NAME = 'unit_name');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `user_custom_food` ADD COLUMN `unit_weight` DECIMAL(8,2) DEFAULT NULL COMMENT ''每个/每份约多少克'' AFTER `unit_name`',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_custom_food' AND COLUMN_NAME = 'unit_weight');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2. 常见食物预置单位参考值 ----------
-- 数值为常见规格的参考克重，仅作为「按个数」换算的默认值，
-- 用户在记录时仍可自行修改每个多少克；管理员也可在食物管理里调整。

DROP TEMPORARY TABLE IF EXISTS `tmp_food_unit`;
CREATE TEMPORARY TABLE `tmp_food_unit` (
    `name`        VARCHAR(100) NOT NULL,
    `unit_name`   VARCHAR(20)  NOT NULL,
    `unit_weight` DECIMAL(8,2) NOT NULL,
    PRIMARY KEY (`name`)
);

INSERT INTO `tmp_food_unit` (`name`, `unit_name`, `unit_weight`) VALUES
    ('鸡蛋',   '个', 50.00),
    ('鸡蛋清', '个', 35.00),
    ('牛奶',   '盒', 250.00),
    ('酸奶',   '杯', 150.00),
    ('豆浆',   '杯', 250.00),
    ('咖啡',   '杯', 250.00),
    ('可乐',   '罐', 330.00),
    ('啤酒',   '罐', 330.00),
    ('苹果',   '个', 200.00),
    ('香蕉',   '根', 120.00),
    ('橙子',   '个', 180.00),
    ('橘子',   '个', 100.00),
    ('梨',     '个', 200.00),
    ('猕猴桃', '个', 80.00),
    ('芒果',   '个', 200.00),
    ('西瓜',   '块', 200.00),
    ('葡萄',   '粒', 8.00),
    ('草莓',   '个', 15.00),
    ('番茄',   '个', 150.00),
    ('黄瓜',   '根', 200.00),
    ('胡萝卜', '根', 100.00),
    ('玉米',   '根', 200.00),
    ('土豆',   '个', 150.00),
    ('红薯',   '个', 200.00),
    ('米饭',   '碗', 200.00),
    ('面条',   '碗', 200.00),
    ('馒头',   '个', 100.00),
    ('包子',   '个', 80.00),
    ('饺子',   '个', 20.00),
    ('馄饨',   '个', 15.00),
    ('面包',   '片', 35.00),
    ('全麦面包', '片', 35.00),
    ('吐司',   '片', 35.00),
    ('饼干',   '片', 10.00),
    ('花生',   '粒', 10.00),
    ('核桃',   '个', 10.00),
    ('巧克力', '块', 10.00),
    ('火腿肠', '根', 40.00),
    ('香肠',   '根', 50.00),
    ('培根',   '片', 20.00),
    ('鸡胸肉', '块', 150.00),
    ('鸡腿',   '个', 120.00),
    ('豆腐',   '块', 100.00),
    ('虾',     '只', 15.00),
    ('蜂蜜',   '勺', 15.00),
    ('燕麦',   '勺', 15.00),
    ('橄榄油', '勺', 10.00),
    ('蛋白粉', '勺', 30.00),
    ('月饼',   '个', 100.00),
    ('蛋糕',   '块', 80.00);

-- 系统食物库：只补未配置过的，不覆盖管理员的设置
UPDATE `food` f
JOIN `tmp_food_unit` t ON t.`name` = f.`name`
SET f.`unit_name` = t.`unit_name`,
    f.`unit_weight` = t.`unit_weight`
WHERE f.`source` = 'system'
  AND (f.`unit_name` IS NULL OR f.`unit_name` = '');

-- 用户自定义食物：同样只补空的，不覆盖用户自己设置的值
UPDATE `user_custom_food` u
JOIN `tmp_food_unit` t ON t.`name` = u.`name`
SET u.`unit_name` = t.`unit_name`,
    u.`unit_weight` = t.`unit_weight`
WHERE u.`unit_name` IS NULL OR u.`unit_name` = '';

DROP TEMPORARY TABLE IF EXISTS `tmp_food_unit`;

-- ---------- 3. 结果确认 ----------
SELECT 'food' AS tbl, COUNT(*) AS total,
       SUM(unit_name IS NOT NULL) AS with_unit FROM `food`
UNION ALL
SELECT 'user_custom_food', COUNT(*), SUM(unit_name IS NOT NULL) FROM `user_custom_food`;
