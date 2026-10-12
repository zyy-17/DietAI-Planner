-- ============================================================
-- DietAI-Planner 数据库初始化脚本（单文件完整版）
-- 版本：v2.0
-- 数据库：diet_ai_planner
-- 字符集：utf8mb4
-- 引擎：InnoDB
--
-- 说明：
--   本脚本为全新数据库初始化所用的唯一 SQL 文件。
--   包含全部 16 张表的建表语句和初始数据。
--   可重复执行（IF NOT EXISTS + ON DUPLICATE KEY UPDATE 保证幂等）。
--
-- 执行方式：mysql -uroot -p < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS diet_ai_planner DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE diet_ai_planner;

-- ============================================================
-- 第一部分：用户与权限（1 张）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表 (user)
--    存储用户身份认证、基本信息、健康档案、饮食目标、
--    营养目标、UI 偏好、通知设置、隐私控制等全部数据。
--    密码为明文存储（教学/演示用途，正式环境请换 BCrypt）。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `username`               VARCHAR(50)   NOT NULL COMMENT '用户名，唯一',
    `email`                  VARCHAR(100)  DEFAULT NULL COMMENT '邮箱，唯一',
    `password_hash`          VARCHAR(100)  NOT NULL COMMENT '登录密码（明文存储，教学演示用）',
    `real_name`              VARCHAR(50)   DEFAULT NULL COMMENT '真实姓名',
    `gender`                 TINYINT       DEFAULT NULL COMMENT '性别：0未知 / 1男 / 2女',
    `birth_date`             DATE          DEFAULT NULL COMMENT '出生日期',
    `height`                 DECIMAL(5,1)  DEFAULT NULL COMMENT '身高(cm)',
    `weight`                 DECIMAL(5,1)  DEFAULT NULL COMMENT '体重(kg)',
    `activity_level`         TINYINT       DEFAULT NULL COMMENT '活动水平：1久坐 / 2轻度 / 3中度 / 4重度 / 5极重度',
    `diet_goal`              VARCHAR(20)   DEFAULT NULL COMMENT '饮食目标：lose减脂 / maintain维持 / gain增重',
    `diet_preference`        VARCHAR(200)  DEFAULT NULL COMMENT '饮食偏好，如：清淡、素食、低糖等',
    `allergy_note`           VARCHAR(500)  DEFAULT NULL COMMENT '过敏食物',
    `disease`                VARCHAR(500)  DEFAULT NULL COMMENT '疾病信息',
    `medication`             VARCHAR(500)  DEFAULT NULL COMMENT '用药信息',
    `target_weight_kg`       DECIMAL(5,1)  DEFAULT NULL COMMENT '目标体重(kg)',
    `target_body_fat_percent` DECIMAL(5,1) DEFAULT NULL COMMENT '目标体脂率(%)',
    `target_deadline`        DATE          DEFAULT NULL COMMENT '期望达成日期',
    `target_calories`        DECIMAL(8,2)  DEFAULT NULL COMMENT '自定义每日热量目标(kcal)',
    `target_protein`         DECIMAL(8,2)  DEFAULT NULL COMMENT '自定义每日蛋白质目标(g)',
    `target_carbohydrate`    DECIMAL(8,2)  DEFAULT NULL COMMENT '自定义每日碳水目标(g)',
    `target_fat`             DECIMAL(8,2)  DEFAULT NULL COMMENT '自定义每日脂肪目标(g)',
    `diet_reminder`          TINYINT(1)    DEFAULT 1 COMMENT '饮食提醒开关',
    `reminder_time`          VARCHAR(5)    DEFAULT '08:00' COMMENT '提醒时间(HH:mm)',
    `goal_reminder`          TINYINT(1)    DEFAULT 1 COMMENT '目标提醒开关',
    `ai_suggestion`          TINYINT(1)    DEFAULT 1 COMMENT 'AI建议推送开关',
    `theme`                  VARCHAR(10)   DEFAULT 'light' COMMENT '主题：light / dark / auto',
    `language`               VARCHAR(10)   DEFAULT 'zh-CN' COMMENT '语言偏好',
    `collapsed_sidebar`      TINYINT(1)    DEFAULT 0 COMMENT '侧边栏是否收起',
    `data_sharing`           TINYINT(1)    DEFAULT 0 COMMENT '数据共享开关',
    `public_records`         TINYINT(1)    DEFAULT 0 COMMENT '饮食记录公开开关',
    `avatar_url`             VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
    `role`                   VARCHAR(20)   NOT NULL DEFAULT 'user' COMMENT '角色：user / admin',
    `status`                 TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1正常 / 0禁用',
    `created_at`             DATETIME      NOT NULL COMMENT '注册时间',
    `updated_at`             DATETIME      NOT NULL COMMENT '更新时间',
    `deleted`                TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 / 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 第二部分：食物数据（4 张）
-- ============================================================

-- ------------------------------------------------------------
-- 2. 食品分类表 (food_category)
--    树形结构，parent_id 指向自身实现多级分类。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `food_category` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `parent_id`  BIGINT       DEFAULT 0 COMMENT '父分类ID，0为一级分类',
    `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
    `icon`       VARCHAR(100) DEFAULT NULL COMMENT '分类图标URL',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 / 0禁用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食品分类表';

-- ------------------------------------------------------------
-- 3. 食物表 (food) —— 公共食物库
--    存放系统食物和审核通过的用户提交食物。
--    food_type：ingredient=基础食材 / dish=成品菜肴（行业平均估算值）。
--    营养为每 100g 的值，unit_name/unit_weight 支持按个数/份数记录。
-- ------------------------------------------------------------
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
    `unit_name`     VARCHAR(20)   DEFAULT NULL COMMENT '常用计量单位名称，如 个/份/盒（为空表示只能按克数记录）',
    `unit_weight`   DECIMAL(8,2)  DEFAULT NULL COMMENT '每个/每份约多少克，配合 unit_name 做按个数换算',
    `food_type`     VARCHAR(20)   NOT NULL DEFAULT 'ingredient' COMMENT '食物类型：ingredient基础食材 / dish成品菜肴',
    `source`        VARCHAR(20)   NOT NULL DEFAULT 'system' COMMENT '来源：system系统 / user用户提交',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'approved' COMMENT '审核状态：pending待审 / approved通过 / rejected拒绝',
    `created_by`    BIGINT        DEFAULT NULL COMMENT '提交用户ID',
    `created_at`    DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物表（公共食物库）';

-- ------------------------------------------------------------
-- 4. 用户自定义食物表 (user_custom_food)
--    与公共食物库物理隔离，用户自己填写的食物仅创建者可见。
--    is_recipe：是否为组合食谱（由多个基础食材组合累加营养）。
-- ------------------------------------------------------------
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
    `unit_name`     VARCHAR(20)   DEFAULT NULL COMMENT '常用计量单位名称，如 个/份（为空表示只能按克数记录）',
    `unit_weight`   DECIMAL(8,2)  DEFAULT NULL COMMENT '每个/每份约多少克',
    `is_recipe`     TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否为组合食谱（由食材组合累加营养）',
    `created_at`    DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_user_name` (`user_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义食物表（私有，不进公共食物库）';

-- ------------------------------------------------------------
-- 5. 用户自定义食谱明细表 (user_custom_food_item)
--    组合食谱由若干基础食材组成，本表记录食材组成明细。
--    营养值为按克数换算后的快照，保证历史数据不受 food 表变更影响。
-- ------------------------------------------------------------
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

-- ============================================================
-- 第三部分：饮食记录（1 张）
-- ============================================================

-- ------------------------------------------------------------
-- 6. 饮食记录表 (diet_record)
--    记录用户每天每餐的食物摄入。
--    food_source 区分来源：system=公共食物库 / user=用户自定义食物。
--    营养值为记录时的快照。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `diet_record` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `food_id`       BIGINT        NOT NULL COMMENT '食物ID（food_source=system→food.id，=user→user_custom_food.id）',
    `food_source`   VARCHAR(20)   NOT NULL DEFAULT 'system' COMMENT '食物来源：system公共食物库 / user用户自定义食物',
    `meal_type`     VARCHAR(20)   NOT NULL COMMENT '餐次：breakfast / lunch / dinner / snack',
    `amount`        DECIMAL(8,2)  NOT NULL COMMENT '食用份量(克)',
    `calories`      DECIMAL(8,2)  NOT NULL COMMENT '本条记录热量(kcal)',
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
-- 第四部分：食谱模块（4 张）
-- ============================================================

-- ------------------------------------------------------------
-- 7. 食谱主表 (meal_plan)
--    存储用户创建的膳食方案，包含目标、周期、餐次配置、
--    执行状态、发布状态、来源溯源等完整信息。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meal_plan` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`             BIGINT        NOT NULL COMMENT '创建者用户ID',
    `name`                VARCHAR(100)  NOT NULL COMMENT '食谱名称',
    `goal`                VARCHAR(20)   NOT NULL COMMENT '目标：lose减脂 / maintain维持 / gain增重',
    `status`              VARCHAR(20)   NOT NULL DEFAULT 'idle' COMMENT '执行状态：idle空闲 / active执行中 / paused暂停 / archived归档',
    `publish_status`      VARCHAR(20)   NOT NULL DEFAULT 'none' COMMENT '发布状态：none未发布 / pending待审核 / approved已上架 / rejected已拒绝',
    `days`                INT           NOT NULL COMMENT '周期天数',
    `meals`               VARCHAR(100)  NOT NULL COMMENT '每日餐次配置，逗号分隔',
    `daily_calories`      DECIMAL(8,2)  DEFAULT NULL COMMENT '每日热量目标(kcal)',
    `preferences`         VARCHAR(200)  DEFAULT NULL COMMENT '饮食偏好',
    `dislikes`            VARCHAR(500)  DEFAULT NULL COMMENT '忌口',
    `extra_requirement`   VARCHAR(500)  DEFAULT NULL COMMENT '额外需求',
    `summary`             VARCHAR(500)  DEFAULT NULL COMMENT '食谱摘要',
    `description`         TEXT          DEFAULT NULL COMMENT '食谱说明/心得',
    `cover_url`           VARCHAR(255)  DEFAULT NULL COMMENT '食谱封面图',
    `tags`                VARCHAR(255)  DEFAULT NULL COMMENT '标签，逗号分隔',
    `difficulty`          VARCHAR(20)   DEFAULT NULL COMMENT '难度等级',
    `expected_loss`       VARCHAR(50)   DEFAULT NULL COMMENT '预计减重效果',
    `origin`              VARCHAR(20)   NOT NULL DEFAULT 'original' COMMENT '来源类型：original原创 / copied应用自他人',
    `source_plan_id`      BIGINT        DEFAULT NULL COMMENT '来源食谱ID（从广场保存而来）',
    `source_author_id`    BIGINT        DEFAULT NULL COMMENT '来源食谱原作者用户ID',
    `source_author_name`  VARCHAR(100)  DEFAULT NULL COMMENT '来源原作者昵称快照',
    `usage_count`         INT           NOT NULL DEFAULT 0 COMMENT '被保存/应用次数',
    `favorite_count`      INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
    `start_date`          DATE          DEFAULT NULL COMMENT '开始执行日期，未执行时为NULL',
    `paused_at_day`       INT           DEFAULT NULL COMMENT '暂停时处在第几天，恢复执行时从这天继续',
    `published_at`        DATETIME      DEFAULT NULL COMMENT '上架时间',
    `reject_reason`       VARCHAR(500)  DEFAULT NULL COMMENT '驳回原因',
    `deleted`             TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记：0正常 / 1已删除',
    `created_at`          DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`          DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_mp_user` (`user_id`),
    KEY `idx_mp_status` (`status`),
    KEY `idx_mp_publish` (`publish_status`),
    KEY `idx_mp_square_hot` (`publish_status`, `favorite_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱主表';

-- ------------------------------------------------------------
-- 8. 食谱明细表 (meal_plan_item)
--    记录食谱中每天每餐的具体食物及用量。
--    营养值为创建时的快照，保证历史食谱不受 food 表变更影响。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meal_plan_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id`       BIGINT        NOT NULL COMMENT '所属食谱ID',
    `day_index`     INT           NOT NULL COMMENT '第几天（从1开始）',
    `meal_type`     VARCHAR(20)   NOT NULL COMMENT '餐次：breakfast / lunch / dinner / snack',
    `food_id`       BIGINT        DEFAULT NULL COMMENT '食物ID',
    `food_name`     VARCHAR(100)  NOT NULL COMMENT '食物名快照',
    `food_source`   VARCHAR(20)   NOT NULL DEFAULT 'system' COMMENT '食物来源：system公共库 / user自定义',
    `amount`        DECIMAL(8,2)  NOT NULL COMMENT '用量(克)',
    `calories`      DECIMAL(8,2)  NOT NULL COMMENT '热量快照(kcal)',
    `protein`       DECIMAL(8,2)  DEFAULT NULL COMMENT '蛋白质快照(g)',
    `carbohydrate`  DECIMAL(8,2)  DEFAULT NULL COMMENT '碳水快照(g)',
    `fat`           DECIMAL(8,2)  DEFAULT NULL COMMENT '脂肪快照(g)',
    `unit_name`     VARCHAR(20)   DEFAULT NULL COMMENT '计量单位快照',
    `unit_weight`   DECIMAL(8,2)  DEFAULT NULL COMMENT '单位克重快照',
    `image_url`     VARCHAR(255)  DEFAULT NULL COMMENT '食物图片快照',
    `sort_order`    INT           DEFAULT NULL COMMENT '排序序号',
    `created_at`    DATETIME      NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_mpi_plan` (`plan_id`),
    KEY `idx_mpi_plan_day` (`plan_id`, `day_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱明细表';

-- ------------------------------------------------------------
-- 9. 食谱审核记录表 (meal_plan_review)
--    记录管理员对发布食谱的审核操作历史。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meal_plan_review` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id`       BIGINT       NOT NULL COMMENT '食谱ID',
    `reviewer_id`   BIGINT       DEFAULT NULL COMMENT '审核人用户ID',
    `reviewer_name` VARCHAR(50)  DEFAULT NULL COMMENT '审核人姓名',
    `result`        VARCHAR(20)  NOT NULL COMMENT '审核结果：approved通过 / rejected拒绝',
    `reason`        VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `reviewed_at`   DATETIME     NOT NULL COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_mpr_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱审核记录表';

-- ------------------------------------------------------------
-- 10. 食谱收藏表 (meal_plan_favorite)
--     用户收藏食谱的 M:N 关联表，user_id + plan_id 联合唯一。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meal_plan_favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
    `plan_id`    BIGINT   NOT NULL COMMENT '食谱ID',
    `created_at` DATETIME NOT NULL COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_meal_plan_favorite` (`user_id`, `plan_id`),
    KEY `idx_mpf_user` (`user_id`),
    KEY `idx_mpf_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱收藏表';

-- ============================================================
-- 第五部分：健康记录（1 张）
-- ============================================================

-- ------------------------------------------------------------
-- 11. 体重记录表 (weight_record)
--     记录用户每日体重和体脂率，每人每天只能有一条记录。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `weight_record` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT        NOT NULL COMMENT '用户ID',
    `weight_kg`        DECIMAL(5,1)  NOT NULL COMMENT '体重(kg)',
    `body_fat_percent` DECIMAL(5,1)  DEFAULT NULL COMMENT '体脂率(%)',
    `record_date`      DATE          NOT NULL COMMENT '记录日期',
    `remark`           VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    `created_at`       DATETIME      NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_weight_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体重记录表';

-- ============================================================
-- 第六部分：AI 对话（3 张）
-- ============================================================

-- ------------------------------------------------------------
-- 12. AI对话会话表 (ai_chat_session)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `title`      VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
    `created_at` DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- ------------------------------------------------------------
-- 13. AI对话消息表 (ai_chat_message)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id`       BIGINT       NOT NULL COMMENT '会话ID',
    `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
    `role`             VARCHAR(20)  NOT NULL COMMENT '角色：user / assistant / system',
    `content`          TEXT         NOT NULL COMMENT '消息内容',
    `context_snapshot` TEXT         DEFAULT NULL COMMENT '发送时的用户上下文快照',
    `created_at`       DATETIME     NOT NULL COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_msg_session` (`session_id`),
    KEY `idx_msg_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';

-- ------------------------------------------------------------
-- 14. AI生成记录表 (ai_generation_log)
--     记录每次 AI 调用的输入、输出、模型、Token消耗、异常标记。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `type`           VARCHAR(30)  NOT NULL COMMENT '类型：chat对话 / diet_plan膳食方案 / nutrition_analysis营养分析',
    `input_summary`  TEXT         DEFAULT NULL COMMENT '输入摘要',
    `output_content` TEXT         DEFAULT NULL COMMENT 'AI生成内容',
    `model_name`     VARCHAR(50)  DEFAULT NULL COMMENT '使用的模型名称',
    `tokens_used`    INT          DEFAULT NULL COMMENT '消耗Token数',
    `is_abnormal`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否异常：0正常 / 1异常',
    `created_at`     DATETIME     NOT NULL COMMENT '生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_ailog_user` (`user_id`),
    KEY `idx_ailog_type` (`type`),
    KEY `idx_ailog_abnormal` (`is_abnormal`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成记录表';

-- ============================================================
-- 第七部分：系统配置（2 张）
-- ============================================================

-- ------------------------------------------------------------
-- 15. 营养标准配置表 (nutrition_standard)
--     按性别和年龄段提供推荐每日热量、蛋白质、碳水、脂肪摄入量。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `nutrition_standard` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `gender`        TINYINT      NOT NULL COMMENT '性别：1男 / 2女',
    `age_min`       INT          NOT NULL COMMENT '年龄下限',
    `age_max`       INT          NOT NULL COMMENT '年龄上限',
    `calories_kcal` INT          NOT NULL COMMENT '推荐热量(kcal)',
    `protein_g`     DECIMAL(8,2) DEFAULT NULL COMMENT '推荐蛋白质(g)',
    `carb_g`        DECIMAL(8,2) DEFAULT NULL COMMENT '推荐碳水(g)',
    `fat_g`         DECIMAL(8,2) DEFAULT NULL COMMENT '推荐脂肪(g)',
    PRIMARY KEY (`id`),
    KEY `idx_ns_gender_age` (`gender`, `age_min`, `age_max`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营养标准配置表';

-- ------------------------------------------------------------
-- 16. 可配置选项表 (profile_option)
--     存储下拉选项的配置数据（活动水平、疾病选项等），
--     通过 option_type 字段分组。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `profile_option` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `option_type` VARCHAR(30)  NOT NULL COMMENT '选项分组类型',
    `option_code` VARCHAR(50)  NOT NULL COMMENT '选项编码',
    `option_label` VARCHAR(100) NOT NULL COMMENT '选项显示文字',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 / 0禁用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可配置字典选项表';

-- ============================================================
-- 初始数据
-- ============================================================

-- ------------------------------------------------------------
-- 管理员账号（密码：admin123，明文存储）
-- ------------------------------------------------------------
INSERT INTO `user` (`username`, `email`, `password_hash`, `real_name`, `gender`, `birth_date`, `height`, `weight`, `activity_level`, `diet_goal`, `role`, `status`, `created_at`, `updated_at`, `deleted`)
VALUES ('admin', 'admin@dietai.com', 'admin123', '系统管理员', 1, '1990-01-01', 175.0, 70.0, 3, 'maintain', 'admin', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `id`=`id`;

-- ------------------------------------------------------------
-- 食品分类（11 个一级分类）
-- ------------------------------------------------------------
INSERT INTO `food_category` (`name`, `parent_id`, `sort_order`, `icon`, `status`) VALUES
('主食',   0, 1,  'rice',    1),
('肉类',   0, 2,  'meat',    1),
('蔬菜',   0, 3,  'vegetable', 1),
('水果',   0, 4,  'fruit',   1),
('奶制品', 0, 5,  'milk',    1),
('豆制品', 0, 6,  'bean',    1),
('零食',   0, 7,  'snack',   1),
('饮品',   0, 8,  'drink',   1),
('海鲜',   0, 9,  'seafood', 1),
('蛋类',   0, 10, 'egg',     1),
('成品菜肴',0, 11, 'dish',    1);

-- ------------------------------------------------------------
-- 基础食材数据（47 种，food_type=ingredient）
-- ------------------------------------------------------------
INSERT INTO `food` (`name`, `category_id`, `calories`, `protein`, `carbohydrate`, `fat`, `fiber`, `image_url`, `source`, `status`, `created_by`, `created_at`, `updated_at`) VALUES
('白米饭', 1, 116.00, 2.60, 25.90, 0.30, 0.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('馒头', 1, 221.00, 7.00, 47.00, 1.10, 1.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('面条(煮)', 1, 110.00, 3.50, 22.80, 0.40, 0.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('糙米饭', 1, 111.00, 2.60, 23.50, 0.90, 1.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('燕麦片', 1, 377.00, 13.50, 66.30, 6.70, 5.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('全麦面包', 1, 246.00, 8.50, 44.30, 3.50, 6.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('红薯', 1, 99.00, 1.10, 23.10, 0.20, 1.60, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('玉米', 1, 112.00, 4.00, 22.80, 1.20, 2.90, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡胸肉', 2, 133.00, 31.00, 0.00, 3.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('猪瘦肉', 2, 143.00, 20.30, 1.50, 6.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('牛肉', 2, 125.00, 19.90, 0.00, 4.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡腿', 2, 181.00, 16.00, 0.00, 13.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡翅', 2, 194.00, 17.40, 0.00, 13.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('猪排骨', 2, 264.00, 18.30, 0.00, 20.40, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('羊肉', 2, 203.00, 19.00, 0.00, 14.10, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('番茄', 3, 15.00, 0.90, 3.30, 0.20, 0.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('西兰花', 3, 36.00, 4.10, 4.30, 0.60, 3.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('菠菜', 3, 28.00, 2.90, 3.60, 0.40, 2.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('胡萝卜', 3, 37.00, 1.00, 8.10, 0.20, 2.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('黄瓜', 3, 16.00, 0.80, 2.90, 0.20, 0.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('生菜', 3, 13.00, 1.30, 1.30, 0.30, 0.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('白菜', 3, 18.00, 1.50, 2.40, 0.20, 0.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('茄子', 3, 23.00, 1.10, 4.60, 0.20, 1.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('青椒', 3, 22.00, 1.00, 4.70, 0.20, 1.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('苹果', 4, 53.00, 0.20, 13.70, 0.20, 2.10, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('香蕉', 4, 93.00, 1.40, 22.00, 0.20, 1.20, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('橙子', 4, 48.00, 0.80, 11.10, 0.20, 0.60, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('葡萄', 4, 44.00, 0.50, 10.30, 0.20, 0.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('西瓜', 4, 31.00, 0.50, 6.80, 0.10, 0.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('草莓', 4, 32.00, 1.00, 7.10, 0.20, 1.10, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('蓝莓', 4, 57.00, 0.70, 14.50, 0.30, 2.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('牛奶', 5, 54.00, 3.00, 3.40, 3.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('酸奶', 5, 72.00, 2.50, 9.30, 2.70, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('豆腐', 6, 82.00, 8.10, 4.20, 3.70, 0.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('豆浆', 6, 31.00, 3.00, 1.20, 1.60, 0.20, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('豆腐干', 6, 140.00, 15.80, 4.20, 5.90, 0.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('花生', 7, 563.00, 24.80, 13.90, 44.40, 5.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('核桃', 7, 627.00, 14.90, 9.60, 58.80, 9.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('绿茶', 8, 1.00, 0.00, 0.00, 0.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('黑咖啡', 8, 1.00, 0.10, 0.00, 0.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('清蒸鱼', 9, 109.00, 19.60, 0.00, 3.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('虾', 9, 87.00, 18.60, 0.80, 0.80, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('三文鱼', 9, 139.00, 21.60, 0.00, 5.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鳕鱼', 9, 88.00, 20.40, 0.00, 0.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡蛋', 10, 144.00, 13.30, 1.50, 8.80, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸭蛋', 10, 180.00, 12.60, 0.50, 13.00, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鹌鹑蛋', 10, 160.00, 12.80, 2.10, 11.10, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW());

-- ------------------------------------------------------------
-- 成品菜肴数据（24 种，food_type=dish，行业平均估算值，仅供参考）
-- 营养为每 100g 估算值，unit_name/unit_weight 表示一份约多少克
-- ------------------------------------------------------------
INSERT INTO `food` (`name`, `category_id`, `calories`, `protein`, `carbohydrate`, `fat`, `fiber`, `image_url`, `unit_name`, `unit_weight`, `food_type`, `source`, `status`, `created_by`, `created_at`, `updated_at`) VALUES
('蛋炒饭',       11, 150.00, 4.50,  22.00, 5.00,  0.80, NULL, '份', 300.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('扬州炒饭',     11, 168.00, 5.20,  24.00, 6.20,  1.00, NULL, '份', 320.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('牛肉炒饭',     11, 172.00, 6.00,  23.50, 6.50,  0.80, NULL, '份', 320.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('黄焖鸡米饭',   11, 155.00, 7.50,  18.00, 6.80,  1.20, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('红烧肉盖饭',   11, 210.00, 8.00,  22.00, 12.00, 0.60, NULL, '份', 400.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('鱼香肉丝盖饭', 11, 185.00, 6.80,  21.00, 9.50,  0.80, NULL, '份', 400.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('宫保鸡丁',     11, 190.00, 9.50,  14.00, 11.00, 0.90, NULL, '份', 350.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('麻辣烫',       11, 120.00, 6.00,  12.00, 6.50,  2.00, NULL, '份', 500.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('牛肉面',       11, 130.00, 6.50,  20.00, 3.80,  1.00, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('炸酱面',       11, 175.00, 6.00,  26.00, 6.00,  1.20, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('螺蛳粉',       11, 115.00, 4.50,  18.00, 4.00,  1.00, NULL, '份', 500.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('兰州拉面',     11, 125.00, 5.50,  21.00, 3.50,  1.00, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('奶茶',         11, 65.00,  1.20,  11.00, 2.50,  0.00, NULL, '杯', 500.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('炸鸡',         11, 250.00, 18.00, 12.00, 15.00, 0.50, NULL, '份', 300.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('包子',         11, 220.00, 8.00,  38.00, 6.00,  1.50, NULL, '个', 80.00,  'dish', 'system', 'approved', NULL, NOW(), NOW()),
('煎饺',         11, 210.00, 8.50,  25.00, 9.00,  1.00, NULL, '个', 35.00,  'dish', 'system', 'approved', NULL, NOW(), NOW()),
('汉堡',         11, 280.00, 13.00, 30.00, 13.00, 1.50, NULL, '个', 200.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('披萨',         11, 265.00, 11.00, 32.00, 11.00, 1.50, NULL, '份', 250.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('薯条',         11, 310.00, 3.50,  40.00, 15.00, 3.00, NULL, '份', 150.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('蛋挞',         11, 320.00, 5.50,  35.00, 18.00, 0.50, NULL, '个', 50.00,  'dish', 'system', 'approved', NULL, NOW(), NOW()),
('饺子',         11, 200.00, 8.00,  28.00, 8.00,  1.00, NULL, '个', 30.00,  'dish', 'system', 'approved', NULL, NOW(), NOW()),
('馄饨',         11, 160.00, 7.00,  24.00, 5.00,  0.80, NULL, '份', 250.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('盖浇饭',       11, 165.00, 7.00,  22.00, 6.50,  1.00, NULL, '份', 400.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('蛋花汤',       11, 30.00,  2.00,  2.50,  1.50,  0.20, NULL, '份', 300.00, 'dish', 'system', 'approved', NULL, NOW(), NOW());

-- ------------------------------------------------------------
-- 营养标准参考值（按性别 + 年龄段）
-- ------------------------------------------------------------
INSERT INTO `nutrition_standard` (`gender`, `age_min`, `age_max`, `calories_kcal`, `protein_g`, `carb_g`, `fat_g`) VALUES
(1, 18, 30, 2600, 65.00, 325.00, 72.00),
(1, 31, 50, 2400, 60.00, 300.00, 67.00),
(1, 51, 70, 2200, 55.00, 275.00, 61.00),
(2, 18, 30, 2100, 55.00, 263.00, 58.00),
(2, 31, 50, 1900, 50.00, 238.00, 53.00),
(2, 51, 70, 1700, 45.00, 213.00, 47.00);

-- ============================================================
-- SQL 文件结束
-- 总计：16 张表
-- 初始化数据：1 管理员 + 11 分类 + 47 基础食材 + 24 成品菜肴 + 6 条营养标准
-- ============================================================