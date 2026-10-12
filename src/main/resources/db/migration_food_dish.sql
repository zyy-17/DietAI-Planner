-- ============================================================
-- 迁移脚本：引入「成品菜肴」层（外食快速记录）
-- ============================================================
-- 背景：
--   食物库三层架构：基础食材库 + 系统成品菜肴库 + 用户自定义食谱库。
--   本脚本把成品菜落到现有 food 表（food_type='dish'），不新建表。
--
-- ⚠️ 本脚本必须兼容 Spring ScriptUtils 执行（借 App DataSource 跑）：
--    不能使用 DELIMITER / 存储过程（Spring 按 ";" 分割语句，不支持它们），
--    可重复执行靠 INSERT ... SELECT ... WHERE NOT EXISTS 与 UPDATE 幂等性保证。
--
-- food_type 列由 ddl-auto=update 自动加列（老行的值为空串 ''，由第 1 步回填）。
-- ============================================================

USE diet_ai_planner;

-- 1) 回填 food_type：历史数据（空串/NULL）默认归为基础食材 ------
UPDATE `food` SET `food_type` = 'ingredient'
WHERE `food_type` IS NULL OR `food_type` = '';

-- 2) 确保「成品菜肴」分类存在（按名称判断，可重复执行）---------
INSERT INTO `food_category` (`name`, `parent_id`, `sort_order`, `icon`, `status`)
SELECT '成品菜肴', 0, 11, 'dish', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `food_category` WHERE `name` = '成品菜肴');

-- 3) 成品菜数据（每 100g 行业平均估算值，仅供参考）-------------
--    营养为每 100g 估算值；unit_name/unit_weight 表示一份约多少克
INSERT INTO `food`
    (`name`, `category_id`, `calories`, `protein`, `carbohydrate`, `fat`, `fiber`,
     `image_url`, `unit_name`, `unit_weight`, `food_type`, `source`, `status`,
     `created_by`, `created_at`, `updated_at`)
SELECT t.n,
       (SELECT `id` FROM `food_category` WHERE `name` = '成品菜肴' LIMIT 1),
       t.cal, t.p, t.c, t.f, t.fi,
       NULL, t.un, t.uw, 'dish', 'system', 'approved', NULL, NOW(), NOW()
FROM (
    SELECT '蛋炒饭' AS n, 150.00 AS cal, 4.50 AS p, 22.00 AS c, 5.00 AS f, 0.80 AS fi, '份' AS un, 300.00 AS uw
    UNION ALL SELECT '扬州炒饭', 168.00, 5.20, 24.00, 6.20, 1.00, '份', 320.00
    UNION ALL SELECT '牛肉炒饭', 172.00, 6.00, 23.50, 6.50, 0.80, '份', 320.00
    UNION ALL SELECT '黄焖鸡米饭', 155.00, 7.50, 18.00, 6.80, 1.20, '份', 450.00
    UNION ALL SELECT '红烧肉盖饭', 210.00, 8.00, 22.00, 12.00, 0.60, '份', 400.00
    UNION ALL SELECT '鱼香肉丝盖饭', 185.00, 6.80, 21.00, 9.50, 0.80, '份', 400.00
    UNION ALL SELECT '宫保鸡丁', 190.00, 9.50, 14.00, 11.00, 0.90, '份', 350.00
    UNION ALL SELECT '麻辣烫', 120.00, 6.00, 12.00, 6.50, 2.00, '份', 500.00
    UNION ALL SELECT '牛肉面', 130.00, 6.50, 20.00, 3.80, 1.00, '份', 450.00
    UNION ALL SELECT '炸酱面', 175.00, 6.00, 26.00, 6.00, 1.20, '份', 450.00
    UNION ALL SELECT '螺蛳粉', 115.00, 4.50, 18.00, 4.00, 1.00, '份', 500.00
    UNION ALL SELECT '兰州拉面', 125.00, 5.50, 21.00, 3.50, 1.00, '份', 450.00
    UNION ALL SELECT '奶茶', 65.00, 1.20, 11.00, 2.50, 0.00, '杯', 500.00
    UNION ALL SELECT '炸鸡', 250.00, 18.00, 12.00, 15.00, 0.50, '份', 300.00
    UNION ALL SELECT '包子', 220.00, 8.00, 38.00, 6.00, 1.50, '个', 80.00
    UNION ALL SELECT '煎饺', 210.00, 8.50, 25.00, 9.00, 1.00, '个', 35.00
    UNION ALL SELECT '汉堡', 280.00, 13.00, 30.00, 13.00, 1.50, '个', 200.00
    UNION ALL SELECT '披萨', 265.00, 11.00, 32.00, 11.00, 1.50, '份', 250.00
    UNION ALL SELECT '薯条', 310.00, 3.50, 40.00, 15.00, 3.00, '份', 150.00
    UNION ALL SELECT '蛋挞', 320.00, 5.50, 35.00, 18.00, 0.50, '个', 50.00
    UNION ALL SELECT '饺子', 200.00, 8.00, 28.00, 8.00, 1.00, '个', 30.00
    UNION ALL SELECT '馄饨', 160.00, 7.00, 24.00, 5.00, 0.80, '份', 250.00
    UNION ALL SELECT '盖浇饭', 165.00, 7.00, 22.00, 6.50, 1.00, '份', 400.00
    UNION ALL SELECT '蛋花汤', 30.00, 2.00, 2.50, 1.50, 0.20, '份', 300.00
) t
WHERE NOT EXISTS (SELECT 1 FROM `food` e WHERE e.name = t.n AND e.food_type = 'dish');

-- 4) 「成品菜肴」分类下的历史数据归为 dish（若存在）------------
UPDATE `food` f
JOIN `food_category` fc ON f.category_id = fc.id AND fc.name = '成品菜肴'
SET f.food_type = 'dish'
WHERE f.food_type = 'ingredient';
