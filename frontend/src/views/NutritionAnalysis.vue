<template>
  <div class="nutrition-analysis">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>营养分析</span>
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
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../utils/api'

const days = ref(7)
const analysis = ref({})
const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

async function loadAnalysis() {
  analysis.value = await api.get('/nutrition/analysis', { params: { days: days.value } })
  await nextTick()
  renderCharts()
}

function renderCharts() {
  if (!analysis.value.dailyData) return

  if (lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: analysis.value.dailyData.map(d => d.date) },
      yAxis: { type: 'value', name: 'kcal' },
      series: [{ data: analysis.value.dailyData.map(d => d.calories), type: 'line', smooth: true, areaStyle: { opacity: 0.3 } }]
    })
  }

  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: [
          { name: '蛋白质', value: analysis.value.proteinRatio },
          { name: '碳水化合物', value: analysis.value.carbRatio },
          { name: '脂肪', value: analysis.value.fatRatio }
        ]
      }]
    })
  }
}

onMounted(loadAnalysis)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-title { text-align: center; font-weight: bold; margin-bottom: 8px; }
.chart-container { height: 300px; }
</style>