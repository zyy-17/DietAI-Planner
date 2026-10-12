# 食谱模块改造说明（应用来源权限 · AI 标识 · 收藏入口 · 热门 Top10）

> 适用系统：智慧膳食 AI 规划系统（DietAI-Planner）
> 涉及层：数据库 → Java 后端 → Vue 前端
> 分支：`feature/yuan80803-alt`

---

## 一、业务逻辑说明（修改后的完整食谱模块规则）

### 1.1 食谱的三种属性（正交）

| 维度 | 取值 | 含义 |
| --- | --- | --- |
| **来源类型 `origin`**（本次新增） | `original` | 用户**原创**：自己创建，可完整编辑、可发布 |
| | `copied` | **应用来源**：源自他人公开食谱，仅可替换/增删食材 |
| 执行状态 `status` | `idle` / `active` / `paused` / `archived` | 未执行 / 执行中 / 已暂停 / 已归档 |
| 发布状态 `publishStatus` | `none` / `pending` / `approved` / `rejected` | 未发布 / 待审核 / 已上架 / 未通过 |

### 1.2 「原创食谱」的规则

- 可完整编辑：名称、目标、周期、餐次、每日热量、简介、封面、标签、难度、预期减重。
- 可发布到广场；发布后再次改动会自动退回待审核（原有逻辑保持不变）。
- 可开始执行、暂停/恢复、结束归档、删除。

### 1.3 「应用来源食谱」的规则（**核心，防侵权**）

用户从食谱广场**应用**他人食谱后，系统不再生成一份「可任意编辑、可重新发布」的副本，而是生成一份**带来源标记的受限副本**：

**允许的操作**
- 查看食谱详情（逐天、逐餐明细）。
- 在食谱内**替换食材**（含 AI 智能替换候选）。
- **增删食材**、把某一天复制到其他日期。
- 开始执行 / 暂停 / 恢复 / 结束归档。
- 从「我的食谱」里删除这份副本（只删自己这份，不影响原作者作品）。

**禁止的操作**
- 修改**基础信息**：食谱名称、简介、心得、目标、周期天数、餐次、封面、标签、难度等。
- **重新发布**到食谱广场（杜绝把他人作品改头换面后再次投稿）。
- 撤回/下架（从未上架，自然也没有该操作）。

**来源溯源（落库留存）**
- `source_plan_id`：原食谱 id。
- `source_author_id`：原作者用户 id。
- `source_author_name`：原作者昵称**快照**（脱敏，如「用户**9」），原作者之后改名也不影响溯源展示。

> 判定口径：`origin = 'copied'` **或** `source_plan_id` 非空，二者取或。
> 后者用于兼容本次改造前就已存在的老数据（迁移脚本会一并回填 `origin='copied'` 与 `source_author_id`）。

### 1.4 AI 生成食谱的标识

- 页面**不再展示「AI 生成」标签**。
- 具体做法：
  1. 后端自动命名由 `「N天减脂食谱(AI生成)」` 改为 `「N天减脂食谱」`，与手动创建口径一致。
  2. 前端对所有食谱名做一次兜底清洗，历史数据里残留的 `(AI生成)/( AI 生成 )` 后缀在渲染时统一抹掉。
- AI 仍是「生成初稿」的工具，生成后直接进入编辑器，与手写食谱在 UI 上完全一视同仁。

### 1.5 收藏

- 收藏仍是「关系」而非副本：作者下架食谱后，收藏自动失效，不留下过期快照。
- 收藏列表只展示**仍在上架**的食谱。
- 「我的食谱」页新增独立入口【⭐ 我的收藏】，默认不展示收藏；点击后在主区域切换为收藏列表。
- 收藏为空时提示：**还没有收藏的食谱**。

### 1.6 热门板块

- 口径由「按被应用次数（`usage_count`）」改为「**按收藏人数（`favorite_count`）从高到低**」。
- 只展示 **Top10**（第 10 条截止，不再往下翻页）。
- 覆盖两处：广场「🔥 热门」排序、以及「我的食谱」右侧「热门推荐」侧边栏。

### 1.7 其它保持不变

- 营养值一律回 Java 按 `food` 表按克数换算，LLM 只产出食物名与克数。
- 缩短周期用 `dayIndex <= days` 过滤而非删除数据。
- 上架食谱被改动 → 自动退回 `pending` 重新审核（`copied` 食谱不参与）。
- 删除一律走软删除（`deleted=true`），广场/收藏/我的食谱查询自动过滤。

---

## 二、需要新增 / 修改的数据库表、字段、注释

### 2.1 表：`meal_plan`（修改，新增 3 列）

| 字段 | 类型 | 允许空 | 默认值 | 注释 |
| --- | --- | --- | --- | --- |
| `origin` | VARCHAR(20) | 否 | `'original'` | 来源类型：`original`=原创，`copied`=应用自他人公开食谱 |
| `source_author_id` | BIGINT | 是 | NULL | 来源食谱原作者用户ID（应用他人食谱时记录，用于溯源） |
| `source_author_name` | VARCHAR(100) | 是 | NULL | 来源食谱原作者昵称快照（脱敏，如「用户**9」） |

已存在并继续沿用：
- `source_plan_id` BIGINT NULL —— 来源食谱ID（从广场应用而来）。
- `usage_count` INT NOT NULL DEFAULT 0 —— 被应用次数。
- `favorite_count` INT NOT NULL DEFAULT 0 —— 收藏人数（**热门排序依据**）。

新增索引：

| 索引名 | 列 | 用途 |
| --- | --- | --- |
| `idx_mp_square_hot` | (`publish_status`, `favorite_count`) | 支撑「广场热门按收藏数取 Top10」 |

### 2.2 新增数据不变更的表

- `meal_plan_favorite`（收藏关系表）结构不变，无需改动。
- 「我的收藏」入口纯前端交互，不新增表。

### 2.3 迁移脚本

新增 `src/main/resources/db/migration_meal_plan_source.sql`（**可重复执行**）：

1. 幂等新增 `origin` / `source_author_id` / `source_author_name` 三列（用 `information_schema` + `PREPARE/EXECUTE` 判存在）。
2. 回填历史数据：
   - `UPDATE meal_plan SET origin='copied' WHERE source_plan_id IS NOT NULL`；
   - `UPDATE meal_plan c JOIN meal_plan s ON c.source_plan_id=s.id SET c.source_author_id=s.user_id`。
3. 幂等创建热门索引 `idx_mp_square_hot`。
4. 末尾 `SELECT` 出相关列做结果确认。

> 新库无需本脚本：`spring.jpa.hibernate.ddl-auto=update` 会按实体自动建列。
> 执行方式：`mysql --default-character-set=utf8mb4 -uroot -p diet_ai_planner < migration_meal_plan_source.sql`。

---

## 三、后端接口逻辑改动说明

> 接口路径不变，改动集中在 `MealPlanService` 与新增字段的透出；前端无需改造调用地址。

### 3.1 `MealPlan` 实体（`entity/MealPlan.java`）

- 新增常量 `ORIGIN_ORIGINAL` / `ORIGIN_COPIED`。
- 新增字段 `origin`、`sourceAuthorId`、`sourceAuthorName`。
- 新增方法：
  - `isCopied()`：`origin='copied'` 或 `sourcePlanId != null`。
  - `isOriginal()`：`!isCopied()`。

### 3.2 `MealPlanVO`（透出给前端）

新增字段：

| 字段 | 说明 |
| --- | --- |
| `origin` | `original` / `copied` |
| `originLabel` | 「原创」/「应用来源」 |
| `sourceAuthorName` | 来源原作者昵称（脱敏） |
| `sourceAuthorId` | 来源原作者 id |
| `basicEditable` | 是否允许改基础信息与发布；`copied` 恒为 `false` |

### 3.3 `MealPlanService` 逐方法改动

| 方法 | 改动 |
| --- | --- |
| `create` | 不传 origin → 默认 `original`（`@Builder.Default`）。 |
| `updateBasic` | 开头新增 `requireOriginalForBasicEdit(plan)`；`copied` 抛「这是应用自他人分享的食谱，仅可替换食材，不能修改食谱信息」。 |
| `updateCover` | 同上拦截（封面属于发布资料/基础信息）。 |
| `publish` | 开头新增 `if (plan.isCopied()) throw ...「不能发布到广场」`。 |
| `copyToMine` | 副本写入 `origin=copied`、`sourcePlanId`、`sourceAuthorId`（原 `user_id`）、`sourceAuthorName`（`maskAuthor` 脱敏快照）；日志补充 sourceAuthorId。 |
| `markEdited` | `copied` 直接强制 `publishStatus=none` 并清空 `publishedAt/rejectReason` 后返回，不再触发重新审核逻辑。 |
| `square` | 热门分支改为按 `favoriteCount` 倒序；**Top10 截断**：`offset >= 10` 直接返回空页，尾页按 `10 - offset` 裁剪；热门不再有「加载更多」。 |
| `toPlanVO` | 填充 `origin` / `originLabel` / `sourceAuthorId` / `sourceAuthorName` / `basicEditable`。 |
| `resolveSourceAuthorName`（新增私有） | 优先用落库快照；老数据只有 `sourceAuthorId` 时现场 `maskAuthor` 补全。 |
| `requireOriginalForBasicEdit`（新增私有） | 统一的「基础信息是否可编辑」校验。 |
| `generate` | 自动命名去掉 `(AI生成)` 后缀。 |

### 3.4 `MealPlanRepository`

新增按收藏数排序的查询（老的口径方法保留）：

- `findByPublishStatusOrderByFavoriteCountDescPublishedAtDescIdDesc`
- `findByPublishStatusAndNameContainingOrderByFavoriteCountDesc`

### 3.5 `MealPlanController`

- `POST /api/meal-plan/square/{id}/copy`：返回文案由「已保存为我的食谱」改为「**已应用到我的食谱**」；补充 Javadoc 说明来源溯源与权限约束。
- 其余接口**签名与路径均未变**，仅行为受服务层拦截影响：

| 接口 | 对 `copied` 食谱的表现 |
| --- | --- |
| `PUT /api/meal-plan/{id}`（改基础信息） | 返回业务错误「仅可替换食材，不能修改食谱信息」 |
| `POST /api/meal-plan/{id}/publish` | 返回业务错误「不能发布到广场」 |
| `POST /api/meal-plan/{id}/cover` | 返回业务错误（不可改封面） |
| `POST /{id}/items`、`PUT /{planId}/item/{itemId}`、`DELETE /{planId}/item/{itemId}`、`POST /{id}/copy-day` | **正常放行**（食材层面的调整） |
| `GET /api/meal-plan/{id}`（详情）、`/current`、`/{id}/apply`、`/pause`、`/resume`、`/{id}/finish`、`DELETE /{id}` | 正常放行 |
| `GET /api/meal-plan/square?sort=hot` | 按收藏数倒序，最多 10 条 |
| `GET /api/meal-plan/favorites` | 不变（只返回在架食谱） |

### 3.6 AI 服务（`ai-service`）

无需改动：标签是「展示层」概念，由后端命名与前端渲染控制。

---

## 四、前端页面交互改动说明

### 4.1 `views/MealPlan.vue`（我的食谱 / 食谱广场）

**问题3：收藏入口**

- 页头新增【⭐ 我的收藏】按钮，带数量角标；与「＋ 新建食谱」并排。
- 新增 `mineView`（`recipes` | `favorites`）视图状态：
  - 默认 `recipes`：与原来一致的筛选栏、搜索栏、执行横幅、食谱卡片。
  - 点击按钮切到 `favorites`：主区域替换为收藏卡片列表；状态筛选栏/搜索栏/执行横幅隐藏。
  - 收藏卡片空态：**「还没有收藏的食谱」**，副文案引导去广场。
- 移除原来常驻在页面下方的「我收藏的」区块。
- 切回「我的食谱」Tab 时自动复位到 `recipes` 视图。

**问题1：应用来源食谱的卡片行为**

- 新增 `displayName(plan)`：渲染时抹掉历史数据里的 `(AI生成)` 后缀（问题2 前端兜底）。
- 卡片新增来源提示行：`🔗 应用自「用户**9」`（`!plan.basicEditable` 时展示）。
- 封面角标：`copied` 显示橙色「应用来源」，原创仍显示「公开 / 个人」。
- 操作区按 `plan.basicEditable` 分流：
  - 原创：`编辑` + 发布/撤回/查看原因/开始执行（原逻辑）。
  - 应用来源：左侧按钮文案变为 **`调整食材`**（仍打开同一天编辑器，可替换/增删食材），右侧仅保留 **`开始执行`** 与「执行详情」，**不渲染任何发布相关按钮**。
- 下拉菜单：
  - 原创 → `编辑食谱` / `发布到广场` / `取消公开` …（原逻辑）。
  - 应用来源 → 首项变为 `查看食谱`，`发布到广场` 与 `取消公开` 选项**整体不渲染**。

**问题4：热门板块**

- 右侧「热门推荐」：请求 `sort=hot&size=10`，展示 Top10；条目统计由「N人收藏」改为真实 `favoriteCount`（原来错用了 `usageCount`）；列表超高时滚动。
- 广场「🔥 热门」：由后端按收藏数倒序且只返回 10 条，因此热门口径下不再出现「加载更多」。
- 「精选食谱」与广场卡片的统计文案统一改为 **`⭐ N 人收藏`**。

### 4.2 `components/RecipeSheet.vue`（食谱详情/编辑弹窗）

- 顶部信息条：
  - 广场模式由「🔥 N+ 人使用中」改为「⭐ N 人收藏」。
  - 自身食谱若为应用来源，显示「🔗 应用自「用户**9」的食谱 · 当前状态」。
- 正文新增来源说明条（仅 `mine` + `copied` 时出现）：
  > 本食谱应用自「用户**9」分享的公开食谱：你可以替换 / 增删其中的食材，但不能修改食谱名称、简介等基础信息，也不能重新发布到广场。
- 工具行：
  - `复制到其他日期` 保留（属食材层面操作）。
  - `修改计划天数` 在应用来源食谱上**隐藏**（周期属基础信息）。
- 底部按钮文案：广场模式由「保存为我的食谱」改为「**应用到我的食谱**」，与后端文案统一。
- 新增计算属性：`isCopied`、`basicEditable`，驱动上述显隐。

### 4.3 样式

- 新增 `.page-head-actions` / `.btn-fav-view`（含选中态）/ `.fav-count` / `.fav-view-head` / `.rc-origin` / `.rc-vis-badge.src` / `.src-note`。
- 同步补齐对应 `html.dark` 深色主题样式。

---

## 五、涉及文件清单

**后端**

| 文件 | 变更 |
| --- | --- |
| `src/main/java/com/zyyqq/entity/MealPlan.java` | 新增 origin / sourceAuthorId / sourceAuthorName 字段与 isCopied() |
| `src/main/java/com/zyyqq/dto/response/MealPlanVO.java` | 新增 5 个来源/权限字段 |
| `src/main/java/com/zyyqq/repository/MealPlanRepository.java` | 新增按 favoriteCount 排序的两个查询 |
| `src/main/java/com/zyyqq/service/MealPlanService.java` | 权限拦截、copyToMine 溯源、热门 Top10、AI 命名去标识 |
| `src/main/java/com/zyyqq/controller/MealPlanController.java` | copy 文案与注释 |
| `src/main/resources/db/migration_meal_plan_source.sql` | **新增**：幂等迁移脚本 |

**前端**

| 文件 | 变更 |
| --- | --- |
| `frontend/src/views/MealPlan.vue` | 收藏入口与视图切换、来源卡片受限交互、热门 Top10、AI 标识兜底 |
| `frontend/src/components/RecipeSheet.vue` | 来源提示、基础信息编辑限制、按钮文案 |

---

## 六、部署与验证步骤

1. **数据库迁移**（老库必做）：
   ```bash
   mysql --default-character-set=utf8mb4 -uroot -p diet_ai_planner < src/main/resources/db/migration_meal_plan_source.sql
   ```
   脚本尾部会打印 `origin / source_plan_id / source_author_id / source_author_name` 四列确认。
2. **重启后端**（实体/VO/Service 有改动）：
   ```bash
   ./mvnw -o -q compile    # 已验证通过
   ```
3. **重启前端**：`cd frontend && npm run dev`（生产 `npm run build` 已验证通过）。
4. **回归重点**：
   - 广场应用他人食谱 → 「我的食谱」出现橙色「应用来源」卡片；
   - 打开该卡片 → 只剩「调整食材」，替换/增删食材可用，发布按钮消失；
   - 直接调 `PUT /api/meal-plan/{id}` 与 `/publish` → 返回业务错误（服务端兜底，防绕过 UI）；
   - 「⭐ 我的收藏」切换正常，无收藏时显示「还没有收藏的食谱」；
   - 广场「🔥 热门」与右侧「热门推荐」条目数与顺序一致（按收藏数，≤10 条）。
