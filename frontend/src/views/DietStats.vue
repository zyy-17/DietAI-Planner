<template>
  <div class="diet-stats">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>📊 饮食统计</span>
          <el-radio-group v-model="period" @change="loadStats">
            <el-radio-button value="week">本周</el-radio-button>
            <el-radio-button value="month">本月</el-radio-button>
            <el-radio-button value="year">今年</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="总记录天数" :value="stats.totalDays" suffix="天" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="日均热量" :value="stats.avgCalories" suffix="kcal" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="日均蛋白质" :value="stats.avgProtein" suffix="g" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="达标天数" :value="stats.onTargetDays" suffix="天" />
        </el-col>
      </el-row>

      <div class="chart-section">
        <div class="chart-title">每日热量摄入趋势</div>
        <div ref="chartRef" class="chart-container"></div>
      </div>

      <div class="chart-section">
        <div class="chart-title">三大营养素日均占比</div>
        <div ref="pieRef" class="chart-container"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import api from '../utils/api'

const period = ref('week')
const stats = ref({ totalDays: 0, avgCalories: 0, avgProtein: 0, onTargetDays: 0 })
const chartRef = ref(null)
const pieRef = ref(null)
let lineChart = null
let pieChart = null

async function loadStats() {
  try {
    const data = await api.get('/diet/stats', { params: { period: period.value } })
    stats.value = data || stats.value
    renderCharts(data)
  } catch (e) {
    renderCharts(null)
  }
}

function renderCharts(data) {
  const days = data?.dailyRecords || []
  const dates = days.map(d => d.date)
  const calories = days.map(d => d.totalCalories || 0)

  if (lineChart) {
    lineChart.setOption({
      xAxis: { type: 'category', data: dates },
      series: [{ data: calories, type: 'line', smooth: true, areaStyle: { opacity: 0.1 }, lineStyle: { color: '#2589ee' }, itemStyle: { color: '#2589ee' } }],
      yAxis: { type: 'value', name: 'kcal' },
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, bottom: 30, top: 30 }
    })
  }

  if (pieChart) {
    pieChart.setOption({
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: [
          { value: data?.avgProtein || 0, name: '蛋白质', itemStyle: { color: '#77b8ef' } },
          { value: data?.avgCarbohydrate || 0, name: '碳水化合物', itemStyle: { color: '#80d1ad' } },
          { value: data?.avgFat || 0, name: '脂肪', itemStyle: { color: '#ffb56b' } }
        ],
        label: { formatter: '{b}: {d}%' }
      }],
      tooltip: { trigger: 'item' }
    })
  }
}

onMounted(async () => {
  await loadStats()
  if (chartRef.value) {
    lineChart = echarts.init(chartRef.value)
  }
  if (pieRef.value) {
    pieChart = echarts.init(pieRef.value)
  }
  loadStats()
})

onBeforeUnmount(() => {
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.diet-stats { padding: 18px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-section { margin-top: 20px; }
.chart-title { font-size: 14px; font-weight: 600; color: #244b6b; margin-bottom: 10px; }
.chart-container { height: 300px; }
</style>