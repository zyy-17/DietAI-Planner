# DietAI-Planner

> 基于 AI 的智能饮食规划与营养管理系统

DietAI-Planner 是一个全栈智能饮食管理平台，支持用户记录每日饮食、生成 AI 膳食方案、管理个人食谱、浏览食谱广场，并通过体重追踪和营养分析辅助健康管理。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 4.1.1 |
| 安全认证 | Spring Security | - |
| 数据持久层 | JPA (Hibernate) | - |
| 数据库 | MySQL | 8.0 |
| 前端框架 | Vue 3 + Composition API | - |
| UI 组件库 | Element Plus | - |
| 构建工具 | Vite | - |
| AI 服务 | Python FastAPI | 5.1.0 |
| AI 模型 | 阿里云通义千问 | DashScope |

---

## 功能模块

### 1. 用户系统
- 注册/登录（用户名 + 明文密码登录，开发调试模式）
- 健康档案管理（身高、体重、体脂率、过敏食物、疾病、用药等）
- 角色权限：普通用户 `user` 与管理员 `admin`
- 逻辑删除（`deleted` 标记，数据不物理删除）

### 2. 饮食记录
- 按餐次（早餐/午餐/晚餐/加餐）记录每日饮食
- 支持从**公共食物库**和**用户自定义食物库**选择食物
- 每次记录保存营养快照（热量、蛋白质、碳水、脂肪）
- 按日期查看饮食汇总

### 3. 食物数据（三层架构）

```
┌─────────────────────────────────────────────┐
│  第一层：公共基础食材库 (food, food_type=ingredient) │
│  系统内置 47 种常见食材，按分类树形组织            │
│  支持用户提交新食材，管理员审核后入库              │
├─────────────────────────────────────────────┤
│  第二层：成品菜肴库 (food, food_type=dish)        │
│  系统内置 24 种常见成品菜（行业平均估算值，仅供参考）│
│  支持按"个/份/盒/杯"等计量单位记录                │
├─────────────────────────────────────────────┤
│  第三层：用户自定义食物 (user_custom_food)        │
│  与公共食物库物理隔离，仅创建者可见                │
│  支持自定义组合食谱（is_recipe=1，多食材累加营养） │
└─────────────────────────────────────────────┘
```

### 4. 计量单位支持
- 食物可配置常用计量单位（`unit_name`：个/份/盒/杯等）
- 配置单位克重（`unit_weight`），自动换算营养值
- 兼容按克数记录 + 按个数/份数记录两种方式

### 5. 食谱模块

#### 5.1 我的食谱
- AI 一键生成膳食方案（指定目标、周期、偏好、忌口）
- 手动创建和编辑食谱
- 按天组织饮食（D1 早餐/午餐/晚餐、D2 早餐...）
- 每条食谱项包含营养快照，保证历史食谱数据稳定

#### 5.2 食谱执行
- 食谱状态流转：`idle` → `active` → `paused` → `archived`
- 支持暂停/恢复执行（记录 `paused_at_day`，恢复时从该天继续）
- 软删除（`deleted` 标记，保留数据）

#### 5.3 食谱广场
- 用户可将食谱发布到公共广场
- 发布审核：`pending` 待审 → `approved` 已上架 / `rejected` 已拒绝
- 支持食谱封面图、标签、难度、预计减重效果、说明心得
- **来源溯源**：每条食谱标记 `origin`（`original` 原创 / `copied` 应用自他人）
- 应用他人食谱时自动记录来源作者信息，**禁止二次发布**（版权保护）

#### 5.4 收藏与热门
- 食谱收藏（`meal_plan_favorite` 表，用户-食谱 M:N 关联）
- 热门 Top10 排行（基于 `favorite_count` 降序）
- 被保存/应用次数统计（`usage_count`）

### 6. AI 对话助手
- 多轮对话，上下文感知（携带用户健康档案快照）
- AI 生成记录归档（`ai_generation_log`，含模型、Token 消耗、异常标记）
- 支持三种场景：闲聊对话、膳食方案生成、营养分析

### 7. 健康追踪
- 每日体重记录（`weight_record`，每人每天唯一一条）
- 体脂率跟踪
- 目标体重/体脂率/达成日期设定
- 自定义三大营养素目标

### 8. 系统配置
- **营养标准配置**（`nutrition_standard`）：按性别 + 年龄段提供推荐每日摄入量
- **可配置字典选项**（`profile_option`）：活动水平、疾病选项等下拉列表的动态配置

---

## 项目结构

```
DietAI-Planner/
├── ai-service/                  # Python AI 服务
│   ├── main.py                  # FastAPI 主入口 (v5.1.0)
│   ├── env.py                   # 环境变量与 API 密钥
│   ├── .venv/                   # Python 3.13 虚拟环境
│   └── utils/
│       └── dashscope_client.py  # 通义千问 API 封装
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── views/               # 页面组件
│       │   ├── MealPlan.vue     # 食谱页面（已完整重写）
│       │   ├── DietRecord.vue   # 饮食记录页面
│       │   ├── FoodDatabase.vue # 食物数据库页面
│       │   ├── UserProfile.vue  # 用户档案页面
│       │   └── ...
│       ├── api/                 # API 请求层
│       └── router/              # 前端路由
├── src/main/java/com/zyyqq/    # Spring Boot 后端
│   ├── controller/              # REST API 控制器
│   ├── service/                 # 业务逻辑
│   ├── entity/                  # JPA 实体
│   ├── repository/              # 数据仓库
│   └── config/
│       └── SecurityConfig.java  # 安全配置（明文密码模式）
├── src/main/resources/
│   ├── db/
│   │   └── init.sql             # 数据库初始化脚本（单文件，全部 16 张表）
│   └── application.yml          # 应用配置
└── pom.xml                      # Maven 配置
```

---

## 数据库设计

### 表概览（共 16 张表）

| # | 表名 | 说明 | 所属模块 |
|---|------|------|----------|
| 1 | `user` | 用户账号、健康档案、饮食目标、UI 偏好 | 用户系统 |
| 2 | `food_category` | 食品分类（树形结构，11 个一级分类） | 食物数据 |
| 3 | `food` | 公共食物库（基础食材 + 成品菜肴） | 食物数据 |
| 4 | `user_custom_food` | 用户自定义食物（物理隔离，仅创建者可见） | 食物数据 |
| 5 | `user_custom_food_item` | 自定义组合食谱的食材组成明细 | 食物数据 |
| 6 | `diet_record` | 每日饮食记录（含营养快照） | 饮食记录 |
| 7 | `meal_plan` | 食谱主表（方案、执行状态、发布状态、来源溯源） | 食谱模块 |
| 8 | `meal_plan_item` | 食谱明细（每天每餐具体食物及用量快照） | 食谱模块 |
| 9 | `meal_plan_review` | 食谱审核记录（管理员审核历史） | 食谱模块 |
| 10 | `meal_plan_favorite` | 食谱收藏（用户-食谱 M:N 关联） | 食谱模块 |
| 11 | `weight_record` | 体重/体脂率每日记录 | 健康追踪 |
| 12 | `ai_chat_session` | AI 对话会话 | AI 对话 |
| 13 | `ai_chat_message` | AI 对话消息 | AI 对话 |
| 14 | `ai_generation_log` | AI 生成记录（模型、Token、异常标记） | AI 对话 |
| 15 | `nutrition_standard` | 营养标准配置（按性别+年龄段） | 系统配置 |
| 16 | `profile_option` | 可配置字典选项（活动水平、疾病等） | 系统配置 |

### 关键设计原则

1. **物理隔离**：用户自定义食物与公共食物库分表存储，互不干扰
2. **营养快照**：`diet_record`、`meal_plan_item`、`user_custom_food_item` 均存储营养快照值，保证历史数据不受食物表后续变更影响
3. **逻辑删除**：`user.deleted`、`meal_plan.deleted` 采用软删除标记，保留数据
4. **来源溯源**：`meal_plan.origin` + `source_plan_id` + `source_author_id` 形成完整版权溯源链，禁止对 `copied` 食谱二次发布
5. **审计字段**：所有核心表均包含 `created_at` / `updated_at` 时间戳

---

## API 接口

### 用户
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/users/register` | 用户注册 |
| POST | `/api/users/login` | 用户登录 |
| GET | `/api/users/{id}` | 获取用户信息 |
| PUT | `/api/users/{id}` | 更新用户档案 |

### 食物
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/foods` | 搜索/列表食物 |
| GET | `/api/foods/{id}` | 获取食物详情 |
| POST | `/api/foods` | 提交新食物（管理员审核） |
| GET | `/api/food-categories` | 获取分类列表 |
| GET | `/api/user-custom-foods` | 获取用户自定义食物列表 |
| POST | `/api/user-custom-foods` | 创建自定义食物/食谱 |
| GET | `/api/user-custom-foods/{id}/items` | 获取组合食谱明细 |

### 食谱
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/meal-plans` | 获取我的食谱列表 |
| POST | `/api/meal-plans` | 创建食谱 |
| PUT | `/api/meal-plans/{id}` | 更新食谱 |
| DELETE | `/api/meal-plans/{id}` | 软删除食谱 |
| PUT | `/api/meal-plans/{id}/status` | 更新执行状态（开始/暂停/恢复/归档） |
| PUT | `/api/meal-plans/{id}/publish` | 发布到广场（待审核） |
| GET | `/api/meal-plans/square` | 食谱广场列表（已上架 + 热门排序） |
| POST | `/api/meal-plans/{id}/save` | 保存/应用他人食谱 |
| POST | `/api/meal-plans/{id}/favorite` | 收藏/取消收藏 |
| GET | `/api/meal-plans/square/hot` | 热门 Top10 |
| POST | `/api/meal-plans/{id}/review` | 管理员审核食谱 |

### AI
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | AI 对话 |
| POST | `/api/ai/generate-meal-plan` | AI 生成膳食方案 |
| GET | `/api/ai-chat/sessions` | 获取对话会话列表 |
| GET | `/api/ai-chat/sessions/{id}/messages` | 获取会话消息 |

### 饮食记录
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/diet-records` | 记录饮食 |
| GET | `/api/diet-records` | 查询饮食记录 |
| DELETE | `/api/diet-records/{id}` | 删除记录 |

### 健康
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/weight-records` | 记录体重 |
| GET | `/api/weight-records` | 查询体重记录 |

### 系统
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/profile-options` | 获取可配置字典选项 |

---

## AI 服务详解

AI 服务基于 Python FastAPI 构建，使用阿里云通义千问（DashScope）大模型。

**启动方式：**
```bash
# 方式一：激活虚拟环境后启动
cd ai-service
.venv\Scripts\Activate.ps1
python main.py

# 方式二：直接指定 Python 解释器
ai-service\.venv\Scripts\python.exe main.py
```

> ⚠️ 必须使用 `ai-service\.venv` 中的 Python 3.13 虚拟环境，系统默认 Python 为 3.14 alpha 测试版，会导致 FastAPI 原生依赖崩溃。

**对话模型：**
- 模型：`qwen-turbo`（日常对话）/ `qwen-plus`（复杂分析）
- 上下文处理：携带用户档案快照（身高、体重、目标、偏好、过敏等）
- 异常标记：每次 AI 调用记录 `is_abnormal` 标记，便于排查

---

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd DietAI-Planner
git checkout feature/yuan80803-alt
```

### 2. 初始化数据库

```bash
mysql -uroot -p < src/main/resources/db/init.sql
```

此脚本会：
- 创建 `diet_ai_planner` 数据库
- 建立全部 16 张表（IF NOT EXISTS 保证幂等）
- 插入初始数据：1 个管理员 + 11 个食品分类 + 47 种基础食材 + 24 种成品菜肴 + 6 条营养标准

**默认管理员账号：**
| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | admin |

### 3. 配置数据库连接

编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/diet_ai_planner
    username: root
    password: your_password
```

### 4. 启动后端

```bash
# Maven 启动
mvn spring-boot:run
```

### 5. 配置并启动 AI 服务

在 `ai-service/env.py` 中配置通义千问 API Key：
```python
DASHSCOPE_API_KEY = "your-api-key"
```

启动 AI 服务：
```bash
ai-service\.venv\Scripts\python.exe main.py
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

---

## 开发注意事项

### 密码存储
当前系统使用**明文密码**存储和对比（为教学/开发调试用途）。`SecurityConfig.java` 中实现了自定义 `PasswordEncoder`：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return rawPassword.toString();
        }
        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            if (encodedPassword == null) return false;
            return rawPassword.toString().equals(encodedPassword);
        }
    };
}
```

> ⚠️ **正式上线前请务必更换为 BCryptPasswordEncoder**，并更新相关密码字段。

### 数据库
- 所有 16 张表均使用 `IF NOT EXISTS` 创建，`init.sql` 可重复执行
- 迁移脚本已全部合并到 `init.sql`，无需单独执行零散迁移文件
- 主表均含 `created_at` / `updated_at` 审计字段
- `application.yml` 含数据库密码，**禁止提交到公开仓库**

### Git 分支
- 开发者 `yuan80803-alt` 的所有改动统一提交到 `feature/yuan80803-alt` 分支
- `main` 分支留给项目主线使用

---

## License

MIT License