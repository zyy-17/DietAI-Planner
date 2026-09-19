<template>
  <div class="nutrition-analysis">
    <!-- 智能营养评估卡片（优化一） -->
    <el-card v-if="evaluation" class="eval-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🧬 智能营养评估</span>
          <el-tag :type="evaluation.nutritionScore >= 80 ? 'success' : evaluation.nutritionScore >= 60 ? 'warning' : 'danger'" size="large" effect="dark">
            {{ evaluation.nutritionScore }} 分
          </el-tag>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="6">
          <div class="eval-metric">
            <div class="metric-label">BMI</div>
            <div class="metric-value">{{ evaluation.bmi || '-' }}</div>
            <div class="metric-tag" :class="bmiClass">{{ bmiDesc }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="eval-metric">
            <div class="metric-label">基础代谢 BMR</div>
            <div class="metric-value">{{ evaluation.bmr || '-' }}</div>
            <div class="metric-unit">kcal/天</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="eval-metric">
            <div class="metric-label">总消耗 TDEE</div>
            <div class="metric-value">{{ evaluation.tdee || '-' }}</div>
            <div class="metric-unit">kcal/天</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="eval-metric">
            <div class="metric-label">目标热量</div>
            <div class="metric-value">{{ evaluation.targetCalories || '-' }}</div>
            <div class="metric-unit">kcal/天</div>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <div class="eval-status-row">
        <div class="status-item">
          <span class="status-label">🔥 热量</span>
          <el-tag :type="statusType(evaluation.calorieStatus)" size="small">{{ evaluation.calorieStatus }}</el-tag>
          <span class="status-gap">{{ formatGap(evaluation.calorieGap) }} kcal</span>
        </div>
        <div class="status-item">
          <span class="status-label">🥩 蛋白质</span>
          <el-tag :type="statusType(evaluation.proteinStatus)" size="small">{{ evaluation.proteinStatus }}</el-tag>
          <span class="status-gap">{{ formatGap(evaluation.proteinGap) }} g</span>
        </div>
        <div class="status-item">
          <span class="status-label">🌾 碳水</span>
          <el-tag :type="statusType(evaluation.carbStatus)" size="small">{{ evaluation.carbStatus }}</el-tag>
          <span class="status-gap">{{ formatGap(evaluation.carbGap) }} g</span>
        </div>
        <div class="status-item">
          <span class="status-label">🫒 脂肪</span>
          <el-tag :type="statusType(evaluation.fatStatus)" size="small">{{ evaluation.fatStatus }}</el-tag>
          <span class="status-gap">{{ formatGap(evaluation.fatGap) }} g</span>
        </div>
      </div>

      <el-divider />

      <div class="eval-score-bar">
        <span class="score-label">营养评分</span>
        <el-progress :percentage="evaluation.nutritionScore" :stroke-width="20" :color="scoreColor" :format="() => evaluation.nutritionScore + '分'" />
      </div>

      <div class="eval-problem" v-if="evaluation.mainProblem">
        <el-alert :title="'⚠️ 主要问题：' + evaluation.mainProblem" :type="evaluation.nutritionScore >= 80 ? 'success' : 'warning'" :closable="false" show-icon />
      </div>
    </el-card>

    <!-- 原有营养分析卡片 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <span>{{ titleText }}</span>
          <el-radio-group v-model="days" @change="loadAnalysis">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <div class="chart-title">{{ lineChartTitle }}</div>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-col>
        <el-col :span="12">
          <div class="chart-title">{{ pieChartTitle }}</div>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top:20px">
        <el-col :span="6"><el-statistic title="日均热量" :value="analysis.avgCalories" suffix="kcal" /></el-col>
        <el-col :span="6"><el-statistic title="日均蛋白质" :value="analysis.avgProtein" suffix="g" /></el-col>
        <el-col :span="6"><el-statistic title="日均碳水" :value="analysis.avgCarbohydrate" suffix="g" /></el-col>
        <el-col :span="6"><el-statistic title="日均脂肪" :value="analysis.avgFat" suffix="g" /></el-col>
      </el-row>

      <div v-if="mode === 'goal'" style="margin-top:20px">
        <div class="chart-title">🎯 目标完成度</div>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-progress type="circle" :percentage="goalCalorie" :width="120" :stroke-width="10" color="#8ed4ae">
              <template #default><b>{{ goalCalorie }}%</b><small>热量</small></template>
            </el-progress>
          </el-col>
          <el-col :span="8">
            <el-progress type="circle" :percentage="goalProtein" :width="120" :stroke-width="10" color="#77b8ef">
              <template #default><b>{{ goalProtein }}%</b><small>蛋白质</small></template>
            </el-progress>
          </el-col>
          <el-col :span="8">
            <el-progress type="circle" :percentage="goalCarb" :width="120" :stroke-width="10" color="#80d1ad">
              <template #default><b>{{ goalCarb }}%</b><small>碳水</small></template>
            </el-progress>
          </el-col>
        </el-row>
      </div>

      <div v-if="mode === 'report'" style="margin-top:20px">
        <el-card shadow="never">
          <template #header><span>📄 营养报告摘要</span></template>
          <p style="line-height:1.8;color:#606266">
            近{{ days }}天日均摄入热量 {{ analysis.avgCalories || 0 }} kcal，
            蛋白质 {{ analysis.avgProtein || 0 }}g，
            碳水化合物 {{ analysis.avgCarbohydrate || 0 }}g，
            脂肪 {{ analysis.avgFat || 0 }}g。
            {{ goalCalorie >= 90 ? '热量摄入基本达标，' : '热量摄入不足，建议适当增加。' }}
            {{ goalProtein >= 90 ? '蛋白质摄入良好。' : '蛋白质摄入偏低，建议增加优质蛋白来源。' }}
          </p>
        </el-card>
      </div>

      <el-card v-if="analysis.aiInterpretation" style="margin-top:20px" shadow="never">
        <template #header><span>🤖 AI智能解读</span></template>
        <p style="line-height:1.8;color:#606266">{{ analysis.aiInterpretation }}</p>
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import api from '../utils/api'

const route = useRoute()
const mode = computed(() => route.meta.mode || 'default')

const titleMap = {
  default: '今日营养',
  trend: '营养趋势',
  nutrients: '营养素分析',
  calorie: '热量分析',
  goal: '目标完成度',
  report: '营养报告'
}
const titleText = computed(() => titleMap[mode.value] || '营养分析')

const lineChartTitle = computed(() => {
  if (mode.value === 'calorie') return '每日热量摄入'
  if (mode.value === 'nutrients') return '每日营养素趋势'
  return '每日热量趋势'
})
const pieChartTitle = computed(() => {
  if (mode.value === 'nutrients') return '营养素构成分析'
  return '三大营养素占比'
})

const days = ref(7)
const analysis = ref({})
const evaluation = ref(null)
const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

const goalCalorie = computed(() => Math.min(Math.round(((analysis.value.avgCalories || 0) / (analysis.value.targetCalories || 2000)) * 100), 100))
const goalProtein = computed(() => Math.min(Math.round(((analysis.value.avgProtein || 0) / (analysis.value.targetProtein || 100)) * 100), 100))
const goalCarb = computed(() => Math.min(Math.round(((analysis.value.avgCarbohydrate || 0) / (analysis.value.targetCarbohydrate || 250)) * 100), 100))

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
  if (status === '正常') return 'success'
  if (status === '不足') return 'warning'
  if (status === '偏高') return 'danger'
  return 'info'
}

function formatGap(gap) {
  if (gap === null || gap === undefined) return '-'
  const num = Number(gap)
  if (num > 0) return `+${num.toFixed(0)}`
  return num.toFixed(0)
}

async function loadEvaluation() {
  try {
    evaluation.value = await api.get('/nutrition/evaluate')
  } catch (e) {
    console.warn('加载营养评估失败', e)
  }
}

async function loadAnalysis() {
  try {
    analysis.value = await api.get('/nutrition/analysis', { params: { days: days.value } })
  } catch (e) {}
  await nextTick()
  renderCharts()
}

function renderCharts() {
  if (!analysis.value.dailyData) return

  if (lineChartRef.value && !lineChart) {
    lineChart = echarts.init(lineChartRef.value)
  }
  if (lineChart) {
    const dailyData = analysis.value.dailyData || []
    const seriesData = mode.value === 'nutrients'
      ? [
          { name: '蛋白质', data: dailyData.map(d => d.protein || 0), type: 'line', smooth: true },
          { name: '碳水', data: dailyData.map(d => d.carbohydrate || 0), type: 'line', smooth: true },
          { name: '脂肪', data: dailyData.map(d => d.fat || 0), type: 'line', smooth: true }
        ]
      : [{ data: dailyData.map(d => d.calories), type: 'line', smooth: true, areaStyle: { opacity: 0.3 } }]

    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dailyData.map(d => d.date) },
      yAxis: { type: 'value', name: mode.value === 'nutrients' ? 'g' : 'kcal' },
      series: seriesData
    })
  }

  if (pieChartRef.value && !pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  if (pieChart) {
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: [
          { name: '蛋白质', value: analysis.value.proteinRatio || 0, itemStyle: { color: '#77b8ef' } },
          { name: '碳水化合物', value: analysis.value.carbRatio || 0, itemStyle: { color: '#80d1ad' } },
          { name: '脂肪', value: analysis.value.fatRatio || 0, itemStyle: { color: '#ffb56b' } }
        ]
      }]
    })
  }
}

onMounted(() => {
  loadEvaluation()
  loadAnalysis()
})

onBeforeUnmount(() => {
  lineChart?.dispose()
  pieChart?.dispose()
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
</style>