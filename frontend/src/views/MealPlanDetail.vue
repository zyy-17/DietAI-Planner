<template>
  <div class="mp-detail-page" v-loading="loading">
    <template v-if="plan">
      <!-- ── 页头 ─────────────────────────────────────── -->
      <div class="page-head">
        <div class="title-block">
          <div class="page-title">🥗 {{ plan.name }}</div>
          <div class="plan-meta">
            <span>目标：<b>{{ plan.goalLabel }}</b></span>
            <span>周期：<b>{{ plan.days }}天</b></span>
            <span>每日目标：<b class="kcal">{{ num(plan.dailyCalories) }} kcal</b></span>
            <span v-if="plan.plannedDailyCalories">方案实际：<b>{{ num(plan.plannedDailyCalories) }} kcal</b></span>
          </div>
        </div>
        <div class="head-actions">
          <el-button round @click="router.push('/meal-plan')">返回</el-button>
          <el-button :type="adjustMode ? 'success' : 'primary'" round @click="adjustMode = !adjustMode">
            {{ adjustMode ? '完成调整' : '调整方案' }}
          </el-button>
        </div>
      </div>

      <div v-if="adjustMode" class="adjust-tip">
        点击任意一条食物即可用「AI 智能替换」换成别的食材
      </div>

      <!-- ── 日期切换 ─────────────────────────────────── -->
      <div class="day-tabs">
        <button v-for="day in planDays" :key="day.dayIndex" class="day-tab"
          :class="{ active: day.dayIndex === activeDay, today: day.dayIndex === plan.currentDayIndex }"
          @click="activeDay = day.dayIndex">
          第{{ day.dayIndex }}天
          <span v-if="day.dayIndex === plan.currentDayIndex" class="today-flag">今天</span>
        </button>
      </div>

      <!-- ── 当天明细 ─────────────────────────────────── -->
      <el-card shadow="never" class="card">
        <template v-if="activeDayData && activeDayData.meals.length">
          <div v-for="group in activeDayData.meals" :key="group.mealType" class="meal-block">
            <div class="meal-head">
              <span class="meal-icon">{{ mealIcon(group.mealType) }}</span>
              <span class="meal-name">{{ group.mealLabel }}</span>
              <span class="meal-sub">{{ num(group.calories) }} kcal</span>
            </div>
            <div v-for="item in group.items" :key="item.id" class="food-row"
              :class="{ clickable: adjustMode }"
              @click="adjustMode && openReplace(item, group)">
              <span class="food-name">
                {{ item.foodName }}
                <span v-if="item.foodSource === 'ai'" class="ai-flag">AI估算</span>
              </span>
              <span class="food-amount">{{ item.amountText }}</span>
              <span class="food-kcal">{{ num(item.calories) }} kcal</span>
              <span v-if="adjustMode" class="food-action">替换</span>
            </div>
          </div>

          <div class="day-total">
            <span>今日预计：</span>
            <b class="kcal">{{ num(activeDayData.calories) }} kcal</b>
            <span class="day-total-sub">
              蛋白质 {{ num(activeDayData.protein) }}g ·
              碳水 {{ num(activeDayData.carbohydrate) }}g ·
              脂肪 {{ num(activeDayData.fat) }}g
            </span>
          </div>
        </template>
        <div v-else class="muted">这一天还没有安排食物</div>
      </el-card>
    </template>

    <!-- ── 替换弹窗 ────────────────────────────────── -->
    <el-dialog v-model="replaceVisible" title="替换这一项" width="560px" :close-on-click-modal="false">
      <div v-if="target" class="replace-origin">
        <span class="origin-label">当前</span>
        <span class="origin-name">{{ target.item.foodName }}</span>
        <span class="origin-amount">{{ target.item.amountText }}</span>
        <span class="origin-kcal">{{ num(target.item.calories) }} kcal</span>
        <span class="origin-meal">（{{ target.group.mealLabel }}）</span>
      </div>

      <el-input v-model="replaceReason" placeholder="为什么不想吃？（可不填，如：不喜欢吃、吃腻了）"
        class="replace-reason" />

      <div class="replace-actions">
        <el-button type="primary" :loading="loadingAlts" @click="loadAlternatives">
          ✨ AI 智能替换
        </el-button>
        <span class="replace-hint">从食物库里挑热量与营养接近的替代项</span>
      </div>

      <el-alert v-if="alternatives && !alternatives.aiUsed" type="warning" :closable="false"
        show-icon class="replace-alert" :title="alternatives.note" />

      <div v-if="alternatives && alternatives.candidates.length" class="alt-list">
        <div v-for="(alt, index) in alternatives.candidates" :key="alt.foodName + index" class="alt-item">
          <div class="alt-main">
            <span class="alt-name">{{ alt.foodName }}</span>
            <span class="alt-amount">{{ alt.amountText }}</span>
            <span class="alt-kcal">{{ num(alt.calories) }} kcal</span>
          </div>
          <div v-if="alt.reason" class="alt-reason">{{ alt.reason }}</div>
          <el-button size="small" type="primary" plain :loading="replacing === alt.foodName"
            @click="applyReplace(alt)">选它</el-button>
        </div>
      </div>
      <div v-else-if="searched && !loadingAlts" class="muted center">没有找到合适的替代项</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const plan = ref(null)
const planDays = ref([])
const activeDay = ref(1)
const adjustMode = ref(false)

const replaceVisible = ref(false)
const target = ref(null)
const replaceReason = ref('')
const alternatives = ref(null)
const loadingAlts = ref(false)
const replacing = ref('')
const searched = ref(false)

const MEAL_ICONS = { breakfast: '🌅', lunch: '☀️', dinner: '🌙', snack: '🍎' }
function mealIcon(mealType) {
  return MEAL_ICONS[mealType] || '🍽️'
}

function num(v) {
  const n = Number(v)
  if (v == null || v === '' || Number.isNaN(n)) return '--'
  return String(Math.round(n))
}

const planId = computed(() => route.params.id)
const activeDayData = computed(() => planDays.value.find(d => d.dayIndex === activeDay.value))

async function load() {
  if (!planId.value) return
  loading.value = true
  try {
    const data = await api.get(`/meal-plan/${planId.value}`)
    plan.value = data?.plan || null
    planDays.value = data?.planDays || []
    activeDay.value = plan.value?.currentDayIndex || 1
  } catch (e) {
    plan.value = null
  } finally {
    loading.value = false
  }
}

function openReplace(item, group) {
  if (!adjustMode.value) return
  if (!item.id) {
    ElMessage.warning('这一项还没有落库，无法替换')
    return
  }
  target.value = { item, group }
  replaceReason.value = ''
  alternatives.value = null
  searched.value = false
  replaceVisible.value = true
}

async function loadAlternatives() {
  if (!target.value) return
  loadingAlts.value = true
  searched.value = true
  try {
    alternatives.value = await api.get(
      `/meal-plan/${planId.value}/item/${target.value.item.id}/alternatives`,
      { params: { reason: replaceReason.value } }
    )
  } catch (e) {
    alternatives.value = null
  } finally {
    loadingAlts.value = false
  }
}

async function applyReplace(alt) {
  if (!target.value) return
  replacing.value = alt.foodName
  try {
    const day = await api.put(
      `/meal-plan/${planId.value}/item/${target.value.item.id}`,
      {
        foodId: alt.foodId,
        foodSource: alt.foodSource,
        foodName: alt.foodName,
        amount: alt.amount
      }
    )
    // 后端返回该天最新明细，就地替换，避免整页重新拉取
    const index = planDays.value.findIndex(d => d.dayIndex === day.dayIndex)
    if (index >= 0) planDays.value[index] = day
    ElMessage.success(`已替换为 ${alt.foodName}`)
    replaceVisible.value = false
  } catch (e) {
    // 拦截器已提示
  } finally {
    replacing.value = ''
  }
}

onMounted(load)
watch(planId, load)
</script>

<style scoped>
.mp-detail-page { padding: 16px 20px 24px; }

.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.page-title { font-size: 18px; font-weight: 700; color: #244b6b; }
.plan-meta { display: flex; flex-wrap: wrap; gap: 20px; margin-top: 8px; font-size: 13px; color: #8ba0b4; }
.plan-meta b { color: #244b6b; margin-left: 2px; }
.plan-meta b.kcal { color: #2589ee; }
.head-actions { flex: none; display: flex; gap: 10px; }

.adjust-tip {
  margin-top: 12px; padding: 9px 12px; border-radius: 6px;
  font-size: 12px; color: #2589ee; background: #eef7ff;
}

.day-tabs { display: flex; flex-wrap: wrap; gap: 8px; margin: 16px 0 14px; }
.day-tab {
  position: relative;
  padding: 6px 16px; border-radius: 999px; cursor: pointer;
  font-size: 13px; color: #55738d; background: #fff;
  border: 1px solid #e8edf2; transition: all 0.2s;
}
.day-tab:hover { color: #2589ee; border-color: #b9dcff; }
.day-tab.active { color: #fff; background: #2589ee; border-color: #2589ee; }
.day-tab.today { font-weight: 700; }
.today-flag {
  margin-left: 6px; padding: 0 5px; border-radius: 999px;
  font-size: 10px; background: #52c41a; color: #fff;
}
.day-tab.active .today-flag { background: rgba(255, 255, 255, 0.28); }

.card { border-radius: 10px; border: none; }
.card :deep(.el-card__body) { padding: 8px 22px 18px; }

.meal-block { padding: 14px 0 6px; border-bottom: 1px dashed #eef2f6; }
.meal-block:last-of-type { border-bottom: none; }
.meal-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 6px; }
.meal-icon { font-size: 15px; }
.meal-name { font-size: 14px; font-weight: 600; color: #244b6b; }
.meal-sub { font-size: 12px; color: #a0a8b0; }

.food-row {
  display: flex; align-items: center; gap: 14px;
  padding: 9px 10px; border-radius: 6px; font-size: 13px;
  transition: background 0.15s;
}
.food-row.clickable { cursor: pointer; }
.food-row.clickable:hover { background: #f2f8ff; }
.food-name { flex: 1; color: #244b6b; }
.ai-flag {
  margin-left: 6px; padding: 1px 6px; border-radius: 999px;
  font-size: 10px; color: #e6a23c; background: #fdf6ec;
}
.food-amount { flex: none; width: 88px; text-align: right; color: #55738d; }
.food-kcal { flex: none; width: 82px; text-align: right; color: #2589ee; }
.food-action { flex: none; font-size: 12px; color: #2589ee; }

.day-total {
  display: flex; align-items: baseline; gap: 10px;
  margin-top: 16px; padding-top: 14px; border-top: 1px dashed #eef2f6;
  font-size: 13px; color: #8ba0b4;
}
.day-total b.kcal { font-size: 17px; color: #2589ee; }
.day-total-sub { margin-left: auto; font-size: 12px; color: #a0a8b0; }

.muted { font-size: 13px; color: #a0a8b0; padding: 12px 0; }
.muted.center { text-align: center; }

/* 替换弹窗 */
.replace-origin {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 6px; background: #f7fafc; font-size: 13px;
}
.origin-label { font-size: 12px; color: #a0a8b0; }
.origin-name { font-weight: 600; color: #244b6b; }
.origin-amount { color: #55738d; }
.origin-kcal { color: #2589ee; }
.origin-meal { font-size: 12px; color: #a0a8b0; }
.replace-reason { margin-top: 12px; }
.replace-actions { margin-top: 12px; display: flex; align-items: center; gap: 12px; }
.replace-hint { font-size: 12px; color: #a0a8b0; }
.replace-alert { margin-top: 12px; }
.alt-list { margin-top: 12px; max-height: 40vh; overflow-y: auto; }
.alt-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 12px; border: 1px solid #eef2f6; border-radius: 8px; margin-bottom: 8px;
}
.alt-main { flex: 1; display: flex; align-items: baseline; gap: 12px; font-size: 13px; }
.alt-name { font-weight: 600; color: #244b6b; }
.alt-amount { color: #55738d; }
.alt-kcal { color: #2589ee; font-size: 12px; }
.alt-reason { font-size: 12px; color: #a0a8b0; margin-top: 4px; }
.alt-item .el-button { flex: none; }

@media (max-width: 780px) {
  .page-head { flex-direction: column; }
  .head-actions { width: 100%; }
}
</style>

<style>
html.dark .mp-detail-page .page-title,
html.dark .mp-detail-page .meal-name,
html.dark .mp-detail-page .food-name,
html.dark .mp-detail-page .origin-name,
html.dark .mp-detail-page .alt-name { color: #c8d6e5; }
html.dark .mp-detail-page .plan-meta,
html.dark .mp-detail-page .food-amount,
html.dark .mp-detail-page .muted,
html.dark .mp-detail-page .day-total { color: #8ea1af; }
html.dark .mp-detail-page .plan-meta b { color: #c8d6e5; }
html.dark .mp-detail-page .day-tab { background: #1f2b45; border-color: #2a3a5c; color: #8ea1af; }
html.dark .mp-detail-page .day-tab.active { background: #2589ee; border-color: #2589ee; color: #fff; }
html.dark .mp-detail-page .meal-block { border-bottom-color: #2a3a5c; }
html.dark .mp-detail-page .day-total { border-top-color: #2a3a5c; }
html.dark .mp-detail-page .food-row.clickable:hover { background: #1e3a5f; }
html.dark .mp-detail-page .replace-origin,
html.dark .mp-detail-page .adjust-tip { background: #1a2744; }
html.dark .mp-detail-page .alt-item { border-color: #2a3a5c; }
html.dark .mp-detail-page .alt-amount { color: #8ea1af; }
</style>
