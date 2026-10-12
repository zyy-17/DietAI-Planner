-- ============================================================
-- 迁移脚本：食谱「应用来源」溯源与版权保护字段
-- 适用：已经存在的库（新库由 Hibernate ddl-auto=update 自动加列，无需本脚本）
--
-- 背景：
--   用户「应用」他人公开食谱后，原来会生成一份可完整编辑、可重新发布的副本，
--   存在把他人原创作品改头换面后再投稿的侵权风险。
--   改造后引入「来源类型」概念：
--     original 原创    —— 自己创建，可完整编辑、可发布
--     copied   应用来源 —— 源自他人公开食谱，仅可替换/增删食材，不可改基础信息、不可二次发布
--   同时记录来源溯源信息（原食谱 id + 原作者 id + 原作者昵称快照）。
--
-- 作用：
--   1. meal_plan 新增 origin / source_author_id / source_author_name 三列
--   2. 回填历史数据：已有 source_plan_id 的记录标记为 copied 并补上原作者 id
--   3. 补一个 (publish_status, favorite_count) 索引，支撑「热门按收藏数 Top10」查询
--
-- 特性：可重复执行（幂等）。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p diet_ai_planner < migration_meal_plan_source.sql
-- ============================================================

-- ---------- 1. 新增「来源类型」列 ----------
-- origin: original 原创 / copied 应用自他人公开食谱
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `origin` VARCHAR(20) NOT NULL DEFAULT ''original'' COMMENT ''来源类型：original=原创，copied=应用自他人公开食谱'' AFTER `source_plan_id`',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'origin');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- source_author_id: 来源食谱原作者用户 id（溯源用）
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `source_author_id` BIGINT DEFAULT NULL COMMENT ''来源食谱原作者用户ID（应用他人食谱时记录，用于溯源）''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'source_author_id');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- source_author_name: 来源原作者昵称快照（脱敏，如「用户**9」）
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `source_author_name` VARCHAR(100) DEFAULT NULL COMMENT ''来源食谱原作者昵称快照（脱敏）''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'source_author_name');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2. 回填历史数据 ----------
-- 老库里凡是「从广场复制而来」（source_plan_id 非空）的记录，统一标记为「应用来源食谱」。
UPDATE `meal_plan`
SET `origin` = 'copied'
WHERE `source_plan_id` IS NOT NULL AND (`origin` IS NULL OR `origin` = 'original');

-- 顺着来源食谱回填原作者 id（昵称快照留空，读取时按 authorId 现场脱敏补全）
UPDATE `meal_plan` c
JOIN `meal_plan` s ON c.`source_plan_id` = s.`id`
SET c.`source_author_id` = s.`user_id`
WHERE c.`source_plan_id` IS NOT NULL AND c.`source_author_id` IS NULL;

-- ---------- 3. 热门排序索引 ----------
-- 支撑「广场热门：按收藏数倒序取 Top10」的查询
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD INDEX `idx_mp_square_hot` (`publish_status`, `favorite_count`)',
    'DO 0')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND INDEX_NAME = 'idx_mp_square_hot');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 4. 结果确认 ----------
SELECT 'meal_plan' AS tbl, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan'
  AND COLUMN_NAME IN ('origin', 'source_plan_id', 'source_author_id', 'source_author_name');
