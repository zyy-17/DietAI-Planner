-- ============================================================
-- 迁移脚本：食谱执行闭环（周期进度 + 暂停/结束 + 软删除）
-- 适用：已经存在的库（新库由 Hibernate ddl-auto 直接建表，无需本脚本）
--
-- 背景：
--   本次为食谱模块补齐「执行闭环」能力：
--   1. 执行状态新增 paused（暂停），可在执行中途暂停、恢复；
--   2. 新增 paused_at_day 记录暂停时处在第几天，恢复时从这天继续；
--   3. 新增 deleted 软删除标记，被膳食方案/执行历史引用的食谱不再物理删除。
--
-- 特性：可重复执行。
-- 执行：mysql -uroot -p diet_ai_planner < migration_meal_plan_execution.sql
-- ============================================================

-- 软删除标记（默认 0 = 未删除）
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''软删除标记''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'deleted');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- 暂停时处在第几天（恢复执行时从这天继续）
SET @stmt := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `meal_plan` ADD COLUMN `paused_at_day` INT DEFAULT NULL COMMENT ''暂停时第几天''',
    'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan' AND COLUMN_NAME = 'paused_at_day');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- 审核记录表（保留完整审核历史）
CREATE TABLE IF NOT EXISTS `meal_plan_review` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_id`       BIGINT       NOT NULL,
    `reviewer_id`   BIGINT       DEFAULT NULL,
    `reviewer_name` VARCHAR(50)  DEFAULT NULL,
    `result`        VARCHAR(20)  NOT NULL,
    `reason`        VARCHAR(500) DEFAULT NULL,
    `reviewed_at`   DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_mpr_plan` (`plan_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '食谱审核记录';

-- ---------- 结果确认 ----------
SELECT 'meal_plan' AS tbl, COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'meal_plan'
  AND COLUMN_NAME IN ('deleted', 'paused_at_day');
