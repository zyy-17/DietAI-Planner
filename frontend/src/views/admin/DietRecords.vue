<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>饮食记录管理</span>
          <div class="header-actions">
            <el-input
              v-model="userId"
              placeholder="按用户ID筛选"
              clearable
              style="width: 160px"
              @keyup.enter="search"
              @clear="search" />
            <el-date-picker
              v-model="date"
              type="date"
              placeholder="按日期筛选"
              value-format="YYYY-MM-DD"
              clearable
              style="width: 180px"
              @change="search" />
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="userName" label="用户名" width="120" show-overflow-tooltip />
        <el-table-column prop="foodName" label="食物" min-width="130" show-overflow-tooltip />
        <el-table-column label="餐次" width="90">
          <template #default="{ row }">{{ mealText(row.mealType) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="份量(g)" width="90" />
        <el-table-column prop="calories" label="热量(kcal)" width="110" />
        <el-table-column prop="protein" label="蛋白质(g)" width="100" />
        <el-table-column prop="carbohydrate" label="碳水(g)" width="100" />
        <el-table-column prop="fat" label="脂肪(g)" width="90" />
        <el-table-column prop="recordDate" label="记录日期" width="120" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        @current-change="loadRecords"
        layout="prev, pager, next, total"
        style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../utils/api'

const records = ref([])
const page = ref(1)
const total = ref(0)
const userId = ref('')
const date = ref('')
const loading = ref(false)

const MEAL_MAP = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' }
function mealText(type) {
  return MEAL_MAP[type] || type || '-'
}

async function loadRecords() {
  loading.value = true
  try {
    const params = { page: page.value - 1 }
    if (userId.value) params.userId = userId.value
    if (date.value) params.date = date.value
    const data = await api.get('/admin/diet-records', { params })
    records.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadRecords()
}

function reset() {
  userId.value = ''
  date.value = ''
  search()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除这条饮食记录？', '提示', { type: 'warning' })
  await api.delete(`/admin/diet-records/${row.id}`)
  ElMessage.success('删除成功')
  loadRecords()
}

onMounted(loadRecords)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
