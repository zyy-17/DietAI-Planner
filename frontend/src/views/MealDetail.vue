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
        <el-table-column label="热量(kcal)" width="120">
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

    <el-dialog v-model="addDialogVisible" :title="`添加食物 - ${mealInfo.name}`" width="620px" top="6vh">
      <div class="multi-add-header">
        🔍 搜索并选择多种食物，一次性添加到{{ mealInfo.name }}
      </div>
      <el-input v-model="searchKeyword" placeholder="输入食物名称搜索，如：鸡蛋、牛奶、米饭..." @input="searchFood" clearable style="margin-bottom:12px" />

      <div class="food-select-list" v-if="filteredFoods.length">
        <div v-for="f in filteredFoods" :key="f.id" class="food-select-item" :class="{ chosen: isChosen(f.id) }" @click="toggleFood(f)">
          <div class="food-select-left">
            <span class="check-box">{{ isChosen(f.id) ? '✅' : '⬜' }}</span>
            <span class="food-select-name">{{ f.name }}</span>
            <small class="food-select-cal">{{ f.calories }} kcal/100g</small>
          </div>
          <div v-if="isChosen(f.id)" class="food-select-amount" @click.stop>
            <el-input-number v-model="getChosenItem(f.id).amount" :min="1" :max="5000" :step="10" size="small" style="width:120px" />
            <small>g</small>
          </div>
        </div>
      </div>
      <el-empty v-else description="未找到食物，试试其他关键词" :image-size="50" />

      <div v-if="chosenFoods.length" class="chosen-summary">
        <div class="chosen-title">已选择 {{ chosenFoods.length }} 种食物</div>
        <div class="chosen-preview">
          <div v-for="c in chosenFoods" :key="c.foodId" class="chosen-item">
            <span>{{ c.foodName }}</span>
            <small>{{ c.amount }}g</small>
            <em>{{ ((c.calories || 0) * c.amount / 100).toFixed(0) }} kcal</em>
            <i @click="removeChosen(c.foodId)">✕</i>
          </div>
        </div>
        <div class="chosen-total">
          合计 🔥 <b>{{ chosenTotalCal }}</b> kcal　
          🥩 <b>{{ chosenTotalProtein }}</b> g　
          🌾 <b>{{ chosenTotalCarb }}</b> g　
          🫒 <b>{{ chosenTotalFat }}</b> g
        </div>
      </div>

      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addDietRecords" :disabled="chosenFoods.length === 0">
          确认添加（{{ chosenFoods.length }} 种）
        </el-button>
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
const allFoods = ref([])
const filteredFoods = ref([])
const chosenFoods = ref([])

function isChosen(foodId) {
  return chosenFoods.value.some(c => c.foodId === foodId)
}

function getChosenItem(foodId) {
  return chosenFoods.value.find(c => c.foodId === foodId)
}

function toggleFood(food) {
  const idx = chosenFoods.value.findIndex(c => c.foodId === food.id)
  if (idx >= 0) {
    chosenFoods.value.splice(idx, 1)
  } else {
    chosenFoods.value.push({
      foodId: food.id,
      foodName: food.name,
      amount: 100,
      calories: food.calories || 0,
      protein: food.protein || 0,
      carbohydrate: food.carbohydrate || 0,
      fat: food.fat || 0
    })
  }
}

function removeChosen(foodId) {
  const idx = chosenFoods.value.findIndex(c => c.foodId === foodId)
  if (idx >= 0) chosenFoods.value.splice(idx, 1)
}

const chosenTotalCal = computed(() => chosenFoods.value.reduce((s, c) => s + (c.calories || 0) * c.amount / 100, 0).toFixed(0))
const chosenTotalProtein = computed(() => chosenFoods.value.reduce((s, c) => s + (c.protein || 0) * c.amount / 100, 0).toFixed(1))
const chosenTotalCarb = computed(() => chosenFoods.value.reduce((s, c) => s + (c.carbohydrate || 0) * c.amount / 100, 0).toFixed(1))
const chosenTotalFat = computed(() => chosenFoods.value.reduce((s, c) => s + (c.fat || 0) * c.amount / 100, 0).toFixed(1))

async function loadData() {
  try { records.value = await api.get('/diet/today/records') } catch (e) {}
  try { allFoods.value = await api.get('/foods/all') } catch (e) {}
}

function openAddDialog() {
  searchKeyword.value = ''
  filteredFoods.value = allFoods.value
  chosenFoods.value = []
  addDialogVisible.value = true
}

function searchFood() {
  if (!searchKeyword.value) { filteredFoods.value = allFoods.value; return }
  const kw = searchKeyword.value.toLowerCase()
  filteredFoods.value = allFoods.value.filter(f => f.name.toLowerCase().includes(kw))
}

async function addDietRecords() {
  if (chosenFoods.value.length === 0) return ElMessage.warning('请至少选择一种食物')
  try {
    const payload = chosenFoods.value.map(c => ({
      foodId: c.foodId,
      amount: c.amount,
      mealType: mealType.value
    }))
    await api.post('/diet/today/add-batch', payload)
    ElMessage.success(`成功添加 ${chosenFoods.value.length} 种食物`)
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

.multi-add-header { font-size: 13px; color: #55738d; margin-bottom: 12px; padding: 8px 12px; background: #f0f7ff; border-radius: 8px; }
.food-select-list { max-height: 280px; overflow-y: auto; border: 1px solid #eef2f6; border-radius: 8px; }
.food-select-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #f5f7fa; transition: background 0.15s; }
.food-select-item:hover { background: #f5f9ff; }
.food-select-item.chosen { background: #eef7ff; }
.food-select-left { display: flex; align-items: center; gap: 8px; }
.check-box { font-size: 16px; }
.food-select-name { font-size: 14px; color: #244b6b; }
.food-select-cal { font-size: 12px; color: #8ea1af; margin-left: 6px; }
.food-select-amount { display: flex; align-items: center; gap: 4px; }
.food-select-amount small { color: #8ea1af; }

.chosen-summary { margin-top: 14px; border: 1px solid #d9eafa; border-radius: 10px; padding: 12px; background: #f8fbff; }
.chosen-title { font-size: 13px; font-weight: 600; color: #2589ee; margin-bottom: 8px; }
.chosen-preview { max-height: 140px; overflow-y: auto; }
.chosen-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; border-bottom: 1px solid #eef2f6; }
.chosen-item span { flex: 1; color: #244b6b; }
.chosen-item small { color: #8499a8; }
.chosen-item em { color: #e6a23c; font-style: normal; }
.chosen-item i { color: #c0c4cc; cursor: pointer; font-style: normal; }
.chosen-item i:hover { color: #f56c6c; }
.chosen-total { margin-top: 8px; font-size: 12px; color: #55738d; padding-top: 8px; border-top: 1px solid #eef2f6; }
.chosen-total b { color: #244b6b; }
</style>