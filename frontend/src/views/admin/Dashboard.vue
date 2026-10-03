<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="8" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: card.bg }">{{ card.icon }}</div>
            <div class="stat-text">
              <div class="stat-value">{{ overview[card.key] ?? 0 }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="trend-card">
      <template #header>
        <div class="card-header">
          <span>饮食记录趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="14">近14天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="chartRef" class="chart"></div>
    </el-card>

    <el-row :gutter="16">
      <el-col :md="12">
        <el-card>
          <template #header><span>快捷入口</span></template>
          <el-button type="primary" plain @click="$router.push('/admin/foods')">➕ 添加食物</el-button>
          <el-button type="info" plain @click="$router.push('/admin/users')">👥 用户管理</el-button>
        </el-card>
      </el-col>
      <el-col :md="12">
        <el-card>
          <template #header><span>AI 服务概况</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="AI 生成总次数">{{ overview.totalAiLogs ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="标记异常次数">
              <el-tag :type="(overview.abnormalAiLogs ?? 0) > 0 ? 'danger' : 'success'">{{ overview.abnormalAiLogs ?? 0 }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../../utils/api'

const overview = reactive({
  totalUsers: 0,
  todayNewUsers: 0,
  totalFoods: 0,
  totalDietRecords: 0,
  todayDietRecords: 0,
  totalAiLogs: 0,
  abnormalAiLogs: 0
})

const cards = [
  { key: 'totalUsers', label: '注册用户', icon: '👥', bg: '#e6f1ff' },
  { key: 'todayNewUsers', label: '今日新增用户', icon: '🆕', bg: '#e8f8ef' },
  { key: 'totalFoods', label: '食物库总数', icon: '🍎', bg: '#fff3e0' },
  { key: 'totalDietRecords', label: '饮食记录总数', icon: '🍽️', bg: '#f0e9ff' },
  { key: 'todayDietRecords', label: '今日记录数', icon: '📅', bg: '#e6f7f8' },
  { key: 'totalAiLogs', label: 'AI 生成次数', icon: '🤖', bg: '#eef2f7' },
  { key: 'abnormalAiLogs', label: 'AI 异常次数', icon: '⚠️', bg: '#fdecea' }
]

const days = ref(7)
const chartRef = ref(null)
let chart = null

async function loadOverview() {
  const data = await api.get('/admin/stats/overview')
  Object.assign(overview, data)
}

async function loadTrend() {
  const data = await api.get('/admin/stats/trend', { params: { days: days.value } })
  if (!chart) return
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.date.slice(5)),
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#c0c4cc' } }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef1f5' } }
    },
    series: [
      {
        name: '记录数',
        type: 'line',
        smooth: true,
        data: data.map((d) => d.count),
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.35)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ])
        }
      }
    ]
  })
}

function handleResize() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartRef.value)
  await Promise.all([loadOverview(), loadTrend()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
  border-radius: 10px;
}
.stat-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #244b6b;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}
.trend-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart {
  height: 320px;
  width: 100%;
}
</style>
