<template>
  <div class="diet-records">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>饮食记录</span>
          <div>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" @change="loadRecords" />
          </div>
        </div>
      </template>
      <el-table :data="records" stripe style="width:100%">
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'

const records = ref([])
const dateRange = ref(null)

function mealName(type) {
  const map = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' }
  return map[type] || type
}

async function loadRecords() {
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      const [start, end] = dateRange.value
      records.value = await api.get('/diet/records', {
        params: { startDate: dayjs(start).format('YYYY-MM-DD'), endDate: dayjs(end).format('YYYY-MM-DD') }
      })
    } else {
      records.value = await api.get('/diet/records')
    }
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
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>