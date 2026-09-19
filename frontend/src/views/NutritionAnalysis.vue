<template>
  <div class="nutrition-analysis">
    <!-- 智能营养评估卡片（仅今日营养模式） -->
    <el-card v-if="evaluation && mode === 'default'" class="eval-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🧬 智能营养评估</span>
          <el-tag :type="evaluation.nutritionScore >= 80 ? 'success' : evaluation.nutritionScore >= 60 ? 'warning' : 'danger'" size="large" effect="dark">
            {{ evaluation.nutritionScore }} 分
          </el-tag>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="6"><div class="eval-metric"><div class="metric-label">BMI</div><div class="metric-value">{{ evaluation.bmi || '-' }}</div><div class="metric-tag" :class="bmiClass">{{ bmiDesc }}</div></div></el-col>
        <el-col :span="6"><div class="eval-metric"><div class="metric-label">基础代谢 BMR</div><div class="metric-value">{{ evaluation.bmr || '-' }}</div><div class="metric-unit">kcal/天</div></div></el-col>
        <el-col :span="6"><div class="eval-metric"><div class="metric-label">总消耗 TDEE</div><div class="metric-value">{{ evaluation.tdee || '-' }}</div><div class="metric-unit">kcal/天</div></div></el-col>
        <el-col :span="6"><div class="eval-metric"><div class="metric-label">目标热量</div><div class="metric-value">{{ evaluation.targetCalories || '-' }}</div><div class="metric-unit">kcal/天</div></div></el-col>
      </el-row>
      <el-divider />
      <div class="eval-status-row">
        <div class="status-item"><span class="status-label">🔥 热量</span><el-tag :type="statusType(evaluation.calorieStatus)" size="small">{{ evaluation.calorieStatus }}</el-tag><span class="status-gap">{{ formatGap(evaluation.calorieGap) }} kcal</span></div>
        <div class="status-item"><span class="status-label">🥩 蛋白质</span><el-tag :type="statusType(evaluation.proteinStatus)" size="small">{{ evaluation.proteinStatus }}</el-tag><span class="status-gap">{{ formatGap(evaluation.proteinGap) }} g</span></div>
        <div class="status-item"><span class="status-label">🌾 碳水</span><el-tag :type="statusType(evaluation.carbStatus)" size="small">{{ evaluation.carbStatus }}</el-tag><span class="status-gap">{{ formatGap(evaluation.carbGap) }} g</span></div>
        <div class="status-item"><span class="status-label">🫒 脂肪</span><el-tag :type="statusType(evaluation.fatStatus)" size="small">{{ evaluation.fatStatus }}</el-tag><span class="status-gap">{{ formatGap(evaluation.fatGap) }} g</span></div>
      </div>
      <el-divider />
      <div class="eval-score-bar"><span class="score-label">营养评分</span><el-progress :percentage="evaluation.nutritionScore" :stroke-width="20" :color="scoreColor" :format="() => evaluation.nutritionScore + '分'" /></div>
      <div class="eval-problem" v-if="evaluation.mainProblem"><el-alert :title="'⚠️ 主要问题：' + evaluation.mainProblem" :type="evaluation.nutritionScore >= 80 ? 'success' : 'warning'" :closable="false" show-icon /></div>
    </el-card>

    <!-- ===== 营养素分析模式 ===== -->
    <el-card v-if="mode === 'nutrients'" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🧪 营养素分析</span>
          <el-radio-group v-model="days" @change="loadModeData">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-if="nutrientData">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="chart-title">每日营养素趋势</div>
            <div ref="nutrientLineRef" class="chart-container"></div>
          </el-col>
          <el-col :span="12">
            <div class="chart-title">营养素供能占比</div>
            <div ref="nutrientPieRef" class="chart-container"></div>
          </el-col>
        </el-row>
        <el-divider content-position="left">日均摄入 vs 目标</el-divider>
        <el-row :gutter="20" style="margin-top:12px">
          <el-col :span="8">
            <el-statistic title="蛋白质" :value="nutrientData.summary.avgProtein" suffix="g" />
            <div class="target-hint">目标 {{ nutrientData.summary.targetProtein }}g</div>
          </el-col>
          <el-col :span="8">
            <el-statistic title="碳水化合物" :value="nutrientData.summary.avgCarbohydrate" suffix="g" />
            <div class="target-hint">目标 {{ nutrientData.summary.targetCarb }}g</div>
          </el-col>
          <el-col :span="8">
            <el-statistic title="脂肪" :value="nutrientData.summary.avgFat" suffix="g" />
            <div class="target-hint">目标 {{ nutrientData.summary.targetFat }}g</div>
          </el-col>
        </el-row>
        <el-divider content-position="left">营养素建议</el-divider>
        <div v-for="advice in nutrientData.advices" :key="advice.nutrient" class="advice-item">
          <el-tag :type="statusType(advice.status)" size="small">{{ advice.nutrient }} {{ advice.status }}</el-tag>
          <span class="advice-text">{{ advice.suggestion }}</span>
        </div>
      </div>
    </el-card>

    <!-- ===== 热量分析模式 ===== -->
    <el-card v-if="mode === 'calorie'" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🔥 热量分析</span>
          <el-radio-group v-model="days" @change="loadModeData">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-if="calorieData">
        <el-row :gutter="20">
          <el-col :span="14">
            <div class="chart-title">每日热量 vs 目标线</div>
            <div ref="calorieLineRef" class="chart-container"></div>
          </el-col>
          <el-col :span="10">
            <div class="chart-title">时段热量分布</div>
            <div ref="calorieMealRef" class="chart-container"></div>
          </el-col>
        </el-row>
        <el-divider content-position="left">热量达标统计</el-divider>
        <el-row :gutter="20" style="margin-top:12px">
          <el-col :span="6"><el-statistic title="日均热量" :value="calorieData.summary.avgCalories" suffix="kcal" /></el-col>
          <el-col :span="6"><el-statistic title="目标热量" :value="calorieData.summary.targetCalories" suffix="kcal" /></el-col>
          <el-col :span="6"><el-statistic title="平均偏差" :value="calorieData.summary.avgDeviation" suffix="kcal" /></el-col>
          <el-col :span="6">
            <el-statistic title="达标率">
              <template #default><span :style="{color: calorieData.summary.hitRate >= 80 ? '#67c23a' : '#f56c6c'}">{{ calorieData.summary.hitRate }}%</span></template>
            </el-statistic>
          </el-col>
        </el-row>
        <el-alert style="margin-top:12px" :title="'整体评价：' + calorieData.summary.overallStatus" :type="calorieData.summary.overallStatus === '良好' ? 'success' : calorieData.summary.overallStatus === '一般' ? 'warning' : 'error'" :closable="false" show-icon />
      </div>
    </el-card>

    <!-- ===== 目标完成度模式 ===== -->
    <el-card v-if="mode === 'goal'" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🎯 目标完成度</span>
          <el-button size="small" @click="loadModeData">刷新</el-button>
        </div>
      </template>
      <div v-if="goalData">
        <el-row :gutter="20">
          <el-col :span="8" v-for="goal in goalData.goals" :key="goal.name">
            <div class="goal-circle">
              <el-progress type="circle" :percentage="Number(goal.completion)" :width="130" :stroke-width="12" :color="goalColor(goal.completion)">
                <template #default><b>{{ goal.completion }}%</b><small>{{ goal.name }}</small></template>
              </el-progress>
            </div>
            <div class="goal-detail">
              <span>目标 {{ goal.target }} · 实际 {{ goal.actual }}</span>
              <el-tag :type="goalTagType(goal.status)" size="small">{{ goal.status }}</el-tag>
            </div>
          </el-col>
        </el-row>
        <el-divider />
        <el-row :gutter="20">
          <el-col :span="12">
            <el-result :icon="goalData.overallCompletion >= 90 ? 'success' : goalData.overallCompletion >= 70 ? 'warning' : 'error'" :title="'综合完成度 ' + goalData.overallCompletion + '%'" :sub-title="'评价：' + goalData.overallStatus" />
          </el-col>
          <el-col :span="12">
            <div class="suggestion-list">
              <h4>💡 改进建议</h4>
              <ul><li v-for="s in goalData.suggestions" :key="s">{{ s }}</li></ul>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- ===== 营养报告模式 ===== -->
    <el-card v-if="mode === 'report'" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>📄 营养报告</span>
          <el-radio-group v-model="days" @change="loadModeData">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-if="reportData">
        <el-descriptions title="基本信息" :column="2" border size="small">
          <el-descriptions-item label="分析周期">{{ reportData.period }}</el-descriptions-item>
          <el-descriptions-item label="营养评分">
            <el-tag :type="reportData.nutritionScore >= 80 ? 'success' : reportData.nutritionScore >= 60 ? 'warning' : 'danger'" effect="dark">{{ reportData.nutritionScore }} 分</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="日均热量">{{ reportData.avgCalories }} kcal</el-descriptions-item>
          <el-descriptions-item label="日均蛋白质">{{ reportData.avgProtein }} g</el-descriptions-item>
          <el-descriptions-item label="日均碳水">{{ reportData.avgCarbohydrate }} g</el-descriptions-item>
          <el-descriptions-item label="日均脂肪">{{ reportData.avgFat }} g</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">各营养素评价</el-divider>
        <el-row :gutter="12" style="margin-top:12px">
          <el-col :span="6"><div class="eval-box"><span class="eval-name">🔥 热量</span><el-tag :type="evalTagType(reportData.calorieEvaluation)">{{ reportData.calorieEvaluation }}</el-tag></div></el-col>
          <el-col :span="6"><div class="eval-box"><span class="eval-name">🥩 蛋白质</span><el-tag :type="evalTagType(reportData.proteinEvaluation)">{{ reportData.proteinEvaluation }}</el-tag></div></el-col>
          <el-col :span="6"><div class="eval-box"><span class="eval-name">🌾 碳水</span><el-tag :type="evalTagType(reportData.carbEvaluation)">{{ reportData.carbEvaluation }}</el-tag></div></el-col>
          <el-col :span="6"><div class="eval-box"><span class="eval-name">🫒 脂肪</span><el-tag :type="evalTagType(reportData.fatEvaluation)">{{ reportData.fatEvaluation }}</el-tag></div></el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top:16px">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header><span>🔍 关键发现</span></template>
              <ul class="finding-list"><li v-for="f in reportData.keyFindings" :key="f">{{ f }}</li></ul>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header><span>💡 改善建议</span></template>
              <ul class="finding-list"><li v-for="r in reportData.recommendations" :key="r">{{ r }}</li></ul>
            </el-card>
          </el-col>
        </el-row>

        <el-divider content-position="left">营养素供能比例</el-divider>
        <el-row :gutter="20" style="margin-top:8px">
          <el-col :span="8"><el-progress :percentage="Number(reportData.proteinRatio)" :stroke-width="18" color="#77b8ef" :format="() => '蛋白质 ' + reportData.proteinRatio + '%'" /></el-col>
          <el-col :span="8"><el-progress :percentage="Number(reportData.carbRatio)" :stroke-width="18" color="#80d1ad" :format="() => '碳水 ' + reportData.carbRatio + '%'" /></el-col>
          <el-col :span="8"><el-progress :percentage="Number(reportData.fatRatio)" :stroke-width="18" color="#ffb56b" :format="() => '脂肪 ' + reportData.fatRatio + '%'" /></el-col>
        </el-row>
      </div>
    </el-card>

    <!-- ===== 今日营养/营养趋势 通用模式 ===== -->
    <el-card v-if="mode === 'default' || mode === 'trend'" style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <span>{{ mode === 'default' ? '今日营养' : '营养趋势' }}</span>
          <el-radio-group v-model="days" @change="loadAnalysis">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="chart-title">每日热量趋势</div>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-col>
        <el-col :span="12">
          <div class="chart-title">三大营养素占比</div>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-col>
      </el-row>
      <el-row :gutter="20" style="margin-top:20px">
        <el-col :span="6"><el-statistic title="日均热量" :value="analysis.avgCalories" suffix="kcal" /></el-col>
        <el-col :span="6"><el-statistic title="日均蛋白质" :value="analysis.avgProtein" suffix="g" /></el-col>
        <el-col :span="6"><el-statistic title="日均碳水" :value="analysis.avgCarbohydrate" suffix="g" /></el-col>
        <el-col :span="6"><el-statistic title="日均脂肪" :value="analysis.avgFat" suffix="g" /></el-col>
      </el-row>
      <el-card v-if="analysis.aiInterpretation" style="margin-top:20px" shadow="never">
        <template #header><span>🤖 AI智能解读</span></template>
        <p style="line-height:1.8;color:#606266">{{ analysis.aiInterpretation }}</p>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import api from '../utils/api'

const route = useRoute()
const mode = computed(() => route.meta.mode || 'default')
const days = ref(7)

const analysis = ref({})
const evaluation = ref(null)
const nutrientData = ref(null)
const calorieData = ref(null)
const goalData = ref(null)
const reportData = ref(null)

const lineChartRef = ref(null)
const pieChartRef = ref(null)
const nutrientLineRef = ref(null)
const nutrientPieRef = ref(null)
const calorieLineRef = ref(null)
const calorieMealRef = ref(null)
let lineChart = null, pieChart = null
let nutrientLineChart = null, nutrientPieChart = null
let calorieLineChart = null, calorieMealChart = null

const bmiDesc = computed(() => {
  if (!evaluation.value || !evaluation.value.bmi) return ''
  const bmi = evaluation.value.bmi
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '偏胖'
  return '肥胖'
})
const bmiClass = computed(() => {
  if (!evaluation.value || !evaluation.value.bmi) return ''
  const bmi = evaluation.value.bmi
  if (bmi < 18.5) return 'underweight'
  if (bmi < 24) return 'normal'
  if (bmi < 28) return 'overweight'
  return 'obese'
})
const scoreColor = computed(() => {
  if (!evaluation.value) return '#409eff'
  const s = evaluation.value.nutritionScore
  if (s >= 80) return '#67c23a'
  if (s >= 60) return '#e6a23c'
  return '#f56c6c'
})

function statusType(status) {
  if (status === '正常' || status === '达标') return 'success'
  if (status === '不足' || status === '接近') return 'warning'
  if (status === '偏高' || status === '超标') return 'danger'
  return 'info'
}
function formatGap(gap) {
  if (gap === null || gap === undefined) return '-'
  const num = Number(gap)
  if (num > 0) return `+${num.toFixed(0)}`
  return num.toFixed(0)
}
function goalColor(completion) {
  const c = Number(completion)
  if (c >= 90) return '#67c23a'
  if (c >= 70) return '#409eff'
  if (c >= 50) return '#e6a23c'
  return '#f56c6c'
}
function goalTagType(status) {
  if (status === '达标') return 'success'
  if (status === '接近') return 'warning'
  if (status === '超标') return 'danger'
  return 'info'
}
function evalTagType(evalStr) {
  if (evalStr === '优秀') return 'success'
  if (evalStr === '良好') return ''
  if (evalStr === '一般') return 'warning'
  return 'danger'
}

async function loadEvaluation() {
  try { evaluation.value = await api.get('/nutrition/evaluate') } catch (e) { console.warn('加载营养评估失败', e) }
}
async function loadAnalysis() {
  try { analysis.value = await api.get('/nutrition/analysis', { params: { days: days.value } }) } catch (e) {}
  await nextTick()
  renderBasicCharts()
}
async function loadModeData() {
  const m = mode.value
  if (m === 'nutrients') {
    try { nutrientData.value = await api.get('/nutrition/nutrients', { params: { days: days.value } }) } catch (e) { console.warn(e) }
    await nextTick(); renderNutrientCharts()
  } else if (m === 'calorie') {
    try { calorieData.value = await api.get('/nutrition/calorie', { params: { days: days.value } }) } catch (e) { console.warn(e) }
    await nextTick(); renderCalorieCharts()
  } else if (m === 'goal') {
    try { goalData.value = await api.get('/nutrition/goal') } catch (e) { console.warn(e) }
  } else if (m === 'report') {
    try { reportData.value = await api.get('/nutrition/report', { params: { days: days.value } }) } catch (e) { console.warn(e) }
  }
}

function renderBasicCharts() {
  if (!analysis.value.dailyData) return
  const dailyData = analysis.value.dailyData || []
  if (lineChartRef.value && !lineChart) lineChart = echarts.init(lineChartRef.value)
  if (lineChart) {
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dailyData.map(d => d.date) },
      yAxis: { type: 'value', name: 'kcal' },
      series: [{ data: dailyData.map(d => d.calories), type: 'line', smooth: true, areaStyle: { opacity: 0.3 } }]
    })
  }
  if (pieChartRef.value && !pieChart) pieChart = echarts.init(pieChartRef.value)
  if (pieChart) {
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['40%', '70%'], data: [
        { name: '蛋白质', value: analysis.value.proteinRatio || 0, itemStyle: { color: '#77b8ef' } },
        { name: '碳水化合物', value: analysis.value.carbRatio || 0, itemStyle: { color: '#80d1ad' } },
        { name: '脂肪', value: analysis.value.fatRatio || 0, itemStyle: { color: '#ffb56b' } }
      ]}]
    })
  }
}

function renderNutrientCharts() {
  if (!nutrientData.value) return
  const daily = nutrientData.value.dailyNutrients || []
  if (nutrientLineRef.value && !nutrientLineChart) nutrientLineChart = echarts.init(nutrientLineRef.value)
  if (nutrientLineChart) {
    nutrientLineChart.setOption({
      tooltip: { trigger: 'axis' }, legend: { data: ['蛋白质', '碳水', '脂肪'] },
      xAxis: { type: 'category', data: daily.map(d => d.date) },
      yAxis: { type: 'value', name: 'g' },
      series: [
        { name: '蛋白质', data: daily.map(d => d.protein), type: 'line', smooth: true, itemStyle: { color: '#77b8ef' } },
        { name: '碳水', data: daily.map(d => d.carbohydrate), type: 'line', smooth: true, itemStyle: { color: '#80d1ad' } },
        { name: '脂肪', data: daily.map(d => d.fat), type: 'line', smooth: true, itemStyle: { color: '#ffb56b' } }
      ]
    })
  }
  if (nutrientPieRef.value && !nutrientPieChart) nutrientPieChart = echarts.init(nutrientPieRef.value)
  if (nutrientPieChart) {
    const s = nutrientData.value.summary
    nutrientPieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['40%', '70%'], data: [
        { name: '蛋白质', value: s.proteinRatio, itemStyle: { color: '#77b8ef' } },
        { name: '碳水', value: s.carbRatio, itemStyle: { color: '#80d1ad' } },
        { name: '脂肪', value: s.fatRatio, itemStyle: { color: '#ffb56b' } }
      ]}]
    })
  }
}

function renderCalorieCharts() {
  if (!calorieData.value) return
  const daily = calorieData.value.dailyCalories || []
  if (calorieLineRef.value && !calorieLineChart) calorieLineChart = echarts.init(calorieLineRef.value)
  if (calorieLineChart) {
    calorieLineChart.setOption({
      tooltip: { trigger: 'axis' }, legend: { data: ['实际热量', '目标线'] },
      xAxis: { type: 'category', data: daily.map(d => d.date) },
      yAxis: { type: 'value', name: 'kcal' },
      series: [
        { name: '实际热量', data: daily.map(d => d.calories), type: 'bar', itemStyle: { color: '#409eff' } },
        { name: '目标线', data: daily.map(d => d.targetCalories), type: 'line', itemStyle: { color: '#f56c6c' }, lineStyle: { type: 'dashed' } }
      ]
    })
  }
  const meals = calorieData.value.mealDistribution || []
  if (calorieMealRef.value && !calorieMealChart) calorieMealChart = echarts.init(calorieMealRef.value)
  if (calorieMealChart) {
    calorieMealChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['35%', '65%'], data: meals.map(m => ({ name: m.mealType, value: m.calories })) , label: { formatter: '{b}: {d}%' } }]
    })
  }
}

function initPage() {
  const m = mode.value
  if (m === 'default' || m === 'trend') {
    loadEvaluation()
    loadAnalysis()
  } else {
    loadModeData()
  }
}

onMounted(initPage)
watch(() => route.fullPath, initPage)

onBeforeUnmount(() => {
  lineChart?.dispose(); pieChart?.dispose()
  nutrientLineChart?.dispose(); nutrientPieChart?.dispose()
  calorieLineChart?.dispose(); calorieMealChart?.dispose()
})
</script>

<style scoped>
.nutrition-analysis { padding: 18px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-title { text-align: center; font-weight: bold; margin-bottom: 8px; font-size: 14px; }
.chart-container { height: 300px; }

.eval-card { border-left: 4px solid #409eff; }
.eval-metric { text-align: center; padding: 12px 0; }
.metric-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.metric-value { font-size: 24px; font-weight: bold; color: #303133; }
.metric-unit { font-size: 11px; color: #b0b5b9; margin-top: 4px; }
.metric-tag { font-size: 11px; margin-top: 4px; padding: 2px 8px; border-radius: 10px; display: inline-block; }
.metric-tag.normal { background: #f0f9eb; color: #67c23a; }
.metric-tag.underweight { background: #fdf6ec; color: #e6a23c; }
.metric-tag.overweight { background: #fef0f0; color: #f56c6c; }
.metric-tag.obese { background: #fef0f0; color: #f56c6c; }
.eval-status-row { display: flex; justify-content: space-around; flex-wrap: wrap; gap: 12px; }
.status-item { display: flex; align-items: center; gap: 8px; }
.status-label { font-size: 13px; color: #606266; }
.status-gap { font-size: 12px; color: #909399; font-family: monospace; }
.eval-score-bar { margin-bottom: 12px; }
.score-label { font-size: 13px; color: #606266; margin-bottom: 6px; display: block; }
.eval-problem { margin-top: 8px; }

.target-hint { font-size: 11px; color: #909399; margin-top: 4px; }
.advice-item { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.advice-text { font-size: 13px; color: #606266; }

.goal-circle { text-align: center; margin: 16px 0 8px; }
.goal-detail { text-align: center; font-size: 12px; color: #909399; display: flex; justify-content: center; align-items: center; gap: 8px; }
.suggestion-list h4 { margin: 0 0 8px; font-size: 14px; }
.suggestion-list ul { padding-left: 18px; line-height: 2; color: #606266; font-size: 13px; }

.eval-box { text-align: center; padding: 12px 0; }
.eval-name { display: block; font-size: 13px; color: #606266; margin-bottom: 6px; }
.finding-list { padding-left: 18px; line-height: 2; color: #606266; font-size: 13px; }
</style>