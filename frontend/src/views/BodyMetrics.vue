<template>
  <div class="body-metrics">
    <!-- 顶部：概览卡片 -->
    <div class="summary-row">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">当前体重</div>
        <div class="summary-value">
          {{ summary.latestWeight ?? '--' }}<span class="unit">kg</span>
        </div>
        <div v-if="summary.startWeight != null && summary.recordCount > 1" class="summary-sub"
             :class="trendClass">
          {{ trendText }}
        </div>
      </el-card>

      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">体脂率</div>
        <div class="summary-value">
          {{ summary.latestBodyFat ?? '--' }}<span v-if="summary.latestBodyFat" class="unit">%</span>
        </div>
        <div v-if="summary.changeBodyFat != null" class="summary-sub"
             :class="fatTrendClass">
          较首次 {{ summary.changeBodyFat > 0 ? '+' : '' }}{{ summary.changeBodyFat }}%
        </div>
      </el-card>

      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">记录次数</div>
        <div class="summary-value">{{ summary.recordCount }}<span class="unit">次</span></div>
        <div class="summary-sub">{{ summary.firstDate || '暂无记录' }} 起</div>
      </el-card>

      <el-card shadow="hover" class="summary-card goal-card">
        <div class="summary-label">目标进度</div>
        <template v-if="target.targetValue != null">
          <el-progress
            :percentage="target.progressPercent ?? 0"
            :stroke-width="10"
            :status="progressStatus"
            style="margin: 6px 0" />
          <div class="summary-sub">
            目标 {{ target.targetValue }}{{ target.unit }}
            <template v-if="target.remaining != null">
              · 还差 {{ Math.abs(Number(target.remaining)).toFixed(1) }}{{ target.unit }}
            </template>
          </div>
          <div v-if="target.estimatedDate" class="summary-sub">
            按当前速度预计 {{ target.estimatedDate }} 达成
          </div>
        </template>
        <div v-else class="summary-sub goal-empty">还未设置身体目标</div>
      </el-card>
    </div>

    <el-alert
      v-if="target.paceTooFast"
      type="warning"
      :closable="false"
      show-icon
      :title="target.paceAdvice"
      style="margin-bottom: 14px" />

    <!-- 折线图 -->
    <el-card style="margin-bottom: 16px">
      <template #header>
        <div class="card-header">
          <span>📈 体重 / 体脂变化趋势</span>
          <el-radio-group v-model="rangeDays" size="small" @change="loadTrend">
            <el-radio-button :value="30">30天</el-radio-button>
            <el-radio-button :value="90">90天</el-radio-button>
            <el-radio-button :value="180">半年</el-radio-button>
            <el-radio-button :value="365">一年</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-show="showFat" ref="fatChartRef" class="chart-container"></div>
      <div ref="weightChartRef" class="chart-container"></div>
      <el-empty v-if="!summary.recordCount" description="还没有记录，先在上方添加一条吧" :image-size="70" />
    </el-card>

    <!-- 记录 + 目标 -->
    <div class="bottom-row">
      <el-card>
        <template #header><span>➕ 添加记录</span></template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
          <el-form-item label="日期">
            <el-date-picker
              v-model="form.recordDate"
              type="date"
              value-format="YYYY-MM-DD"
              :disabled-date="disableFuture"
              style="width: 100%" />
          </el-form-item>
          <el-form-item label="体重" prop="weightKg">
            <el-input-number v-model="form.weightKg" :min="20" :max="300" :precision="1" :step="0.1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="体脂率">
            <el-input-number v-model="form.bodyFatPercent" :min="1" :max="70" :precision="1" :step="0.1" style="width: 100%" />
            <div class="form-tip">没有体脂秤可以留空，只记录体重</div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" placeholder="如：早上空腹 / 运动后" maxlength="50" />
          </el-form-item>
          <el-button type="primary" style="width: 100%" :loading="saving" @click="save">保存记录</el-button>
        </el-form>
      </el-card>

      <el-card>
        <template #header>
          <div class="card-header">
            <span>🎯 身体目标</span>
            <el-button type="primary" size="small" @click="openGoalDialog">设置目标</el-button>
          </div>
        </template>

        <el-descriptions :column="1" border v-if="goal.targetWeightKg || goal.targetBodyFatPercent">
          <el-descriptions-item v-if="goal.targetWeightKg" label="目标体重">
            {{ goal.targetWeightKg }} kg
          </el-descriptions-item>
          <el-descriptions-item v-if="goal.targetBodyFatPercent" label="目标体脂率">
            {{ goal.targetBodyFatPercent }} %
          </el-descriptions-item>
          <el-descriptions-item v-if="goal.targetDeadline" label="期望达成">
            {{ goal.targetDeadline }}
          </el-descriptions-item>
          <el-descriptions-item label="当前饮食目标">
            {{ goalLabel }}
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="设置目标后，系统会按目标体重推算每日热量" :image-size="70" />

        <el-alert
          v-if="goal.targetWeightKg && needGoalHint"
          type="info"
          :closable="false"
          show-icon
          :title="`当前饮食目标是「${goalLabel}」${
            goalLabel === '减脂' ? '，目标体重比现在重，可能需要先调整饮食目标' :
            goalLabel === '增肌' ? '，目标体重比现在轻，热量会算反' : ''
          }`"
          style="margin-top: 12px" />
      </el-card>

      <el-card>
        <template #header>
          <div class="card-header">
            <span>📋 历史记录</span>
            <el-button v-if="hasMore" type="primary" link size="small" @click="loadMore">加载更多</el-button>
          </div>
        </template>

        <div v-loading="loading">
          <div v-for="r in records" :key="r.id" class="record-item">
            <div class="record-date">{{ shortDate(r.recordDate) }}</div>
            <div class="record-values">
              <span class="record-weight">{{ r.weightKg }} kg</span>
              <span v-if="r.bodyFatPercent" class="record-fat">{{ r.bodyFatPercent }}% 体脂</span>
              <span v-if="r.remark" class="record-remark">{{ r.remark }}</span>
            </div>
            <div class="record-ops">
              <el-button size="small" type="primary" link @click="editRecord(r)">编辑</el-button>
              <el-button size="small" type="danger" link @click="removeRecord(r)">删除</el-button>
            </div>
          </div>
          <el-empty v-if="!records.length && !loading" description="暂无记录" :image-size="60" />
        </div>
      </el-card>
    </div>

    <!-- 目标设置弹窗 -->
    <el-dialog v-model="goalDialogVisible" title="设置身体目标" width="480px">
      <el-form :model="goalForm" label-width="100px">
        <el-form-item label="饮食目标">
          <el-tag v-if="goalForm.dietGoal" type="info">{{ dietGoalLabel(goalForm.dietGoal) }}</el-tag>
          <div class="form-tip" style="margin-top: 4px">
            来自个人中心，如需修改请到「个人中心 → 饮食目标」
          </div>
        </el-form-item>
        <el-form-item label="目标体重">
          <el-input-number v-model="goalForm.targetWeightKg" :min="20" :max="300" :precision="1" :step="0.5" style="width: 100%" />
          <div class="form-tip">{{ goalHint }}</div>
        </el-form-item>
        <el-form-item label="目标体脂率">
          <el-input-number v-model="goalForm.targetBodyFatPercent" :min="1" :max="70" :precision="1" :step="0.5" style="width: 100%" />
          <div class="form-tip">可选。体脂比体重更能反映真实进展，设了则以体脂为准</div>
        </el-form-item>
        <el-form-item label="期望达成日期">
          <el-date-picker
            v-model="goalForm.targetDeadline"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disablePast"
            placeholder="不填则按每周 0.5kg 的温和速率推算"
            style="width: 100%" />
        </el-form-item>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="系统会按目标体重与达成时间反推每日热量（7700kcal≈1kg脂肪），并限制在安全区间内。填得太激进会被自动收敛。"
        />
      </el-form>
      <template #footer>
        <el-button @click="goalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="goalSaving" @click="saveGoal">保存目标</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import api from '../utils/api'

const PAGE_SIZE = 15

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const page = ref(0)
const hasMore = ref(false)
const rangeDays = ref(90)

const trend = ref({ points: [], summary: {}, target: {} })
const summary = computed(() => trend.value.summary || {})
const target = computed(() => trend.value.target || {})

const goal = ref({})
const goalDialogVisible = ref(false)
const goalSaving = ref(false)
const goalForm = reactive({
  targetWeightKg: null,
  targetBodyFatPercent: null,
  targetDeadline: null,
  dietGoal: null
})

const formRef = ref()
const editingId = ref(null)
const form = reactive({
  recordDate: todayStr(),
  weightKg: null,
  bodyFatPercent: null,
  remark: ''
})

const rules = {
  weightKg: [{ required: true, message: '请填写体重', trigger: 'blur' }]
}

const weightChartRef = ref(null)
const fatChartRef = ref(null)
let weightChart = null
let fatChart = null

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function shortDate(s) {
  if (!s) return ''
  const [, m, d] = s.split('-')
  return `${m}-${d}`
}

function dietGoalLabel(g) {
  return { lose: '减脂', maintain: '维持', gain: '增肌' }[g] || '未设置'
}

const goalLabel = computed(() => dietGoalLabel(goal.value.dietGoal))

// 有体脂数据才画体脂图，否则留个空位很难看
const showFat = computed(() => trend.value.points?.some(p => p.bodyFatPercent != null))

const progressStatus = computed(() => {
  const p = target.value.progressPercent ?? 0
  return p >= 100 ? 'success' : ''
})

const changeKg = computed(() => Number(summary.value.changeKg || 0))

const trendClass = computed(() => (changeKg.value < 0 ? 'good' : changeKg.value > 0 ? 'warn' : ''))
const fatTrendClass = computed(() => {
  const c = Number(summary.value.changeBodyFat || 0)
  return c < 0 ? 'good' : c > 0 ? 'warn' : ''
})

const trendText = computed(() => {
  const c = changeKg.value
  const perWeek = Number(summary.value.avgChangePerWeek || 0)
  const dir = c > 0 ? '+' : ''
  return `${dir}${c.toFixed(1)}kg · 每周${perWeek > 0 ? '+' : ''}${perWeek.toFixed(2)}kg`
})

// 目标体重方向与饮食目标矛盾时给提示
const needGoalHint = computed(() => {
  const tw = Number(goal.value.targetWeightKg || 0)
  const cw = Number(goal.value.currentWeight || 0)
  if (!tw || !cw) return false
  if (goal.value.dietGoal === 'lose') return tw > cw
  if (goal.value.dietGoal === 'gain') return tw < cw
  return false
})

const goalHint = computed(() => {
  const cw = Number(goal.value.currentWeight || 0)
  const tw = Number(goalForm.targetWeightKg || 0)
  if (!cw || !tw) return '先在个人中心完善当前体重，或直接填目标值'
  const d = tw - cw
  if (Math.abs(d) < 0.1) return '与当前体重相同'
  return d < 0
    ? `比当前轻 ${Math.abs(d).toFixed(1)}kg，按每周 0.5kg 约需 ${Math.ceil(Math.abs(d) / 0.5)} 周`
    : `比当前重 ${d.toFixed(1)}kg，按每周 0.5kg 约需 ${Math.ceil(d / 0.5)} 周`
})

function disableFuture(d) {
  return d.getTime() > Date.now()
}

function disablePast(d) {
  return d.getTime() < Date.now() - 86400000
}

// ── 数据加载 ──────────────────────────────────────

async function loadTrend() {
  try {
    trend.value = await api.get('/user/weight/trend', { params: { days: rangeDays.value } })
    await nextTick()
    renderCharts()
  } catch (e) {
    console.warn('加载趋势失败', e)
  }
}

async function loadRecords(reset = true) {
  loading.value = true
  try {
    const p = reset ? 0 : page.value
    const res = await api.get('/user/weight/records', { params: { page: p, size: PAGE_SIZE } })
    const list = res?.content || res || []
    records.value = reset ? list : [...records.value, ...list]
    page.value = p + 1
    hasMore.value = list.length === PAGE_SIZE
  } finally {
    loading.value = false
  }
}

function loadMore() {
  loadRecords(false)
}

async function loadGoal() {
  try {
    const g = await api.get('/user/weight/goal')
    goal.value = g || {}
  } catch (e) {}
}

// ── 图表 ──────────────────────────────────────────

function renderCharts() {
  const points = trend.value.points || []
  const dates = points.map(p => p.date)

  if (weightChartRef.value) {
    if (!weightChart) weightChart = echarts.init(weightChartRef.value)

    // 目标线：无目标时不画
    const targetLine = []
    const tw = target.value.targetWeightKg
    if (tw != null) {
      // 目标线画在未来区间才有意义，这里只画到最后一个记录点
      for (let i = 0; i < points.length; i++) targetLine.push(Number(tw))
    }

    const series = [{
      name: '体重(kg)',
      data: points.map(p => p.weightKg),
      type: 'line',
      smooth: true,
      connectNulls: true,
      areaStyle: { opacity: 0.12 },
      lineStyle: { width: 2.5, color: '#2589ee' },
      itemStyle: { color: '#2589ee' },
      symbolSize: 6
    }]
    if (targetLine.length) {
      series.push({
        name: '目标体重',
        data: targetLine,
        type: 'line',
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#ff8f5a', width: 2 }
      })
    }

    weightChart.setOption({
      title: showFat.value ? { text: '体重变化', left: 'center', top: 0, textStyle: { fontSize: 13, color: '#55738d' } } : undefined,
      tooltip: { trigger: 'axis' },
      legend: { data: series.map(s => s.name), top: showFat.value ? 26 : 0 },
      grid: { left: 46, right: 20, top: showFat.value ? 66 : 40, bottom: 34 },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
      yAxis: {
        type: 'value',
        name: 'kg',
        scale: true,
        axisLabel: { fontSize: 11 }
      },
      series
    })
  }

  if (showFat.value && fatChartRef.value) {
    if (!fatChart) fatChart = echarts.init(fatChartRef.value)
    const targetFat = []
    const tf = target.value.targetBodyFatPercent
    for (let i = 0; i < points.length; i++) targetFat.push(tf != null ? Number(tf) : null)

    const fatSeries = [{
      name: '体脂率(%)',
      data: points.map(p => p.bodyFatPercent),
      type: 'line',
      smooth: true,
      connectNulls: true,
      lineStyle: { width: 2, color: '#80d1ad' },
      itemStyle: { color: '#80d1ad' },
      symbolSize: 5
    }]
    if (tf != null) {
      fatSeries.push({
        name: '目标体脂',
        data: targetFat,
        type: 'line',
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#ffb56b', width: 2 }
      })
    }

    fatChart.setOption({
      title: { text: '体脂率变化', left: 'center', top: 0, textStyle: { fontSize: 13, color: '#55738d' } },
      tooltip: { trigger: 'axis', valueFormatter: v => v == null ? '--' : v + '%' },
      legend: { data: fatSeries.map(s => s.name), top: 26 },
      grid: { left: 46, right: 20, top: 66, bottom: 34 },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', name: '%', scale: true, axisLabel: { fontSize: 11 } },
      series: fatSeries
    })
  }
}

// ── 记录操作 ──────────────────────────────────────

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await api.post('/user/weight', {
      recordDate: form.recordDate,
      weightKg: form.weightKg,
      bodyFatPercent: form.bodyFatPercent,
      remark: form.remark
    })
    ElMessage.success(editingId.value ? '已更新该日记录' : '记录成功')
    editingId.value = null
    form.weightKg = null
    form.bodyFatPercent = null
    form.remark = ''
    form.recordDate = todayStr()
    await Promise.all([loadRecords(true), loadTrend(), loadGoal()])
  } finally {
    saving.value = false
  }
}

function editRecord(r) {
  editingId.value = r.id
  form.recordDate = r.recordDate
  form.weightKg = Number(r.weightKg)
  form.bodyFatPercent = r.bodyFatPercent != null ? Number(r.bodyFatPercent) : null
  form.remark = r.remark || ''
  ElMessage.info(`正在编辑 ${r.recordDate} 的记录，保存后将覆盖当天数据`)
}

async function removeRecord(r) {
  await ElMessageBox.confirm(
    `确定删除 ${r.recordDate} 的记录（${r.weightKg}kg）？`,
    '提示',
    { type: 'warning' }
  )
  await api.delete(`/user/weight/${r.id}`)
  ElMessage.success('已删除')
  await Promise.all([loadRecords(true), loadTrend(), loadGoal()])
}

// ── 目标操作 ──────────────────────────────────────

function openGoalDialog() {
  goalForm.targetWeightKg = goal.value.targetWeightKg != null ? Number(goal.value.targetWeightKg) : null
  goalForm.targetBodyFatPercent = goal.value.targetBodyFatPercent != null ? Number(goal.value.targetBodyFatPercent) : null
  goalForm.targetDeadline = goal.value.targetDeadline || null
  goalForm.dietGoal = goal.value.dietGoal || null
  goalDialogVisible.value = true
}

async function saveGoal() {
  if (goalForm.targetWeightKg == null && goalForm.targetBodyFatPercent == null) {
    ElMessage.warning('请至少填写目标体重或目标体脂率')
    return
  }
  goalSaving.value = true
  try {
    goal.value = await api.put('/user/weight/goal', {
      targetWeightKg: goalForm.targetWeightKg,
      targetBodyFatPercent: goalForm.targetBodyFatPercent,
      targetDeadline: goalForm.targetDeadline
    }) || goal.value
    goalDialogVisible.value = false
    ElMessage.success('目标已保存，每日热量目标已同步调整')
    await Promise.all([loadTrend(), loadGoal()])
  } finally {
    goalSaving.value = false
  }
}

let resizeHandler = null

onMounted(async () => {
  await Promise.all([loadTrend(), loadRecords(true), loadGoal()])
  window.addEventListener('resize', resizeHandler = () => {
    weightChart?.resize()
    fatChart?.resize()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  weightChart?.dispose()
  fatChart?.dispose()
  weightChart = null
  fatChart = null
})
</script>

<style scoped>
.body-metrics { padding: 18px; }

.summary-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.summary-card :deep(.el-card__body) { padding: 16px; }
.summary-label { font-size: 12px; color: #8ba0b4; margin-bottom: 6px; }
.summary-value { font-size: 26px; font-weight: 600; color: #244b6b; line-height: 1.2; }
.summary-value .unit { font-size: 13px; color: #8ba0b4; margin-left: 3px; font-weight: 400; }
.summary-sub { font-size: 12px; color: #8ba0b4; margin-top: 6px; }
.summary-sub.good { color: #2aa05a; }
.summary-sub.warn { color: #e08a2e; }
.goal-empty { margin-top: 10px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container { height: 300px; width: 100%; }

.bottom-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  align-items: start;
}

.form-tip { font-size: 12px; color: #a0a8b0; line-height: 1.6; }

.record-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px solid #f2f5f7;
}
.record-item:last-child { border-bottom: none; }
.record-date { font-size: 13px; color: #55738d; width: 46px; flex-shrink: 0; }
.record-values { flex: 1; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.record-weight { font-size: 14px; font-weight: 600; color: #244b6b; }
.record-fat { font-size: 12px; color: #80d1ad; }
.record-remark { font-size: 12px; color: #a0a8b0; }
.record-ops { flex-shrink: 0; }
</style>
