<template>
  <div class="meal-detail">
    <div class="meal-header" :class="mealType">
      <div class="meal-icon">{{ mealInfo.icon }}</div>
      <div>
        <h1>{{ mealInfo.icon }} {{ mealInfo.name }}</h1>
        <p>{{ mealInfo.time }} · {{ todayStr }}</p>
      </div>
      <button class="add-btn" @click="openAddDialog">＋ 添加食物</button>
    </div>

    <div class="meal-summary">
      <div class="summary-item">
        <span class="label">🔥 热量</span>
        <b>{{ totalCalories }}</b>
        <small>kcal</small>
      </div>
      <div class="summary-item">
        <span class="label">🥩 蛋白质</span>
        <b>{{ totalProtein }}</b>
        <small>g</small>
      </div>
      <div class="summary-item">
        <span class="label">🌾 碳水</span>
        <b>{{ totalCarb }}</b>
        <small>g</small>
      </div>
      <div class="summary-item">
        <span class="label">🫒 脂肪</span>
        <b>{{ totalFat }}</b>
        <small>g</small>
      </div>
    </div>

    <el-card v-if="mealRecords.length === 0" class="empty-card">
      <el-empty description="暂无饮食记录，快去添加食物吧～" :image-size="80" />
    </el-card>

    <el-card v-else>
      <el-table :data="mealRecords" stripe style="width:100%">
        <el-table-column prop="foodName" label="食物" />
        <el-table-column prop="amount" label="份量(g)" width="100" />
        <el-table-column prop="calories$amount" label="热量(kcal)" width="120">
          <template #default="{ row }">{{ ((row.calories || 0) * (row.amount || 100) / 100).toFixed(0) }}</template>
        </el-table-column>
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

    <el-dialog v-model="addDialogVisible" :title="`添加食物 - ${mealInfo.name}`" width="480px">
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const route = useRoute()
const mealType = computed(() => route.meta.mealType || 'breakfast')

const mealMap = {
  breakfast: { name: '早餐', icon: '🍳', time: '07:00 - 09:00' },
  lunch: { name: '午餐', icon: '🥗', time: '11:00 - 13:00' },
  dinner: { name: '晚餐', icon: '🍗', time: '17:30 - 19:30' },
  snack: { name: '加餐', icon: '🍎', time: '15:00 - 16:00' }
}

const mealInfo = computed(() => mealMap[mealType.value] || mealMap.breakfast)

const now = new Date()
const todayStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`

const records = ref([])
const mealRecords = computed(() => records.value.filter(r => r.mealType === mealType.value))

const totalCalories = computed(() => mealRecords.value.reduce((s, r) => s + (r.calories || 0), 0))
const totalProtein = computed(() => mealRecords.value.reduce((s, r) => s + (r.protein || 0), 0).toFixed(1))
const totalCarb = computed(() => mealRecords.value.reduce((s, r) => s + (r.carbohydrate || 0), 0).toFixed(1))
const totalFat = computed(() => mealRecords.value.reduce((s, r) => s + (r.fat || 0), 0).toFixed(1))

const addDialogVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const allFoods = ref([])
const addForm = reactive({ foodId: null, amount: 100, mealType: 'breakfast' })

async function loadData() {
  try { records.value = await api.get('/diet/today/records') } catch (e) {}
  try {
    allFoods.value = await api.get('/foods/all')
    searchResults.value = allFoods.value
  } catch (e) {}
}

function openAddDialog() {
  addForm.mealType = mealType.value
  addForm.foodId = null
  addForm.amount = 100
  searchKeyword.value = ''
  searchResults.value = allFoods.value
  addDialogVisible.value = true
}

function searchFood() {
  if (searchKeyword.value.length < 1) { searchResults.value = allFoods.value; return }
  const kw = searchKeyword.value.toLowerCase()
  searchResults.value = allFoods.value.filter(f => f.name.toLowerCase().includes(kw))
}

async function addDietRecord() {
  if (!addForm.foodId) return ElMessage.warning('请选择食物')
  try {
    await api.post('/diet/today/add', addForm)
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error('添加失败') }
}

async function deleteRecord(id) {
  await ElMessageBox.confirm('确定删除此记录？', '提示', { type: 'warning' })
  try {
    await api.delete(`/diet/record/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {}
}

onMounted(loadData)
</script>

<style scoped>
.meal-detail { padding: 18px; }
.meal-header { display: flex; align-items: center; gap: 16px; padding: 20px 24px; border-radius: 13px; margin-bottom: 16px; }
.meal-header.breakfast { background: linear-gradient(100deg, #fffaf0, #fff5e6); }
.meal-header.lunch { background: linear-gradient(100deg, #f2fbf6, #e8f8ee); }
.meal-header.dinner { background: linear-gradient(100deg, #f1f8ff, #e6f0ff); }
.meal-header.snack { background: linear-gradient(100deg, #fef8f4, #fff0e8); }
.meal-icon { font-size: 48px; }
.meal-header h1 { margin: 0; font-size: 22px; color: #153d67; }
.meal-header p { margin: 4px 0 0; color: #718b9c; font-size: 13px; }
.add-btn { margin-left: auto; border: 0; background: #288df0; color: #fff; border-radius: 18px; padding: 10px 20px; cursor: pointer; font-size: 14px; white-space: nowrap; }
.meal-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.summary-item { background: #fff; border: 1px solid #e5edf2; border-radius: 10px; padding: 16px; text-align: center; }
.summary-item .label { display: block; font-size: 12px; color: #8ea1af; margin-bottom: 6px; }
.summary-item b { font-size: 22px; color: #244b6b; }
.summary-item small { font-size: 11px; color: #8ea1af; margin-left: 2px; }
.empty-card { margin-top: 16px; }
</style>