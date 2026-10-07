# 智慧膳食 DietAI-Planner — 系统技术文档

> 基于AI的智能膳食规划与营养管理平台  
> 技术栈：Spring Boot 4.1 + Vue 3 + FastAPI + MySQL + Ollama/云端LLM

---

## 一、项目概述

**智慧膳食**是一个全栈Web应用，帮助用户进行日常饮食记录、营养分析、AI膳食建议，并支持食谱手工编写、食谱广场分享与审核、体重体脂追踪等功能。

### 核心功能模块

| 模块 | 功能 | 入口 |
|------|------|------|
| 今日概览 | 每日饮食记录、营养摄入总览、AI今日建议 | `/today` |
| 饮食记录 | 按餐次添加/删除食物，历史记录查询与统计 | `/today/breakfast` 等 |
| 营养分析 | 今日营养评估、营养趋势、目标完成度、营养报告 | `/nutrition` |
| AI饮食助手 | 多轮对话、膳食方案生成、营养咨询 | `/chat` |
| 食谱 | 用户自建食谱、按天执行、食谱广场、发布审核 | `/meal-plan` |
| 体重体脂 | 体重/体脂记录、趋势图、身体目标追踪 | `/nutrition/body-metrics` |
| 食物库 | 全部食物浏览、搜索、分类 | `/foods` |
| 个人中心 | 身体数据、饮食目标、饮食偏好、忌口设置 | `/profile` |
| 管理后台 | 用户管理、食物管理、食谱审核、档案选项配置 | `/admin/*` |

---

## 二、系统架构

```
┌─────────────────────────────────────────────────┐
│                   用户浏览器                      │
│              Vue 3 + Element Plus                │
└──────────┬──────────────────────┬────────────────┘
           │ /api/*               │ /ai-api/*
           ▼                      ▼
┌──────────────────┐   ┌──────────────────────┐
│   Spring Boot    │   │    FastAPI (Python)   │
│   Java 后端      │──▶│    AI 服务            │
│   端口 8080      │   │    端口 8000          │
└────────┬─────────┘   └──────────┬───────────┘
         │                        │
         ▼                        ▼
┌──────────────────┐   ┌──────────────────────┐
│   MySQL 8.x      │   │  Ollama / 云端LLM    │
│   端口 3306      │   │  本地或远程API        │
└──────────────────┘   └──────────────────────┘
```

### 请求流转

1. 前端通过 `axios` 发起HTTP请求
2. 开发环境通过 Vite `proxy` 转发到后端
3. Spring Boot 处理业务逻辑，通过 JPA 操作 MySQL
4. AI相关请求由 Spring Boot 通过 `RestTemplate` 转发到 FastAPI
5. FastAPI 调用 Ollama 或云端LLM API 获取AI响应

---

## 三、后端架构（Spring Boot）

### 3.1 分层结构

```
src/main/java/com/zyyqq/
├── config/          # 配置类（Security、RestTemplate连接池、Cache、WebMvc）
├── controller/      # 用户端API控制器
│   └── admin/       # 管理端API控制器
├── dto/
│   ├── request/     # 请求DTO
│   └── response/    # 响应DTO/VO
├── entity/          # JPA实体（对应数据库表）
├── exception/       # 异常定义与全局处理
├── repository/      # JPA Repository（数据访问层）
├── security/        # JWT认证相关
└── service/         # 业务逻辑层
```

### 3.2 Controller 一览

| Controller | 路径前缀 | 职责 |
|-----------|---------|------|
| `AuthController` | `/api/auth` | 登录、注册、刷新Token |
| `UserController` | `/api/user` | 用户信息、个人资料、打招呼接口 |
| `TodayDietController` | `/api/diet/today` | 今日饮食记录、AI建议应用 |
| `FoodController` | `/api/foods` | 食物库查询 |
| `AiChatController` | `/api/chat` | AI对话 |
| `NutritionAnalysisController` | `/api/nutrition` | 营养分析 |
| `RecommendationController` | `/api/recommendation` | 食物推荐 |
| `MealPlanController` | `/api/meal-plan` | 食谱CRUD、广场、收藏 |
| `WeightRecordController` | `/api/weight` | 体重体脂记录 |
| `AdminUserController` | `/api/admin/users` | 用户管理 |
| `AdminFoodController` | `/api/admin/foods` | 食物管理 |
| `AdminMealPlanController` | `/api/admin/meal-plans` | 食谱审核、下架 |
| `AdminProfileOptionController` | `/api/admin/profile-options` | 档案选项配置 |

### 3.3 Service 一览

| Service | 职责 |
|---------|------|
| `UserService` | 用户CRUD、资料更新、密码修改 |
| `FoodService` | 食物库查询（带缓存）、批量查询 |
| `DietRecordService` | 饮食记录增删改查、按食物名称批量添加 |
| `AiChatService` | AI对话、历史消息管理、上下文压缩 |
| `NutritionAnalysisService` | 营养分析计算、目标完成度 |
| `NutritionEvaluationService` | 智能营养评估引擎 |
| `RecommendationService` | 多因素评分食物推荐算法 |
| `MealPlanService` | 食谱全生命周期管理（最大Service，~800行） |
| `WeightRecordService` | 体重体脂记录与趋势 |
| `ProfileOptionService` | 档案选项配置 |
| `AdminService` | 管理后台统计 |

### 3.4 关键配置

**`application.yml` 核心配置项：**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/diet_ai_planner
    username: root
    password: 123456
  jpa:
    hibernate:
      ddl-auto: update    # 自动根据Entity建表/加列

ai-service:
  url: http://localhost:8000
  read-timeout: 300       # AI响应慢，需长超时
  pool:
    max-total: 50         # RestTemplate连接池
    max-per-route: 20

jwt:
  secret: DietAI-Planner-Secret-Key-...
  expiration: 86400000    # 24小时
```

---

## 四、前端架构（Vue 3）

### 4.1 目录结构

```
frontend/src/
├── api/             # API调用封装
├── assets/          # 静态资源
├── components/      # 公共组件
│   └── RecipeSheet.vue  # 食谱详情弹窗（核心组件）
├── layouts/         # 布局组件
│   ├── MainLayout.vue  # 用户端主布局（侧边栏+顶栏）
│   └── AdminLayout.vue # 管理端布局
├── router/          # 路由配置
├── utils/           # 工具函数
│   └── api.js       # axios实例与拦截器
└── views/           # 页面组件
    ├── admin/       # 管理端页面
    ├── TodayDiet.vue    # 今日概览主页
    ├── MealDetail.vue   # 餐次详情
    ├── AiSuggest.vue    # AI今日建议
    ├── AiChat.vue       # AI对话
    ├── MealPlan.vue     # 食谱主页（我的食谱+广场）
    ├── BodyMetrics.vue  # 体重体脂
    ├── NutritionAnalysis.vue  # 营养分析
    └── ...
```

### 4.2 认证机制

- 登录后后端返回JWT Token
- 前端存储在 `localStorage`
- axios拦截器自动在请求头添加 `Authorization: Bearer <token>`
- 401响应自动跳转登录页

---

## 五、AI服务架构（FastAPI）

### 5.1 目录结构

```
ai-service/
├── main.py              # FastAPI应用入口
├── config/
│   └── settings.py      # LLM配置（Ollama/云端API）
├── api/
│   ├── chat.py          # AI对话接口
│   ├── diet_plan.py     # 膳食方案生成接口
│   ├── meal_plan.py     # 食谱替换推荐接口
│   └── health.py        # 健康检查接口
├── service/
│   ├── llm_service.py   # LLM调用统一封装
│   ├── chat_service.py  # 对话业务逻辑
│   ├── diet_plan_service.py  # 膳食方案生成
│   └── meal_plan_service.py  # 食谱替换推荐
├── prompt/
│   └── system_prompt.py # 系统提示词
├── model/
│   ├── request.py       # 请求模型
│   └── response.py      # 响应模型
├── .env.example         # 环境变量模板
└── requirements.txt     # Python依赖
```

### 5.2 LLM后端切换

AI服务支持两种LLM后端，通过 `ai-service/.env` 配置切换：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `LLM_BACKEND` | `api`（云端）/ `ollama`（本地）/ `auto`（自动） | `auto` |
| `API_KEY` | 云端API密钥 | 空 |
| `API_BASE_URL` | 云端API地址 | `https://api.deepseek.com` |
| `API_MODEL` | 云端模型名 | `deepseek-chat` |
| `API_TEMPERATURE` | 生成温度 | `0.6` |
| `API_MAX_TOKENS` | 最大输出token | `2048` |
| `API_TIMEOUT` | 请求超时（秒） | `120` |
| `OLLAMA_HOST` | Ollama服务地址 | `http://localhost:11434` |

**`auto`模式逻辑**：配置了 `API_KEY` 就用云端API，否则退回Ollama。

---

## 六、数据库设计

### 6.1 ER关系图

```
user ──1:N──> diet_record ──N:1──> food
  │                │
  │                └──> food_category
  │
  ├──1:N──> meal_plan ──1:N──> meal_plan_item
  │            │
  │            └──1:N──> meal_plan_favorite
  │
  ├──1:N──> weight_record
  ├──1:N──> ai_chat_session ──1:N──> ai_chat_message
  ├──1:N──> user_custom_food
  └──1:N──> ai_generation_log

nutrition_standard (独立参考表)
profile_option ──N:1──> profile_option_type
```

### 6.2 表字段详解

#### `user` — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `username` | VARCHAR(50) | 登录用户名，唯一 |
| `password` | VARCHAR(255) | BCrypt加密密码 |
| `real_name` | VARCHAR(50) | 真实姓名，主页打招呼用 |
| `role` | VARCHAR(20) | 角色：`USER` / `ADMIN` |
| `gender` | VARCHAR(10) | 性别 |
| `age` | INT | 年龄 |
| `height` | DECIMAL | 身高(cm) |
| `weight` | DECIMAL | 体重(kg) |
| `activity_level` | VARCHAR(20) | 活动水平：久坐/轻度/中度/重度 |
| `target_calories` | DECIMAL | 用户自定义每日目标热量 |
| `target_protein` | DECIMAL | 用户自定义每日目标蛋白质(g) |
| `target_carbohydrate` | DECIMAL | 用户自定义每日目标碳水(g) |
| `target_fat` | DECIMAL | 用户自定义每日目标脂肪(g) |
| `dietary_preference` | VARCHAR(100) | 饮食偏好 |
| `allergen` | VARCHAR(200) | 过敏原/忌口 |
| `health_condition` | VARCHAR(200) | 健康状况 |
| `avatar_url` | VARCHAR(255) | 头像URL |
| `deleted` | TINYINT | 逻辑删除标记(0/1) |
| `created_at` / `updated_at` | DATETIME | 创建/更新时间 |

#### `food` — 食物库表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `name` | VARCHAR(100) | 食物名称 |
| `category_id` | BIGINT FK | 所属分类 |
| `calories` | DECIMAL | 每100g热量(kcal) |
| `protein` | DECIMAL | 每100g蛋白质(g) |
| `carbohydrate` | DECIMAL | 每100g碳水(g) |
| `fat` | DECIMAL | 每100g脂肪(g) |
| `fiber` | DECIMAL | 每100g膳食纤维(g) |
| `unit_name` | VARCHAR(20) | 常用单位名（如"个"、"碗"） |
| `unit_weight` | DECIMAL | 常用单位对应克数 |
| `image_url` | VARCHAR(255) | 食物图片URL |
| `approved` | TINYINT | 是否审核通过(0/1) |
| `custom` | TINYINT | 是否用户自定义食物(0/1) |
| `user_id` | BIGINT | 自定义食物的创建者 |

#### `diet_record` — 饮食记录表（用户"实际吃了什么"）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `user_id` | BIGINT FK | 所属用户 |
| `food_id` | BIGINT FK | 食物ID |
| `meal_type` | VARCHAR(20) | 餐次：breakfast/lunch/dinner/snack |
| `amount` | DECIMAL | 食用量(g) |
| `calories` | DECIMAL | 实际热量(kcal)，按amount换算 |
| `protein` | DECIMAL | 实际蛋白质(g) |
| `carbohydrate` | DECIMAL | 实际碳水(g) |
| `fat` | DECIMAL | 实际脂肪(g) |
| `record_date` | DATE | 记录日期 |
| `deleted` | TINYINT | 逻辑删除 |

#### `meal_plan` — 食谱/膳食方案表（用户"打算吃什么"）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `user_id` | BIGINT | 创建者 |
| `status` | VARCHAR(20) | 执行状态：`idle`/`active`/`archived` |
| `name` | VARCHAR(100) | 食谱名 |
| `goal` | VARCHAR(20) | 目标：`lose`/`gain`/`maintain` |
| `days` | INT | 计划天数(3/7/14) |
| `meals` | VARCHAR(100) | 每日餐次，逗号分隔 |
| `daily_calories` | DECIMAL | 每日目标热量 |
| `summary` | VARCHAR(500) | 一句话概述 |
| `description` | VARCHAR(1000) | 心得/思路 |
| `publish_status` | VARCHAR(20) | 发布状态：`none`/`pending`/`approved`/`rejected` |
| `cover_url` | VARCHAR(255) | 封面图URL |
| `tags` | VARCHAR(200) | 标签，顿号分隔 |
| `difficulty` | VARCHAR(20) | 难度：入门/进阶/挑战 |
| `expected_loss` | VARCHAR(30) | 预期减重 |
| `source_plan_id` | BIGINT | 从广场复制时的原食谱ID |
| `usage_count` | INT | 被保存为我的食谱的次数 |
| `favorite_count` | INT | 被收藏次数 |
| `published_at` | DATETIME | 上架时间 |
| `reject_reason` | VARCHAR(500) | 审核驳回原因 |
| `start_date` | DATE | 开始执行日期 |

#### `meal_plan_item` — 食谱条目表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `plan_id` | BIGINT FK | 所属食谱 |
| `day_index` | INT | 第几天(从1开始) |
| `meal_type` | VARCHAR(20) | 餐次 |
| `food_id` | BIGINT | 食物ID（AI估算的可为空） |
| `food_source` | VARCHAR(20) | 来源：`system`/`user`/`ai` |
| `food_name` | VARCHAR(100) | 食物名快照 |
| `amount` | DECIMAL | 克数 |
| `unit_name` | VARCHAR(20) | 单位名快照 |
| `unit_weight` | DECIMAL | 单位克数快照 |
| `calories` | DECIMAL | 换算后热量快照 |
| `protein` / `carbohydrate` / `fat` | DECIMAL | 三大营养素快照 |
| `image_url` | VARCHAR(255) | 图片快照 |
| `sort_order` | INT | 同餐次内排序 |

#### `meal_plan_favorite` — 食谱收藏表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `user_id` | BIGINT | 收藏者 |
| `plan_id` | BIGINT | 被收藏的食谱 |
| `created_at` | DATETIME | 收藏时间 |

#### `weight_record` — 体重体脂记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `user_id` | BIGINT | 所属用户 |
| `weight` | DECIMAL | 体重(kg) |
| `body_fat` | DECIMAL | 体脂率(%) |
| `record_date` | DATE | 记录日期 |
| `note` | VARCHAR(200) | 备注 |

#### `ai_chat_session` / `ai_chat_message` — AI对话表

| 字段 | 类型 | 说明 |
|------|------|------|
| session: `id`, `user_id`, `title`, `created_at` | | 对话会话 |
| message: `id`, `session_id`, `role`, `content`, `created_at` | | 对话消息 |

#### `ai_generation_log` — AI生成日志表

记录每次AI调用的输入、输出、耗时、token数，用于管理后台的AI记录页面。

#### `nutrition_standard` — 营养标准参考表

存储DRIs（膳食营养素参考摄入量），按年龄/性别/活动水平提供营养素推荐值。

#### `profile_option` / `profile_option_type` — 档案选项配置表

管理员可配置的档案选项（如活动水平选项、饮食偏好选项等），`profile_option_type` 定义选项类型，`profile_option` 存储具体选项值。

#### `user_custom_food` — 用户自定义食物表

用户自行添加的食物，结构与 `food` 类似但绑定到特定用户。

#### `food_category` — 食物分类表

食物分类（如谷薯类、蔬菜类、肉蛋类等），`food` 通过 `category_id` 关联。

---

## 七、食谱功能详解

### 7.1 功能架构

```
用户端                              管理端
┌─────────────┐  ┌──────────────┐  ┌──────────────┐
│  我的食谱    │  │  食谱广场     │  │  食谱审核     │
│  - 新建食谱  │  │  - 热门/最新  │  │  - 统计卡片   │
│  - 编辑食物  │  │  - 搜索筛选   │  │  - 批量审核   │
│  - 开始执行  │  │  - 收藏       │  │  - 通过/驳回  │
│  - 发布到广场│  │  - 保存为我的  │  │  - 强制下架   │
│  - AI智能替换│  │              │  │  - 查看原因   │
└─────────────┘  └──────────────┘  └──────────────┘
```

### 7.2 食谱生命周期

```
新建(idle) ──→ 编辑食物 ──→ 发布待审(pending) ──→ 审核通过(approved) ──→ 上架广场
                  │              │                      │
                  │              │←── 撤回(unpublish) ←─┘
                  │              │
                  │              └──→ 审核驳回(rejected) ──→ 查看原因 ──→ 修改后重新发布
                  │
                  └──→ 开始执行(active) ──→ 执行完成(archived)
```

### 7.3 关键设计决策

1. **营养值不由AI算**：用户从食物库选食物填克数，热量与三大营养素一律按食物库每100g的值换算。AI只在「替换」里做候选排序，且是可选的。

2. **今日进度不存状态位**：直接看今天 `diet_record` 里有没有对应餐次记录，避免"食谱说吃了、记录里没有"的两套账。

3. **已上架食谱被改动要重新审核**：否则审核过的内容可以被悄悄换成别的，广场就成了绕过审核的后门。

4. **缩短天数不删数据**：改回长天数时原来的安排还在，只是暂时不显示、也不计入统计。

5. **营养值快照**：`meal_plan_item` 里的营养值是按克数换算后的快照，不是每100g的值。食物名、单位也一起快照，这样管理员后续改名甚至删掉某个食物，用户已采用的方案也不会显示成空白。

### 7.4 API接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/meal-plan` | 新建食谱 |
| GET | `/api/meal-plan` | 我的食谱列表 |
| GET | `/api/meal-plan/current` | 当前执行中食谱+今日进度 |
| GET | `/api/meal-plan/{id}` | 食谱详情 |
| PUT | `/api/meal-plan/{id}` | 修改食谱基本信息 |
| DELETE | `/api/meal-plan/{id}` | 删除食谱 |
| POST | `/api/meal-plan/{id}/apply` | 开始执行 |
| POST | `/api/meal-plan/{id}/items` | 添加食物条目 |
| PUT | `/api/meal-plan/{planId}/item/{itemId}` | 修改/替换食物 |
| DELETE | `/api/meal-plan/{planId}/item/{itemId}` | 移除食物 |
| POST | `/api/meal-plan/{id}/copy-day` | 复制某天到其他天 |
| GET | `/api/meal-plan/{planId}/item/{itemId}/candidates` | 手动替换候选 |
| GET | `/api/meal-plan/{planId}/item/{itemId}/alternatives` | AI智能替换候选 |
| POST | `/api/meal-plan/{id}/cover` | 上传封面图 |
| POST | `/api/meal-plan/{id}/publish` | 发布到广场 |
| POST | `/api/meal-plan/{id}/unpublish` | 下架/撤回 |
| GET | `/api/meal-plan/square` | 广场列表 |
| GET | `/api/meal-plan/square/{id}` | 广场食谱详情 |
| POST | `/api/meal-plan/square/{id}/favorite` | 收藏 |
| DELETE | `/api/meal-plan/square/{id}/favorite` | 取消收藏 |
| POST | `/api/meal-plan/square/{id}/copy` | 保存为我的食谱 |
| GET | `/api/admin/meal-plans` | 管理员-按状态分页 |
| POST | `/api/admin/meal-plans/{id}/approve` | 管理员-审核通过 |
| POST | `/api/admin/meal-plans/{id}/reject` | 管理员-审核驳回 |
| POST | `/api/admin/meal-plans/{id}/takedown` | 管理员-强制下架 |

---

## 八、AI模型配置指南

### 8.1 当前使用的模型

系统默认配置为 **`auto` 模式**：

- 如果配置了 `API_KEY`，使用 **云端API**（默认 DeepSeek）
- 如果没有配置 `API_KEY`，使用 **本地 Ollama**（默认 qwen2.5:7b）

### 8.2 配置文件位置

**唯一需要修改的文件：`ai-service/.env`**（从 `.env.example` 复制）

```
ai-service/
├── .env.example    ← 模板（已提交到GitHub）
└── .env            ← 实际配置（已在.gitignore中，不会提交）
```

### 8.3 切换到云端API（推荐）

1. 复制配置模板：
   ```bash
   cp ai-service/.env.example ai-service/.env
   ```

2. 编辑 `ai-service/.env`，填入你的API Key：
   ```env
   LLM_BACKEND=auto
   
   # DeepSeek（默认，便宜、中文好）
   API_KEY=sk-xxxxxxxxxxxxxxxx
   API_BASE_URL=https://api.deepseek.com
   API_MODEL=deepseek-flash
   ```

3. 重启AI服务即可生效

### 8.4 支持的云端模型

| 厂商 | `API_BASE_URL` | `API_MODEL` | 说明 |
|------|----------------|-------------|------|
| **DeepSeek** | `https://api.deepseek.com` | `deepseek-flash` | 默认，便宜快速 |
| DeepSeek | `https://api.deepseek.com` | `deepseek-v4-pro` | 更强推理，约3倍价格 |
| 火山方舟/豆包 | `https://ark.cn-beijing.volces.com/api/v3` | 控制台模型ID | 国内速度快 |
| 阿里百炼/通义 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` | 阿里云 |
| Moonshot/Kimi | `https://api.moonshot.cn/v1` | `moonshot-v1-8k` | 长上下文 |
| 智谱/GLM | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` | 智谱AI |

> 所有接口都兼容 OpenAI 格式，只需改 `API_BASE_URL` + `API_MODEL` + `API_KEY` 即可切换。

### 8.5 切换到本地Ollama

```env
LLM_BACKEND=ollama
OLLAMA_HOST=http://localhost:11434
```

Ollama会按以下优先级自动检测已安装的模型：
1. `qwen2.5:7b` → 2. `qwen2.5:latest` → 3. `qwen2.5:3b` → 4. `qwen2:7b` → 5. 任意已安装模型

### 8.6 调整模型参数

在 `ai-service/.env` 中：

```env
API_TEMPERATURE=0.6     # 生成随机性，0=确定性，1=最随机
API_MAX_TOKENS=2048     # 单次最大输出token数
API_TIMEOUT=120         # 请求超时秒数
```

或在 `ai-service/config/settings.py` 中修改 `MODEL_OPTIONS`：

```python
MODEL_OPTIONS = {
    "temperature": 0.6,
    "num_ctx": 16384,      # 上下文窗口大小
    "top_p": 0.9,
    "repeat_penalty": 1.1,
}
```

### 8.7 Spring Boot侧AI配置

在 `src/main/resources/application.yml` 中：

```yaml
ai-service:
  url: http://localhost:8000    # AI服务地址
  connect-timeout: 10           # 连接超时(秒)
  read-timeout: 300             # 读取超时(秒)，AI响应慢需设长
  pool:
    max-total: 50               # 连接池最大连接数
    max-per-route: 20           # 每个路由最大连接数
```

---

## 九、性能优化总结

| 优化项 | 方案 | 效果 |
|--------|------|------|
| RestTemplate连接池 | Apache HttpClient5，复用TCP连接 | AI响应速度提升50%+ |
| Spring Cache + Caffeine | 食物库查询结果缓存 | 重复查询减少90%+ |
| N+1查询优化 | 批量查询替代循环单条查询 | 数据库操作减少80%+ |
| 事务拆分 | AI调用期间不持有数据库锁 | 避免长事务阻塞 |
| 上下文压缩 | 限制历史对话轮次(最多10轮) | AI输入token减少60%+ |
| 异常统一处理 | RuntimeException → BusinessException | 消除信息泄露风险 |

---

## 十、安全机制

| 机制 | 实现 |
|------|------|
| 认证 | JWT无状态Token，24小时过期 |
| 授权 | Spring Security，`USER`/`ADMIN`角色 |
| 密码 | BCrypt加密存储 |
| CORS | Spring Boot配置白名单 |
| API Key | `.env`文件存储，`.gitignore`排除，日志脱敏 |
| 逻辑删除 | `deleted`字段标记，不物理删除数据 |
| 食谱审核 | 发布需管理员审核，已上架改动需重新审核 |

---

## 十一、本地开发启动

### 前置条件

- JDK 25+
- Node.js 20+
- Python 3.12+
- MySQL 8.x
- Ollama（可选，使用云端API则不需要）

### 启动步骤

```bash
# 1. 数据库
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS diet_ai_planner DEFAULT CHARSET utf8mb4"

# 2. 后端
mvn spring-boot:run

# 3. AI服务
cd ai-service
pip install -r requirements.txt
cp .env.example .env   # 编辑填入API_KEY
python main.py

# 4. 前端
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 即可使用。

---

## 十二、项目文件统计

| 层级 | 文件数 | 说明 |
|------|--------|------|
| Java Entity | 12 | 数据库实体 |
| Java Repository | 11 | 数据访问 |
| Java Service | 12 | 业务逻辑 |
| Java Controller | 14 | API接口（含admin） |
| Java DTO | 25+ | 请求/响应对象 |
| Vue页面 | 20+ | 用户端+管理端页面 |
| Vue组件 | 5+ | 公共组件 |
| Python模块 | 12 | AI服务 |
| SQL脚本 | 4 | 初始化+迁移 |