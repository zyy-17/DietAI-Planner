# 🍽️ DietAI-Planner — AI 个性化膳食规划与饮食健康管理系统

<p align="center">
  <b>基于 Spring Boot + Vue 3 + FastAPI 的全栈智能饮食管理平台</b><br/>
  科学饮食 · 精准营养 · AI 赋能
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

- 📊 **记录每日饮食**：早餐 / 午餐 / 晚餐 / 加餐，支持一次添加多种食物
- 🎯 **自定义营养目标**：热量、蛋白质、碳水、脂肪四项目标自由设定
- 🤖 **AI 膳食建议**：基于 Ollama 本地大模型，结合用户画像生成个性化建议
- 📈 **营养分析**：今日营养、趋势图、目标完成度、营养报告
- 🍱 **食物库管理**：内置 30+ 常见食物，支持搜索、分类、用户添加
- 👤 **个人中心**：身体数据、饮食偏好、忌口设置、活动水平
- ⚙️ **系统设置**：通知提醒、界面主题、隐私控制、密码修改
- 🛡️ **管理后台**：用户管理、食物审核、分类管理、AI 日志监控

---

## 🌍 项目背景

随着健康意识提升，越来越多人关注日常饮食的营养搭配。传统饮食记录 App 存在以下痛点：

| 痛点 | 本项目解决方案 |
|------|---------------|
| 手动查食物营养太麻烦 | 内置食物库 + 搜索，一键添加 |
| 不知道该吃什么 | AI 根据用户画像 + 剩余目标推荐 |
| 目标设置不灵活 | 四大营养素目标可自由调整，支持手动加减 |
| 记录效率低 | 支持一次选择多种食物批量添加 |
| 缺乏长期趋势分析 | 多维度营养趋势图 + 目标完成度 |

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 25 | 编程语言 |
| Spring Boot | 4.1.1 | 应用框架 |
| Spring Security | — | 认证授权 |
| Spring Data JPA | — | ORM 持久层 |
| MySQL | 8.0 | 关系型数据库 |
| JWT (jjwt) | 0.12.6 | 无状态认证令牌 |
| Lombok | — | 减少样板代码 |
| BCrypt | — | 密码加密 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.x | 渐进式前端框架 |
| Vue Router | 4.4.x | 路由管理 |
| Pinia | 2.2.x | 状态管理 |
| Element Plus | 2.8.x | UI 组件库 |
| ECharts | 5.5.x | 数据可视化 |
| Axios | 1.7.x | HTTP 客户端 |
| Day.js | 1.11.x | 日期处理 |
| Vite | 5.4.x | 构建工具 |

### AI 服务

| 技术 | 版本 | 说明 |
|------|------|------|
| Python | 3.10+ | 编程语言 |
| FastAPI | 0.115.0 | 高性能 Web 框架 |
| Uvicorn | 0.30.6 | ASGI 服务器 |
| Pydantic | 2.9.2 | 数据校验 |
| Ollama | — | 本地大模型推理（默认 qwen2.5-coder:7b） |

---

## 🧩 功能模块

### 用户端

```
🏠 今日饮食
   ├─ 🏠 今日概览        每日摄入总览、目标进度、快速记录
   ├─ 🍳 早餐            按餐次记录食物（支持多选批量添加）
   ├─ 🥗 午餐
   ├─ 🍗 晚餐
   ├─ 🍎 加餐
   └─ 🤖 AI今日建议      基于今日摄入的个性化建议

📋 饮食记录
   ├─ 📅 历史记录        按日期浏览饮食记录
   ├─ 📆 周记录          按周汇总
   ├─ 🗓️ 月记录          按月汇总
   ├─ 🔍 条件查询        按日期范围筛选
   └─ 📊 饮食统计        总记录天数、总热量统计

📊 营养分析
   ├─ 📊 今日营养        四大营养素摄入详情
   ├─ 📈 营养趋势        近7天/30天趋势折线图
   ├─ 🥩 营养素分析      蛋白质/碳水/脂肪占比分析
   ├─ 🔥 热量分析        每餐热量分布
   ├─ 🎯 目标完成度      目标达成百分比
   └─ 📄 营养报告        综合营养报告

🤖 AI对话
   ├─ 💬 新建对话        自由对话
   ├─ 🥗 膳食规划        预设：膳食规划
   ├─ 🔥 减脂方案        预设：减脂方案
   ├─ 💪 增肌方案        预设：增肌方案
   ├─ 🍎 饮食咨询        预设：饮食咨询
   └─ 🕘 历史对话        查看历史会话

🍱 食物库（侧边栏辅助）
   ├─ 🍎 全部食物        浏览所有食物
   ├─ 🔍 食物搜索        按名称搜索
   ├─ 🥩 食物分类        按分类浏览
   └─ ➕ 添加食物        用户提交新食物

👤 个人中心（侧边栏辅助）
   ├─ 👤 基本资料        姓名/性别/生日
   ├─ 📏 身体数据        身高/体重
   ├─ 🎯 饮食目标        减脂/维持/增肌
   ├─ 🥗 饮食偏好        清淡/素食/低糖等
   ├─ 🚫 忌口设置
   ├─ 🏃 活动水平        久坐~重度运动5级
   └─ ❤️ 健康信息

⚙️ 系统设置（侧边栏辅助）
   ├─ 🔔 通知设置        饮食提醒/目标提醒/AI推送
   ├─ 🎨 界面设置        主题/语言/侧边栏
   ├─ 🔐 隐私设置        数据共享/记录公开
   └─ 🔑 修改密码
```

### 管理端

```
🛡️ 后台管理
   ├─ 👥 用户管理        列表/禁用/删除
   ├─ 🍱 食物管理        列表/编辑/审核/删除
   ├─ 📂 分类管理        增删改分类
   └─ 🤖 AI日志          生成记录/异常标记
```

---

## 📁 项目结构

```
DietAI-Planner/
│
├── src/main/java/com/zyyqq/          # ☕ Spring Boot 后端
│   ├── entity/                        # JPA 实体类（8 张表）
│   │   ├── User.java                  # 用户
│   │   ├── Food.java                  # 食物
│   │   ├── FoodCategory.java          # 食物分类
│   │   ├── DietRecord.java            # 饮食记录
│   │   ├── AiChatSession.java         # AI 会话
│   │   ├── AiChatMessage.java         # AI 消息
│   │   ├── AiGenerationLog.java       # AI 生成日志
│   │   └── NutritionStandard.java     # 营养标准配置
│   ├── repository/                    # 数据访问层
│   ├── service/                       # 业务逻辑层
│   │   ├── UserService.java           # 用户服务（BMR/TDEE/目标计算）
│   │   ├── DietRecordService.java     # 饮食记录服务
│   │   ├── FoodService.java           # 食物服务
│   │   └── AiChatService.java         # AI 对话服务
│   ├── controller/                    # API 控制器
│   │   ├── AuthController.java        # 认证（注册/登录）
│   │   ├── UserController.java        # 用户（资料/设置/密码）
│   │   ├── TodayDietController.java   # 今日饮食
│   │   ├── FoodController.java        # 食物
│   │   ├── FoodCategoryController.java# 分类
│   │   ├── AiChatController.java      # AI 对话
│   │   ├── NutritionAnalysisController.java # 营养分析
│   │   └── admin/                     # 管理员控制器
│   ├── dto/
│   │   ├── request/                   # 请求 DTO
│   │   └── response/                  # 响应 DTO
│   ├── config/                        # 配置类（CORS/MVC）
│   ├── security/                      # JWT 安全组件
│   └── exception/                     # 全局异常处理
│
├── frontend/                          # 🖥️ Vue 3 前端
│   └── src/
│       ├── views/                     # 页面组件
│       │   ├── Login.vue              # 登录页
│       │   ├── TodayDiet.vue          # 今日概览
│       │   ├── MealDetail.vue         # 餐次详情（早/午/晚/加餐）
│       │   ├── DietRecords.vue        # 饮食记录
│       │   ├── DietStats.vue          # 饮食统计
│       │   ├── NutritionAnalysis.vue  # 营养分析
│       │   ├── AiChat.vue             # AI 对话
│       │   ├── AiChatHistory.vue      # 历史对话
│       │   ├── AiSuggest.vue          # AI 今日建议
│       │   ├── FoodLibrary.vue        # 食物库
│       │   ├── Profile.vue            # 个人中心
│       │   ├── Settings.vue           # 系统设置
│       │   └── admin/                 # 管理员页面
│       ├── layouts/
│       │   └── MainLayout.vue         # 主布局（顶栏+侧栏+内容区）
│       ├── router/
│       │   └── index.js               # 路由配置
│       ├── stores/
│       │   └── user.js                # 用户状态（Pinia）
│       └── utils/
│           └── api.js                 # Axios 封装
│
├── ai-service/                        # 🐍 Python AI 服务
│   ├── main.py                        # FastAPI 主程序
│   └── requirements.txt               # Python 依赖
│
└── src/main/resources/
    ├── application.yml                # 后端配置
    └── db/
        └── init.sql                   # 数据库初始化脚本
```

---

## 🗄️ 数据库设计

### 数据库：`diet_ai_planner`（MySQL 8.0，utf8mb4）

### 表结构总览

| # | 表名 | 说明 | 核心字段 |
|---|------|------|---------|
| 1 | `user` | 用户表 | id, username, email, password_hash, 身体数据, 饮食目标, 设置项 |
| 2 | `food_category` | 食物分类表 | id, name, parent_id, sort_order |
| 3 | `food` | 食物表 | id, name, category_id, calories, protein, carbohydrate, fat, fiber |
| 4 | `diet_record` | 饮食记录表 | id, user_id, food_id, meal_type, amount, record_date |
| 5 | `ai_chat_session` | AI 会话表 | id, user_id, title |
| 6 | `ai_chat_message` | AI 消息表 | id, session_id, user_id, role, content |
| 7 | `ai_generation_log` | AI 生成日志表 | id, user_id, type, model_name, tokens_used |
| 8 | `nutrition_standard` | 营养标准表 | id, gender, age_min, age_max, calories_kcal |

### ER 关系图

```
┌──────────┐       ┌──────────────┐       ┌──────────┐
│   user   │1─────N│ diet_record  │N─────1│   food   │
│          │       │              │       │          │
│ id (PK)  │       │ user_id (FK) │       │ id (PK)  │
│ username │       │ food_id (FK) │       │ name     │
│ email    │       │ meal_type    │       │ calories │
│ ...      │       │ amount       │       │ protein  │
│ settings │       │ record_date  │       │ ...      │
└──────────┘       └──────────────┘       └──────────┘
     │                                          │
     │1                                         │N
     │                                          │
     N                                          1
┌──────────────┐                        ┌──────────────┐
│ai_chat_session│                       │food_category │
│              │                        │              │
│ id (PK)      │                        │ id (PK)      │
│ user_id (FK) │                        │ name         │
│ title        │                        │ parent_id    │
└──────────────┘                        └──────────────┘
     │1
     │
     N
┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐
│ai_chat_message│       │ai_generation_log │       │nutrition_standard│
│              │       │                  │       │                  │
│ id (PK)      │       │ id (PK)          │       │ id (PK)          │
│ session_id   │       │ user_id (FK)     │       │ gender           │
│ role         │       │ type             │       │ age_min/max      │
│ content      │       │ model_name       │       │ calories_kcal    │
└──────────────┘       └──────────────────┘       └──────────────────┘
```

### 核心关系说明

- **user → diet_record**：一对多，一个用户有多条饮食记录
- **food → diet_record**：一对多，一个食物可出现在多条记录中
- **food_category → food**：一对多，一个分类下有多个食物
- **user → ai_chat_session**：一对多，一个用户有多个 AI 会话
- **ai_chat_session → ai_chat_message**：一对多，一个会话包含多条消息
- **user → ai_generation_log**：一对多，一个用户有多条 AI 生成记录
- **nutrition_standard**：独立配置表，按性别+年龄段提供推荐摄入量

### user 表详细字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| username | VARCHAR(50) UK | 用户名 |
| email | VARCHAR(100) UK | 邮箱 |
| password_hash | VARCHAR(100) | BCrypt 加密密码 |
| real_name | VARCHAR(50) | 真实姓名 |
| gender | TINYINT | 0未知/1男/2女 |
| birth_date | DATE | 出生日期 |
| height | DECIMAL(5,1) | 身高 cm |
| weight | DECIMAL(5,1) | 体重 kg |
| activity_level | TINYINT | 活动水平 1-5 |
| diet_goal | VARCHAR(20) | lose/maintain/gain |
| diet_preference | VARCHAR(200) | 饮食偏好 |
| target_calories | DECIMAL(7,2) | 自定义热量目标 kcal |
| target_protein | DECIMAL(7,2) | 自定义蛋白质目标 g |
| target_carbohydrate | DECIMAL(7,2) | 自定义碳水目标 g |
| target_fat | DECIMAL(7,2) | 自定义脂肪目标 g |
| diet_reminder | TINYINT(1) | 饮食提醒开关 |
| reminder_time | VARCHAR(5) | 提醒时间 HH:mm |
| goal_reminder | TINYINT(1) | 目标提醒开关 |
| ai_suggestion | TINYINT(1) | AI 建议推送开关 |
| theme | VARCHAR(10) | 主题 light/dark/auto |
| language | VARCHAR(10) | 语言 |
| collapsed_sidebar | TINYINT(1) | 侧边栏收起 |
| data_sharing | TINYINT(1) | 数据共享开关 |
| public_records | TINYINT(1) | 饮食记录公开开关 |
| avatar_url | VARCHAR(255) | 头像 URL |
| role | VARCHAR(20) | 角色 user/admin |
| status | TINYINT | 1正常/0禁用 |
| created_at | DATETIME | 注册时间 |
| updated_at | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除 0/1 |

---

## 🎨 前端设计

### 导航架构：一级导航 + 二级导航

```
┌─────────────────────────────────────────────────────────┐
│  顶部导航栏：🏠今日饮食  📋饮食记录  📊营养分析  🤖AI对话  │
├──────────┬──────────────────────────────────────────────┤
│ 左侧边栏  │  主内容区                                     │
│          │                                              │
│ 二级菜单  │  ┌──────────────────────────────────────┐   │
│ (动态)   │  │ 标签页导航（二级功能切换）              │   │
│          │  ├──────────────────────────────────────┤   │
│ ──────── │  │                                      │   │
│ 🍱食物库  │  │  页面内容                             │   │
│ 👤个人中心│  │                                      │   │
│ ⚙️系统设置│  └──────────────────────────────────────┘   │
└──────────┴──────────────────────────────────────────────┘
```

- **顶部导航**：4 个核心一级模块，始终可见
- **左侧二级菜单**：根据当前一级模块动态切换
- **侧边栏辅助区域**：食物库 / 个人中心 / 系统设置固定在底部，不随一级模块变化
- **主区域标签页**：辅助模块的子功能通过顶部标签页切换，无需展开折叠

### 技术要点

| 方面 | 实现 |
|------|------|
| 路由 | Vue Router 4，嵌套路由 + meta 参数实现同组件多模式 |
| 状态管理 | Pinia，存储用户登录态和 token |
| HTTP 请求 | Axios 封装，自动携带 JWT，统一错误处理 |
| 代理 | Vite devServer proxy，`/api` → `localhost:8080` |
| 组件库 | Element Plus，按需自动导入 |
| 图表 | ECharts 5，营养趋势图、目标完成度环形图 |
| 布局 | MainLayout.vue，顶栏 + 侧栏 + 内容区响应式 |

---

## 🔌 后端 API 接口

### 认证模块 `/api/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/register` | 用户注册 | ❌ |
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
| GET | `/stats` | 饮食统计（总天数/总热量） | ✅ |

### 食物模块 `/api/foods`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/` | 分页获取食物列表 | ✅ |
| GET | `/{id}` | 获取食物详情 | ✅ |
| GET | `/search` | 按名称搜索食物 | ✅ |
| GET | `/all` | 获取全部食物 | ✅ |
| POST | `/` | 添加新食物 | ✅ |

### 分类模块 `/api/categories`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/` | 获取所有分类 | ✅ |
| GET | `/{parentId}/sub` | 获取子分类 | ✅ |

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
| GET | `/analysis` | 获取营养分析数据 | ✅ |

### 管理员模块 `/api/admin`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
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
| GET | `/ai-logs` | AI 生成日志 | 🔒 Admin |
| PUT | `/ai-logs/{id}/abnormal` | 标记异常 | 🔒 Admin |

### AI 服务接口（FastAPI `localhost:8000`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 单轮对话 |
| POST | `/api/chat/history` | 多轮对话（带历史上下文） |
| POST | `/api/diet-plan` | 生成膳食规划 |
| GET | `/api/health` | 健康检查 |

---

## 🤖 AI 服务详解

### 架构概览

```
用户提问 → Spring Boot 后端 → FastAPI AI 服务 → Ollama 本地模型 → 返回结果
                │                    │
                │                    ├─ 成功：返回 AI 生成内容
                │                    └─ 失败：返回 fallback 预设回复
                │
                ├─ 保存会话和消息到数据库
                └─ 记录 AI 生成日志（token 消耗、异常标记）
```

### AI 服务技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Python | 3.10+ | 运行环境 |
| FastAPI | 0.115.0 | 高性能异步 Web 框架 |
| Uvicorn | 0.30.6 | ASGI 服务器，监听 `0.0.0.0:8000` |
| Pydantic | 2.9.2 | 请求/响应数据校验 |
| Ollama Python SDK | latest | 调用本地 Ollama 推理服务 |
| Ollama | 最新 | 本地大模型运行时 |
| 默认模型 | qwen2.5-coder:7b | 通义千问 2.5 编码版 7B 参数 |

### AI 服务文件结构

```
ai-service/
├── main.py              # FastAPI 主程序（所有逻辑）
└── requirements.txt     # Python 依赖清单
```

### 核心 System Prompt

AI 助手名为 **"智慧膳食"**，具备以下能力：

1. 根据用户健康数据（身高/体重/年龄/活动水平/饮食目标/饮食偏好）提供个性化膳食建议
2. 分析每日营养摄入是否合理，指出营养缺口和改进方向
3. 推荐适合用户目标（减脂/维持/增肌）的食物和食谱，优先考虑饮食偏好
4. 回答食物营养、热量、健康饮食相关问题
5. 结合剩余热量和营养素缺口给出精准建议

**个性化规则**：
- 根据性别、年龄、体重、身高给出针对性建议
- 根据饮食偏好调整推荐（偏好清淡→少油少盐，偏好中式→中式食谱）
- 根据活动水平判断热量需求（久坐人群适当降低碳水比例）
- 严格遵守用户饮食偏好（素食、低糖、无辣等）
- 营养均衡推荐比例：碳水 50%、蛋白质 20%、脂肪 30%

### 接口详细说明

#### POST `/api/chat` — 单轮对话

请求体：
```json
{
  "message": "我今天还能吃什么？",
  "context": "性别:男, 身高:175cm, 体重:70kg, 目标:减脂, 今日已摄入:1200kcal, 剩余:400kcal",
  "session_id": 1,
  "user_id": 1
}
```

响应：
```json
{
  "response": "根据您今日剩余400kcal，建议...",
  "session_id": 1
}
```

> `context` 由 Spring Boot 后端根据用户画像 + 今日摄入自动构建，传入 AI 服务

#### POST `/api/chat/history` — 多轮对话

请求体：
```json
{
  "message": "那晚餐呢？",
  "context": "...",
  "session_id": 1,
  "user_id": 1,
  "history": [
    {"role": "user", "content": "我今天还能吃什么？"},
    {"role": "assistant", "content": "根据您今日剩余400kcal，建议..."}
  ]
}
```

> `history` 携带历史对话，AI 基于上下文连贯回答

#### POST `/api/diet-plan` — 生成膳食规划

请求体：
```json
{
  "user_id": 1,
  "target_calories": 2000,
  "diet_goal": "lose",
  "remaining_calories": 800
}
```

响应：
```json
{
  "target_calories": 1600,
  "ai_plan": "AI 生成的详细膳食计划文本...",
  "meals": {
    "breakfast": {"calories": 480, "suggestion": "燕麦+鸡蛋+牛奶"},
    "lunch": {"calories": 640, "suggestion": "糙米饭+鸡胸肉+蔬菜"},
    "dinner": {"calories": 400, "suggestion": "清蒸鱼+蔬菜+少量主食"},
    "snack": {"calories": 80, "suggestion": "水果或坚果"}
  }
}
```

> `diet_goal` 为 `lose` 时自动乘 0.8，`gain` 时乘 1.15

#### GET `/api/health` — 健康检查

响应：
```json
{
  "status": "ok",
  "service": "DietAI AI Service",
  "model": "qwen2.5-coder:7b"
}
```

### AI 对话用户体验优化

#### 智能加载状态
- ✅ **打字机效果**："正在思考中，请稍候..." 逐字显示 + 闪烁光标 `|`
- ✅ **实时等待计时**：显示 `⏱️ 已等待 X 秒`，每秒更新
- ✅ **取消请求按钮**：支持中断正在进行的 AI 调用（`AbortController`）
- ✅ **优雅降级**：组件卸载时自动清理定时器，防止内存泄漏

#### 超时保护机制
为适配 Ollama 本地模型生成时间（通常 5-30 秒），系统配置了完整的超时保护：

| 组件 | 超时设置 | 说明 |
|------|---------|------|
| 前端 Axios | **120 秒** | HTTP 请求总超时 |
| 后端 RestTemplate 连接 | **10 秒** | 连接 AI 服务超时 |
| 后端 RestTemplate 读取 | **120 秒** | 等待 AI 响应超时 |

#### AI 响应时间参考

| 场景 | 预计响应时间 | 说明 |
|------|-------------|------|
| 简单问题（如"你好"） | 2-5 秒 | 模型已加载到内存 |
| 复杂问题（如"制定一周减脂餐"） | 10-30 秒 | 需要生成较长文本 |
| 首次调用（冷启动） | 30-60 秒 | 模型需从磁盘加载到内存 |
| 超过 120 秒 | 自动超时 | 显示错误提示，建议重试 |

#### 技术实现要点
- 使用 `AbortController` 实现请求取消（现代浏览器原生支持）
- `setInterval` 实现打字机动画（100ms/字符）和实时计时（1s/次）
- CSS `@keyframes` 实现光标闪烁动画（1秒循环）
- 组件 `onUnmounted` 生命周期清理所有定时器和未完成请求

### Markdown 渲染支持

#### 背景
通义千问（Qwen）、Llama、ChatGPT 等现代大语言模型**倾向于使用 Markdown 格式**输出结构化内容：
- `**文字**` → 加粗
- `*文字*` → 斜体
- `# 标题` → 标题
- `- 列表项` → 无序列表
- `` `代码` `` → 行内代码

#### 解决方案
前端使用 **`marked` + `DOMPurify`** 组合实现安全的 Markdown 渲染：

| 技术 | 作用 | 安全性 |
|------|------|--------|
| **marked** | 将 Markdown 文本转换为 HTML | ⚠️ 可能包含危险标签 |
| **DOMPurify** | 清理 HTML 防止 XSS 攻击 | ✅ 移除 `<script>`、`onclick` 等 |

#### 支持的 Markdown 语法

| 语法 | 渲染效果 | 示例 |
|------|---------|------|
| `**text**` | **加粗** | **核心原因** |
| `*text*` | *斜体* | *重要提示* |
| `# ## ###` | 标题（H1-H3） | 章节标题 |
| `- item` / `1. item` | 列表 | • 第一项 |
| `` `code` `` | 行内代码 | `const x = 1` |
| ```code``` | 代码块 | 多行代码 |
| `> quote` | 引用块 | 重要说明 |
| `[text](url)` | 超链接 | 点击跳转 |

#### 视觉效果对比

**优化前** ❌：
```
🤖 **财政危机**：法国在18世纪中面临严重的财政问题...
   **贵族与教士的特权**：贵族和教会享有免税权...
```
（用户看到原始的 `**` 符号，体验不佳）

**优化后** ✅：
```
🤖 **财政危机**：法国在18世纪中面临严重的财政问题...
   **贵族与教士的特权**：贵族和教会享有免税权...
```
（真正的**加粗**效果，专业美观）

#### 技术实现
```javascript
// AiChat.vue - Markdown 渲染函数
import { marked } from 'marked'
import DOMPurify from 'dompurify'

function renderMarkdown(content) {
  const html = marked(content)           // Markdown → HTML
  return DOMPurify.sanitize(html)        // 清理XSS攻击
}

// 模板中使用 v-html 渲染
<div class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
```

#### 依赖包信息

| 包名 | 版本 | 大小（gzip） | 用途 |
|------|------|-------------|------|
| **marked** | latest | ~10 KB | Markdown 解析器 |
| **dompurify** | latest | ~8 KB | HTML 消毒剂 |
| **总计** | — | **~18 KB** | 可接受范围 |

### AI 回复耗时显示

#### 功能说明
每条 AI 回复消息的底部会显示**实际响应耗时**，帮助用户了解 AI 处理速度。

#### 时间格式规则

| 耗时范围 | 显示格式 | 示例 |
|---------|---------|------|
| < 60 秒 | `X秒` | `⏱️ 回复耗时：15秒` |
| ≥ 60 秒 | `X分X秒` | `⏱️ 回复耗时：1分23秒` |
| ≥ 3600 秒 | `X小时X分X秒` | （暂未实现，可扩展） |

#### 显示位置与样式
- **位置**：AI 回复消息最下方
- **分隔符**：虚线边框（`border-top: 1px dashed`）
- **对齐方式**：右对齐
- **字体颜色**：灰色次要文本（`#909399`）
- **字体大小**：12px

#### 界面效果示例
```
┌──────────────────────────────────────┐
│ 🤖                                   │
│ ┌──────────────────────────────────┐ │
│ │ 根据您的问题，分析如下：          │ │
│ │                                  │ │
│ │ **核心原因**：                    │ │
│ │ 1. **财政危机**                 │ │
│ │ 2. **社会不平等**               │ │
│ │                                  │ │
│ │ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │ │
│ │     ⏱️ 回复耗时：1分23秒         │ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

#### 技术实现
```javascript
// 记录请求开始时间
requestStartTime = Date.now()

// AI回复后计算耗时（秒）
const duration = Math.round((Date.now() - requestStartTime) / 1000)

// 格式化显示
function formatDuration(seconds) {
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}分${remainingSeconds}秒`
}
```

#### 性能影响
- ✅ 无额外网络请求（纯前端计算）
- ✅ 内存占用极小（仅存储一个时间戳）
- ✅ 不影响AI响应速度

### Fallback 降级机制

当 Ollama 服务不可用或调用失败时，AI 服务会自动降级到 **预设回复**：

| 用户消息关键词 | 降级回复内容 |
|---------------|-------------|
| 吃什么/推荐/食谱 | 通用膳食搭配建议 |
| 热量/卡路里 | BMR/TDEE 计算公式说明 |
| 减脂/减肥/瘦 | 减脂核心原则 |
| 增肌/肌肉 | 增肌核心原则 |
| 其他 | 通用欢迎语 |

> 降级响应中会附带 `"fallback": true` 和 `"error": "异常信息"` 字段

### 后端调用 AI 服务的流程

```
AiChatService.send(userId, message)
  │
  ├─ 1. 构建 context：用户画像 + 今日摄入 + 剩余目标
  ├─ 2. 调用 FastAPI /api/chat/history（带历史消息）
  ├─ 3. 保存用户消息到 ai_chat_message 表
  ├─ 4. 保存 AI 回复到 ai_chat_message 表
  ├─ 5. 记录到 ai_generation_log 表（token 消耗、是否异常）
  └─ 6. 返回 AI 回复给前端
```

### 如何更换 AI 模型

编辑 `ai-service/main.py` 第 16 行：

```python
OLLAMA_MODEL = "qwen2.5-coder:7b"  # 改为你想用的模型
```

常用可选模型：

| 模型 | 参数量 | 说明 | 拉取命令 |
|------|--------|------|---------|
| qwen2.5-coder:7b | 7B | 默认，中文优秀 | `ollama pull qwen2.5-coder:7b` |
| qwen2.5:7b | 7B | 通义千问通用版 | `ollama pull qwen2.5:7b` |
| llama3.1:8b | 8B | Meta Llama 3.1 | `ollama pull llama3.1:8b` |
| mistral:7b | 7B | Mistral 7B | `ollama pull mistral:7b` |
| glm4:9b | 9B | 智谱 GLM-4 | `ollama pull glm4:9b` |

> 更换模型后需重启 AI 服务：`python main.py`

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
| Ollama | 最新 | [ollama.com](https://ollama.com/) |

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
- 创建 8 张表
- 插入管理员账号（admin / admin123）
- 插入 10 个食物分类
- 插入 30+ 常见食物数据
- 插入 6 条营养标准配置

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
```

后端启动成功后访问：`http://localhost:8080`

### 第五步：启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动成功后访问：`http://localhost:3000`

> Vite 开发服务器已配置代理：`/api` → `localhost:8080`，无需额外配置跨域

### 第六步：启动 AI 服务（可选）

> ⚠️ **重要提示**：AI 对话功能依赖以下三个服务同时运行：
> - Spring Boot 后端（端口 `8080`）
> - Vue 前端（端口 `3000` 或 `5173`）
> - Python AI 服务（端口 `8000`）
>
> 缺少任一服务，AI 对话功能将无法正常工作

1. 安装 Ollama 并拉取模型：

```bash
ollama pull qwen2.5-coder:7b
```

2. 启动 AI 服务：

```bash
cd ai-service
pip install -r requirements.txt
python main.py
```

AI 服务启动后运行在：`http://localhost:8000`

启动成功后会看到：
```
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
INFO:     模型已检测到: qwen2.5-coder:7b
```

3. **验证 AI 服务是否正常**：

打开浏览器访问 `http://localhost:8000/docs`，应能看到 FastAPI 自动生成的 API 文档页面。

> **AI 响应时间说明**：
> - 简单问题（如"你好"）：**2-5 秒**
> - 复杂问题（如"制定一周减脂餐"）：**10-30 秒**
> - 首次调用（模型冷启动）：**30-60 秒**
> - 系统已配置 **120 秒超时保护**，超过此时间将自动显示错误提示
>
> 如果不启动 AI 服务，AI 对话功能将不可用，但其他功能正常使用

### 第七步：登录系统

打开浏览器访问 `http://localhost:3000`，使用默认管理员账号登录：

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

也可以点击"注册"创建新用户。

---

## 🔑 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| admin | admin | admin123 | 系统管理员，可访问后台管理 |

---

## 📸 项目截图

> 以下为系统主要页面示意

| 页面 | 说明 |
|------|------|
| 登录页 | 用户登录 / 注册 |
| 今日概览 | 每日摄入总览、四大营养素进度、修改目标 |
| 餐次详情 | 早/午/晚/加餐食物列表，多选批量添加 |
| AI 对话 | 多轮对话，预设方案快捷入口 |
| 营养分析 | 趋势图、营养素占比、目标完成度 |
| 管理后台 | 用户管理、食物审核、AI 日志监控 |

---

## 📄 License

本项目仅供学习交流使用。