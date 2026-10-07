-- ============================================================
-- 迁移脚本：食谱（手工编写 + 食谱广场 + 发布审核）改造
-- 适用：已经存在的库（新库由 Hibernate ddl-auto 直接建表，无需本脚本）
--
-- 背景：
--   改造后「食谱」生命周期变成   编写 → 发布待审 → 上架广场 → 收藏/应用 → 执行，
--   食谱在「开始执行」之前是没有开始日期的。
--   实体里 MealPlan.startDate 已允许为空，但老库里 meal_plan.start_date 是 NOT NULL，
--   Hibernate 的 ddl-auto=update 只会新增列，不会放宽已有列的非空约束，
--   因此这里显式把该列改成可空。
--
-- 作用：
--   1. 把 meal_plan.start_date 改为允许 NULL（仅在仍为 NOT NULL 时执行）
--   2. 兜底补齐改造新增的列（Hibernate 通常已自动加好，已存在则跳过）
--
-- 特性：可重复执行。
-- 执行：mysql -uroot -p diet_ai_planner < migration_meal_plan_recipe.sql
-- ============================================================

-- ---------- 1. 放宽 start_date 非空约束 ----------
SET @stmt := (SELECT IF(IS_NULLABLE = 'NO',
    'ALTER TABLE `meal_plan` MODIFY COLUMN `start_date` DATE NULL COMMENT ''开始执行日期，未执行时为 NULL''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'start_date');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2. 兜底补齐食谱新增列（已存在则跳过） ----------
-- status: 执行状态 idle/active/archived；publish_status: 发布状态 none/pending/approved/rejected
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''idle'' COMMENT ''执行状态'' AFTER `user_id`',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'status');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `publish_status` VARCHAR(20) NOT NULL DEFAULT ''none'' COMMENT ''发布状态''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'publish_status');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `cover_url` VARCHAR(255) DEFAULT NULL COMMENT ''食谱封面''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'cover_url');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT ''标签，逗号分隔''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'tags');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `difficulty` VARCHAR(20) DEFAULT NULL COMMENT ''难度''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'difficulty');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `expected_loss` VARCHAR(50) DEFAULT NULL COMMENT ''预计减重''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'expected_loss');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `description` TEXT DEFAULT NULL COMMENT ''食谱说明/心得''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'description');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `source_plan_id` BIGINT DEFAULT NULL COMMENT ''来源食谱ID（从广场保存而来）''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'source_plan_id');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `usage_count` INT NOT NULL DEFAULT 0 COMMENT ''被保存/应用次数''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'usage_count');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `favorite_count` INT NOT NULL DEFAULT 0 COMMENT ''收藏数''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'favorite_count');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `published_at` DATETIME DEFAULT NULL COMMENT ''上架时间''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'published_at');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT ''驳回原因''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'reject_reason');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- 食谱条目保存食物图片快照
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan_item` ADD COLUMN `image_url` VARCHAR(255) DEFAULT NULL COMMENT ''食物图片快照''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan_item' AND COLUMN_NAME = 'image_url');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- 食谱收藏表（Hibernate 会自动建，这里兜底）
CREATE TABLE IF NOT EXISTS `meal_plan_favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `plan_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_meal_plan_favorite` (`user_id`, `plan_id`),
    KEY `idx_mpf_user` (`user_id`),
    KEY `idx_mpf_plan` (`plan_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '食谱收藏';

-- ---------- 3. 结果确认 ----------
SELECT 'meal_plan' AS tbl, COLUMN_NAME, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'start_date';
