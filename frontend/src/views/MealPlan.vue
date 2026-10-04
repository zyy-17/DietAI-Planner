<template>
  <div class="meal-plan-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">🍲 食谱</h2>
        <p class="page-sub">自己写食谱、按天执行，也可以去广场看看别人怎么吃</p>
      </div>
      <el-button type="primary" @click="openCreate">＋ 新建食谱</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ══════════ 我的食谱 ══════════ -->
      <el-tab-pane label="我的食谱" name="mine">
        <!-- 正在执行 -->
        <el-card v-if="current.plan" shadow="never" class="exec-card">
          <div class="exec-head">
            <div class="exec-title">
              <span class="exec-name">{{ current.plan.name }}</span>
              <span class="badge-active">● 执行中</span>
            </div>
            <div class="exec-meta">
              <span>目标：{{ current.plan.goalLabel }}</span>
              <span>周期：{{ current.plan.days }}天</span>
              <span>每日：{{ fmt(current.plan.plannedDailyCalories) }} kcal</span>
              <span v-if="current.plan.dailyCalories">目标热量：{{ fmt(current.plan.dailyCalories) }} kcal</span>
            </div>
          </div>

          <div class="progress-row">
            <span class="progress-label">今日进度</span>
            <span v-for="p in current.progress || []" :key="p.mealType" class="progress-chip"
              :class="{ done: p.done }">
              {{ p.mealLabel }} {{ p.done ? '✓' : '○' }}
            </span>
            <span class="progress-intake" v-if="current.todayIntakeCalories">
              已记录 {{ fmt(current.todayIntakeCalories) }} kcal
            </span>
          </div>

          <div class="today-meals">
            <div class="today-meal" v-for="g in current.today?.meals || []" :key="g.mealType">
              <div class="today-meal-label">{{ mealIcon(g.mealType) }} {{ g.mealLabel }}</div>
              <div class="today-meal-items" v-if="g.items.length">
                {{ g.items.map(i => `${i.foodName} ${i.amountText}`).join(' · ') }}
              </div>
              <div class="today-meal-items empty" v-else>还没安排</div>
            </div>
          </div>

          <div class="exec-actions">
            <el-button size="small" @click="openSheet(current.plan.id, 'mine')">查看完整食谱</el-button>
            <el-button size="small" type="primary" plain @click="openSheet(current.plan.id, 'mine')">
              调整今日饮食
            </el-button>
          </div>
        </el-card>

        <div class="section-head">
          <span class="section-title">我的食谱（{{ myRecipes.length }}）</span>
          <el-radio-group v-model="mineFilter" size="small">
            <el-radio-button value="recipes">在用</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="archived">历史</el-radio-button>
          </el-radio-group>
        </div>

        <div class="recipe-grid" v-loading="mineLoading">
          <div class="recipe-card" v-for="plan in myRecipes" :key="plan.id" @click="openSheet(plan.id, 'mine')">
            <div class="rc-cover" :style="coverStyle(plan)">
              <span class="rc-status">{{ plan.statusLabel }}</span>
              <span class="rc-publish" :class="'pub-' + plan.publishStatus">{{ plan.publishStatusLabel }}</span>
            </div>
            <div class="rc-body">
              <div class="rc-name">{{ plan.name }}</div>
              <div class="rc-meta">
                <span>📅 {{ plan.days }}天</span>
                <span>{{ plan.goalLabel }}</span>
                <span>{{ fmt(plan.plannedDailyCalories) }} kcal/天</span>
              </div>
              <div class="rc-tags" v-if="plan.tags?.length">
                <span v-for="t in plan.tags.slice(0, 3)" :key="t" class="rc-tag">{{ t }}</span>
              </div>
              <div class="rc-actions" @click.stop>
                <el-button size="small" @click="openSheet(plan.id, 'mine')">编辑</el-button>
                <el-button size="small" type="primary" plain v-if="plan.status !== 'active'"
                  @click="applyPlan(plan)">开始执行</el-button>
                <el-button size="small" v-if="plan.onSquare" @click="unpublish(plan)">下架</el-button>
                <el-button size="small" type="success" plain v-else @click="openPublish(plan)">发布</el-button>
                <el-button size="small" type="danger" plain @click="removePlan(plan)">删除</el-button>
              </div>
            </div>
          </div>
          <div class="empty-box" v-if="!myRecipes.length && !mineLoading">
            还没有食谱，点右上角「＋ 新建食谱」自己搭一份吧
          </div>
        </div>

        <div class="section-head" v-if="favorites.length">
          <span class="section-title">我收藏的（{{ favorites.length }}）</span>
        </div>
        <div class="recipe-grid" v-if="favorites.length">
          <div class="recipe-card" v-for="plan in favorites" :key="'f' + plan.id"
            @click="openSheet(plan.id, 'square')">
            <div class="rc-cover" :style="coverStyle(plan)">
              <span class="rc-publish pub-approved">已收藏</span>
            </div>
            <div class="rc-body">
              <div class="rc-name">{{ plan.name }}</div>
              <div class="rc-meta">
                <span>📅 {{ plan.days }}天</span>
                <span>{{ plan.goalLabel }}</span>
                <span>{{ plan.usageCount }}+ 人使用</span>
              </div>
              <div class="rc-author">由{{ plan.authorName }}创建</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ══════════ 食谱广场 ══════════ -->
      <el-tab-pane label="食谱广场" name="square">
        <div class="square-head">
          <el-radio-group v-model="squareSort" size="small" @change="loadSquare(true)">
            <el-radio-button value="hot">🔥 热门</el-radio-button>
            <el-radio-button value="new">🆕 最新</el-radio-button>
          </el-radio-group>
          <el-input v-model="squareKeyword" size="small" placeholder="搜索食谱名称" clearable
            style="width: 220px" @keyup.enter="loadSquare(true)" @clear="loadSquare(true)">
            <template #append><el-button @click="loadSquare(true)">搜索</el-button></template>
          </el-input>
        </div>

        <div class="recipe-grid" v-loading="squareLoading">
          <div class="recipe-card" v-for="plan in squareList" :key="'s' + plan.id"
            @click="openSheet(plan.id, 'square')">
            <div class="rc-cover" :style="coverStyle(plan)">
              <div class="rc-tags-top" v-if="plan.tags?.length">
                <span v-for="t in plan.tags.slice(0, 4)" :key="t">{{ t }}</span>
              </div>
            </div>
            <div class="rc-body">
              <div class="rc-name">{{ plan.name }}</div>
              <div class="rc-meta">
                <span>📅 {{ plan.days }}天</span>
                <span>{{ plan.goalLabel }}</span>
                <span>{{ plan.expectedLoss || plan.difficulty || '—' }}</span>
              </div>
              <div class="rc-foot">
                <span class="rc-usage">🔥 {{ plan.usageCount }}+ 人使用中</span>
                <span class="rc-fav" v-if="plan.favorited">★</span>
              </div>
            </div>
          </div>
          <div class="empty-box" v-if="!squareList.length && !squareLoading">
            广场上还没有食谱，去「我的食谱」发布一份试试
          </div>
        </div>

        <div class="more-row" v-if="squareHasMore">
          <button class="more-btn" @click="loadSquare(false)">👨‍🍳 更多热门食谱</button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建食谱 -->
    <el-dialog v-model="createDialog" title="新建食谱" width="480px">
      <el-form label-width="80px">
        <el-form-item label="食谱名">
          <el-input v-model="createForm.name" placeholder="留空自动命名为「7天减脂食谱」" />
        </el-form-item>
        <el-form-item label="膳食目标">
          <el-radio-group v-model="createForm.goal">
            <el-radio-button value="lose">减脂</el-radio-button>
            <el-radio-button value="gain">增重</el-radio-button>
            <el-radio-button value="maintain">维持体重</el-radio-button>
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
            <el-checkbox value="breakfast">早餐</el-checkbox>
            <el-checkbox value="lunch">午餐</el-checkbox>
            <el-checkbox value="dinner">晚餐</el-checkbox>
            <el-checkbox value="snack">加餐</el-checkbox>
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
          <el-input v-model="publishForm.expectedLoss" placeholder="如 -0.5~-1" />
        </el-form-item>
        <el-form-item label="一句话概述">
          <el-input v-model="publishForm.summary" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="心得 / 思路">
          <el-input v-model="publishForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit
            placeholder="说说这份食谱的整体思路，广场详情里会展示" />
        </el-form-item>
      </el-form>
      <div class="publish-note">提交后需管理员审核通过才会出现在广场；审核期间可以继续编辑。</div>
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPublish">提交审核</el-button>
      </template>
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

const activeTab = ref(route.path === '/meal-plan/square' ? 'square' : 'mine')
const current = ref({})
const myRecipes = ref([])
const favorites = ref([])
const mineFilter = ref('recipes')
const mineLoading = ref(false)

const squareList = ref([])
const squarePage = ref(0)
const squareHasMore = ref(false)
const squareLoading = ref(false)
const squareSort = ref('hot')
const squareKeyword = ref('')

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

function fmt(v) {
  const n = Number(v)
  if (!n || Number.isNaN(n)) return '0'
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

function mealIcon(type) {
  return { breakfast: '🌅', lunch: '☀️', dinner: '🌙', snack: '🍎' }[type] || '🍽️'
}

function coverStyle(plan) {
  return plan.coverUrl
    ? { backgroundImage: `url(${plan.coverUrl})` }
    : { background: 'linear-gradient(120deg, #8fd3a8, #4caf7d)' }
}

async function loadCurrent() {
  try {
    current.value = (await api.get('/meal-plan/current')) || {}
  } catch (e) {
    current.value = {}
  }
}

async function loadMine() {
  mineLoading.value = true
  try {
    myRecipes.value = (await api.get('/meal-plan', { params: { status: mineFilter.value } })) || []
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

async function loadSquare(reset = true) {
  if (reset) squarePage.value = 0
  squareLoading.value = true
  try {
    const res = await api.get('/meal-plan/square', {
      params: {
        sort: squareSort.value,
        keyword: squareKeyword.value || undefined,
        page: squarePage.value,
        size: 12
      }
    })
    const content = res?.content || []
    squareList.value = reset ? content : squareList.value.concat(content)
    squareHasMore.value = !res?.last && content.length > 0
    if (content.length) squarePage.value += 1
  } catch (e) {
    if (reset) squareList.value = []
    squareHasMore.value = false
  } finally {
    squareLoading.value = false
  }
}

function onTabChange(name) {
  router.replace(name === 'square' ? '/meal-plan/square' : '/meal-plan')
  if (name === 'square') loadSquare(true)
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
    // 拦截器已提示
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
  } catch (e) {
    // 拦截器已提示
  }
}

async function applyPlan(plan) {
  try {
    await api.post(`/meal-plan/${plan.id}/apply`)
    ElMessage.success('已开始执行')
    await Promise.all([loadMine(), loadCurrent()])
  } catch (e) {
    // 拦截器已提示
  }
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
  } catch (e) {
    // 拦截器已提示
  }
}

async function unpublish(plan) {
  try {
    await api.post(`/meal-plan/${plan.id}/unpublish`)
    ElMessage.success('已下架')
    await Promise.all([loadMine(), loadSquare(true)])
  } catch (e) {
    // 拦截器已提示
  }
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

async function uploadCover(option) {
  const form = new FormData()
  form.append('file', option.file)
  try {
    const res = await api.post(`/meal-plan/${publishPlan.value.id}/cover`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // 拦截器已解包，这里拿到的是 URL 字符串
    publishForm.coverUrl = typeof res === 'string' ? res : (res?.data || '')
    ElMessage.success('封面已上传')
  } catch (e) {
    // 拦截器已提示
  }
}

async function submitPublish() {
  saving.value = true
  try {
    await api.post(`/meal-plan/${publishPlan.value.id}/publish`, { ...publishForm })
    publishDialog.value = false
    ElMessage.success('已提交审核，通过后会出现在广场')
    await loadMine()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

watch(mineFilter, loadMine)
watch(() => route.path, (path) => {
  const tab = path === '/meal-plan/square' ? 'square' : 'mine'
  if (tab !== activeTab.value) activeTab.value = tab
})

onMounted(() => {
  loadCurrent()
  loadMine()
  loadFavorites()
  if (activeTab.value === 'square') loadSquare(true)
})
</script>

<style scoped>
.meal-plan-page { padding: 4px; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 8px; }
.page-title { margin: 0; font-size: 20px; color: #2f4152; }
.page-sub { margin: 4px 0 0; font-size: 12px; color: #8b98a5; }

.exec-card { border-radius: 12px; margin-bottom: 18px; border: 1px solid #e6efe9; }
.exec-head { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.exec-title { display: flex; align-items: center; gap: 10px; }
.exec-name { font-size: 16px; font-weight: 700; color: #2f4152; }
.badge-active { font-size: 12px; color: #3fae74; }
.exec-meta { display: flex; gap: 16px; font-size: 13px; color: #7b8a97; }

.progress-row { display: flex; align-items: center; gap: 10px; margin: 14px 0 10px; flex-wrap: wrap; }
.progress-label { font-size: 13px; color: #8b98a5; }
.progress-chip { font-size: 12px; padding: 4px 10px; border-radius: 14px; background: #f2f6f3; color: #9aa7b4; }
.progress-chip.done { background: #e6f6ec; color: #3fae74; font-weight: 600; }
.progress-intake { margin-left: auto; font-size: 12px; color: #8b98a5; }

.today-meals { background: #fafcfb; border-radius: 10px; padding: 10px 14px; }
.today-meal { display: flex; gap: 10px; padding: 5px 0; font-size: 13px; }
.today-meal-label { width: 84px; color: #5b6b7a; flex: none; }
.today-meal-items { color: #33414f; }
.today-meal-items.empty { color: #a9b6c1; }
.exec-actions { margin-top: 12px; display: flex; justify-content: flex-end; gap: 8px; }

.section-head { display: flex; align-items: center; justify-content: space-between; margin: 22px 0 12px; }
.section-title { font-size: 15px; font-weight: 700; color: #2f4152; }

.recipe-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 16px; }
.recipe-card { border: 1px solid #e9f0ec; border-radius: 12px; overflow: hidden; cursor: pointer;
  background: #fff; transition: box-shadow .2s, transform .2s; }
.recipe-card:hover { box-shadow: 0 6px 18px rgba(76, 175, 125, .14); transform: translateY(-2px); }
.rc-cover { position: relative; height: 110px; background-size: cover; background-position: center; }
.rc-status, .rc-publish { position: absolute; top: 8px; font-size: 11px; padding: 3px 8px; border-radius: 10px; }
.rc-status { left: 8px; background: rgba(255,255,255,.9); color: #3fae74; }
.rc-publish { right: 8px; background: rgba(0,0,0,.42); color: #fff; }
.pub-pending { background: rgba(240,160,75,.92) !important; }
.pub-approved { background: rgba(76,175,125,.92) !important; }
.pub-rejected { background: rgba(224,96,85,.92) !important; }
.rc-tags-top { display: flex; flex-wrap: wrap; gap: 6px; padding: 10px; }
.rc-tags-top span { background: rgba(255,255,255,.82); color: #456; font-size: 11px;
  padding: 3px 8px; border-radius: 4px; }
.rc-body { padding: 12px 14px 14px; }
.rc-name { font-size: 15px; font-weight: 700; color: #2f4152; margin-bottom: 8px; }
.rc-meta { display: flex; flex-wrap: wrap; gap: 10px; font-size: 12px; color: #7b8a97; }
.rc-tags { display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.rc-tag { font-size: 11px; color: #3fae74; background: #eef7f1; padding: 2px 8px; border-radius: 4px; }
.rc-author { margin-top: 8px; font-size: 12px; color: #9aa7b4; }
.rc-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.rc-usage { font-size: 12px; color: #3fae74; }
.rc-fav { color: #f0a04b; }
.rc-actions { display: flex; gap: 6px; margin-top: 12px; flex-wrap: wrap; }

.square-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.more-row { display: flex; justify-content: center; margin: 22px 0 8px; }
.more-btn { border: none; background: #4caf7d; color: #fff; border-radius: 24px; padding: 12px 44px;
  font-size: 15px; cursor: pointer; }
.empty-box { grid-column: 1 / -1; text-align: center; color: #a9b6c1; font-size: 13px; padding: 36px 0; }

.cover-upload { display: flex; align-items: center; gap: 12px; }
.cover-preview { width: 64px; height: 64px; border-radius: 10px; object-fit: cover; background: #f3f6f4; }
.cover-tip { font-size: 12px; color: #a9b6c1; }
.publish-note { font-size: 12px; color: #8b98a5; padding: 0 8px; }

html.dark .page-title, html.dark .exec-name, html.dark .section-title,
html.dark .rc-name, html.dark .today-meal-items { color: #d8e2ea; }
html.dark .exec-card, html.dark .recipe-card { background: #1b2129; border-color: #2a333d; }
html.dark .today-meals { background: #202730; }
html.dark .progress-chip { background: #232b34; }
</style>
