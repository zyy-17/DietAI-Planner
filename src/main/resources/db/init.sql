CREATE DATABASE IF NOT EXISTS diet_ai_planner DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE diet_ai_planner;

-- ============================================================
-- 1. 用户表 (user)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `username`       VARCHAR(50)   NOT NULL COMMENT '用户名，唯一',
    `email`          VARCHAR(100)  DEFAULT NULL COMMENT '邮箱，唯一',
    `password_hash`  VARCHAR(100)  NOT NULL COMMENT 'BCrypt加密密码',
    `real_name`      VARCHAR(50)   DEFAULT NULL COMMENT '真实姓名',
    `gender`         TINYINT       DEFAULT NULL COMMENT '性别：0未知/1男/2女',
    `birth_date`     DATE          DEFAULT NULL COMMENT '出生日期',
    `height`         DECIMAL(5,1)  DEFAULT NULL COMMENT '身高(cm)',
    `weight`         DECIMAL(5,1)  DEFAULT NULL COMMENT '体重(kg)',
    `activity_level` TINYINT       DEFAULT NULL COMMENT '活动水平1-5',
    `diet_goal`      VARCHAR(20)   DEFAULT NULL COMMENT '减脂/维持/增肌/健康管理',
    `diet_preference` VARCHAR(200) DEFAULT NULL COMMENT '饮食偏好，如：清淡、素食、低糖等',
    `avatar_url`     VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
    `role`           VARCHAR(20)   NOT NULL DEFAULT 'user' COMMENT '角色：user/admin',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1正常/0禁用',
    `created_at`     DATETIME      NOT NULL COMMENT '注册时间',
    `updated_at`     DATETIME      NOT NULL COMMENT '更新时间',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常/1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 食品分类表 (food_category)
-- ============================================================
CREATE TABLE IF NOT EXISTS `food_category` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `parent_id`  BIGINT       DEFAULT 0 COMMENT '父分类ID，0为一级分类',
    `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
    `icon`       VARCHAR(100) DEFAULT NULL COMMENT '分类图标URL',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用/0禁用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食品分类表';

-- ============================================================
-- 3. 食物表 (food)
-- ============================================================
CREATE TABLE IF NOT EXISTS `food` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(100)  NOT NULL COMMENT '食物名称',
    `category_id`   BIGINT        NOT NULL COMMENT '关联分类ID',
    `calories`      DECIMAL(8,2)  NOT NULL COMMENT '热量(kcal/100g)',
    `protein`       DECIMAL(8,2)  DEFAULT NULL COMMENT '蛋白质(g/100g)',
    `carbohydrate`  DECIMAL(8,2)  DEFAULT NULL COMMENT '碳水(g/100g)',
    `fat`           DECIMAL(8,2)  DEFAULT NULL COMMENT '脂肪(g/100g)',
    `fiber`         DECIMAL(8,2)  DEFAULT NULL COMMENT '膳食纤维(g/100g)',
    `image_url`     VARCHAR(255)  DEFAULT NULL COMMENT '食物图片',
    `source`        VARCHAR(20)   NOT NULL DEFAULT 'system' COMMENT '来源：system/user',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'approved' COMMENT 'pending/approved/rejected',
    `created_by`    BIGINT        DEFAULT NULL COMMENT '提交用户ID',
    `created_at`    DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物表';

-- ============================================================
-- 4. 饮食记录表 (diet_record)
-- ============================================================
CREATE TABLE IF NOT EXISTS `diet_record` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `food_id`       BIGINT        NOT NULL COMMENT '食物ID',
    `meal_type`     VARCHAR(20)   NOT NULL COMMENT 'breakfast/lunch/dinner/snack',
    `amount`        DECIMAL(8,2)  NOT NULL COMMENT '食用份量(克)',
    `calories`      DECIMAL(8,2)  NOT NULL COMMENT '本条记录热量',
    `protein`       DECIMAL(8,2)  DEFAULT NULL COMMENT '本条蛋白质(g)',
    `carbohydrate`  DECIMAL(8,2)  DEFAULT NULL COMMENT '本条碳水(g)',
    `fat`           DECIMAL(8,2)  DEFAULT NULL COMMENT '本条脂肪(g)',
    `record_date`   DATE          NOT NULL COMMENT '记录日期',
    `created_at`    DATETIME      NOT NULL COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `record_date`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';

-- ============================================================
-- 5. AI对话会话表 (ai_chat_session)
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `title`      VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
    `created_at` DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- ============================================================
-- 6. AI对话消息表 (ai_chat_message)
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id`      BIGINT       NOT NULL COMMENT '会话ID',
    `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
    `role`            VARCHAR(20)  NOT NULL COMMENT 'user/assistant/system',
    `content`         TEXT         NOT NULL COMMENT '消息内容',
    `context_snapshot` TEXT        DEFAULT NULL COMMENT '当时注入的用户上下文快照',
    `created_at`      DATETIME     NOT NULL COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_session` (`session_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';

-- ============================================================
-- 7. AI生成记录表 (ai_generation_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `type`           VARCHAR(30)  NOT NULL COMMENT 'chat/diet_plan/nutrition_analysis',
    `input_summary`  TEXT         DEFAULT NULL COMMENT '输入摘要',
    `output_content` TEXT         DEFAULT NULL COMMENT 'AI生成内容',
    `model_name`     VARCHAR(50)  DEFAULT NULL COMMENT '使用的模型名称',
    `tokens_used`    INT          DEFAULT NULL COMMENT '消耗Token数',
    `is_abnormal`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否异常',
    `created_at`     DATETIME     NOT NULL COMMENT '生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_abnormal` (`is_abnormal`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成记录表';

-- ============================================================
-- 8. 营养标准配置表 (nutrition_standard)
-- ============================================================
CREATE TABLE IF NOT EXISTS `nutrition_standard` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gender`        TINYINT      NOT NULL COMMENT '性别：1男/2女',
    `age_min`       INT          NOT NULL COMMENT '年龄下限',
    `age_max`       INT          NOT NULL COMMENT '年龄上限',
    `calories_kcal` INT          NOT NULL COMMENT '推荐热量(kcal)',
    `protein_g`     DECIMAL(8,2) DEFAULT NULL COMMENT '推荐蛋白质(g)',
    `carb_g`        DECIMAL(8,2) DEFAULT NULL COMMENT '推荐碳水(g)',
    `fat_g`         DECIMAL(8,2) DEFAULT NULL COMMENT '推荐脂肪(g)',
    PRIMARY KEY (`id`),
    KEY `idx_gender_age` (`gender`, `age_min`, `age_max`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营养标准配置表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号 (密码: admin123)
INSERT INTO `user` (`username`, `email`, `password_hash`, `real_name`, `gender`, `birth_date`, `height`, `weight`, `activity_level`, `diet_goal`, `role`, `status`, `created_at`, `updated_at`, `deleted`)
VALUES ('admin', 'admin@dietai.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 1, '1990-01-01', 175.0, 70.0, 3, 'maintain', 'admin', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `id`=`id`;

-- 食品分类
INSERT INTO `food_category` (`name`, `parent_id`, `sort_order`, `icon`, `status`) VALUES
('主食', 0, 1, 'rice', 1),
('肉类', 0, 2, 'meat', 1),
('蔬菜', 0, 3, 'vegetable', 1),
('水果', 0, 4, 'fruit', 1),
('奶制品', 0, 5, 'milk', 1),
('豆制品', 0, 6, 'bean', 1),
('零食', 0, 7, 'snack', 1),
('饮品', 0, 8, 'drink', 1),
('海鲜', 0, 9, 'seafood', 1),
('蛋类', 0, 10, 'egg', 1);

-- 常见食物数据
INSERT INTO `food` (`name`, `category_id`, `calories`, `protein`, `carbohydrate`, `fat`, `fiber`, `image_url`, `source`, `status`, `created_by`, `created_at`, `updated_at`) VALUES
('白米饭', 1, 116.00, 2.60, 25.90, 0.30, 0.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('馒头', 1, 221.00, 7.00, 47.00, 1.10, 1.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('面条(煮)', 1, 110.00, 3.50, 22.80, 0.40, 0.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('糙米饭', 1, 111.00, 2.60, 23.50, 0.90, 1.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡胸肉', 2, 133.00, 31.00, 0.00, 3.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('猪瘦肉', 2, 143.00, 20.30, 1.50, 6.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('牛肉', 2, 125.00, 19.90, 0.00, 4.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡蛋', 10, 144.00, 13.30, 1.50, 8.80, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('牛奶', 5, 54.00, 3.00, 3.40, 3.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('酸奶', 5, 72.00, 2.50, 9.30, 2.70, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('番茄', 3, 15.00, 0.90, 3.30, 0.20, 0.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('西兰花', 3, 36.00, 4.10, 4.30, 0.60, 3.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('菠菜', 3, 28.00, 2.90, 3.60, 0.40, 2.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('苹果', 4, 53.00, 0.20, 13.70, 0.20, 2.10, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('香蕉', 4, 93.00, 1.40, 22.00, 0.20, 1.20, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('橙子', 4, 48.00, 0.80, 11.10, 0.20, 0.60, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('豆腐', 6, 82.00, 8.10, 4.20, 3.70, 0.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('清蒸鱼', 9, 109.00, 19.60, 0.00, 3.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('虾', 9, 87.00, 18.60, 0.80, 0.80, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('燕麦片', 1, 377.00, 13.50, 66.30, 6.70, 5.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('全麦面包', 1, 246.00, 8.50, 44.30, 3.50, 6.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('红薯', 1, 99.00, 1.10, 23.10, 0.20, 1.60, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('玉米', 1, 112.00, 4.00, 22.80, 1.20, 2.90, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡腿', 2, 181.00, 16.00, 0.00, 13.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡翅', 2, 194.00, 17.40, 0.00, 13.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('猪排骨', 2, 264.00, 18.30, 0.00, 20.40, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('羊肉', 2, 203.00, 19.00, 0.00, 14.10, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('三文鱼', 9, 139.00, 21.60, 0.00, 5.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鳕鱼', 9, 88.00, 20.40, 0.00, 0.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('胡萝卜', 3, 37.00, 1.00, 8.10, 0.20, 2.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('黄瓜', 3, 16.00, 0.80, 2.90, 0.20, 0.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('生菜', 3, 13.00, 1.30, 1.30, 0.30, 0.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('白菜', 3, 18.00, 1.50, 2.40, 0.20, 0.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('茄子', 3, 23.00, 1.10, 4.60, 0.20, 1.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('青椒', 3, 22.00, 1.00, 4.70, 0.20, 1.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('葡萄', 4, 44.00, 0.50, 10.30, 0.20, 0.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('西瓜', 4, 31.00, 0.50, 6.80, 0.10, 0.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('草莓', 4, 32.00, 1.00, 7.10, 0.20, 1.10, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('蓝莓', 4, 57.00, 0.70, 14.50, 0.30, 2.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('豆浆', 6, 31.00, 3.00, 1.20, 1.60, 0.20, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('豆腐干', 6, 140.00, 15.80, 4.20, 5.90, 0.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('花生', 7, 563.00, 24.80, 13.90, 44.40, 5.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('核桃', 7, 627.00, 14.90, 9.60, 58.80, 9.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('绿茶', 8, 1.00, 0.00, 0.00, 0.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('黑咖啡', 8, 1.00, 0.10, 0.00, 0.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸭蛋', 10, 180.00, 12.60, 0.50, 13.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鹌鹑蛋', 10, 160.00, 12.80, 2.10, 11.10, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW());

-- 营养标准配置
INSERT INTO `nutrition_standard` (`gender`, `age_min`, `age_max`, `calories_kcal`, `protein_g`, `carb_g`, `fat_g`) VALUES
(1, 18, 30, 2600, 65.00, 325.00, 72.00),
(1, 31, 50, 2400, 60.00, 300.00, 67.00),
(1, 51, 70, 2200, 55.00, 275.00, 61.00),
(2, 18, 30, 2100, 55.00, 263.00, 58.00),
(2, 31, 50, 1900, 50.00, 238.00, 53.00),
(2, 51, 70, 1700, 45.00, 213.00, 47.00);