<template>
  <div class="diet-records">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ titleText }}</span>
          <div>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束-date" @change="loadRecords" />
          </div>
        </div>
      </template>

      <div v-if="mode === 'search'" style="margin-bottom:16px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-select v-model="searchForm.mealType" placeholder="餐次" clearable style="width:100%">
              <el-option value="breakfast" label="早餐" />
              <el-option value="lunch" label="午餐" />
              <el-option value="dinner" label="晚餐" />
              <el-option value="snack" label="加餐" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-input v-model="searchForm.foodName" placeholder="食物名称" clearable />
          </el-col>
          <el-col :span="6">
            <el-select v-model="searchForm.sortBy" placeholder="排序" style="width:100%">
              <el-option value="date" label="按日期" />
              <el-option value="calories" label="按热量" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-button type="primary" @click="loadRecords" style="width:100%">查询</el-button>
          </el-col>
        </el-row>
      </div>

      <el-table :data="filteredRecords" stripe style="width:100%">
        <el-table-column prop="recordDate" label="日期" width="120" />
        <el-table-column prop="mealType" label="餐次" width="80">
          <template #default="{ row }">{{ mealName(row.mealType) }}</template>
        </el-table-column>
        <el-table-column prop="foodName" label="食物" />
        <el-table-column prop="amount" label="份量(g)" width="100" />
        <el-table-column prop="calories" label="热量(kcal)" width="120" />
        <el-table-column prop="protein" label="蛋白质(g)" width="100" />
        <el-table-column prop="carbohydrate" label="碳水(g)" width="100" />
        <el-table-column prop="fat" label="脂肪(g)" width="100" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="danger" text size="small" @click="deleteRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'

const route = useRoute()
const mode = computed(() => route.meta.mode || 'default')

const titleMap = { default: '饮食记录', week: '周记录', month: '月记录', search: '条件查询' }
const titleText = computed(() => titleMap[mode.value] || '饮食记录')

const records = ref([])
const dateRange = ref(null)
const searchForm = reactive({ mealType: '', foodName: '', sortBy: 'date' })

function mealName(type) {
  const map = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' }
  return map[type] || type
}

const filteredRecords = computed(() => {
  let list = [...records.value]
  if (mode.value === 'search') {
    if (searchForm.mealType) list = list.filter(r => r.mealType === searchForm.mealType)
    if (searchForm.foodName) list = list.filter(r => (r.foodName || '').includes(searchForm.foodName))
    if (searchForm.sortBy === 'calories') list.sort((a, b) => (b.calories || 0) - (a.calories || 0))
  }
  return list
})

async function loadRecords() {
  try {
    let params = {}
    if (mode.value === 'week') {
      const end = dayjs()
      const start = end.subtract(7, 'day')
      params = { startDate: start.format('YYYY-MM-DD'), endDate: end.format('YYYY-MM-DD') }
    } else if (mode.value === 'month') {
      const end = dayjs()
      const start = end.subtract(30, 'day')
      params = { startDate: start.format('YYYY-MM-DD'), endDate: end.format('YYYY-MM-DD') }
    } else if (dateRange.value && dateRange.value.length === 2) {
      const [start, end] = dateRange.value
      params = { startDate: dayjs(start).format('YYYY-MM-DD'), endDate: dayjs(end).format('YYYY-MM-DD') }
    }
    records.value = await api.get('/diet/records', { params })
  } catch (e) {}
}

async function deleteRecord(id) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await api.delete(`/diet/record/${id}`)
  ElMessage.success('删除成功')
  loadRecords()
}

onMounted(loadRecords)
</script>

<style scoped>
.diet-records { padding: 18px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>