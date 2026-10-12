CREATE DATABASE IF NOT EXISTS diet_ai_planner DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE diet_ai_planner;

-- ============================================================
-- 1. 用户表 (user)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `username`       VARCHAR(50)   NOT NULL COMMENT '用户名，唯一',
    `email`          VARCHAR(100)  DEFAULT NULL COMMENT '邮箱，唯一',
    `password_hash`  VARCHAR(100)  NOT NULL COMMENT '登录密码（明文存储，教学演示用）',
    `real_name`      VARCHAR(50)   DEFAULT NULL COMMENT '真实姓名',
    `gender`         TINYINT       DEFAULT NULL COMMENT '性别：0未知/1男/2女',
    `birth_date`     DATE          DEFAULT NULL COMMENT '出生日期',
    `height`         DECIMAL(5,1)  DEFAULT NULL COMMENT '身高(cm)',
    `weight`         DECIMAL(5,1)  DEFAULT NULL COMMENT '体重(kg)',
    `target_weight_kg`         DECIMAL(5,1)  DEFAULT NULL COMMENT '目标体重(kg)，减重/增重时设定后据此算热量缺口',
    `target_body_fat_percent`  DECIMAL(5,1)  DEFAULT NULL COMMENT '目标体脂率(%)，可选',
    `target_deadline`          DATE          DEFAULT NULL COMMENT '期望达成目标日期，用于估算还需多久并提示是否过快',
    `activity_level` TINYINT       DEFAULT NULL COMMENT '活动水平1-5',
    `diet_goal`      VARCHAR(20)   DEFAULT NULL COMMENT '减脂/维持/增肌/健康管理',
    `diet_preference` VARCHAR(200) DEFAULT NULL COMMENT '饮食偏好，如：清淡、素食、低糖等',
    `allergy_note`   VARCHAR(200)  DEFAULT NULL COMMENT '忌口食物，多个以「、」分隔，取值来自 profile_option 表 allergy 分组',
    `disease`        VARCHAR(200)  DEFAULT NULL COMMENT '慢性疾病，多个以「、」分隔，取值来自 profile_option 表 disease 分组',
    `medication`     VARCHAR(500)  DEFAULT NULL COMMENT '用药情况，自由文本',
    `target_calories`   DECIMAL(7,2) DEFAULT NULL COMMENT '自定义每日热量目标kcal',
    `target_protein`    DECIMAL(7,2) DEFAULT NULL COMMENT '自定义每日蛋白质目标g',
    `target_carbohydrate` DECIMAL(7,2) DEFAULT NULL COMMENT '自定义每日碳水目标g',
    `target_fat`        DECIMAL(7,2) DEFAULT NULL COMMENT '自定义每日脂肪目标g',
    `diet_reminder`     TINYINT(1)   DEFAULT 1 COMMENT '饮食提醒开关',
    `reminder_time`     VARCHAR(5)   DEFAULT '08:00' COMMENT '提醒时间HH:mm',
    `goal_reminder`     TINYINT(1)   DEFAULT 1 COMMENT '目标提醒开关',
    `ai_suggestion`     TINYINT(1)   DEFAULT 1 COMMENT 'AI建议推送开关',
    `theme`             VARCHAR(10)  DEFAULT 'light' COMMENT '主题: light/dark/auto',
    `language`          VARCHAR(10)  DEFAULT 'zh-CN' COMMENT '语言',
    `collapsed_sidebar` TINYINT(1)   DEFAULT 0 COMMENT '侧边栏收起',
    `data_sharing`      TINYINT(1)   DEFAULT 0 COMMENT '数据共享开关',
    `public_records`    TINYINT(1)   DEFAULT 0 COMMENT '饮食记录公开开关',
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
-- 3. 食物表 (food) —— 公共食物库
-- 只存放系统食物（source='system'），由管理员维护。
-- 用户自己填写的自定义食物不写入本表，见 user_custom_food 表。
-- food_type 区分基础食材(ingredient)与成品菜肴(dish)。
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
    `unit_name`     VARCHAR(20)   DEFAULT NULL COMMENT '常用计量单位名称，如 个/份/盒（为空表示只能按克数记录）',
    `unit_weight`   DECIMAL(8,2)  DEFAULT NULL COMMENT '每个/每份约多少克，配合 unit_name 做按个数换算',
    `food_type`     VARCHAR(20)   NOT NULL DEFAULT 'ingredient' COMMENT '食物类型：ingredient基础食材/dish成品菜肴（行业平均估算值）',
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
    `food_id`       BIGINT        NOT NULL COMMENT '食物ID：food_source=system 时指向 food.id，=user 时指向 user_custom_food.id',
    `food_source`   VARCHAR(20)   NOT NULL DEFAULT 'system' COMMENT '食物来源：system公共食物库/user用户自定义食物',
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
-- 4.1 用户自定义食物表 (user_custom_food)
-- 与公共食物库（food 表）物理隔离：用户自己填写的食物只存在这里，
-- 仅创建者本人可见可用，不会出现在食物库、食物搜索与食物推荐中。
-- ============================================================
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

-- ============================================================
-- 4.2 用户自定义食谱组成明细表 (user_custom_food_item)
-- 组合食谱由若干基础食材组成，本表记录「哪个食谱 → 哪种食材 → 多少克」，
-- 营养值为按克数换算后的快照。
-- ============================================================
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
-- 9. 膳食方案/食谱表 (meal_plan)
-- 用户「打算吃什么」的 N 天计划，与 diet_record（实际吃了什么）不同。
-- 三种正交状态：
--   status         执行状态：idle(未执行)/active(执行中)/paused(暂停)/archived(历史)
--   publish_status 发布状态：none/pending/approved/rejected
--   origin         来源类型：original(原创)/copied(应用自他人公开食谱)
-- copied 食谱仅可替换/增删食材，不可改基础信息、不可二次发布，保护原创版权。
-- ============================================================
CREATE TABLE IF NOT EXISTS `meal_plan` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`             BIGINT        NOT NULL COMMENT '归属用户ID',
    `status`              VARCHAR(20)   NOT NULL DEFAULT 'idle' COMMENT '执行状态：idle/active/paused/archived',
    `name`                VARCHAR(100)  NOT NULL COMMENT '食谱名，如「7天健康减脂计划」',
    `goal`                VARCHAR(20)   NOT NULL COMMENT '膳食目标：lose减脂/gain增重/maintain维持',
    `days`                INT           NOT NULL COMMENT '方案周期（天）：3/7',
    `meals`               VARCHAR(100)  NOT NULL COMMENT '每日餐次，逗号分隔：breakfast,lunch,dinner,snack',
    `daily_calories`      DECIMAL(8,2)  DEFAULT NULL COMMENT '每日目标热量(kcal)，可空——实际热量按食物库累计',
    `preferences`         VARCHAR(200)  DEFAULT NULL COMMENT '饮食偏好标签，以「、」分隔',
    `dislikes`            VARCHAR(500)  DEFAULT NULL COMMENT '不喜欢/不能吃的食物，以「、」分隔',
    `extra_requirement`   VARCHAR(500)  DEFAULT NULL COMMENT '其他要求（自由文本）',
    `summary`             VARCHAR(500)  DEFAULT NULL COMMENT '一句话概述，卡片上展示',
    `description`         VARCHAR(1000) DEFAULT NULL COMMENT '发布时的心得/整体思路，详情弹窗里可展开',
    `publish_status`      VARCHAR(20)   NOT NULL DEFAULT 'none' COMMENT '发布状态：none/pending/approved/rejected',
    `cover_url`           VARCHAR(255)  DEFAULT NULL COMMENT '封面图地址（相对路径）',
    `tags`                VARCHAR(200)  DEFAULT NULL COMMENT '展示标签，以「、」分隔',
    `difficulty`          VARCHAR(20)   DEFAULT NULL COMMENT '难度：入门/进阶',
    `expected_loss`       VARCHAR(30)   DEFAULT NULL COMMENT '预期减重，如「-0.5~-1」或「-2kg」',
    `source_plan_id`      BIGINT        DEFAULT NULL COMMENT '从广场复制而来时，指回原食谱ID',
    `origin`              VARCHAR(20)   NOT NULL DEFAULT 'original' COMMENT '来源类型：original原创/copied应用自他人公开食谱',
    `source_author_id`    BIGINT        DEFAULT NULL COMMENT '来源食谱原作者用户ID（应用他人食谱时记录，用于溯源）',
    `source_author_name`  VARCHAR(100)  DEFAULT NULL COMMENT '来源食谱原作者昵称快照（脱敏，如「用户**9」）',
    `usage_count`         INT           NOT NULL DEFAULT 0 COMMENT '被「保存为我的食谱」的次数',
    `favorite_count`      INT           NOT NULL DEFAULT 0 COMMENT '被收藏次数',
    `published_at`        DATETIME      DEFAULT NULL COMMENT '上架时间',
    `reject_reason`       VARCHAR(500)  DEFAULT NULL COMMENT '审核不通过的原因',
    `start_date`          DATE          DEFAULT NULL COMMENT '开始执行日期，未执行时为NULL',
    `created_at`          DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`          DATETIME      NOT NULL COMMENT '更新时间',
    `deleted`             TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除标记：0正常/1删除',
    `paused_at_day`       INT           DEFAULT NULL COMMENT '暂停时第几天（恢复时从这天继续）',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_publish` (`publish_status`),
    KEY `idx_mp_square_hot` (`publish_status`, `favorite_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='膳食方案/食谱表';

-- ============================================================
-- 9.1 膳食方案食物条目表 (meal_plan_item)
-- 方案里的一条食物（第几天 · 哪一餐 · 什么食物 · 多少克）。
-- 营养值是按克数换算后的快照，食物名/单位也快照下来，
-- 管理员后续改名或删掉食物，用户已采用的方案不会显示空白。
-- food_source=ai 时 food_id 为空，营养值取 AI 估算值。
-- ============================================================
CREATE TABLE IF NOT EXISTS `meal_plan_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id`       BIGINT        NOT NULL COMMENT '所属方案ID（meal_plan.id）',
    `day_index`     INT           NOT NULL COMMENT '第几天，从1开始',
    `meal_type`     VARCHAR(20)   NOT NULL COMMENT '餐次：breakfast/lunch/dinner/snack',
    `food_id`       BIGINT        DEFAULT NULL COMMENT '食物库主键；AI估算的食物为空',
    `food_source`   VARCHAR(20)   NOT NULL DEFAULT 'system' COMMENT '食物来源：system食物库/user自定义/ai估算',
    `food_name`     VARCHAR(100)  NOT NULL COMMENT '食物名快照',
    `image_url`     VARCHAR(255)  DEFAULT NULL COMMENT '食物图片快照',
    `amount`        DECIMAL(8,2)  NOT NULL COMMENT '克数',
    `unit_name`     VARCHAR(20)   DEFAULT NULL COMMENT '常用单位名快照（个/份/盒…）',
    `unit_weight`   DECIMAL(8,2)  DEFAULT NULL COMMENT '每个/每份约多少克',
    `calories`      DECIMAL(8,2)  NOT NULL COMMENT '该条目热量快照(kcal)',
    `protein`       DECIMAL(8,2)  DEFAULT NULL COMMENT '该条目蛋白质快照(g)',
    `carbohydrate`  DECIMAL(8,2)  DEFAULT NULL COMMENT '该条目碳水快照(g)',
    `fat`           DECIMAL(8,2)  DEFAULT NULL COMMENT '该条目脂肪快照(g)',
    `sort_order`    INT           DEFAULT 0 COMMENT '同一餐内的展示顺序',
    `created_at`    DATETIME      NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan` (`plan_id`),
    KEY `idx_plan_day` (`plan_id`, `day_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='膳食方案食物条目表';

-- ============================================================
-- 9.2 食谱审核记录表 (meal_plan_review)
-- 每次通过/驳回都落一条，保留审核人、时间、结果与原因，保留完整审核历史。
-- 与 meal_plan.reject_reason 不同，这里不覆盖，可追溯多次审核过程。
-- ============================================================
CREATE TABLE IF NOT EXISTS `meal_plan_review` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id`       BIGINT       NOT NULL COMMENT '所属方案ID（meal_plan.id）',
    `reviewer_id`   BIGINT       DEFAULT NULL COMMENT '审核人用户ID（管理员），系统自动审核时可为空',
    `reviewer_name` VARCHAR(50)  DEFAULT NULL COMMENT '审核人用户名快照，避免管理员改名/删号后显示空白',
    `result`        VARCHAR(20)  NOT NULL COMMENT '审核结果：approved通过/rejected驳回',
    `reason`        VARCHAR(500) DEFAULT NULL COMMENT '审核原因',
    `reviewed_at`   DATETIME     NOT NULL COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_mpr_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱审核记录表';

-- ============================================================
-- 9.3 食谱收藏表 (meal_plan_favorite)
-- 谁收藏了广场上的哪份食谱。只存关系，不存食谱副本——
-- 食谱被作者下架时收藏自然失效，不留下过期快照。
-- ============================================================
CREATE TABLE IF NOT EXISTS `meal_plan_favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT   NOT NULL COMMENT '收藏用户ID',
    `plan_id`    BIGINT   NOT NULL COMMENT '被收藏的方案ID',
    `created_at` DATETIME NOT NULL COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_meal_plan_favorite` (`user_id`, `plan_id`),
    KEY `idx_mpf_user` (`user_id`),
    KEY `idx_mpf_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱收藏表';

-- ============================================================
-- 10. 用户档案选项字典表 (profile_option)
-- 个人中心里的饮食目标、活动水平、饮食偏好、忌口食物、慢性疾病
-- 全部是下拉/多选，管理员可在后台增删改查。
-- code 是存进 user 表的真实值，label 是给用户看的文案。
-- 删除用 status=0 停用而非物理删除，不破坏已有用户数据。
-- ============================================================
CREATE TABLE IF NOT EXISTS `profile_option` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `option_type`   VARCHAR(30)  NOT NULL COMMENT '选项分组：diet_goal/activity_level/diet_preference/allergy/disease',
    `option_code`   VARCHAR(50)  NOT NULL COMMENT '存入user表的值，如 lose/1/清淡。分组内唯一',
    `option_label`  VARCHAR(100) NOT NULL COMMENT '界面展示文案，如 减脂/久坐（几乎不运动）',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
    `status`        INT          NOT NULL DEFAULT 1 COMMENT '1启用/0停用（停用后用户端不再展示，历史用户数据不受影响）',
    `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注，仅后台可见',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`option_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户档案选项字典表';

-- ============================================================
-- 11. 体重/体脂记录表 (weight_record)
-- 每天最多一条的身体数据快照，同一天重复提交会覆盖而非追加。
-- 体脂率可空——很多人家里没有体脂秤，只记体重也是合法用法。
-- ============================================================
CREATE TABLE IF NOT EXISTS `weight_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT       NOT NULL COMMENT '用户ID',
    `record_date`       DATE         NOT NULL COMMENT '记录日期',
    `weight_kg`         DECIMAL(5,1) NOT NULL COMMENT '体重(kg)',
    `body_fat_percent`  DECIMAL(5,1) DEFAULT NULL COMMENT '体脂率(%)，可选',
    `remark`            VARCHAR(200) DEFAULT NULL COMMENT '备注，如"早上空腹""刚运动完"',
    `created_at`        DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_weight_user_date` (`user_id`, `record_date`),
    KEY `idx_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体重/体脂记录表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号 (密码明文存储: admin123)
INSERT INTO `user` (`username`, `email`, `password_hash`, `real_name`, `gender`, `birth_date`, `height`, `weight`, `activity_level`, `diet_goal`, `role`, `status`, `created_at`, `updated_at`, `deleted`)
VALUES ('admin', 'admin@dietai.com', 'admin123', '系统管理员', 1, '1990-01-01', 175.0, 70.0, 3, 'maintain', 'admin', 1, NOW(), NOW(), 0)
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
('蛋类', 0, 10, 'egg', 1),
('成品菜肴', 0, 11, 'dish', 1);

-- 常见食物数据（基础食材）
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

-- 成品菜肴数据（分类11：成品菜肴；food_type=dish；为行业平均估算值，仅供参考）
-- 营养为每100g估算值，unit_name/unit_weight 表示一份约多少克，便于按"份"记录外食
INSERT INTO `food` (`name`, `category_id`, `calories`, `protein`, `carbohydrate`, `fat`, `fiber`, `image_url`, `unit_name`, `unit_weight`, `food_type`, `source`, `status`, `created_by`, `created_at`, `updated_at`) VALUES
('蛋炒饭', 11, 150.00, 4.50, 22.00, 5.00, 0.80, NULL, '份', 300.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('扬州炒饭', 11, 168.00, 5.20, 24.00, 6.20, 1.00, NULL, '份', 320.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('牛肉炒饭', 11, 172.00, 6.00, 23.50, 6.50, 0.80, NULL, '份', 320.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('黄焖鸡米饭', 11, 155.00, 7.50, 18.00, 6.80, 1.20, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('红烧肉盖饭', 11, 210.00, 8.00, 22.00, 12.00, 0.60, NULL, '份', 400.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('鱼香肉丝盖饭', 11, 185.00, 6.80, 21.00, 9.50, 0.80, NULL, '份', 400.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('宫保鸡丁', 11, 190.00, 9.50, 14.00, 11.00, 0.90, NULL, '份', 350.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('麻辣烫', 11, 120.00, 6.00, 12.00, 6.50, 2.00, NULL, '份', 500.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('牛肉面', 11, 130.00, 6.50, 20.00, 3.80, 1.00, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('炸酱面', 11, 175.00, 6.00, 26.00, 6.00, 1.20, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('螺蛳粉', 11, 115.00, 4.50, 18.00, 4.00, 1.00, NULL, '份', 500.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('兰州拉面', 11, 125.00, 5.50, 21.00, 3.50, 1.00, NULL, '份', 450.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('奶茶', 11, 65.00, 1.20, 11.00, 2.50, 0.00, NULL, '杯', 500.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('炸鸡', 11, 250.00, 18.00, 12.00, 15.00, 0.50, NULL, '份', 300.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('包子', 11, 220.00, 8.00, 38.00, 6.00, 1.50, NULL, '个', 80.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('煎饺', 11, 210.00, 8.50, 25.00, 9.00, 1.00, NULL, '个', 35.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('汉堡', 11, 280.00, 13.00, 30.00, 13.00, 1.50, NULL, '个', 200.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('披萨', 11, 265.00, 11.00, 32.00, 11.00, 1.50, NULL, '份', 250.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('薯条', 11, 310.00, 3.50, 40.00, 15.00, 3.00, NULL, '份', 150.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('蛋挞', 11, 320.00, 5.50, 35.00, 18.00, 0.50, NULL, '个', 50.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('饺子', 11, 200.00, 8.00, 28.00, 8.00, 1.00, NULL, '个', 30.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('馄饨', 11, 160.00, 7.00, 24.00, 5.00, 0.80, NULL, '份', 250.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('盖浇饭', 11, 165.00, 7.00, 22.00, 6.50, 1.00, NULL, '份', 400.00, 'dish', 'system', 'approved', NULL, NOW(), NOW()),
('蛋花汤', 11, 30.00, 2.00, 2.50, 1.50, 0.20, NULL, '份', 300.00, 'dish', 'system', 'approved', NULL, NOW(), NOW());

-- 营养标准配置
INSERT INTO `nutrition_standard` (`gender`, `age_min`, `age_max`, `calories_kcal`, `protein_g`, `carb_g`, `fat_g`) VALUES
(1, 18, 30, 2600, 65.00, 325.00, 72.00),
(1, 31, 50, 2400, 60.00, 300.00, 67.00),
(1, 51, 70, 2200, 55.00, 275.00, 61.00),
(2, 18, 30, 2100, 55.00, 263.00, 58.00),
(2, 31, 50, 1900, 50.00, 238.00, 53.00),
(2, 51, 70, 1700, 45.00, 213.00, 47.00);

-- 用户档案选项（饮食目标）
INSERT INTO `profile_option` (`option_type`, `option_code`, `option_label`, `sort_order`, `status`, `remark`) VALUES
('diet_goal', 'lose', '减脂', 1, 1, '低热量高蛋白，缺口 15%~20%'),
('diet_goal', 'maintain', '维持', 2, 1, '保持当前体重，热量持平'),
('diet_goal', 'gain', '增肌', 3, 1, '热量盈余 10%~15%，配合力量训练');

-- 用户档案选项（活动水平）
INSERT INTO `profile_option` (`option_type`, `option_code`, `option_label`, `sort_order`, `status`, `remark`) VALUES
('activity_level', '1', '久坐（几乎不运动）', 1, 1, '久坐办公，基本不锻炼'),
('activity_level', '2', '轻度活动（每周1-3次）', 2, 1, '每周运动 1~3 次'),
('activity_level', '3', '中度活动（每周3-5次）', 3, 1, '每周运动 3~5 次'),
('activity_level', '4', '高度活动（每周6-7次）', 4, 1, '每周运动 6~7 次'),
('activity_level', '5', '极高活动（体力劳动）', 5, 1, '重体力劳动或运动员');

-- 用户档案选项（饮食偏好）
INSERT INTO `profile_option` (`option_type`, `option_code`, `option_label`, `sort_order`, `status`, `remark`) VALUES
('diet_preference', '清淡', '清淡', 1, 1, NULL),
('diet_preference', '中式', '中式', 2, 1, NULL),
('diet_preference', '西式', '西式', 3, 1, NULL),
('diet_preference', '素食', '素食', 4, 1, NULL),
('diet_preference', '低糖', '低糖', 5, 1, NULL),
('diet_preference', '低脂', '低脂', 6, 1, NULL),
('diet_preference', '高蛋白', '高蛋白', 7, 1, NULL),
('diet_preference', '无辣', '无辣', 8, 1, NULL),
('diet_preference', '地中海', '地中海饮食', 9, 1, NULL),
('diet_preference', '生酮', '生酮饮食', 10, 1, NULL);

-- 用户档案选项（忌口食物）
INSERT INTO `profile_option` (`option_type`, `option_code`, `option_label`, `sort_order`, `status`, `remark`) VALUES
('allergy', '海鲜', '海鲜', 1, 1, '虾蟹贝类'),
('allergy', '牛奶', '牛奶', 2, 1, '乳制品'),
('allergy', '鸡蛋', '鸡蛋', 3, 1, NULL),
('allergy', '花生', '花生', 4, 1, NULL),
('allergy', '大豆', '大豆', 5, 1, NULL),
('allergy', '麸质', '麸质', 6, 1, '小麦、大麦等'),
('allergy', '坚果', '坚果', 7, 1, NULL);

-- 用户档案选项（慢性疾病）
INSERT INTO `profile_option` (`option_type`, `option_code`, `option_label`, `sort_order`, `status`, `remark`) VALUES
('disease', '糖尿病', '糖尿病', 1, 1, '需控糖'),
('disease', '高血压', '高血压', 2, 1, '需控盐'),
('disease', '高血脂', '高血脂', 3, 1, '需控脂'),
('disease', '痛风', '痛风', 4, 1, '需控嘌呤'),
('disease', '脂肪肝', '脂肪肝', 5, 1, '需控总热量');
