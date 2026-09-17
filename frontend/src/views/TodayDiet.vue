<template>
  <div class="today-diet">
    <el-row :gutter="20" class="overview-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">已摄入</div>
          <div class="stat-value">{{ overview.totalCalories || 0 }} <span class="unit">kcal</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card remain">
          <div class="stat-label">剩余可摄入</div>
          <div class="stat-value">{{ overview.remainingCalories || 0 }} <span class="unit">kcal</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">蛋白质</div>
          <div class="stat-value">{{ overview.totalProtein || 0 }} / {{ overview.targetProtein || 0 }} <span class="unit">g</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">碳水</div>
          <div class="stat-value">{{ overview.totalCarbohydrate || 0 }} / {{ overview.targetCarbohydrate || 0 }} <span class="unit">g</span></div>
        </el-card>
      </el-col>
    </el-row>

    <div v-for="meal in meals" :key="meal.type" class="meal-section">
      <div class="meal-header">
        <span class="meal-icon">{{ meal.icon }}</span>
        <span class="meal-name">{{ meal.name }}</span>
        <el-button type="primary" size="small" @click="openAddDialog(meal.type)">添加食物</el-button>
      </div>
      <el-table :data="getMealRecords(meal.type)" stripe empty-text="暂无记录" style="width:100%">
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
    </div>

    <el-dialog v-model="addDialogVisible" title="添加食物" width="500px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="搜索食物">
          <el-input v-model="searchKeyword" placeholder="输入食物名称搜索" @input="searchFood" />
        </el-form-item>
        <el-form-item label="选择食物">
          <el-select v-model="addForm.foodId" filterable placeholder="请选择食物" style="width:100%">
            <el-option v-for="f in searchResults" :key="f.id" :label="`${f.name} (${f.calories}kcal/100g)`" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="份量(g)">
          <el-input-number v-model="addForm.amount" :min="1" :max="5000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addDietRecord">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const overview = ref({})
const records = ref([])
const addDialogVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const addForm = reactive({ foodId: null, amount: 100, mealType: 'breakfast' })

const meals = [
  { type: 'breakfast', name: '早餐', icon: '🍳' },
  { type: 'lunch', name: '午餐', icon: '🍱' },
  { type: 'dinner', name: '晚餐', icon: '🍲' },
  { type: 'snack', name: '加餐', icon: '🍪' }
]

function getMealRecords(mealType) {
  return records.value.filter(r => r.mealType === mealType)
}

async function loadData() {
  try {
    overview.value = await api.get('/diet/today')
    records.value = await api.get('/diet/today/records')
  } catch (e) {}
}

function openAddDialog(mealType) {
  addForm.mealType = mealType
  addForm.foodId = null
  addForm.amount = 100
  searchKeyword.value = ''
  searchResults.value = []
  addDialogVisible.value = true
}

async function searchFood() {
  if (searchKeyword.value.length < 1) return
  searchResults.value = await api.get('/foods/search', { params: { keyword: searchKeyword.value } })
}

async function addDietRecord() {
  if (!addForm.foodId) return ElMessage.warning('请选择食物')
  await api.post('/diet/today/add', addForm)
  ElMessage.success('添加成功')
  addDialogVisible.value = false
  loadData()
}

async function deleteRecord(id) {
  await ElMessageBox.confirm('确定删除此记录？', '提示', { type: 'warning' })
  await api.delete(`/diet/record/${id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.overview-row { margin-bottom: 20px; }
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-value { font-size: 24px; font-weight: bold; color: #409eff; }
.stat-value .unit { font-size: 14px; color: #909399; }
.stat-card.remain .stat-value { color: #67c23a; }
.meal-section { margin-bottom: 20px; }
.meal-header { display: flex; align-items: center; margin-bottom: 12px; gap: 8px; }
.meal-icon { font-size: 24px; }
.meal-name { font-size: 18px; font-weight: bold; flex: 1; }
</style>