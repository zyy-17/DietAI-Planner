<template>
  <div class="meal-plan-page">
    <!-- ── 页头 ─────────────────────────────────────────── -->
    <div class="page-head">
      <div class="title-block">
        <div class="page-title">🍱 膳食方案</div>
        <div class="page-sub">看当前执行的方案、看今天吃什么、创建或调整方案</div>
      </div>
      <el-button type="primary" round @click="openCreate">＋ 创建膳食方案</el-button>
    </div>

    <el-tabs v-model="activeTab" class="mp-tabs" @tab-change="onTabChange">
      <!-- ══ 当前方案 ══════════════════════════════════ -->
      <el-tab-pane label="当前方案" name="current">
        <div v-loading="loading">
          <!-- 还没方案 -->
          <el-card v-if="!current.plan" shadow="never" class="card empty-card">
            <div class="empty-icon">🍽️</div>
            <div class="empty-title">还没有正在执行的方案</div>
            <div class="empty-desc">
              告诉 AI 你的目标、周期和忌口，它会结合你的健康档案与食物库，
              排出一份可以逐天执行的膳食方案
            </div>
            <el-button type="primary" round @click="openCreate">＋ 创建膳食方案</el-button>
          </el-card>

          <!-- 当前方案 -->
          <el-card v-else shadow="never" class="card plan-card">
            <div class="plan-head">
              <div class="plan-name">
                🥗 {{ current.plan.name }}
                <span class="status-dot" :class="current.plan.status === 'active' ? 'on' : 'off'"></span>
                <span class="status-text">
                  {{ current.plan.status === 'active' ? '执行中' : '已归档' }}
                </span>
              </div>
              <el-button link type="primary" size="small" @click="openCreate">换一份</el-button>
            </div>

            <div class="plan-meta">
              <span class="meta-item">目标：<b>{{ current.plan.goalLabel }}</b></span>
              <span class="meta-item">周期：<b>{{ current.plan.days }}天</b></span>
              <span class="meta-item">每日：<b class="kcal">{{ num(current.plan.dailyCalories) }} kcal</b></span>
              <span class="meta-item">
                今天是第 <b>{{ current.currentDayIndex }}</b> 天
              </span>
            </div>

            <div v-if="current.plan.summary" class="plan-summary">{{ current.plan.summary }}</div>

            <!-- 今日进度：由今日饮食记录推导 -->
            <div class="block">
              <div class="block-title">今日进度</div>
              <div class="progress-row">
                <div v-for="p in current.progress" :key="p.mealType" class="progress-item"
                  :class="{ done: p.done }">
                  <span class="progress-mark">{{ p.done ? '✓' : '○' }}</span>
                  <span class="progress-label">{{ p.mealLabel }}</span>
                </div>
                <span class="progress-intake">
                  今日已记录 {{ num(current.todayIntakeCalories) }} kcal
                </span>
              </div>
            </div>

            <!-- 今日膳食 -->
            <div class="block">
              <div class="block-title">今日膳食</div>
              <template v-if="todayGroups.length">
                <div v-for="group in todayGroups" :key="group.mealType" class="meal-line">
                  <span class="meal-icon">{{ mealIcon(group.mealType) }}</span>
                  <span class="meal-name">{{ group.mealLabel }}</span>
                  <span class="meal-foods">
                    {{ group.items.map(i => `${i.foodName} ${i.amountText}`).join(' · ') }}
                  </span>
                  <span class="meal-kcal">{{ num(group.calories) }} kcal</span>
                </div>
              </template>
              <div v-else class="muted">今天这一天还没有安排食物</div>
            </div>

            <div class="plan-foot">
              <el-button round @click="goDetail">查看完整方案</el-button>
              <el-button type="primary" round @click="router.push('/today')">调整今日饮食</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ══ 历史方案 ══════════════════════════════════ -->
      <el-tab-pane label="历史方案" name="history">
        <div v-loading="historyLoading">
          <el-card v-if="!history.length" shadow="never" class="card empty-card">
            <div class="empty-icon">🗂️</div>
            <div class="empty-title">还没有历史方案</div>
            <div class="empty-desc">创建新方案时，原来正在执行的方案会自动移到这里</div>
          </el-card>

          <div v-else class="history-grid">
            <el-card v-for="plan in history" :key="plan.id" shadow="never" class="card history-card">
              <div class="history-name">{{ plan.name }}</div>
              <div class="history-meta">
                {{ plan.goalLabel }} · {{ plan.days }}天 · {{ num(plan.dailyCalories) }} kcal/天
              </div>
              <div class="history-sub">
                {{ plan.itemCount }} 条食物 · 开始于 {{ plan.startDate }}
              </div>
              <div class="history-foot">
                <el-button link type="primary" size="small" @click="goDetail(plan.id)">查看</el-button>
                <el-button link type="danger" size="small" @click="removePlan(plan)">删除</el-button>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ══ 创建方案弹窗 ══════════════════════════════ -->
    <el-dialog v-model="createVisible" title="创建个性化膳食方案" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="96px">
        <el-form-item label="膳食目标">
          <el-radio-group v-model="form.goal">
            <el-radio-button value="lose">减脂</el-radio-button>
            <el-radio-button value="gain">增重</el-radio-button>
            <el-radio-button value="maintain">维持体重</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="方案周期">
          <el-radio-group v-model="form.days">
            <el-radio-button :value="3">3天</el-radio-button>
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="14">14天</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="每日餐次">
          <el-checkbox-group v-model="form.meals">
            <el-checkbox value="breakfast">早餐</el-checkbox>
            <el-checkbox value="lunch">午餐</el-checkbox>
            <el-checkbox value="dinner">晚餐</el-checkbox>
            <el-checkbox value="snack">加餐</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="饮食偏好">
          <el-select v-model="form.preferences" multiple filterable allow-create default-first-option
            placeholder="如：清淡、高蛋白、家常菜" style="width:100%">
            <el-option v-for="o in preferenceOptions" :key="o.code" :value="o.label" :label="o.label" />
          </el-select>
        </el-form-item>

        <el-form-item label="不喜欢/不能吃">
          <el-select v-model="form.dislikes" multiple filterable allow-create default-first-option
            placeholder="输入后回车，如：鸡胸肉、芹菜" style="width:100%">
            <el-option v-for="name in dislikeOptions" :key="name" :value="name" :label="name" />
          </el-select>
        </el-form-item>

        <el-form-item label="其他要求">
          <el-input v-model="form.extraRequirement" type="textarea" :rows="2"
            placeholder="如：中午在公司吃、不要太辣" />
        </el-form-item>
      </el-form>

      <div class="dialog-hint">
        AI 会综合你的健康档案、目标热量、饮食偏好、忌口与系统食物库来排餐，
        热量与营养值全部按食物库精确换算，不由模型估算。
      </div>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="generate">
          AI 生成方案
        </el-button>
      </template>
    </el-dialog>

    <!-- ══ 方案预览弹窗 ══════════════════════════════ -->
    <el-dialog v-model="previewVisible" :title="draft ? draft.name : '方案预览'" width="720px"
      :close-on-click-modal="false" top="6vh">
      <div v-if="draft" class="preview">
        <div class="preview-meta">
          <span>目标：<b>{{ draft.goalLabel }}</b></span>
          <span>周期：<b>{{ draft.days }}天</b></span>
          <span>每日目标：<b class="kcal">{{ num(draft.dailyCalories) }} kcal</b></span>
        </div>
        <div v-if="draft.summary" class="plan-summary">{{ draft.summary }}</div>

        <el-alert v-if="draft.unmatchedFoods && draft.unmatchedFoods.length" type="warning"
          :closable="false" show-icon class="preview-alert"
          :title="`有 ${draft.unmatchedFoods.length} 种食物不在食物库里，已跳过：${draft.unmatchedFoods.join('、')}`" />

        <div class="preview-days">
          <div v-for="day in draft.planDays" :key="day.dayIndex" class="preview-day">
            <div class="preview-day-head">
              <span class="day-label">第{{ day.dayIndex }}天</span>
              <span class="day-kcal">{{ num(day.calories) }} kcal</span>
            </div>
            <div v-for="group in day.meals" :key="group.mealType" class="preview-meal">
              <span class="meal-icon">{{ mealIcon(group.mealType) }}</span>
              <span class="meal-name">{{ group.mealLabel }}</span>
              <span class="meal-foods">
                {{ group.items.map(i => `${i.foodName} ${i.amountText}`).join(' · ') }}
              </span>
            </div>
          </div>
        </div>

        <div class="preview-nutrition">
          <div class="nutrition-title">预计每日</div>
          <div class="nutrition-row">
            <span>热量 <b class="kcal">{{ avgNutrition.calories }}</b> kcal</span>
            <span>蛋白质 <b>{{ avgNutrition.protein }}</b> g</span>
            <span>碳水 <b>{{ avgNutrition.carbohydrate }}</b> g</span>
            <span>脂肪 <b>{{ avgNutrition.fat }}</b> g</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button :loading="generating" @click="generate">重新生成</el-button>
        <el-button type="primary" :loading="adopting" @click="adopt">采用此方案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const historyLoading = ref(false)
const generating = ref(false)
const adopting = ref(false)
const createVisible = ref(false)
const previewVisible = ref(false)
const draft = ref(null)

const current = ref({ plan: null, today: null, progress: [], todayIntakeCalories: 0, currentDayIndex: 1 })
const history = ref([])
const preferenceOptions = ref([])

const form = reactive({
  goal: 'lose',
  days: 7,
  meals: ['breakfast', 'lunch', 'dinner'],
  preferences: [],
  dislikes: [],
  extraRequirement: ''
})

// 「不喜欢/不能吃」的候选来自食物库里的食物名，也可自己输入
const dislikeOptions = ref([])

const activeTab = ref(route.path === '/meal-plan/history' ? 'history' : 'current')

const MEAL_ICONS = { breakfast: '🌅', lunch: '☀️', dinner: '🌙', snack: '🍎' }
function mealIcon(mealType) {
  return MEAL_ICONS[mealType] || '🍽️'
}

function num(v) {
  const n = Number(v)
  if (v == null || v === '' || Number.isNaN(n)) return '--'
  return String(Math.round(n))
}

/** 今天这一天的按餐分组 */
const todayGroups = computed(() => current.value.today?.meals || [])

/** 预览里的日均营养：按天求和再除以天数 */
const avgNutrition = computed(() => {
  const days = draft.value?.planDays || []
  if (!days.length) return { calories: '--', protein: '--', carbohydrate: '--', fat: '--' }
  const sum = (key) => days.reduce((acc, d) => acc + Number(d[key] || 0), 0) / days.length
  return {
    calories: Math.round(sum('calories')),
    protein: Math.round(sum('protein')),
    carbohydrate: Math.round(sum('carbohydrate')),
    fat: Math.round(sum('fat'))
  }
})

async function loadCurrent() {
  loading.value = true
  try {
    current.value = (await api.get('/meal-plan/current')) || { plan: null, progress: [] }
  } catch (e) {
    // 拦截器已经提示过错误，这里保持空态即可
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    history.value = (await api.get('/meal-plan', { params: { status: 'archived' } })) || []
  } catch (e) {
    history.value = []
  } finally {
    historyLoading.value = false
  }
}

async function loadOptions() {
  try {
    const data = await api.get('/user/options')
    preferenceOptions.value = (data && data.diet_preference) || []
  } catch (e) {
    preferenceOptions.value = []
  }
}

/** 用食物库前若干条当忌口候选，避免手打错别字 */
async function loadDislikeOptions() {
  try {
    const data = await api.get('/foods', { params: { page: 0, size: 200 } })
    dislikeOptions.value = (data?.content || []).map(f => f.name).slice(0, 200)
  } catch (e) {
    dislikeOptions.value = []
  }
}

function onTabChange(name) {
  router.push(name === 'history' ? '/meal-plan/history' : '/meal-plan')
}

function openCreate() {
  createVisible.value = true
}

function generate() {
  if (!form.meals.length) {
    ElMessage.warning('至少要选择一餐')
    return
  }
  generating.value = true
  // 弹窗里点了「重新生成」时保持弹窗打开，生成完直接刷新预览内容
  api.post('/meal-plan/generate', {
    goal: form.goal,
    days: form.days,
    meals: form.meals,
    preferences: form.preferences,
    dislikes: form.dislikes,
    extraRequirement: form.extraRequirement
  }).then((data) => {
    draft.value = data
    createVisible.value = false
    previewVisible.value = true
  }).catch(() => {
    // 拦截器已提示失败原因（例如 AI 服务未启动）
  }).finally(() => {
    generating.value = false
  })
}

/** 把预览到的内容原样回传，营养值由后端按食物库重算 */
function buildAdoptPayload() {
  const d = draft.value
  return {
    name: d.name,
    summary: d.summary,
    goal: d.goal,
    dailyCalories: d.dailyCalories,
    meals: d.meals,
    preferences: d.preferences,
    dislikes: d.dislikes,
    extraRequirement: d.extraRequirement,
    days: (d.planDays || []).map(day => ({
      day: day.dayIndex,
      meals: Object.fromEntries((day.meals || []).map(group => [
        group.mealType,
        (group.items || []).map(item => ({ food: item.foodName, amount: item.amount }))
      ]))
    }))
  }
}

async function adopt() {
  adopting.value = true
  try {
    await api.post('/meal-plan', buildAdoptPayload())
    ElMessage.success('方案已采用，开始执行')
    previewVisible.value = false
    draft.value = null
    await Promise.all([loadCurrent(), loadHistory()])
    router.push('/meal-plan')
  } catch (e) {
    // 拦截器已提示
  } finally {
    adopting.value = false
  }
}

function goDetail(planId) {
  const id = typeof planId === 'number' ? planId : current.value?.plan?.id
  if (!id) return
  router.push(`/meal-plan/${id}`)
}

async function removePlan(plan) {
  try {
    await ElMessageBox.confirm(`确定删除「${plan.name}」吗？删除后不可恢复。`, '删除方案', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await api.delete(`/meal-plan/${plan.id}`)
    ElMessage.success('已删除')
    await loadHistory()
  } catch (e) {}
}

onMounted(async () => {
  await Promise.all([loadCurrent(), loadHistory(), loadOptions(), loadDislikeOptions()])
})

// 从侧边栏切换「我的方案 / 历史方案」时同步刷新
watch(() => route.path, (path) => {
  activeTab.value = path === '/meal-plan/history' ? 'history' : 'current'
  if (path === '/meal-plan/history') loadHistory()
  else if (path === '/meal-plan') loadCurrent()
})
</script>

<style scoped>
.meal-plan-page { padding: 16px 20px 24px; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.page-title { font-size: 18px; font-weight: 700; color: #244b6b; }
.page-sub { font-size: 12px; color: #a0a8b0; margin-top: 4px; }

.mp-tabs :deep(.el-tabs__header) { margin-bottom: 14px; }

.card { border-radius: 10px; border: none; }
.card :deep(.el-card__body) { padding: 18px 22px; }

/* 空态 */
.empty-card :deep(.el-card__body) { text-align: center; padding: 46px 22px; }
.empty-icon { font-size: 40px; }
.empty-title { font-size: 15px; font-weight: 600; color: #244b6b; margin: 10px 0 6px; }
.empty-desc { font-size: 13px; color: #8ba0b4; line-height: 1.7; max-width: 460px; margin: 0 auto 18px; }

/* 当前方案卡 */
.plan-head { display: flex; align-items: center; justify-content: space-between; }
.plan-name { font-size: 17px; font-weight: 700; color: #244b6b; display: flex; align-items: center; gap: 8px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.status-dot.on { background: #52c41a; box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.15); }
.status-dot.off { background: #c8d2dc; }
.status-text { font-size: 12px; font-weight: 400; color: #52c41a; }
.plan-meta { display: flex; flex-wrap: wrap; gap: 22px; margin-top: 14px; font-size: 13px; color: #8ba0b4; }
.plan-meta b { color: #244b6b; font-weight: 600; margin-left: 2px; }
.plan-meta b.kcal { color: #2589ee; }
.plan-summary {
  margin-top: 12px; padding: 9px 12px; border-radius: 6px;
  font-size: 12px; color: #2589ee; background: #eef7ff; line-height: 1.6;
}

.block { margin-top: 18px; padding-top: 14px; border-top: 1px dashed #eef2f6; }
.block-title { font-size: 13px; font-weight: 600; color: #244b6b; margin-bottom: 10px; }

.progress-row { display: flex; align-items: center; gap: 26px; flex-wrap: wrap; }
.progress-item { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #8ba0b4; }
.progress-item.done { color: #244b6b; }
.progress-mark { font-size: 15px; font-weight: 700; color: #cfd8e3; }
.progress-item.done .progress-mark { color: #52c41a; }
.progress-label { font-weight: 600; }
.progress-intake { margin-left: auto; font-size: 12px; color: #a0a8b0; }

.meal-line {
  display: flex; align-items: baseline; gap: 10px;
  padding: 8px 0; border-bottom: 1px solid #f4f7fa; font-size: 13px;
}
.meal-line:last-child { border-bottom: none; }
.meal-icon { font-size: 14px; }
.meal-name { flex: none; width: 42px; font-weight: 600; color: #244b6b; }
.meal-foods { flex: 1; color: #55738d; line-height: 1.7; }
.meal-kcal { flex: none; font-size: 12px; color: #2589ee; }

.plan-foot { margin-top: 18px; display: flex; justify-content: flex-end; gap: 10px; }

/* 历史方案 */
.history-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.history-card :deep(.el-card__body) { padding: 16px 18px; }
.history-name { font-size: 15px; font-weight: 600; color: #244b6b; }
.history-meta { font-size: 13px; color: #2589ee; margin-top: 8px; }
.history-sub { font-size: 12px; color: #a0a8b0; margin-top: 6px; }
.history-foot { margin-top: 12px; display: flex; justify-content: flex-end; gap: 12px; }

.muted { font-size: 13px; color: #a0a8b0; }

/* 预览弹窗 */
.preview-meta { display: flex; gap: 22px; font-size: 13px; color: #8ba0b4; }
.preview-meta b { color: #244b6b; margin-left: 2px; }
.preview-meta b.kcal { color: #2589ee; }
.preview-alert { margin-top: 12px; }
.preview-days { max-height: 46vh; overflow-y: auto; margin-top: 14px; padding-right: 4px; }
.preview-day { padding: 12px 0; border-bottom: 1px solid #f0f4f8; }
.preview-day:last-child { border-bottom: none; }
.preview-day-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.day-label { font-size: 13px; font-weight: 700; color: #244b6b; }
.day-kcal { font-size: 12px; color: #2589ee; }
.preview-meal { display: flex; align-items: baseline; gap: 8px; font-size: 12px; padding: 3px 0; }
.preview-meal .meal-name { width: 38px; font-size: 12px; }
.preview-nutrition {
  margin-top: 14px; padding: 12px 14px; border-radius: 8px; background: #f7fafc;
}
.nutrition-title { font-size: 12px; color: #8ba0b4; margin-bottom: 8px; }
.nutrition-row { display: flex; gap: 26px; font-size: 13px; color: #55738d; flex-wrap: wrap; }
.nutrition-row b { color: #244b6b; }
.nutrition-row b.kcal { color: #2589ee; }

.dialog-hint {
  font-size: 12px; color: #a0a8b0; background: #f7fafc;
  border-radius: 6px; padding: 9px 12px; line-height: 1.7;
}

@media (max-width: 780px) {
  .page-head { flex-direction: column; align-items: flex-start; gap: 10px; }
  .plan-meta { gap: 14px; }
}
</style>

<style>
html.dark .meal-plan-page .page-title,
html.dark .meal-plan-page .plan-name,
html.dark .meal-plan-page .block-title,
html.dark .meal-plan-page .history-name,
html.dark .meal-plan-page .empty-title,
html.dark .meal-plan-page .meal-name,
html.dark .meal-plan-page .day-label { color: #c8d6e5; }
html.dark .meal-plan-page .page-sub,
html.dark .meal-plan-page .plan-meta,
html.dark .meal-plan-page .progress-item,
html.dark .meal-plan-page .history-sub,
html.dark .meal-plan-page .muted { color: #8ea1af; }
html.dark .meal-plan-page .plan-meta b,
html.dark .meal-plan-page .nutrition-row b { color: #c8d6e5; }
html.dark .meal-plan-page .meal-foods,
html.dark .meal-plan-page .nutrition-row { color: #8ea1af; }
html.dark .meal-plan-page .block { border-top-color: #2a3a5c; }
html.dark .meal-plan-page .meal-line { border-bottom-color: #2a3a5c; }
html.dark .meal-plan-page .plan-summary { background: #1e3a5f; color: #74b9ff; }
html.dark .meal-plan-page .preview-day { border-bottom-color: #2a3a5c; }
html.dark .meal-plan-page .preview-nutrition,
html.dark .meal-plan-page .dialog-hint { background: #1a2744; }
</style>
