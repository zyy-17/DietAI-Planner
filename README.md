# 🍽️ DietAI-Planner — AI 个性化膳食规划与饮食健康管理系统

<p align="center">
  <b>基于 Spring Boot + Vue 3 + FastAPI 的全栈智能饮食管理平台</b><br/>
  科学饮食 · 精准营养 · AI 赋能 · 食谱社区
</p>

---

## 📖 目录

- [项目简介](#-项目简介)
- [项目背景](#-项目背景)
- [技术栈](#-技术栈)
- [功能模块](#-功能模块)
- [项目结构](#-项目结构)
- [数据库设计](#-数据库设计)
- [前端设计](#-前端设计)
- [后端 API 接口](#-后端-api-接口)
- [AI 服务详解](#-ai-服务详解)
- [快速开始](#-快速开始)
- [默认账号](#-默认账号)
- [项目截图](#-项目截图)

---

## 📝 项目简介

DietAI-Planner 是一个面向个人用户的 **AI 驱动饮食健康管理平台**，帮助用户：

- 📊 **记录每日饮食**：早餐 / 午餐 / 晚餐 / 加餐，支持一次添加多种食物，支持按个数/份数计量
- 🎯 **自定义营养目标**：热量、蛋白质、碳水、脂肪四项目标自由设定，修改即时同步
- 🤖 **AI 膳食建议**：支持 **云端大模型 API**（豆包/DeepSeek/通义/Kimi/智谱 等，填个 key 就能用）与 **本机 Ollama** 双后端，结合用户画像生成个性化建议，4种套餐方案一键切换
- 📈 **营养分析**：今日营养、趋势图、营养素分析、热量分析、目标完成度、营养报告，六大维度差异化展示
- 🧬 **智能营养评估**：BMI/BMR/TDEE自动计算 + 营养评分 + 状态判定 + 改进建议
- ⚖️ **体重体脂追踪**：每日体重/体脂记录 + 身体目标设定 + 趋势图 + 目标进度 + 安全速率提示
- 🍱 **食物库管理**：三层架构（基础食材 + 成品菜肴 + 用户自定义食谱），内置 70+ 食物，支持搜索、分类、用户添加
- 📋 **食谱/膳食方案**：用户手工编写多天食谱 + AI 智能生成 + 食谱广场 + 发布审核 + 执行闭环（暂停/恢复/结束）+ 来源溯源与版权保护
- 🤖 **AI 智能替换**：食谱里单条食物的 AI 替换候选，AI 不可用时退化为算法排序
- 👤 **个人中心**：身体数据、饮食目标、饮食偏好、忌口设置、活动水平、慢性疾病、用药情况
- ⚙️ **系统设置**：通知提醒、界面主题、隐私控制、密码修改
- 🛡️ **管理后台**：数据看板、用户管理、食物管理、分类管理、饮食记录、营养标准、档案选项、食谱审核、AI 日志监控

---

## 🌍 项目背景

随着健康意识提升，越来越多人关注日常饮食的营养搭配。传统饮食记录 App 存在以下痛点：

| 痛点 | 本项目解决方案 |
|------|---------------|
| 手动查食物营养太麻烦 | 内置食物库 + 搜索，一键添加到今日饮食 |
| 不知道该吃什么 | AI 根据用户画像 + 剩余目标推荐，4种套餐方案换一换 |
| 目标设置不灵活 | 四大营养素目标可自由调整，修改即时同步到所有页面 |
| 记录效率低 | 支持一次选择多种食物批量添加 + 按个数/份数计量 |
| 缺乏长期趋势分析 | 六大维度营养分析 + 智能评估 + 综合报告 + 体重体脂趋势 |
| 各分析页面千篇一律 | 营养素分析/热量分析/目标完成度/营养报告各自独立后端接口+差异化展示 |
| 没有长期规划 | 多天食谱/膳食方案 + 食谱广场社区 + 执行进度追踪 |
| 外食记录困难 | 成品菜肴库（蛋炒饭/黄焖鸡/汉堡等 24 道菜，按"份"记录） |

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 25 | 编程语言 |
| Spring Boot | 4.1.1 | 应用框架 |
| Spring Security | — | 认证授权（JWT 无状态认证） |
| Spring Data JPA | — | ORM 持久层（Hibernate ddl-auto=update） |
| MySQL | 8.0 | 关系型数据库 |
| JWT (jjwt) | 0.12.6 | 无状态认证令牌 |
| Caffeine | — | 本地缓存（食物库等热点数据缓存） |
| Spring Cache | — | 缓存抽象层 |
| HttpClient5 | — | HTTP 客户端 |
| Lombok | — | 减少样板代码 |
| BCrypt | — | 密码加密（兼容历史数据） |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.x | 渐进式前端框架 |
| Vue Router | 4.4.x | 路由管理（嵌套路由 + meta 参数） |
| Pinia | 2.2.x | 状态管理 |
| Element Plus | 2.8.x | UI 组件库（按需自动导入） |
| ECharts | 5.5.x | 数据可视化 |
| Axios | 1.7.x | HTTP 客户端 |
| Day.js | 1.11.x | 日期处理 |
| Marked | 18.x | Markdown 渲染（AI 回复） |
| DOMPurify | 3.4.x | HTML 消毒防 XSS |
| Vite | 5.4.x | 构建工具 |

### AI 服务

| 技术 | 版本 | 说明 |
|------|------|------|
| Python | 3.10+ | 编程语言 |
| FastAPI | 0.135+ | 高性能 Web 框架 |
| Uvicorn | 0.30+ | ASGI 服务器 |
| Pydantic | 2.12+ | 数据校验 |
| HTTPX | 0.27+ | 调用云端大模型 API（OpenAI 兼容，非流式 + SSE 流式） |
| Ollama Python SDK | latest | 调用本地 Ollama 推理服务（可选后端） |
| 云端 API | — | **默认推荐**，任选一家 OpenAI 兼容接口（豆包/DeepSeek/通义/智谱/Kimi） |
| Ollama | — | 可选后端，本地大模型推理（默认 qwen2.5:3b） |

---

## 🧩 功能模块

### 用户端

```
🏠 今日饮食
   ├─ 🏠 今日概览        每日摄入总览、目标进度、快速记录
   ├─ 🍳 早餐            按餐次记录食物（支持多选批量添加、按个数/份数计量）
   ├─ 🥗 午餐
   ├─ 🍗 晚餐
   ├─ 🍎 加餐
   └─ 🤖 AI今日建议      4种套餐方案换一换 + 结构化膳食方案(AI)

📋 饮食记录
   ├─ 📅 历史记录        按日期范围浏览饮食记录
   ├─ 📆 周记录          近7天记录汇总 + 统计摘要
   ├─ 🗓️ 月记录          近30天记录汇总 + 统计摘要
   ├─ 🔍 条件查询        按餐次/食物名/日期范围筛选排序
   └─ 📊 饮食统计        每日热量趋势图 + 三大营养素占比饼图 + 达标天数

📊 营养分析
   ├─ 📊 今日营养        四大营养素摄入详情 + 智能营养评估
   ├─ 📈 营养趋势        近7天/30天热量趋势 + 营养素占比 + AI解读
   ├─ ⚖️ 体重体脂        体重/体脂双折线图 + 身体目标 + 目标进度 + 安全速率提示
   ├─ 🥩 营养素分析      每日营养素三线趋势图 + 供能占比饼图 + 日均vs目标 + 建议
   ├─ 🔥 热量分析        每日热量柱状图vs目标线 + 时段分布饼图 + 达标率 + 偏差
   ├─ 🎯 目标完成度      四项环形进度条 + 综合完成度 + 改进建议
   └─ 📄 营养报告        综合评分 + 各项评价 + 关键发现 + 改善建议 + 供能比例

🤖 AI饮食助手（页面自带三栏，不显示外层左侧导航）
   ├─ 第一栏 会话列表     新建对话 + 历史会话（标题由 AI 自动概括）
   ├─ 第二栏 对话区       首次进入自动欢迎语 + 4 个引导问题；底部常驻快捷操作
   │                      🍱 推荐晚餐 / 📊 分析今日饮食 / 🔄 替换食物 / 🥗 推荐健康食物
   └─ 第三栏 今日营养数据 热量 / 蛋白质 / 碳水 / 脂肪 进度条 + 今日目标

   每次提问都会自动携带当前登录用户的数据：
   · 健康档案（性别/年龄/身高/体重/活动水平）+ 饮食目标 + 饮食偏好 + 忌口 + 慢性疾病
   · 每日营养目标（热量 + 三大营养素）
   · 今日已摄入与「还差多少」
   · 今日饮食明细（按早/午/晚/加餐列出食物名、克重、热量）
   · 近 7 天逐日饮食明细 + 趋势（日均热量/三大营养素）
   · 近 90 天体重体脂趋势 + 身体目标与完成度
   · 算法评分 Top5 候选食物

📋 食谱 / 膳食方案
   ├─ 📝 我的食谱        手动创建 / AI 智能生成，逐天逐餐编辑食物与克数
   │                      ├─ 复制某天到其他天
   │                      ├─ 单条食物替换（手选候选 + AI 智能替换）
   │                      └─ 封面上传、标签、难度、心得
   ├─ ▶️ 执行闭环        开始执行 → 周期进度 + 每日打卡 + 计划vs实际 → 暂停/恢复/结束
   ├─ 🏪 食谱广场        浏览/搜索公开食谱（热门Top10/最新），收藏、应用他人食谱
   └─ 📤 发布审核        提交发布 → 管理员审核 → 上架广场 / 驳回（附原因）

   版权保护：应用他人食谱的副本仅可替换/增删食材，不可改基础信息、不可二次发布，
             同时记录来源溯源（原食谱 id + 原作者 id + 脱敏昵称快照）

🍱 食物库（三层架构）
   ├─ 🍎 基础食材        46+ 常见食材，按克数记录
   ├─ 🍔 成品菜肴        24 道外食菜肴（行业平均估算值，按"份"记录）
   └─ ➕ 自定义食物      用户自建食物（私有，不进公共库）
                        └─ 组合食谱：由基础食材组合，营养自动累加

👤 个人中心
   ├─ 👤 基本资料        姓名/性别/生日
   ├─ 📏 身体数据        身高/体重
   ├─ 🎯 饮食目标        减脂/维持/增肌（选项由后端字典表下发）
   ├─ 🥗 饮食偏好        清淡/素食/低糖等（多选，字典表下发）
   ├─ 🚫 忌口设置        海鲜/牛奶/鸡蛋等（多选，字典表下发）
   ├─ 🏃 活动水平        久坐~极高活动5级
   └─ ❤️ 健康信息        慢性疾病 + 用药情况

⚙️ 系统设置
   ├─ 🔔 通知设置        饮食提醒/提醒时间/目标提醒/AI推送
   ├─ 🎨 界面设置        主题(浅色/深色/跟随系统)/语言/侧边栏
   ├─ 🔐 隐私设置        数据共享/记录公开
   └─ 🔑 修改密码        校验旧密码+新密码确认
```

### 管理端

```
🛡️ 后台管理
   ├─ 📊 数据看板        用户/食物/记录/食谱统计概览 + 趋势图
   ├─ 👥 用户管理        列表/禁用/删除
   ├─ 🍱 食物管理        列表/编辑/审核/删除
   ├─ 📂 分类管理        增删改分类
   ├─ 📋 饮食记录        全站饮食记录查看/删除
   ├─ 📏 营养标准        按性别+年龄段的推荐摄入量增删改
   ├─ 📝 档案选项        饮食目标/活动水平/饮食偏好/忌口/慢性疾病选项增删改
   ├─ 📤 食谱审核        待审核食谱列表/通过/驳回/下架 + 审核历史
   └─ 🤖 AI日志          生成记录/异常标记
```

---

## 📁 项目结构

```
DietAI-Planner/
│
├── src/main/java/com/zyyqq/          # ☕ Spring Boot 后端
│   ├── entity/                        # JPA 实体类（16 张表）
│   │   ├── User.java                  # 用户（含身体目标/忌口/疾病/用药）
│   │   ├── Food.java                  # 食物（基础食材+成品菜肴）
│   │   ├── FoodCategory.java          # 食物分类
│   │   ├── DietRecord.java           # 饮食记录
│   │   ├── UserCustomFood.java       # 用户自定义食物（含组合食谱标记）
│   │   ├── UserCustomFoodItem.java    # 用户自定义食谱组成明细
│   │   ├── MealPlan.java             # 膳食方案/食谱（多天计划+发布+执行+溯源）
│   │   ├── MealPlanItem.java          # 膳食方案食物条目
│   │   ├── MealPlanReview.java       # 食谱审核记录
│   │   ├── MealPlanFavorite.java     # 食谱收藏
│   │   ├── ProfileOption.java        # 用户档案选项字典
│   │   ├── WeightRecord.java         # 体重/体脂记录
│   │   ├── AiChatSession.java        # AI 会话
│   │   ├── AiChatMessage.java        # AI 消息
│   │   ├── AiGenerationLog.java      # AI 生成日志
│   │   └── NutritionStandard.java    # 营养标准配置
│   ├── repository/                    # 数据访问层（16 个 Repository）
│   ├── service/                       # 业务逻辑层
│   │   ├── UserService.java           # 用户服务（BMR/TDEE/目标计算/身体目标）
│   │   ├── DietRecordService.java     # 饮食记录服务
│   │   ├── FoodService.java           # 食物服务
│   │   ├── FoodCategoryService.java   # 分类服务
│   │   ├── UserCustomFoodService.java # 用户自定义食物/食谱服务
│   │   ├── MealPlanService.java       # 膳食方案服务（创建/执行/发布/广场/溯源）
│   │   ├── RecommendationService.java # 食物推荐服务（多因素评分 Top-N）
│   │   ├── FoodScoreCalculator.java   # 食物评分算法（热量40%+蛋白30%+偏好15%+目标15%）
│   │   ├── NutritionAnalysisService.java # 营养分析服务（6个专用分析方法）
│   │   ├── NutritionEvaluationService.java # 营养评估服务
│   │   ├── WeightRecordService.java   # 体重/体脂记录服务
│   │   ├── ProfileOptionService.java   # 档案选项服务
│   │   ├── ProfileOptionSeeder.java   # 档案选项默认数据初始化（幂等）
│   │   ├── AiChatService.java         # AI 对话服务（上下文构建+历史管理）
│   │   └── AdminService.java          # 管理后台服务
│   ├── controller/                    # API 控制器
│   │   ├── AuthController.java        # 认证（注册/登录）
│   │   ├── UserController.java        # 用户（资料/设置/密码）
│   │   ├── TodayDietController.java   # 今日饮食
│   │   ├── FoodController.java        # 食物
│   │   ├── FoodCategoryController.java# 分类
│   │   ├── MealPlanController.java    # 膳食方案（我的食谱/广场/执行/发布）
│   │   ├── RecommendationController.java # 食物推荐
│   │   ├── WeightRecordController.java# 体重/体脂记录与身体目标
│   │   ├── UserOptionController.java  # 档案选项下发
│   │   ├── AiChatController.java       # AI 对话
│   │   ├── NutritionAnalysisController.java # 营养分析
│   │   └── admin/                      # 管理员控制器
│   │       ├── AdminUserController.java
│   │       ├── AdminFoodController.java
│   │       ├── AdminCategoryController.java
│   │       ├── AdminDietRecordController.java
│   │       ├── AdminNutritionStandardController.java
│   │       ├── AdminProfileOptionController.java
│   │       ├── AdminMealPlanController.java
│   │       ├── AdminStatsController.java
│   │       └── AdminAiLogController.java
│   ├── dto/
│   │   ├── request/                   # 请求 DTO（20+ 个）
│   │   └── response/                  # 响应 DTO（25+ 个 VO）
│   ├── config/                        # 配置类
│   │   ├── SecurityConfig.java        # 安全配置（JWT + 密码明文/BCrypt兼容）
│   │   ├── CacheConfig.java           # Caffeine 缓存配置
│   │   ├── RestTemplateConfig.java    # RestTemplate（调 AI 服务）
│   │   └── WebMvcConfig.java          # MVC + 静态资源
│   ├── security/                      # JWT 安全组件
│   └── exception/                     # 全局异常处理
│
├── frontend/                          # 🖥️ Vue 3 前端
│   └── src/
│       ├── views/                     # 页面组件
│       │   ├── Login.vue              # 登录页
│       │   ├── TodayDiet.vue          # 今日概览
│       │   ├── MealDetail.vue         # 餐次详情（早/午/晚/加餐）
│       │   ├── AiSuggest.vue          # AI 今日建议
│       │   ├── DietRecords.vue        # 饮食记录（历史/周/月/搜索）
│       │   ├── DietStats.vue          # 饮食统计
│       │   ├── NutritionAnalysis.vue  # 营养分析（6 种模式）
│       │   ├── BodyMetrics.vue        # 体重体脂追踪
│       │   ├── AiChat.vue             # AI 对话（三栏工作台）
│       │   ├── AiChatHistory.vue       # 历史对话
│       │   ├── MealPlan.vue           # 食谱（我的食谱 + 食谱广场）
│       │   ├── FoodLibrary.vue        # 食物库（三层 Tab）
│       │   ├── Profile.vue            # 个人中心
│       │   ├── Settings.vue           # 系统设置
│       │   └── admin/                 # 管理员页面（9 个）
│       ├── components/                # 可复用组件
│       │   ├── RecipeSheet.vue        # 食谱详情/编辑全屏弹窗
│       │   └── FoodQuantity.vue       # 食物计量（克/个/份切换）
│       ├── layouts/
│       │   ├── MainLayout.vue         # 用户端主布局
│       │   └── AdminLayout.vue        # 管理端布局
│       ├── router/index.js            # 路由配置
│       ├── stores/user.js             # 用户状态（Pinia）
│       └── utils/api.js               # Axios 封装
│
├── ai-service/                        # 🐍 Python AI 服务
│   ├── main.py                        # FastAPI 主程序
│   ├── requirements.txt               # Python 依赖
│   ├── .env.example                   # ⭐ 配置模板
│   ├── api/                           # API 路由层
│   │   ├── chat.py                    # 对话接口（单轮/多轮）
│   │   ├── diet_plan.py               # 膳食规划接口
│   │   ├── meal_plan.py               # 食谱生成/替换接口
│   │   └── health.py                  # 健康检查
│   ├── config/settings.py             # 全局配置（.env 读取）
│   ├── model/                         # Pydantic 请求/响应模型
│   ├── prompt/system_prompt.py        # 系统提示词模板
│   └── service/                       # 业务逻辑层
│       ├── chat_service.py            # 对话服务
│       ├── diet_plan_service.py       # 膳食规划服务
│       ├── meal_plan_service.py       # 食谱生成/替换服务
│       └── llm_service.py             # LLM 调用（云端 API + Ollama）
│
├── docs/                              # 设计文档
│   └── AI-Recipe-Plan-Design.html     # AI 食谱生成设计文档
└── src/main/resources/
    ├── application.yml                # 后端配置
    └── db/
        └── init.sql                   # 数据库初始化脚本（建表 + 种子数据）
```

---

## 🗄️ 数据库设计

### 数据库：`diet_ai_planner`（MySQL 8.0，utf8mb4）

### 表结构总览

| # | 表名 | 说明 | 核心字段 |
|---|------|------|---------|
| 1 | `user` | 用户表 | id, username, email, password_hash, 身体数据, 身体目标, 饮食目标, 忌口, 疾病, 用药, 设置项 |
| 2 | `food_category` | 食物分类表 | id, name, parent_id, sort_order |
| 3 | `food` | 食物表（公共库） | id, name, category_id, calories, protein, food_type, unit_name, unit_weight |
| 4 | `diet_record` | 饮食记录表 | id, user_id, food_id, food_source, meal_type, amount, record_date |
| 5 | `user_custom_food` | 用户自定义食物表 | id, user_id, name, calories, unit_name, is_recipe |
| 6 | `user_custom_food_item` | 自定义食谱明细表 | id, recipe_id, food_id, amount, calories |
| 7 | `meal_plan` | 膳食方案/食谱表 | id, user_id, name, goal, days, status, publish_status, origin, source_plan_id |
| 8 | `meal_plan_item` | 膳食方案条目表 | id, plan_id, day_index, meal_type, food_id, food_source, amount |
| 9 | `meal_plan_review` | 食谱审核记录表 | id, plan_id, reviewer_id, result, reason, reviewed_at |
| 10 | `meal_plan_favorite` | 食谱收藏表 | id, user_id, plan_id |
| 11 | `profile_option` | 档案选项字典表 | id, option_type, option_code, option_label, sort_order |
| 12 | `weight_record` | 体重/体脂记录表 | id, user_id, record_date, weight_kg, body_fat_percent |
| 13 | `ai_chat_session` | AI 会话表 | id, user_id, title |
| 14 | `ai_chat_message` | AI 消息表 | id, session_id, user_id, role, content, context_snapshot |
| 15 | `ai_generation_log` | AI 生成日志表 | id, user_id, type, model_name, tokens_used, is_abnormal |
| 16 | `nutrition_standard` | 营养标准表 | id, gender, age_min, age_max, calories_kcal |

### ER 关系图

```
┌──────────┐       ┌──────────────┐       ┌──────────┐
│   user   │1─────N│ diet_record  │N─────1│   food   │
│          │       │              │       │          │
│ id (PK)  │       │ user_id (FK) │       │ id (PK)  │
│ username │       │ food_id (FK) │       │ name     │
│ ...      │       │ food_source  │       │ calories │
│ 目标/忌口 │       │ record_date  │       │ food_type│
└──────────┘       └──────────────┘       └──────────┘
     │   │                                     │
     │   │1                                    │N
     │   │                                     │
     │   N                                     1
     │   │                              ┌──────────────┐
     │   │                              │food_category │
     │   │                              └──────────────┘
     │   │
     │   │1                    ┌──────────────────┐
     │   N───────────────────│  weight_record   │
     │                         │ (每天最多一条)    │
     │                         └──────────────────┘
     │
     │1                                    ┌──────────────────┐
     N─────────────────────────────────────│   meal_plan      │
                                          │                  │
     ┌──────────────┐                     │ id (PK)          │
     │ai_chat_session│                    │ user_id (FK)     │
     │              │                     │ status           │
     │ id (PK)      │                     │ publish_status   │
     │ user_id (FK) │                     │ origin           │
     │ title        │                     │ source_plan_id   │
     └──────────────┘                     └──────────────────┘
          │1                                    │1
          │                                     │
          N                                     N
     ┌──────────────┐                     ┌──────────────────┐
     │ai_chat_message│                     │ meal_plan_item   │
     │              │                     │                  │
     │ id (PK)      │                     │ id (PK)          │
     │ session_id   │                     │ plan_id (FK)     │
     │ role         │                     │ day_index        │
     │ content      │                     │ food_id/food_src │
     └──────────────┘                     └──────────────────┘
                                               │
                                               │N
                                               │
                                          ┌──────────────────┐
     ┌──────────────────┐                 │meal_plan_review  │
     │ai_generation_log │                 │ (审核历史)        │
     │                  │                 └──────────────────┘
     │ id (PK)          │
     │ user_id (FK)     │     ┌──────────────────┐
     │ type             │     │meal_plan_favorite│
     │ model_name       │     │ (谁收藏了哪份)    │
     └──────────────────┘     └──────────────────┘

┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
     │ nutrition_     │  │ profile_option   │  │ user_custom_food │
     │ standard       │  │ (字典表)         │  │ (私有食物库)     │
     └──────────────────┘  └──────────────────┘  └──────────────────┘
```

### 核心关系说明

- **user → diet_record**：一对多，一个用户有多条饮食记录
- **food → diet_record**：一对多，一个食物可出现在多条记录中（food_source 区分系统/自定义）
- **food_category → food**：一对多，一个分类下有多个食物
- **user → user_custom_food**：一对多，用户私有的自定义食物/食谱
- **user_custom_food → user_custom_food_item**：一对多，组合食谱由多个基础食材组成
- **user → meal_plan**：一对多，一个用户有多个膳食方案
- **meal_plan → meal_plan_item**：一对多，一个方案包含多天多条食物
- **meal_plan → meal_plan_review**：一对多，保留完整审核历史
- **user ↔ meal_plan**（通过 meal_plan_favorite）：多对多，用户收藏广场上的食谱
- **meal_plan.source_plan_id → meal_plan.id**：自引用，应用他人食谱时溯源
- **user → ai_chat_session → ai_chat_message**：两级一对多
- **user → ai_generation_log**：一对多，AI 调用记录
- **user → weight_record**：一对多，每天最多一条
- **nutrition_standard**：独立配置表，按性别+年龄段提供推荐摄入量
- **profile_option**：独立字典表，5 个分组（饮食目标/活动水平/饮食偏好/忌口/疾病）

### user 表新增字段（身体目标与健康信息）

| 字段 | 类型 | 说明 |
|------|------|------|
| target_weight_kg | DECIMAL(5,1) | 目标体重(kg)，减重/增重时设定后据此算热量缺口 |
| target_body_fat_percent | DECIMAL(5,1) | 目标体脂率(%)，比体重更准确 |
| target_deadline | DATE | 期望达成目标日期，估算还需多久并提示是否过快 |
| allergy_note | VARCHAR(200) | 忌口食物，多个以「、」分隔 |
| disease | VARCHAR(200) | 慢性疾病，多个以「、」分隔 |
| medication | VARCHAR(500) | 用药情况，自由文本 |

> ⚠️ `password_hash` 列存的是**明文密码**（教学/演示用途），兼容历史 BCrypt 数据（`$2a$` 开头走 BCrypt 校验）。正式环境请换回 BCrypt。

---

## 🎨 前端设计

### 导航架构：一级导航 + 二级导航

```
┌─────────────────────────────────────────────────────────────┐
│ 顶部导航：🏠今日饮食  📋饮食记录  📊营养分析  🤖AI助手  📋食谱  │
├──────────┬──────────────────────────────────────────────────┤
│ 左侧边栏  │  主内容区                                         │
│          │                                                  │
│ 二级菜单  │  ┌────────────────────────────────────────────┐   │
│ (动态)   │  │ 标签页导航（二级功能切换）                    │   │
│          │  ├────────────────────────────────────────────┤   │
│ ──────── │  │                                            │   │
│ 🍱食物库  │  │  页面内容                                   │   │
│ 👤个人中心│  │                                            │   │
│ ⚙️系统设置│  └────────────────────────────────────────────┘   │
└──────────┴──────────────────────────────────────────────────┘
```

- **顶部导航**：5 个核心一级模块 + 管理后台入口，始终可见
- **左侧二级菜单**：根据当前一级模块动态切换
- **侧边栏辅助区域**：食物库 / 个人中心 / 系统设置固定在底部
- **AI 饮食助手**：三栏工作台，不显示外层左侧导航

### 技术要点

| 方面 | 实现 |
|------|------|
| 路由 | Vue Router 4，嵌套路由 + meta 参数实现同组件多模式 |
| 状态管理 | Pinia，存储用户登录态和 token |
| HTTP 请求 | Axios 封装，自动携带 JWT，统一错误处理 |
| 代理 | Vite devServer proxy，`/api` → `localhost:8080`，`/ai-api` → `localhost:8000`，`/avatars` → `localhost:8080` |
| 组件库 | Element Plus，按需自动导入（unplugin-auto-import + unplugin-vue-components） |
| 图表 | ECharts 5，营养趋势、目标完成度、时段分布、体重体脂双折线 |
| 图表切换 | 路由切换时 dispose 旧实例 + 重新 init |
| 主题切换 | 浅色/深色/跟随系统三种模式，后端持久化 + 前端 CSS 类切换 |
| Markdown | marked + DOMPurify 渲染 AI 回复，防 XSS |
| 布局 | MainLayout（用户端）/ AdminLayout（管理端），响应式 |

### 营养分析算法

#### 基础代谢率 BMR（Mifflin-St Jeor 公式）

```
男性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 + 5
女性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 - 161
```

#### 总消耗 TDEE（活动系数法）

| 活动水平 | 系数 | 说明 |
|---------|------|------|
| 久坐 | 1.2 | 几乎不运动 |
| 轻度活动 | 1.375 | 每周1-3天轻度运动 |
| 中度活动 | 1.55 | 每周3-5天中等运动 |
| 高度活动 | 1.725 | 每周6-7天高强度运动 |
| 极高强度 | 1.9 | 每天高强度训练或体力劳动 |

#### 目标热量计算

```
有身体目标(目标体重 + 期限):
  每日缺口 = (目标体重 - 当前体重) × 7700kcal/kg / 期限天数
  目标热量 = TDEE + 每日缺口（增重为正、减重为负）
  结果夹在 TDEE 的 60%~130%，避免过低或过高
  方向矛盾(减脂却目标增重)时不参与计算

无身体目标:
  减脂: target = TDEE × 0.8
  维持: target = TDEE
  增肌: target = TDEE × 1.15

优先级: 用户手动设置 > 身体目标计算 > 饮食目标系数
```

#### 智能营养评估评分

| 评估项 | 权重 | 说明 |
|--------|------|------|
| 热量匹配度 | 40% | 实际热量与目标热量的接近程度 |
| 蛋白质匹配度 | 30% | 实际蛋白质与目标蛋白质的接近程度 |
| 用户偏好匹配 | 15% | 饮食偏好符合度 |
| 饮食目标匹配 | 15% | 减脂/增肌/维持目标符合度 |

#### 食物推荐评分（FoodScoreCalculator）

与营养评估同算法，对食物库逐个打分，输出 Top-N 推荐：

```
综合分 = 热量匹配分×0.40 + 蛋白匹配分×0.30 + 偏好匹配分×0.15 + 目标匹配分×0.15
```

#### 营养素供能比例计算

```
蛋白质供能比 = (蛋白质g × 4) / 总热量 × 100%
碳水供能比   = (碳水g × 4) / 总热量 × 100%
脂肪供能比   = (脂肪g × 9) / 总热量 × 100%

推荐比例: 碳水 50% / 蛋白质 20% / 脂肪 30%
```

#### 热量达标判定

```
达标条件: |实际热量 - 目标热量| ≤ 目标热量 × 10%
达标率 = 达标天数 / 有记录天数 × 100%
```

---

## 🔌 后端 API 接口

### 认证模块 `/api/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/register` | 用户注册（含身份选择：普通用户/管理员） | ❌ |
| POST | `/login` | 用户登录，返回 JWT | ❌ |

### 用户模块 `/api/user`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/profile` | 获取用户资料 | ✅ |
| PUT | `/profile` | 更新用户资料 | ✅ |
| POST | `/avatar` | 上传头像（multipart） | ✅ |
| GET | `/settings` | 获取用户设置 | ✅ |
| PUT | `/settings` | 保存用户设置 | ✅ |
| PUT | `/password` | 修改密码（校验旧密码） | ✅ |
| GET | `/options` | 获取档案选项（饮食目标/活动水平/偏好/忌口/疾病） | ❌ |

### 体重/体脂模块 `/api/user/weight`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/records` | 分页查询历史记录 | ✅ |
| POST | `/` | 保存一条记录（同天覆盖，同步体重到 user 表） | ✅ |
| DELETE | `/{id}` | 删除记录 | ✅ |
| GET | `/trend` | 趋势数据（折线图 + 区间概览 + 目标进度） | ✅ |
| PUT | `/goal` | 设定身体目标（目标体重/体脂/期限） | ✅ |
| GET | `/goal` | 获取当前身体目标 | ✅ |

### 今日饮食模块 `/api/diet`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/today` | 获取今日饮食概览（摄入/目标/剩余） | ✅ |
| GET | `/today/records` | 获取今日饮食记录列表 | ✅ |
| POST | `/today/add` | 添加单条饮食记录 | ✅ |
| POST | `/today/add-batch` | 批量添加饮食记录 | ✅ |
| PUT | `/today/target` | 更新用户自定义目标 | ✅ |
| DELETE | `/record/{id}` | 删除饮食记录 | ✅ |
| GET | `/records` | 按日期/范围查询记录 | ✅ |
| GET | `/records/week` | 获取近7天记录 | ✅ |
| GET | `/records/month` | 获取近30天记录 | ✅ |
| GET | `/stats` | 饮食统计（支持week/month/year周期） | ✅ |

### 食物模块 `/api/foods`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/` | 分页获取食物列表 | GET 公开 |
| GET | `/{id}` | 获取食物详情 | GET 公开 |
| GET | `/search` | 按名称搜索食物 | GET 公开 |
| GET | `/all` | 获取全部食物 | ✅ |
| POST | `/` | 添加新食物 | ✅ |
| POST | `/my` | 创建用户自定义食物 | ✅ |
| PUT | `/my/{id}` | 修改用户自定义食物 | ✅ |
| DELETE | `/my/{id}` | 删除用户自定义食物 | ✅ |
| POST | `/my/recipe` | 保存组合食谱（食材累加营养） | ✅ |

### 分类模块 `/api/categories`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/` | 获取所有分类 | GET 公开 |
| GET | `/{parentId}/sub` | 获取子分类 | GET 公开 |

### 膳食方案模块 `/api/meal-plan`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/` | 新建空食谱 | ✅ |
| POST | `/generate` | AI 智能生成完整 N 天膳食计划 | ✅ |
| PUT | `/{id}` | 改食谱基本信息 | ✅ |
| GET | `/current` | 当前执行中的食谱 + 今日进度 | ✅ |
| GET | `/` | 我的食谱列表 | ✅ |
| GET | `/list` | 我的食谱列表（带状态计数 + 筛选） | ✅ |
| GET | `/favorites` | 我收藏的食谱 | ✅ |
| GET | `/{id}` | 食谱详情（逐天明细） | ✅ |
| DELETE | `/{id}` | 删除食谱（软删除） | ✅ |
| POST | `/{id}/apply` | 开始执行 | ✅ |
| POST | `/{id}/pause` | 暂停执行 | ✅ |
| POST | `/{id}/resume` | 恢复执行 | ✅ |
| POST | `/{id}/finish` | 提前结束（归档） | ✅ |
| GET | `/{id}/execution` | 执行进度（周期进度 + 计划vs实际） | ✅ |
| GET | `/{id}/reviews` | 审核历史 | ✅ |
| POST | `/{id}/items` | 加一条食物 | ✅ |
| PUT | `/{planId}/item/{itemId}` | 改/替换一条食物 | ✅ |
| DELETE | `/{planId}/item/{itemId}` | 移除一条食物 | ✅ |
| POST | `/{id}/copy-day` | 复制某天到其他天 | ✅ |
| GET | `/{planId}/item/{itemId}/candidates` | 手选替换候选 | ✅ |
| GET | `/{planId}/item/{itemId}/alternatives` | AI 智能替换候选 | ✅ |
| POST | `/{id}/cover` | 上传食谱封面 | ✅ |
| POST | `/{id}/publish` | 提交发布（进入待审核） | ✅ |
| POST | `/{id}/unpublish` | 撤回/下架 | ✅ |
| GET | `/square` | 食谱广场列表（热门/最新 + 搜索） | ✅ |
| GET | `/square/{id}` | 广场食谱详情 | ✅ |
| POST | `/square/{id}/favorite` | 收藏 | ✅ |
| DELETE | `/square/{id}/favorite` | 取消收藏 | ✅ |
| POST | `/square/{id}/copy` | 应用他人食谱到我的食谱 | ✅ |

### 食物推荐模块 `/api/recommendation`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/foods` | 个性化食物推荐 Top-N（多因素评分算法） | ✅ |

### AI 对话模块 `/api/chat`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/sessions` | 获取用户会话列表 | ✅ |
| GET | `/sessions/{sessionId}/messages` | 获取会话消息 | ✅ |
| POST | `/send` | 发送消息（调用 AI） | ✅ |
| POST | `/sessions` | 创建新会话 | ✅ |
| DELETE | `/sessions/{sessionId}` | 删除会话 | ✅ |

### 营养分析模块 `/api/nutrition`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/analysis` | 获取营养分析数据（趋势+占比） | ✅ |
| GET | `/evaluate` | 智能营养评估（BMI/BMR/TDEE/评分） | ✅ |
| GET | `/nutrients` | 营养素详细分析 | ✅ |
| GET | `/calorie` | 热量详细分析 | ✅ |
| GET | `/goal` | 目标完成度分析 | ✅ |
| GET | `/report` | 营养报告 | ✅ |

### 管理员模块 `/api/admin`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/stats/overview` | 数据看板概览 | 🔒 Admin |
| GET | `/stats/trend` | 趋势统计 | 🔒 Admin |
| GET | `/users` | 用户列表 | 🔒 Admin |
| GET | `/users/{id}` | 用户详情 | 🔒 Admin |
| PUT | `/users/{id}/status` | 修改用户状态 | 🔒 Admin |
| DELETE | `/users/{id}` | 删除用户 | 🔒 Admin |
| GET | `/foods` | 食物列表 | 🔒 Admin |
| PUT | `/foods/{id}` | 编辑食物 | 🔒 Admin |
| PUT | `/foods/{id}/status` | 审核食物状态 | 🔒 Admin |
| DELETE | `/foods/{id}` | 删除食物 | 🔒 Admin |
| GET | `/foods/pending` | 待审核食物 | 🔒 Admin |
| POST | `/categories` | 添加分类 | 🔒 Admin |
| PUT | `/categories/{id}` | 编辑分类 | 🔒 Admin |
| DELETE | `/categories/{id}` | 删除分类 | 🔒 Admin |
| GET | `/diet-records` | 全站饮食记录 | 🔒 Admin |
| DELETE | `/diet-records/{id}` | 删除饮食记录 | 🔒 Admin |
| GET | `/nutrition-standards` | 营养标准列表 | 🔒 Admin |
| POST | `/nutrition-standards` | 添加营养标准 | 🔒 Admin |
| PUT | `/nutrition-standards/{id}` | 编辑营养标准 | 🔒 Admin |
| DELETE | `/nutrition-standards/{id}` | 删除营养标准 | 🔒 Admin |
| GET | `/profile-options/types` | 档案选项分组类型 | 🔒 Admin |
| GET | `/profile-options` | 档案选项列表 | 🔒 Admin |
| POST | `/profile-options` | 添加档案选项 | 🔒 Admin |
| PUT | `/profile-options/{id}` | 编辑档案选项 | 🔒 Admin |
| DELETE | `/profile-options/{id}` | 删除档案选项 | 🔒 Admin |
| GET | `/meal-plans` | 食谱列表 | 🔒 Admin |
| GET | `/meal-plans/pending-count` | 待审核数量 | 🔒 Admin |
| GET | `/meal-plans/{id}` | 食谱详情 | 🔒 Admin |
| POST | `/meal-plans/{id}/approve` | 通过审核 | 🔒 Admin |
| POST | `/meal-plans/{id}/reject` | 驳回审核 | 🔒 Admin |
| POST | `/meal-plans/{id}/takedown` | 下架 | 🔒 Admin |
| GET | `/meal-plans/{id}/reviews` | 审核历史 | 🔒 Admin |
| GET | `/ai-logs` | AI 生成日志 | 🔒 Admin |
| PUT | `/ai-logs/{id}/abnormal` | 标记异常 | 🔒 Admin |

### AI 服务接口（FastAPI `localhost:8000`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 单轮对话 |
| POST | `/api/chat/history` | 多轮对话（带历史上下文） |
| POST | `/api/diet-plan` | 生成膳食规划 |
| POST | `/api/diet-plan/structured` | 生成结构化膳食方案（Pydantic 校验） |
| POST | `/api/meal-plan/generate` | AI 生成完整 N 天膳食计划（结构化数据） |
| POST | `/api/meal-plan/replace` | 食物替换推荐（AI 为可选增强） |
| GET | `/api/health` | 健康检查（返回当前后端 / 模型 / 接口地址） |

---

## 🤖 AI 服务详解

### 架构概览

```
用户提问 → Spring Boot 后端 → FastAPI AI 服务 → 云端大模型 API / 本机 Ollama → 返回结果
                │                    │
                │                    ├─ 成功：返回 AI 生成内容
                │                    └─ 失败：返回 fallback 提示（附 fallback:true 和 error）
                │
                ├─ 保存会话和消息到数据库
                └─ 记录 AI 生成日志（token 消耗、异常标记）
```

### AI 服务文件结构

```
ai-service/
├── main.py                       # FastAPI 主程序（路由注册、启动入口）
├── requirements.txt              # Python 依赖清单
├── .env.example                  # ⭐ 配置模板（复制为 .env 后填自己的 API_KEY）
├── .env                          # 本地实际配置（含密钥，已被 gitignore）
├── api/                          # API 路由层
│   ├── chat.py                   # 对话接口（单轮/多轮）
│   ├── diet_plan.py              # 膳食规划接口
│   ├── meal_plan.py              # 食谱生成/替换接口
│   └── health.py                 # 健康检查接口
├── config/
│   └── settings.py               # 全局配置（.env 读取、后端选择、模型名、超时等）
├── model/                        # 数据模型层
│   ├── request.py                # 请求模型（Pydantic）
│   └── response.py               # 响应模型（Pydantic）
├── prompt/
│   └── system_prompt.py          # 系统提示词模板
└── service/                      # 业务逻辑层
    ├── chat_service.py           # 对话服务（上下文构建、历史管理）
    ├── diet_plan_service.py      # 膳食规划服务
    ├── meal_plan_service.py      # 食谱生成/替换服务
    └── llm_service.py            # LLM 调用服务（云端 API + Ollama 双后端）
```

### 核心 System Prompt

AI 助手名为 **"智慧膳食"**，具备以下能力：

1. 根据用户健康数据（身高/体重/年龄/活动水平/饮食目标/饮食偏好/忌口/疾病）提供个性化膳食建议
2. 分析每日营养摄入是否合理，指出营养缺口和改进方向
3. 推荐适合用户目标（减脂/维持/增肌）的食物和食谱，优先考虑饮食偏好
4. 回答食物营养、热量、健康饮食相关问题
5. 结合剩余热量和营养素缺口给出精准建议
6. 结合体重体脂趋势、身体目标与完成度回答进度类问题

**个性化规则**：
- 根据性别、年龄、体重、身高给出针对性建议
- 严格遵守用户饮食偏好与忌口（素食、低糖、无辣、海鲜过敏等）
- 营养均衡推荐比例：碳水 50%、蛋白质 20%、脂肪 30%

### AI 对话用户体验优化

- ✅ **打字机效果**："正在思考中，请稍候..." 逐字显示 + 闪烁光标
- ✅ **实时等待计时**：显示 `⏱️ 已等待 X 秒`
- ✅ **取消请求按钮**：支持中断正在进行的 AI 调用（`AbortController`）
- ✅ **Markdown 渲染**：marked + DOMPurify，支持加粗/列表/代码块/引用
- ✅ **回复耗时显示**：每条 AI 回复底部显示实际响应耗时
- ✅ **优雅降级**：组件卸载时自动清理定时器和未完成请求

### 超时保护机制

| 组件 | 超时设置 | 说明 |
|------|---------|------|
| 前端 Axios | **300 秒（5分钟）** | HTTP 请求总超时 |
| 后端 RestTemplate 连接 | **10 秒** | 连接 AI 服务超时 |
| 后端 RestTemplate 读取 | **300 秒（5分钟）** | 等待 AI 响应超时 |

### Fallback 降级机制

当云端 API 调用失败（key 错误、欠费、超时）或本机 Ollama 未就绪时，返回明确的失败提示（附 `fallback: true` 和 `error`），不伪造营养建议。常见错误翻译：

| 现象 | 提示 |
|------|------|
| 401 | 认证失败：`API_KEY` 填错了或已失效 |
| 403 | 无权限：key 没开通该模型 |
| 404 | 接口/模型不存在：检查 `API_BASE_URL` 与 `API_MODEL` |
| 429 | 请求过于频繁或额度不足 |
| 500+ | 云端服务暂时不可用，稍后重试 |

> 401/403/404 属于配置错误，会**立即失败不重试**；网络抖动类错误会自动重试。

### 如何切换 AI 后端 / 更换模型

所有 AI 配置都集中在 **`ai-service/.env`** 一个文件里，改完重启 AI 服务即可，**不用改代码**。

#### 方式一：接云端 API（推荐）

复制模板文件，填入自己的 key：

```bash
cd ai-service
cp .env.example .env      # Windows: copy .env.example .env
```

然后编辑 `.env`，把 `API_KEY` 换成自己的：

```ini
LLM_BACKEND=auto          # auto = 填了 key 就用云端，没填就用 Ollama

API_KEY=sk-你的密钥
API_BASE_URL=https://api.deepseek.com
API_MODEL=deepseek-flash
```

**各家厂商怎么填**（任选一家，其它 OpenAI 兼容接口同理）：

| 厂商 | API_BASE_URL | API_MODEL 示例 | Key 获取地址 |
|------|--------------|----------------|--------------|
| DeepSeek | `https://api.deepseek.com` | `deepseek-flash` | platform.deepseek.com |
| 火山方舟（豆包） | `https://ark.cn-beijing.volces.com/api/v3` | `doubao-seed-2-1-pro-260628` | console.volcengine.com/ark |
| 阿里云百炼（通义） | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` | bailian.console.aliyun.com |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | `glm-5` | open.bigmodel.cn |
| Kimi（月之暗面） | `https://api.moonshot.cn/v1` | `kimi-k3` | platform.moonshot.cn |

> ⚠️ `API_BASE_URL` 填到**版本段为止**即可，程序会自动拼 `/chat/completions`。
> ⚠️ **模型名会随厂商版本更新而变**。若报 404「模型不存在」，去厂商控制台的模型列表复制当前可用的模型 ID 替换 `API_MODEL` 即可。

#### 方式二：用本机 Ollama

```ini
LLM_BACKEND=ollama
```

先拉取模型：

| 模型 | 参数量 | 说明 | 拉取命令 |
|------|--------|------|---------|
| qwen2.5:7b | 7B | 通义千问通用版，中文好 | `ollama pull qwen2.5:7b` |
| qwen2.5:3b | 3B | 体积小、速度快，精度一般 | `ollama pull qwen2.5:3b` |
| llama3.1:8b | 8B | Meta Llama 3.1 | `ollama pull llama3.1:8b` |
| mistral:7b | 7B | Mistral 7B | `ollama pull mistral:7b` |
| glm4:9b | 9B | 智谱 GLM-4 | `ollama pull glm4:9b` |

> 改完 `.env` 或模型后需重启 AI 服务：`python main.py`
> 验证当前生效的后端与模型：浏览器打开 `http://localhost:8000/api/health`

---

## 🚀 快速开始

### 环境要求

| 工具 | 版本要求 | 安装指引 |
|------|---------|---------|
| JDK | 25+ | [Adoptium](https://adoptium.net/) |
| Maven | 3.9+ | [maven.apache.org](https://maven.apache.org/download.cgi) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org/) |
| npm | 9+ | 随 Node.js 安装 |
| MySQL | 8.0+ | [dev.mysql.com](https://dev.mysql.com/downloads/) |
| Python | 3.10+ | [python.org](https://www.python.org/) |
| Ollama | 最新（可选） | [ollama.com](https://ollama.com/) — 仅在用本机模型时需要 |

### 第一步：克隆项目

```bash
git clone https://github.com/zyy-17/DietAI-Planner.git
cd DietAI-Planner
```

### 第二步：初始化数据库

1. 启动 MySQL 服务
2. 使用 MySQL 客户端（如 Navicat、MySQL Workbench 或命令行）执行初始化脚本：

```bash
mysql -u root -p < src/main/resources/db/init.sql
```

该脚本会：
- 创建数据库 `diet_ai_planner`
- 创建 16 张表（用户、食物、饮食记录、自定义食物、膳食方案、审核、收藏、档案选项、体重记录、AI 会话/消息/日志、营养标准等）
- 插入管理员账号（admin / admin123，明文存储）
- 插入 11 个食物分类
- 插入 70+ 食物数据（46 基础食材 + 24 成品菜肴）
- 插入 6 条营养标准配置
- 插入 30 条档案选项（饮食目标 3 + 活动水平 5 + 饮食偏好 10 + 忌口 7 + 疾病 5）

> 后端启动时 Hibernate `ddl-auto=update` 会自动补齐实体新增的列，init.sql 是完整建表脚本，二者不冲突。

### 第三步：配置后端

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/diet_ai_planner?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root        # ← 改为你的 MySQL 用户名
    password: 123456      # ← 改为你的 MySQL 密码
```

### 第四步：启动后端

```bash
# 在项目根目录
mvn spring-boot:run
# Windows 也可用：./mvnw spring-boot:run
```

后端启动成功后访问：`http://localhost:8080`

### 第五步：启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动成功后访问：`http://localhost:3000`

> Vite 开发服务器已配置代理：`/api` → `localhost:8080`，`/ai-api` → `localhost:8000`，无需额外配置跨域

### 第六步：启动 AI 服务

> ⚠️ **重要提示**：AI 对话与 AI 食谱生成功能依赖以下三个服务同时运行：
> - Spring Boot 后端（端口 `8080`）
> - Vue 前端（端口 `3000`）
> - Python AI 服务（端口 `8000`）
>
> 缺少 AI 服务时，其他功能正常使用，仅 AI 对话与 AI 食谱生成不可用

**第一步：配置大模型**（只需做一次）

```bash
cd ai-service
cp .env.example .env      # Windows: copy .env.example .env
```

- **【推荐】接云端 API**：打开 `.env`，把 `API_KEY` 填成自己的密钥（DeepSeek / 豆包 / 通义 / 智谱 / Kimi 任选一家）
- **用本机 Ollama**：`.env` 里设 `LLM_BACKEND=ollama`，并先拉模型：`ollama pull qwen2.5:7b`

**第二步：启动 AI 服务**

```bash
cd ai-service
pip install -r requirements.txt
python main.py
```

AI 服务启动后运行在：`http://localhost:8000`

接云端 API 时会看到：
```
INFO: 启动 AI 服务 v5.1 | 后端=云端API | 接口=https://api.deepseek.com | 模型=deepseek-flash
INFO: 监听地址 http://127.0.0.1:8000
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
```

> 🔒 默认只监听 **127.0.0.1**（仅本机可访问）。本服务没有鉴权接口，
> 若改成 `0.0.0.0`，同一局域网下的其他人也能调用你的 AI 接口并消耗你的 API 额度。

**第三步：验证 AI 服务是否正常**

浏览器访问 `http://localhost:8000/api/health`：

```json
{ "status": "ok", "backend": "api", "model": "deepseek-flash", "api_base_url": "https://api.deepseek.com" }
```

也可以访问 `http://localhost:8000/docs` 查看 FastAPI 自动生成的 API 文档。

### 第七步：登录系统

打开浏览器访问 `http://localhost:3000`，使用默认管理员账号登录：

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

也可以点击"注册"创建新用户（注册时可选身份：普通用户 / 管理员）。

---

## 🔑 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| admin | admin | admin123 | 系统管理员，可访问后台管理 |

> 密码明文存储（教学演示用途），兼容历史 BCrypt 数据。正式环境请换回 BCrypt。

---

## 📸 项目截图

> 以下为系统主要页面示意

| 页面 | 说明 |
|------|------|
| 登录页 | 用户登录 / 注册（含身份选择） |
| 今日概览 | 每日摄入总览、四大营养素进度、修改目标 |
| 餐次详情 | 早/午/晚/加餐食物列表，多选批量添加，按个数/份数计量 |
| AI今日建议 | 4种套餐方案换一换，结构化膳食方案展示 |
| 饮食记录 | 历史记录/周记录/月记录/条件查询，各模式独立后端接口 |
| 饮食统计 | 每日热量趋势折线图 + 三大营养素占比饼图 + 达标统计 |
| 体重体脂 | 体重/体脂双折线图 + 身体目标 + 目标进度 + 安全速率提示 |
| 营养素分析 | 每日营养素三线趋势图 + 供能占比饼图 + 日均vs目标 |
| 热量分析 | 每日热量柱状图vs目标线 + 时段分布饼图 + 达标率 |
| 目标完成度 | 四项环形进度条 + 综合完成度评价 + 改进建议 |
| 营养报告 | 综合评分 + 各项评价 + 关键发现 + 改善建议 |
| 食物库 | 三层 Tab（基础食材/成品菜肴/自定义食物），点击添加到今日饮食 |
| AI 对话 | 三栏工作台，多轮对话，Markdown 渲染，回复耗时显示 |
| 我的食谱 | 手动创建/AI 生成，逐天逐餐编辑，执行进度，热门推荐侧边栏 |
| 食谱广场 | 热门Top10/最新排序，搜索、收藏、应用他人食谱 |
| 个人中心 | 基本资料/身体数据/饮食目标/饮食偏好/忌口/活动水平/健康信息 |
| 管理后台 | 数据看板/用户/食物/分类/饮食记录/营养标准/档案选项/食谱审核/AI日志 |

---

## 📄 License

本项目仅供学习交流使用。
