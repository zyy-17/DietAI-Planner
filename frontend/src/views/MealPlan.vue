<template>
  <div class="meal-plan-page">
    <!-- ══════════ 我的食谱（左右布局：主内容 + 热门推荐侧边栏） ══════════ -->
    <template v-if="activeTab === 'mine'">
      <div class="mine-layout">
        <!-- 左侧：原有内容区（完全不变） -->
        <div class="mine-main">
          <div class="page-card">
            <!-- 标题操作区 -->
            <div class="page-head">
              <div>
                <h2 class="page-title">我的食谱</h2>
                <p class="page-sub">管理自己创建和收藏的食谱，随时应用到膳食方案</p>
              </div>
              <el-button type="primary" round class="btn-create" @click="openCreate">
                <span class="btn-create-icon">＋</span> 新建食谱
              </el-button>
            </div>

        <!-- 状态筛选区（数量内联到 pill 中） -->
        <div class="status-filter">
          <span
            v-for="f in statusFilters" :key="f.key"
            class="status-pill"
            :class="{ active: mineFilter === f.key }"
            @click="setMineFilter(f.key)"
          >
            {{ f.label }} <em class="pill-count">{{ f.count }}</em>
          </span>
        </div>

        <!-- 搜索工具栏 -->
        <div class="tool-bar">
          <el-input
            v-model="mineKeyword" placeholder="搜索食谱名称" clearable
            class="search-input" @keyup.enter="loadMine" @clear="loadMine"
          >
            <template #prefix>
              <span class="search-icon">🔍</span>
            </template>
          </el-input>
          <el-select v-model="mineMealType" placeholder="食谱类型" clearable class="tool-select" @change="loadMine">
            <el-option label="单餐" value="single" />
            <el-option label="单日" value="day" />
            <el-option label="多日" value="multi" />
          </el-select>
          <el-select v-model="mineVisibility" placeholder="可见范围" clearable class="tool-select" @change="loadMine">
            <el-option label="个人可见" value="private" />
            <el-option label="公开" value="public" />
          </el-select>
          <el-select v-model="mineSort" class="tool-select sort-select" @change="loadMine">
            <el-option label="最近更新" value="updated" />
            <el-option label="创建时间" value="created" />
          </el-select>
        </div>

        <!-- 执行横幅 - 紧凑版 -->
        <div class="exec-banner" v-if="current.plan" @click="openSheet(current.plan.id, 'mine')">
          <div class="exec-banner-left">
            <span class="exec-badge" :class="current.plan.status === 'paused' ? 'badge-paused' : 'badge-active'">
              {{ current.plan.status === 'paused' ? '⏸' : '●' }}
            </span>
            <div class="exec-info">
              <span class="exec-name">{{ current.plan.name }}</span>
              <span class="exec-detail">
                {{ current.plan.goalLabel }} · {{ current.plan.days }}天 · {{ fmt(current.plan.plannedDailyCalories) }} kcal/天
              </span>
            </div>
          </div>
          <div class="exec-banner-progress" v-if="execution" @click.stop>
            <div class="exec-progress-bar">
              <div class="exec-progress-fill" :style="{ width: execution.progressPercent + '%' }"></div>
            </div>
            <span class="exec-progress-text">
              已完成 {{ execution.currentDayIndex }}/{{ execution.totalDays }} 天
            </span>
          </div>
          <div class="exec-banner-actions" @click.stop>
            <el-button size="small" text @click="openSheet(current.plan.id, 'mine')">查看食谱</el-button>
            <template v-if="current.plan.status === 'active'">
              <el-button size="small" text @click="pausePlan(current.plan)">暂停</el-button>
              <el-button size="small" text type="warning" @click="finishPlan(current.plan)">结束</el-button>
            </template>
            <template v-else-if="current.plan.status === 'paused'">
              <el-button size="small" text type="primary" @click="resumePlan(current.plan)">恢复</el-button>
              <el-button size="small" text type="warning" @click="finishPlan(current.plan)">结束</el-button>
            </template>
          </div>
        </div>

        <!-- 食谱卡片区 -->
        <div class="recipe-grid" v-loading="mineLoading">
          <div
            class="recipe-card" v-for="plan in filteredMyRecipes" :key="plan.id"
            @click="openSheet(plan.id, 'mine')"
          >
            <!-- 封面 -->
            <div class="rc-cover" :style="coverStyle(plan)">
              <div class="rc-cover-placeholder" v-if="!plan.coverUrl">
                <span class="rc-cover-emoji">{{ coverEmoji(plan) }}</span>
              </div>
              <span v-if="plan.status === 'active' || plan.status === 'paused'" class="rc-exec-dot"></span>
              <span class="rc-vis-badge" :class="{ pub: plan.onSquare }">
                {{ plan.onSquare ? '公开' : '个人' }}
              </span>
            </div>
            <!-- 卡片内容 -->
            <div class="rc-body">
              <div class="rc-title-row">
                <span class="rc-name" :title="plan.name">{{ plan.name }}</span>
                <span class="rc-status-tag" :class="statusTagClass(plan)">{{ cardStatusText(plan) }}</span>
              </div>
              <div class="rc-meta">
                <span>{{ mealTypeLabel(plan) }}</span>
                <span class="rc-meta-dot">·</span>
                <span>{{ plan.days }}天</span>
                <span class="rc-meta-dot">·</span>
                <span>{{ totalMeals(plan) }}餐</span>
              </div>
              <div class="rc-nutrition">
                <template v-if="plan.itemCount > 0">
                  <span class="rn-kcal">{{ fmt(plan.plannedDailyCalories) }}</span>
                  <span class="rn-unit"> kcal/天</span>
                  <span class="rn-sep">|</span>
                  <span class="rn-label">营养估算</span>
                </template>
                <span v-else class="rn-empty">待计算</span>
              </div>
              <div class="rc-progress" v-if="plan.status === 'active' && execution && execution.planId === plan.id">
                <div class="rc-progress-bar">
                  <div class="rc-progress-fill" :style="{ width: execution.progressPercent + '%' }"></div>
                </div>
                <span class="rp-text">已完成 {{ execution.currentDayIndex }}/{{ plan.days }} 天</span>
              </div>
              <div class="rc-divider"></div>
              <div class="rc-actions" @click.stop>
                <span class="rc-action-left">
                  <el-button size="small" text class="action-btn" @click="openSheet(plan.id, 'mine')">
                    编辑
                  </el-button>
                </span>
                <span class="rc-action-right">
                  <!-- 未发布 -->
                  <template v-if="plan.publishStatus === 'none'">
                    <el-button size="small" text type="primary" class="action-btn" @click="openPublish(plan)">
                      发布
                    </el-button>
                  </template>
                  <!-- 审核中 -->
                  <template v-else-if="plan.publishStatus === 'pending'">
                    <el-button size="small" text class="action-btn" @click="unpublish(plan)">
                      撤回申请
                    </el-button>
                  </template>
                  <!-- 审核驳回 -->
                  <template v-else-if="plan.publishStatus === 'rejected'">
                    <el-button size="small" text type="warning" class="action-btn" @click="openSheet(plan.id, 'mine')">
                      查看原因
                    </el-button>
                  </template>
                  <!-- 已发布 -->
                  <template v-else-if="plan.publishStatus === 'approved' && plan.status !== 'active' && plan.status !== 'paused'">
                    <el-button size="small" text type="primary" class="action-btn" @click="applyPlan(plan)">
                      开始执行
                    </el-button>
                  </template>
                  <!-- 执行中 / 暂停 -->
                  <template v-if="plan.status === 'active' || plan.status === 'paused'">
                    <el-button size="small" text type="primary" class="action-btn" @click="openExecution">
                      执行详情
                    </el-button>
                  </template>
                  <el-dropdown trigger="click" @command="(cmd) => onPlanCommand(cmd, plan)">
                    <el-button size="small" text class="action-btn action-more">⋯</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit">编辑食谱</el-dropdown-item>
                        <el-dropdown-item v-if="plan.publishStatus === 'none' || plan.publishStatus === 'rejected'" command="publish">发布到广场</el-dropdown-item>
                        <el-dropdown-item v-if="plan.onSquare" command="unpublish">取消公开</el-dropdown-item>
                        <el-dropdown-item v-if="plan.status === 'active'" command="pause">暂停执行</el-dropdown-item>
                        <el-dropdown-item v-if="plan.status === 'paused'" command="resume">恢复执行</el-dropdown-item>
                        <el-dropdown-item v-if="plan.status === 'active' || plan.status === 'paused'" command="finish">结束执行</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>删除食谱</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </span>
              </div>
            </div>
          </div>
          <div class="empty-box" v-if="!filteredMyRecipes.length && !mineLoading">
            <div class="empty-icon">📋</div>
            <div class="empty-text">还没有食谱</div>
            <div class="empty-sub">点右上角「＋ 新建食谱」开始创建你的第一份食谱吧</div>
          </div>
        </div>

        <!-- 收藏区 -->
        <template v-if="favorites.length">
          <div class="section-head">
            <span class="section-title">
              <span class="section-icon">⭐</span> 我收藏的
              <span class="section-count">{{ favorites.length }}</span>
            </span>
          </div>
          <div class="recipe-grid">
            <div
              class="recipe-card" v-for="plan in favorites" :key="'f' + plan.id"
              @click="openSheet(plan.id, 'square')"
            >
              <div class="rc-cover" :style="coverStyle(plan)">
                <div class="rc-cover-placeholder" v-if="!plan.coverUrl">
                  <span class="rc-cover-emoji">{{ coverEmoji(plan) }}</span>
                </div>
              </div>
              <div class="rc-body">
                <div class="rc-title-row">
                  <span class="rc-name" :title="plan.name">{{ plan.name }}</span>
                </div>
                <div class="rc-meta">
                  <span>{{ mealTypeLabel(plan) }}</span>
                  <span class="rc-meta-dot">·</span>
                  <span>{{ plan.days }}天</span>
                  <span class="rc-meta-dot">·</span>
                  <span>{{ totalMeals(plan) }}餐</span>
                </div>
                <div class="rc-nutrition">
                  <template v-if="plan.itemCount > 0">
                    <span class="rn-kcal">{{ fmt(plan.plannedDailyCalories) }}</span>
                    <span class="rn-unit"> kcal/天</span>
                    <span class="rn-sep">|</span>
                    <span class="rn-label">营养估算</span>
                  </template>
                  <span v-else class="rn-empty">待计算</span>
                </div>
                <div class="rc-divider"></div>
                <div class="rc-actions" @click.stop>
                  <span class="rc-author">作者：{{ plan.authorName || '匿名' }}</span>
                  <el-button size="small" text type="primary" class="action-btn" @click="openSheet(plan.id, 'square')">
                    查看食谱
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
          </div><!-- /page-card -->
        </div><!-- /mine-main -->

        <!-- 右侧：热门推荐侧边栏 -->
        <div class="mine-sidebar">
          <div class="sidebar-card">
            <div class="sidebar-head">
              <span class="sidebar-title">热门推荐</span>
              <el-button type="primary" size="small" round @click="openCreate">＋ 新建食谱</el-button>
            </div>
            <div class="sidebar-list" v-loading="hotLoading">
              <div class="hot-item" v-for="plan in hotList" :key="'hot' + plan.id" @click="openSheet(plan.id, 'square')">
                <img class="hot-cover" :src="plan.coverUrl || defaultCoverSmall" alt="" />
                <div class="hot-info">
                  <div class="hot-name">{{ plan.name }}</div>
                  <div class="hot-stats">{{ plan.usageCount || 0 }}人收藏</div>
                </div>
                <button class="hot-fav-btn" :class="{ on: plan.favorited }" @click.stop="toggleHotFavorite(plan)">
                  {{ plan.favorited ? '已收藏' : '收藏' }}
                </button>
              </div>
              <div class="hot-empty" v-if="!hotList.length && !hotLoading">暂无热门食谱</div>
            </div>

            <!-- 小贴士 -->
            <div class="sidebar-tips">
              <div class="tips-title">💡 小贴士</div>
              <ul class="tips-list">
                <li>公开发布的食谱需要管理员审核后才会出现在广场展示。</li>
                <li>你可以从食谱保存到个人菜，或用AI智能膳食方案中，合理搭配食材、均衡营养，才是健康的关键！</li>
              </ul>
            </div>

            <!-- 底部装饰区 -->
            <div class="sidebar-deco">
              <div class="deco-text">科学饮食<br/>让生活更美好</div>
              <div class="deco-leaf">🌿</div>
            </div>
          </div>
        </div>
      </div><!-- /mine-layout -->
    </template>

    <!-- ══════════ 食谱广场 ══════════ -->
    <template v-else>
      <div class="page-card">
        <!-- 标题区 -->
        <div class="page-head">
          <div>
            <h2 class="page-title">食谱广场</h2>
            <p class="page-sub">发现健康膳食灵感，浏览并收藏适合自己的食谱</p>
          </div>
          <el-button plain round class="btn-share" @click="goMine">📤 分享我的食谱</el-button>
        </div>

        <!-- 搜索区 -->
        <div class="square-search-box">
          <el-input
            v-model="squareKeyword" placeholder="搜索食谱名称、食物或餐次"
            clearable class="square-search-input" @keyup.enter="loadSquare(true)" @clear="loadSquare(true)"
          >
            <template #prefix>
              <span class="search-icon">🔍</span>
            </template>
          </el-input>
        </div>

        <!-- 分类筛选区 -->
        <div class="square-categories">
          <span
            v-for="c in squareCategories" :key="c.key"
            class="cat-pill"
            :class="{ active: squareCategory === c.key }"
            @click="setSquareCategory(c.key)"
          >
            <span class="cat-icon">{{ c.icon }}</span>
            {{ c.label }}
          </span>
        </div>

        <!-- 精选推荐区 -->
        <template v-if="featuredList.length">
          <div class="section-head">
            <span class="section-title">
              <span class="section-icon">✨</span> 精选食谱
            </span>
            <span class="section-hint">社区推荐</span>
          </div>
          <div class="featured-grid">
            <div
              class="recipe-card featured-card" v-for="plan in featuredList" :key="'feat' + plan.id"
              @click="openSheet(plan.id, 'square')"
            >
              <div class="fc-cover" :style="coverStyle(plan)">
                <div class="rc-cover-placeholder" v-if="!plan.coverUrl">
                  <span class="rc-cover-emoji large">{{ coverEmoji(plan) }}</span>
                </div>
                <span class="fc-fav-badge" :class="{ on: plan.favorited }" @click.stop="toggleFavorite(plan)">
                  {{ plan.favorited ? '★' : '☆' }}
                </span>
              </div>
              <div class="fc-body">
                <div class="fc-name">{{ plan.name }}</div>
                <div class="fc-meta">
                  <span>{{ mealTypeLabel(plan) }}</span>
                  <span class="rc-meta-dot">·</span>
                  <span>{{ plan.days }}天</span>
                  <span class="rc-meta-dot">·</span>
                  <span>{{ mealText(plan) }}</span>
                </div>
                <div class="fc-nutrition">
                  <span class="fc-kcal">{{ fmt(plan.plannedDailyCalories) }}</span>
                  <span class="fc-kcal-unit"> kcal/天</span>
                </div>
                <div class="fc-foot">
                  <span class="fc-author">👤 {{ plan.authorName || '匿名用户' }}</span>
                  <span class="fc-usage">🔥 {{ plan.usageCount || 0 }} 人使用</span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 全部公开食谱 -->
        <div class="section-head">
          <span class="section-title">
            <span class="section-icon">📖</span> 全部公开食谱
          </span>
          <div class="sort-pills">
            <span class="sort-pill" :class="{ active: squareSort === 'hot' }" @click="squareSort='hot';loadSquare(true)">🔥 热门</span>
            <span class="sort-pill" :class="{ active: squareSort === 'new' }" @click="squareSort='new';loadSquare(true)">🆕 最新</span>
          </div>
        </div>
        <div class="recipe-grid" v-loading="squareLoading">
          <div
            class="recipe-card" v-for="plan in squareList" :key="'s' + plan.id"
            @click="openSheet(plan.id, 'square')"
          >
            <div class="rc-cover" :style="coverStyle(plan)">
              <div class="rc-cover-placeholder" v-if="!plan.coverUrl">
                <span class="rc-cover-emoji">{{ coverEmoji(plan) }}</span>
              </div>
            </div>
            <div class="rc-body">
              <div class="rc-title-row">
                <span class="rc-name" :title="plan.name">{{ plan.name }}</span>
              </div>
              <div class="rc-meta">
                <span>{{ mealTypeLabel(plan) }}</span>
                <span class="rc-meta-dot">·</span>
                <span>{{ plan.days }}天</span>
                <span class="rc-meta-dot">·</span>
                <span>{{ mealText(plan) }}</span>
              </div>
              <div class="rc-nutrition">
                <template v-if="plan.itemCount > 0">
                  <span class="rn-kcal">{{ fmt(plan.plannedDailyCalories) }}</span>
                  <span class="rn-unit"> kcal/天</span>
                  <span class="rn-sep">|</span>
                  <span class="rn-label">营养估算</span>
                </template>
                <span v-else class="rn-empty">待计算</span>
              </div>
              <div class="rc-divider"></div>
              <div class="rc-actions" @click.stop>
                <span class="rc-author">👤 {{ plan.authorName || '匿名' }}</span>
                <span class="rc-action-right">
                  <span class="rc-fav" :class="{ on: plan.favorited }" @click="toggleFavorite(plan)">
                    {{ plan.favorited ? '★' : '☆' }}
                  </span>
                  <el-button size="small" text type="primary" class="action-btn" @click="openSheet(plan.id, 'square')">
                    查看食谱
                  </el-button>
                </span>
              </div>
            </div>
          </div>
          <div class="empty-box" v-if="!squareList.length && !squareLoading">
            <div class="empty-icon">🏛️</div>
            <div class="empty-text">广场还没有食谱</div>
            <div class="empty-sub">去「我的食谱」发布一份健康的食谱吧</div>
          </div>
        </div>

        <div class="more-row" v-if="squareHasMore">
          <button class="more-btn" @click="loadSquare(false)">加载更多食谱</button>
        </div>
      </div>
    </template>

    <!-- 新建食谱 -->
    <el-dialog v-model="createDialog" title="新建食谱" width="480px" class="create-dialog">
      <el-form label-width="88px">
        <el-form-item label="食谱名称">
          <el-input v-model="createForm.name" placeholder="如：七天减脂食谱" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="膳食目标">
          <el-radio-group v-model="createForm.goal">
            <el-radio-button value="lose">🔥 减脂</el-radio-button>
            <el-radio-button value="gain">💪 增重</el-radio-button>
            <el-radio-button value="maintain">⚖️ 维持体重</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="计划天数">
          <el-radio-group v-model="createForm.days">
            <el-radio-button :value="3">3天</el-radio-button>
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="14">14天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="每日餐次">
          <el-checkbox-group v-model="createForm.meals">
            <el-checkbox value="breakfast">🌅 早餐</el-checkbox>
            <el-checkbox value="lunch">☀️ 午餐</el-checkbox>
            <el-checkbox value="dinner">🌙 晚餐</el-checkbox>
            <el-checkbox value="snack">🍎 加餐</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建并开始编写</el-button>
      </template>
    </el-dialog>

    <!-- 发布到广场 -->
    <el-dialog v-model="publishDialog" title="发布到食谱广场" width="560px">
      <el-form label-width="88px">
        <el-form-item label="封面图">
          <div class="cover-upload">
            <img :src="publishForm.coverUrl || defaultCover" class="cover-preview" />
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadCover">
              <el-button size="small">上传封面</el-button>
            </el-upload>
            <span class="cover-tip">建议正方形，2MB 以内</span>
          </div>
        </el-form-item>
        <el-form-item label="展示标签">
          <el-select v-model="publishForm.tags" multiple filterable allow-create default-first-option
            placeholder="回车添加，如「经典碳循环」" style="width: 100%" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="publishForm.difficulty">
            <el-radio-button value="入门">入门</el-radio-button>
            <el-radio-button value="进阶">进阶</el-radio-button>
            <el-radio-button value="挑战">挑战</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预期减重">
          <el-input v-model="publishForm.expectedLoss" placeholder="如 -0.5~-1 kg" />
        </el-form-item>
        <el-form-item label="一句话概述">
          <el-input v-model="publishForm.summary" maxlength="60" show-word-limit placeholder="用一句话描述这份食谱" />
        </el-form-item>
        <el-form-item label="心得思路">
          <el-input v-model="publishForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit
            placeholder="说说这份食谱的整体思路，广场详情里会展示" />
        </el-form-item>
      </el-form>
      <div class="publish-note">💡 提交后需管理员审核通过才会出现在广场；审核期间可以继续编辑。</div>
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPublish">提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 执行进度弹窗 -->
    <el-dialog v-model="execDialog" title="执行进度" width="640px">
      <div class="exec-dialog" v-loading="execLoading">
        <div class="exec-summary" v-if="execution">
          <div class="exec-summary-item">
            <div class="es-value">{{ execution.doneMeals }}</div>
            <div class="es-label">已打卡 / {{ execution.totalMeals }} 餐</div>
          </div>
          <div class="exec-summary-item">
            <div class="es-value">{{ fmt(execution.plannedDailyCalories) }}</div>
            <div class="es-label">日均计划 kcal</div>
          </div>
          <div class="exec-summary-item">
            <div class="es-value">{{ fmt(execution.actualDailyCalories) }}</div>
            <div class="es-label">日均实际 kcal</div>
          </div>
        </div>
        <div class="day-progress-list">
          <div class="day-progress" v-for="d in execution?.days || []" :key="d.dayIndex"
            :class="{ done: d.completed }">
            <div class="dp-head">
              <span class="dp-day">第 {{ d.dayIndex }} 天</span>
              <span class="dp-date">{{ d.date }}</span>
              <span class="dp-status" :class="{ ok: d.completed }">
                {{ d.completed ? '✓ 已完成' : `${d.doneMeals}/${d.totalMeals} 餐` }}
              </span>
            </div>
            <div class="dp-meals">
              <span v-for="m in d.meals" :key="m.mealType" class="dp-meal" :class="{ ok: m.done }">
                {{ m.mealLabel }}{{ m.done ? ' ✓' : ' ○' }}
              </span>
            </div>
            <div class="dp-kcal">
              计划 {{ fmt(d.plannedCalories) }} kcal · 实际 {{ fmt(d.actualCalories) }} kcal
            </div>
          </div>
          <div class="empty-box" v-if="!execution?.days?.length && !execLoading">还没有执行记录</div>
        </div>
      </div>
    </el-dialog>

    <RecipeSheet v-model="sheetVisible" :plan-id="sheetPlanId" :mode="sheetMode"
      @changed="onSheetChanged" @copy="onCopyFromSquare" @applied="onApplied" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import RecipeSheet from '../components/RecipeSheet.vue'

const route = useRoute()
const router = useRouter()

const defaultCover = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="200" height="200" fill="%23eef7f1"/><text x="100" y="115" font-size="60" text-anchor="middle">🍲</text></svg>'
const defaultCoverSmall = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="60"><rect width="80" height="60" fill="%23e8f5e9" rx="6"/><text x="40" y="38" font-size="24" text-anchor="middle">🍽️</text></svg>'

const activeTab = ref(route.path === '/meal-plan/square' ? 'square' : 'mine')
const current = ref({})
const myRecipes = ref([])
const favorites = ref([])
const mineFilter = ref('all')
const mineKeyword = ref('')
const mineMealType = ref(null)
const mineVisibility = ref(null)
const mineSort = ref('updated')
const mineLoading = ref(false)
const mineCounts = ref({ publish: {}, status: {} })

const squareList = ref([])
const squarePage = ref(0)
const squareHasMore = ref(false)
const squareLoading = ref(false)
const squareSort = ref('hot')
const squareKeyword = ref('')
const squareCategory = ref('all')
const featuredList = ref([])

// 热门推荐侧边栏
const hotList = ref([])
const hotLoading = ref(false)

const saving = ref(false)
const createDialog = ref(false)
const createForm = reactive({ name: '', goal: 'lose', days: 7, meals: ['breakfast', 'lunch', 'dinner'] })
const publishDialog = ref(false)
const publishPlan = ref(null)
const publishForm = reactive({
  coverUrl: '', tags: [], difficulty: '入门', expectedLoss: '', summary: '', description: ''
})

const sheetVisible = ref(false)
const sheetPlanId = ref(null)
const sheetMode = ref('mine')

const execDialog = ref(false)
const execLoading = ref(false)
const execution = ref(null)

const squareCategories = [
  { key: 'all', label: '全部食谱', icon: '📋' },
  { key: 'breakfast', label: '早餐', icon: '🌅' },
  { key: 'lunch', label: '午餐', icon: '☀️' },
  { key: 'dinner', label: '晚餐', icon: '🌙' },
  { key: 'snack', label: '加餐', icon: '🍎' },
  { key: 'single', label: '单日食谱', icon: '📅' },
  { key: 'multi', label: '多日食谱', icon: '📆' }
]

const statusFilters = computed(() => {
  const p = mineCounts.value.publish || {}
  const s = mineCounts.value.status || {}
  const active = (s.active || 0) + (s.paused || 0)
  const published = p.approved || 0
  return [
    { key: 'all', label: '全部食谱', count: mineCounts.value.total || 0 },
    { key: 'none', label: '未发布', count: (p.none || 0) + (p.rejected || 0) },
    { key: 'pending', label: '审核中', count: p.pending || 0 },
    { key: 'approved', label: '已发布', count: published },
    { key: 'active', label: '执行中', count: active }
  ]
})

const filteredMyRecipes = computed(() => {
  let list = myRecipes.value
  if (mineMealType.value) {
    const days = (list[0]?.days) || 0
    list = list.filter(p => {
      const totalDays = p.days || 0
      if (mineMealType.value === 'single') return totalDays === 1
      if (mineMealType.value === 'day') return totalDays === 1
      if (mineMealType.value === 'multi') return totalDays > 1
      return true
    })
  }
  if (mineVisibility.value) {
    list = list.filter(p => {
      if (mineVisibility.value === 'public') return p.onSquare
      return !p.onSquare
    })
  }
  return list
})

function fmt(v) {
  const n = Number(v)
  if (!n || Number.isNaN(n)) return '0'
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

function coverStyle(plan) {
  if (plan.coverUrl) {
    return { backgroundImage: `url(${plan.coverUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  const colors = [
    'linear-gradient(135deg, #e8f5e9, #c8e6c9)',
    'linear-gradient(135deg, #fff3e0, #ffe0b2)',
    'linear-gradient(135deg, #e3f2fd, #bbdefb)',
    'linear-gradient(135deg, #fce4ec, #f8bbd0)',
    'linear-gradient(135deg, #f3e5f5, #e1bee7)',
    'linear-gradient(135deg, #e0f2f1, #b2dfdb)'
  ]
  const idx = (plan.id || 0) % colors.length
  return { background: colors[idx] }
}

function coverEmoji(plan) {
  const goalMap = { lose: '🥗', gain: '💪', maintain: '🥑' }
  return goalMap[plan.goal] || '🍲'
}

function totalMeals(plan) {
  return (plan.days || 0) * (plan.meals?.length || 0)
}

function mealText(plan) {
  const map = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' }
  return (plan.meals || []).map(m => map[m] || m).join('·') || '—'
}

function mealTypeLabel(plan) {
  const days = plan.days || 0
  if (days === 1) return '单日'
  if (days <= 3) return '短期'
  return '多日'
}

function cardStatusText(plan) {
  if (plan.status === 'active') return '执行中'
  if (plan.status === 'paused') return '已暂停'
  const map = { none: '未发布', pending: '审核中', approved: '已发布', rejected: '未通过' }
  return map[plan.publishStatus] || '未发布'
}

function statusTagClass(plan) {
  const base = 'tag-' + (plan.publishStatus || 'none')
  if (plan.status === 'active' || plan.status === 'paused') return base + ' tag-executing'
  return base
}

async function loadCurrent() {
  try {
    current.value = (await api.get('/meal-plan/current')) || {}
  } catch (e) {
    current.value = {}
  }
  await loadExecution()
}

async function loadExecution() {
  const planId = current.value?.plan?.id
  if (!planId || (current.value.plan.status !== 'active' && current.value.plan.status !== 'paused')) {
    execution.value = null
    return
  }
  try {
    const data = await api.get(`/meal-plan/${planId}/execution`)
    execution.value = data
    execution.value.planId = planId
  } catch (e) {
    execution.value = null
  }
}

function openExecution() {
  execDialog.value = true
}

async function pausePlan(plan) {
  try {
    await api.post(`/meal-plan/${plan.id}/pause`)
    ElMessage.success('已暂停')
    await Promise.all([loadCurrent(), loadMine()])
  } catch (e) {}
}

async function resumePlan(plan) {
  try {
    await api.post(`/meal-plan/${plan.id}/resume`)
    ElMessage.success('已恢复执行')
    await Promise.all([loadCurrent(), loadMine()])
  } catch (e) {}
}

async function finishPlan(plan) {
  try {
    await ElMessageBox.confirm(`确定提前结束「${plan.name}」？结束后会归档到历史。`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await api.post(`/meal-plan/${plan.id}/finish`)
    ElMessage.success('已结束执行')
    await Promise.all([loadCurrent(), loadMine()])
  } catch (e) {}
}

function setMineFilter(key) {
  mineFilter.value = key
  loadMine()
}

async function loadMine() {
  mineLoading.value = true
  try {
    const params = {}
    if (mineFilter.value !== 'all') params.status = mineFilter.value
    if (mineKeyword.value) params.keyword = mineKeyword.value
    params.sort = mineSort.value
    const data = await api.get('/meal-plan/list', { params }) || {}
    myRecipes.value = data.plans || []
    mineCounts.value = { publish: data.publishCounts || {}, status: data.statusCounts || {}, total: data.total || 0 }
  } catch (e) {
    myRecipes.value = []
  } finally {
    mineLoading.value = false
  }
}

async function loadFavorites() {
  try {
    favorites.value = (await api.get('/meal-plan/favorites')) || []
  } catch (e) {
    favorites.value = []
  }
}

async function loadHotRecipes() {
  hotLoading.value = true
  try {
    const res = await api.get('/meal-plan/square', { params: { sort: 'hot', page: 0, size: 5 } })
    hotList.value = (res?.content || []).slice(0, 5)
  } catch (e) {
    hotList.value = []
  } finally {
    hotLoading.value = false
  }
}

function toggleHotFavorite(plan) {
  toggleFavorite(plan)
}

function matchCategory(plan) {
  switch (squareCategory.value) {
    case 'all': return true
    case 'single': return (plan.days || 0) === 1
    case 'multi': return (plan.days || 0) > 1
    default: return (plan.meals || []).includes(squareCategory.value)
  }
}

async function loadSquare(reset = true) {
  if (reset) squarePage.value = 0
  squareLoading.value = true
  try {
    const params = {
      sort: squareSort.value,
      keyword: squareKeyword.value || undefined,
      page: squarePage.value,
      size: 12
    }
    const res = await api.get('/meal-plan/square', { params })
    const content = (res?.content || []).filter(matchCategory)
    squareList.value = reset ? content : squareList.value.concat(content)
    squareHasMore.value = !res?.last && content.length > 0
    if (content.length) squarePage.value += 1
    if (reset) {
      const hotRes = await api.get('/meal-plan/square', { params: { sort: 'hot', page: 0, size: 6 } })
      const hotList = hotRes?.content || []
      featuredList.value = hotList.length >= 6 ? hotList.slice(0, 4) : []
    }
  } catch (e) {
    if (reset) squareList.value = []
    squareHasMore.value = false
  } finally {
    squareLoading.value = false
  }
}

function setSquareCategory(c) {
  squareCategory.value = c
  loadSquare(true)
}

async function toggleFavorite(plan) {
  const on = plan.favorited
  try {
    await api({ url: `/meal-plan/square/${plan.id}/favorite`, method: on ? 'delete' : 'post' })
    plan.favorited = !on
    plan.favoriteCount = Math.max(0, (plan.favoriteCount || 0) + (on ? -1 : 1))
    ElMessage.success(on ? '已取消收藏' : '已收藏')
  } catch (e) {}
}

function goMine() {
  activeTab.value = 'mine'
  router.replace('/meal-plan')
}

function openCreate() {
  createForm.name = ''
  createForm.goal = 'lose'
  createForm.days = 7
  createForm.meals = ['breakfast', 'lunch', 'dinner']
  createDialog.value = true
}

async function submitCreate() {
  if (!createForm.meals.length) {
    ElMessage.warning('至少要选一餐')
    return
  }
  saving.value = true
  try {
    const plan = await api.post('/meal-plan', { ...createForm })
    createDialog.value = false
    ElMessage.success('已创建，接下来往里面加食物')
    await loadMine()
    openSheet(plan.id, 'mine')
  } catch (e) {
  } finally {
    saving.value = false
  }
}

function openSheet(planId, mode) {
  sheetPlanId.value = planId
  sheetMode.value = mode
  sheetVisible.value = true
}

async function onSheetChanged() {
  await Promise.all([loadMine(), loadCurrent(), loadFavorites()])
}

async function onApplied() {
  await Promise.all([loadMine(), loadCurrent()])
}

async function onCopyFromSquare() {
  if (!sheetPlanId.value) return
  try {
    await api.post(`/meal-plan/square/${sheetPlanId.value}/copy`)
    ElMessage.success('已保存到「我的食谱」')
    sheetVisible.value = false
    activeTab.value = 'mine'
    await Promise.all([loadMine(), loadSquare(true)])
  } catch (e) {}
}

async function applyPlan(plan) {
  try {
    await api.post(`/meal-plan/${plan.id}/apply`)
    ElMessage.success('已开始执行')
    await Promise.all([loadMine(), loadCurrent()])
  } catch (e) {}
}

async function removePlan(plan) {
  try {
    await ElMessageBox.confirm(`确定删除「${plan.name}」？删除后不可恢复。`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await api.delete(`/meal-plan/${plan.id}`)
    ElMessage.success('已删除')
    await Promise.all([loadMine(), loadCurrent(), loadFavorites()])
  } catch (e) {}
}

async function unpublish(plan) {
  try {
    await api.post(`/meal-plan/${plan.id}/unpublish`)
    ElMessage.success('已撤回/下架')
    await Promise.all([loadMine(), loadSquare(true)])
  } catch (e) {}
}

function openPublish(plan) {
  publishPlan.value = plan
  publishForm.coverUrl = plan.coverUrl || ''
  publishForm.tags = plan.tags ? [...plan.tags] : []
  publishForm.difficulty = plan.difficulty || '入门'
  publishForm.expectedLoss = plan.expectedLoss || ''
  publishForm.summary = plan.summary || ''
  publishForm.description = plan.description || ''
  publishDialog.value = true
}

function onPlanCommand(cmd, plan) {
  switch (cmd) {
    case 'edit': openSheet(plan.id, 'mine'); break
    case 'publish': openPublish(plan); break
    case 'unpublish': unpublish(plan); break
    case 'pause': pausePlan(plan); break
    case 'resume': resumePlan(plan); break
    case 'finish': finishPlan(plan); break
    case 'delete': removePlan(plan); break
  }
}

async function uploadCover(option) {
  const form = new FormData()
  form.append('file', option.file)
  try {
    const res = await api.post(`/meal-plan/${publishPlan.value.id}/cover`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    publishForm.coverUrl = typeof res === 'string' ? res : (res?.data || '')
    ElMessage.success('封面已上传')
  } catch (e) {}
}

async function submitPublish() {
  saving.value = true
  try {
    await api.post(`/meal-plan/${publishPlan.value.id}/publish`, { ...publishForm })
    publishDialog.value = false
    ElMessage.success('已提交审核，通过后会出现在广场')
    await loadMine()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

watch(activeTab, (tab) => {
  router.replace(tab === 'square' ? '/meal-plan/square' : '/meal-plan')
  if (tab === 'square') loadSquare(true)
  else loadMine()
})

watch(() => route.path, (path) => {
  const tab = path === '/meal-plan/square' ? 'square' : 'mine'
  if (tab !== activeTab.value) activeTab.value = tab
})

onMounted(() => {
  loadCurrent()
  loadMine()
  loadFavorites()
  loadHotRecipes()
  if (activeTab.value === 'square') loadSquare(true)
})
</script>

<style scoped>
.meal-plan-page { padding: 4px; }
.mine-layout {
  display: flex; gap: 20px; align-items: flex-start;
  max-width: 1280px; margin: 0 auto;
}
.mine-main {
  flex: 1; min-width: 0; /* 确保内容区收缩时不溢出 */
}
.mine-sidebar {
  width: 280px; flex: none;
}
.page-card {
  background: #fff; border: 1px solid #eef1f5; border-radius: 16px;
  padding: 28px 30px 32px; /* 移除 max-width，由外层 mine-layout 控制 */
}

/* ====== 标题操作区 ====== */
.page-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 6px; }
.page-title { margin: 0; font-size: 22px; font-weight: 700; color: #1f2d3a; }
.page-sub { margin: 6px 0 0; font-size: 13px; color: #8b98a5; line-height: 1.5; }
.btn-create {
  background: #2b7de9 !important; border-color: #2b7de9 !important;
  font-weight: 600; padding: 12px 24px; font-size: 14px; border-radius: 10px;
}
.btn-create:hover { background: #1a6dd4 !important; }
.btn-create-icon { font-size: 16px; margin-right: 2px; }
.btn-share { border-radius: 10px; font-size: 13px; padding: 10px 18px; }

/* ====== 热门推荐侧边栏 ====== */
.sidebar-card {
  background: #fff; border: 1px solid #eef1f5; border-radius: 16px;
  padding: 20px; position: sticky; top: 4px;
}
.sidebar-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.sidebar-title { font-size: 16px; font-weight: 700; color: #1f2d3a; }
.sidebar-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
.hot-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px; border-radius: 10px; cursor: pointer;
  transition: background .2s;
}
.hot-item:hover { background: #f5f8f6; }
.hot-cover {
  width: 48px; height: 36px; border-radius: 8px; object-fit: cover;
  background: #f0f4f2; flex: none;
}
.hot-info { flex: 1; min-width: 0; }
.hot-name {
  font-size: 13px; font-weight: 600; color: #33414f;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hot-stats { font-size: 11px; color: #9aa7b4; margin-top: 2px; }
.hot-fav-btn {
  flex: none; border: 1px solid #e3e8ee; background: #fff; border-radius: 14px;
  padding: 4px 12px; font-size: 12px; cursor: pointer; color: #7b8a97;
  transition: all .2s;
}
.hot-fav-btn:hover { border-color: #2b7de9; color: #2b7de9; }
.hot-fav-btn.on { background: #e8f1fd; border-color: #2b7de9; color: #2b7de9; }
.hot-empty { text-align: center; color: #a9b6c1; font-size: 13px; padding: 20px 0; }

.sidebar-tips {
  background: #fafcfb; border-radius: 12px; padding: 14px 16px; margin-bottom: 18px;
}
.tips-title { font-size: 13px; font-weight: 600; color: #3fae74; margin-bottom: 8px; }
.tips-list { margin: 0; padding-left: 16px; }
.tips-list li { font-size: 12px; color: #7b8a97; line-height: 1.8; }

.sidebar-deco {
  position: relative; text-align: center; padding: 20px 0 8px;
  background: linear-gradient(180deg, transparent, #f0faf3); border-radius: 12px;
  overflow: hidden;
}
.deco-text {
  font-size: 13px; font-weight: 600; color: #4caf7d; line-height: 1.6;
}
.deco-leaf { font-size: 40px; margin-top: 4px; opacity: .6; }

/* ====== 状态筛选区 ====== */
.status-filter {
  display: flex; gap: 6px; flex-wrap: wrap;
  margin: 20px 0 0; padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}
.status-pill {
  padding: 8px 18px; border-radius: 20px; font-size: 13px; color: #4a5a68;
  background: #f5f6f8; cursor: pointer; transition: all .2s;
  display: flex; align-items: center; gap: 6px;
}
.status-pill:hover { background: #eef1f7; color: #2b7de9; }
.status-pill.active {
  background: #e8f1fd; color: #2b7de9; font-weight: 600;
}
.pill-count {
  font-style: normal; font-size: 12px; background: rgba(43,125,233,.12);
  border-radius: 10px; padding: 1px 8px; font-weight: 600;
}
.status-pill.active .pill-count { background: rgba(43,125,233,.2); }

/* ====== 搜索工具栏 ====== */
.tool-bar { display: flex; gap: 10px; align-items: center; margin: 16px 0 6px; }
.search-input { flex: 1; }
.search-input :deep(.el-input__wrapper) {
  border-radius: 10px; box-shadow: 0 0 0 1px #e3e8ee inset;
}
.search-input :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #c4cdd6 inset; }
.search-icon { font-size: 14px; }
.tool-select { width: 120px; }
.tool-select :deep(.el-select__wrapper) { border-radius: 10px; }
.sort-select { width: 110px; }

/* ====== 执行横幅（紧凑版） ====== */
.exec-banner {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(135deg, #f0faf3, #e8f5ec);
  border: 1px solid #c8e6d3; border-radius: 12px;
  padding: 14px 18px; margin-bottom: 20px; cursor: pointer;
  transition: box-shadow .2s;
}
.exec-banner:hover { box-shadow: 0 2px 12px rgba(76,175,125,.12); }
.exec-banner-left { display: flex; align-items: center; gap: 10px; flex: none; }
.exec-badge { font-size: 10px; }
.badge-active { color: #4caf7d; }
.badge-paused { color: #f0a04b; }
.exec-info { display: flex; flex-direction: column; }
.exec-name { font-size: 14px; font-weight: 700; color: #1f2d3a; }
.exec-detail { font-size: 12px; color: #7b8a97; margin-top: 2px; }
.exec-banner-progress { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.exec-progress-bar {
  height: 6px; border-radius: 3px; background: #d4e8db; overflow: hidden;
}
.exec-progress-fill {
  height: 100%; border-radius: 3px; background: #4caf7d;
  transition: width .4s ease;
}
.exec-progress-text { font-size: 11px; color: #4caf7d; font-weight: 600; }
.exec-banner-actions { display: flex; gap: 4px; flex: none; }

/* ====== 卡片网格 ====== */
.recipe-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
@media (max-width: 960px) {
  .recipe-grid { grid-template-columns: repeat(2, 1fr); }
  .mine-sidebar { display: none; }
}
@media (max-width: 640px) {
  .recipe-grid { grid-template-columns: 1fr; }
}
.recipe-card {
  border: 1px solid #eef1f5; border-radius: 14px; overflow: hidden; cursor: pointer;
  background: #fff; transition: box-shadow .25s, transform .25s;
  display: flex; flex-direction: column;
}
.recipe-card:hover {
  box-shadow: 0 8px 28px rgba(31,45,58,.1); transform: translateY(-3px);
}

/* 封面 */
.rc-cover {
  position: relative; height: 170px; background-size: cover; background-position: center;
  flex: none;
}
.rc-cover-placeholder {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
}
.rc-cover-emoji { font-size: 56px; opacity: .7; }
.rc-cover-emoji.large { font-size: 72px; }
.rc-exec-dot {
  position: absolute; top: 10px; right: 10px; width: 10px; height: 10px; border-radius: 50%;
  background: #4caf7d; box-shadow: 0 0 0 4px rgba(76,175,125,.25);
}
.rc-vis-badge {
  position: absolute; top: 10px; left: 10px;
  font-size: 11px; padding: 3px 10px; border-radius: 10px;
  background: rgba(255,255,255,.85); color: #5b6b7a;
  backdrop-filter: blur(4px);
}
.rc-vis-badge.pub { color: #2b7de9; background: rgba(232,241,253,.9); }

/* 卡片内容 */
.rc-body { padding: 14px 16px 16px; display: flex; flex-direction: column; flex: 1; }
.rc-title-row { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.rc-name {
  font-size: 15px; font-weight: 700; color: #1f2d3a;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0;
}
.rc-status-tag { flex: none; font-size: 11px; padding: 3px 10px; border-radius: 10px; font-weight: 500; }
.tag-none { background: #f3f5f8; color: #7b8a97; }
.tag-pending { background: #fef3e7; color: #d98a2b; }
.tag-approved { background: #e8f1fd; color: #2b7de9; }
.tag-rejected { background: #fdeceb; color: #e06055; }
.tag-executing { background: #e6f6ec !important; color: #3fae74 !important; }

.rc-meta { margin-top: 8px; font-size: 12px; color: #8b98a5; display: flex; align-items: center; gap: 4px; }
.rc-meta-dot { color: #c8cdd4; }

.rc-nutrition { margin-top: 10px; display: flex; align-items: baseline; flex-wrap: wrap; }
.rn-kcal { font-size: 18px; font-weight: 700; color: #1f2d3a; }
.rn-unit { font-size: 12px; color: #7b8a97; margin-right: 6px; }
.rn-sep { font-size: 12px; color: #d0d5db; margin: 0 6px; }
.rn-label { font-size: 11px; color: #4caf7d; background: #e6f6ec; border-radius: 4px; padding: 2px 8px; }
.rn-empty { font-size: 13px; color: #9aa7b4; }

.rc-progress { margin-top: 10px; display: flex; flex-direction: column; gap: 4px; }
.rc-progress-bar {
  height: 5px; border-radius: 3px; background: #eef3f0; overflow: hidden;
}
.rc-progress-fill {
  height: 100%; border-radius: 3px; background: #4caf7d;
  transition: width .4s ease;
}
.rp-text { font-size: 11px; color: #4caf7d; font-weight: 500; }

.rc-divider { height: 1px; background: #f0f2f5; margin: 14px -16px 0; }
.rc-actions {
  display: flex; align-items: center; justify-content: space-between; margin-top: 12px; flex: none;
}
.rc-action-left { display: flex; gap: 4px; }
.rc-action-right { display: flex; align-items: center; gap: 4px; }
.action-btn { font-size: 12px !important; padding: 4px 8px !important; }
.action-more { font-size: 16px !important; letter-spacing: 1px; color: #9aa7b4 !important; }
.rc-author { font-size: 12px; color: #9aa7b4; }
.rc-fav { font-size: 16px; color: #b8c2cc; cursor: pointer; transition: color .2s; }
.rc-fav.on { color: #f0a04b; }
.rc-fav:hover { color: #f0a04b; }

/* ====== 分区标题 ====== */
.section-head { display: flex; align-items: center; justify-content: space-between; margin: 28px 0 14px; }
.section-title { font-size: 16px; font-weight: 700; color: #1f2d3a; display: flex; align-items: center; gap: 6px; }
.section-icon { font-size: 15px; }
.section-count {
  font-size: 12px; background: #f3f5f8; color: #7b8a97; border-radius: 10px;
  padding: 2px 10px; font-weight: 400;
}
.section-hint { font-size: 12px; color: #9aa7b4; }

/* ====== 广场搜索 ====== */
.square-search-box { margin-top: 18px; }
.square-search-input :deep(.el-input__wrapper) {
  border-radius: 12px; box-shadow: 0 0 0 1px #e3e8ee inset; padding: 10px 14px;
}
.square-search-input :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #2b7de9 inset; }

/* ====== 广场分类筛选 ====== */
.square-categories {
  display: flex; gap: 8px; flex-wrap: wrap;
  margin: 16px 0; padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f5;
}
.cat-pill {
  padding: 8px 16px; border-radius: 20px; font-size: 13px; color: #4a5a68;
  background: #f5f6f8; cursor: pointer; transition: all .2s;
  display: flex; align-items: center; gap: 5px;
}
.cat-pill:hover { background: #eef1f7; color: #2b7de9; }
.cat-pill.active { background: #e8f1fd; color: #2b7de9; font-weight: 600; }
.cat-icon { font-size: 14px; }

/* ====== 排序 pills ====== */
.sort-pills { display: flex; gap: 6px; }
.sort-pill {
  padding: 6px 14px; border-radius: 16px; font-size: 12px; color: #7b8a97;
  background: #f5f6f8; cursor: pointer; transition: all .2s;
}
.sort-pill:hover { color: #2b7de9; }
.sort-pill.active { background: #e8f1fd; color: #2b7de9; font-weight: 600; }

/* ====== 精选推荐 ====== */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 960px) {
  .featured-grid { grid-template-columns: repeat(2, 1fr); }
}
.fc-cover {
  position: relative; height: 200px; background-size: cover; background-position: center;
}
.fc-fav-badge {
  position: absolute; top: 10px; right: 10px; font-size: 18px; color: rgba(255,255,255,.7);
  cursor: pointer; text-shadow: 0 1px 2px rgba(0,0,0,.15); transition: color .2s;
}
.fc-fav-badge.on { color: #f0a04b; }
.fc-body { padding: 14px 16px; }
.fc-name { font-size: 15px; font-weight: 700; color: #1f2d3a;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fc-meta { margin-top: 6px; font-size: 12px; color: #8b98a5; display: flex; align-items: center; gap: 4px; }
.fc-nutrition { margin-top: 8px; }
.fc-kcal { font-size: 20px; font-weight: 700; color: #1f2d3a; }
.fc-kcal-unit { font-size: 11px; color: #8b98a5; }
.fc-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 12px; padding-top: 10px; border-top: 1px solid #f0f2f5;
}
.fc-author { font-size: 12px; color: #9aa7b4; }
.fc-usage { font-size: 12px; color: #d98a2b; }

/* ====== 加载更多 ====== */
.more-row { display: flex; justify-content: center; margin: 24px 0 6px; }
.more-btn {
  border: 1px solid #e3e8ee; background: #fff; color: #4a5a68; border-radius: 22px;
  padding: 10px 40px; font-size: 13px; cursor: pointer; transition: all .2s;
}
.more-btn:hover { color: #2b7de9; border-color: #bcd7f7; background: #fafcff; }

/* ====== 空状态 ====== */
.empty-box {
  grid-column: 1 / -1; text-align: center; color: #a9b6c1; padding: 40px 0;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.empty-icon { font-size: 40px; }
.empty-text { font-size: 15px; font-weight: 600; color: #8b98a5; }
.empty-sub { font-size: 13px; color: #a9b6c1; max-width: 280px; }

/* ====== 弹窗 ====== */
.cover-upload { display: flex; align-items: center; gap: 12px; }
.cover-preview { width: 64px; height: 64px; border-radius: 10px; object-fit: cover; background: #f3f6f4; }
.cover-tip { font-size: 12px; color: #a9b6c1; }
.publish-note { font-size: 12px; color: #8b98a5; padding: 6px 8px; background: #fafcfb; border-radius: 8px; margin-top: 4px; }

.exec-summary { display: flex; gap: 12px; margin-bottom: 14px; }
.exec-summary-item {
  flex: 1; background: #f5f8f6; border-radius: 12px; padding: 14px 8px; text-align: center;
}
.es-value { font-size: 20px; font-weight: 700; color: #3fae74; line-height: 1.2; }
.es-label { margin-top: 4px; font-size: 12px; color: #8b98a5; }
.day-progress-list { max-height: 52vh; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.day-progress { border: 1px solid #e9f0ec; border-radius: 10px; padding: 12px 14px; }
.day-progress.done { border-color: #c8e6d3; background: #f5fbf7; }
.dp-head { display: flex; align-items: center; gap: 10px; }
.dp-day { font-size: 14px; font-weight: 600; color: #1f2d3a; }
.dp-date { font-size: 12px; color: #a9b6c1; }
.dp-status { margin-left: auto; font-size: 12px; color: #8b98a5; }
.dp-status.ok { color: #4caf7d; font-weight: 600; }
.dp-meals { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.dp-meal { font-size: 12px; padding: 3px 9px; border-radius: 12px; background: #f2f6f3; color: #9aa7b4; }
.dp-meal.ok { background: #e6f6ec; color: #3fae74; }
.dp-kcal { margin-top: 8px; font-size: 12px; color: #7b8a97; }

/* ====== 暗色主题 ====== */
html.dark .page-card { background: #1b2129; border-color: #2a333d; }
html.dark .page-title, html.dark .rc-name, html.dark .section-title,
html.dark .rn-kcal, html.dark .fc-name, html.dark .fc-kcal,
html.dark .exec-name, html.dark .dp-day { color: #d8e2ea; }
html.dark .page-sub, html.dark .rc-meta, html.dark .fc-meta, html.dark .rc-author,
html.dark .fc-author, html.dark .rn-unit { color: #8ea0b0; }
html.dark .status-pill { background: #232b34; color: #9aabba; }
html.dark .status-pill.active { background: #1d334d; color: #5b9bd5; }
html.dark .pill-count { background: rgba(91,155,213,.15); }
html.dark .status-pill.active .pill-count { background: rgba(91,155,213,.25); }
html.dark .cat-pill { background: #232b34; color: #9aabba; }
html.dark .cat-pill.active { background: #1d334d; color: #5b9bd5; }
html.dark .sort-pill { background: #232b34; }
html.dark .sort-pill.active { background: #1d334d; color: #5b9bd5; }
html.dark .recipe-card, html.dark .featured-card { background: #1b2129; border-color: #2a333d; }
html.dark .rc-divider, html.dark .fc-foot { border-color: #2a333d; background: #2a333d; }
html.dark .status-filter, html.dark .square-categories { border-bottom-color: #2a333d; }
html.dark .tool-bar .search-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #333e49 inset; background: #232b34;
}
html.dark .exec-banner { background: #1e2a24; border-color: #2e4637; }
html.dark .exec-progress-bar { background: #2a3d32; }
html.dark .exec-detail { color: #8ea0b0; }
html.dark .section-count { background: #232b34; color: #8ea0b0; }
html.dark .rn-label { background: #24322b; }
html.dark .more-btn { background: #232b34; border-color: #333e49; color: #c8d6e5; }
html.dark .more-btn:hover { background: #28323d; }
html.dark .day-progress { border-color: #2a333d; }
html.dark .day-progress.done { background: #203028; border-color: #2e4637; }
html.dark .dp-meal { background: #232b34; }
html.dark .exec-summary-item { background: #202730; }
html.dark .tag-none { background: #232b34; color: #9aabba; }
html.dark .publish-note { background: #202730; }
html.dark .cover-preview { background: #232b34; }
html.dark .rc-vis-badge { background: rgba(27,33,41,.85); color: #8ea0b0; }
html.dark .rc-vis-badge.pub { background: rgba(29,51,77,.9); color: #5b9bd5; }
html.dark .sidebar-card { background: #1b2129; border-color: #2a333d; }
html.dark .sidebar-title { color: #d8e2ea; }
html.dark .hot-item:hover { background: #232b34; }
html.dark .hot-name { color: #d8e2ea; }
html.dark .hot-fav-btn { background: #232b34; border-color: #333e49; color: #c8d6e5; }
html.dark .sidebar-tips { background: #202730; }
html.dark .tips-title { color: #3fae74; }
html.dark .tips-list li { color: #8ea0b0; }
html.dark .sidebar-deco { background: linear-gradient(180deg, transparent, #1a2820); }
</style>