<template>
  <div class="today-page">
    <section class="welcome">
      <div class="welcome-text">
        <span class="greeting-icon">{{ greetingIcon }}</span>
        <div>
          <h1>{{ greetingText }}，<br>今天也要好好吃饭！</h1>
          <p>合理膳食 · 科学营养 · 健康生活</p>
        </div>
      </div>
      <div class="welcome-right">
        <div class="date-card">📅 {{ todayStr }}　{{ weekDay }}</div>
        <div class="target-card">
          🎯 <small>今日目标</small>
          <b>{{ overview.targetCalories || 0 }} <i>kcal</i></b>
          <button @click="showTargetDialog = true">修改目标</button>
        </div>
      </div>
    </section>

    <section class="stats">
      <div class="card calorie-card">
        <div class="circle-wrap">
          <svg class="ring" viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="52" fill="none" stroke="#e8edf2" stroke-width="10"/>
            <circle cx="60" cy="60" r="52" fill="none" stroke="#8ed4ae" stroke-width="10"
              :stroke-dasharray="calorieDash" stroke-linecap="round" transform="rotate(-90 60 60)"/>
          </svg>
          <div class="circle-inner">
            🔥<small>已摄入</small>
            <b>{{ overview.totalCalories || 0 }}</b>
            <i>/ {{ overview.targetCalories || 0 }} kcal</i>
            <small>剩余 {{ overview.remainingCalories || 0 }} kcal</small>
          </div>
        </div>
      </div>

      <div v-for="n in nutrients" :key="n.key" class="card macro-card">
        <div>{{ n.icon }}　{{ n.name }}</div>
        <strong>{{ n.current }} <i>/ {{ n.target }} g</i></strong>
        <small>目标 {{ n.target }} g</small>
        <div class="progress"><span :style="{ width: n.percent + '%' }" :class="n.barClass"></span></div>
        <em>{{ n.percent }}%</em>
      </div>

      <div class="card quick-card">
        <h3>⚡ 快速记录</h3>
        <div class="search-bar">
          🔍 <input v-model="searchKeyword" placeholder="搜索食物名称，如：鸡蛋、牛奶..." @input="searchFood">
        </div>
        <div v-if="searchResults.length" class="search-results">
          <div v-for="f in searchResults.slice(0,5)" :key="f.id" class="search-item" @click="selectFood(f)">
            {{ f.name }} <small>{{ f.calories }}kcal/100g</small>
          </div>
        </div>
        <div class="quick-btns">
          <button @click="openAddDialog('breakfast')">🍳<small>早餐</small></button>
          <button @click="openAddDialog('lunch')">🍱<small>午餐</small></button>
          <button @click="openAddDialog('dinner')">🍲<small>晚餐</small></button>
          <button @click="openAddDialog('snack')">🍪<small>加餐</small></button>
        </div>
      </div>
    </section>

    <section class="grid">
      <div class="left">
        <div v-for="meal in meals" :key="meal.type" class="card meal-card" :class="meal.type">
          <div class="meal-img">{{ meal.icon }}</div>
          <div class="meal-info">
            <div class="meal-title">
              <h2>{{ meal.icon }} {{ meal.name }}</h2>
              <small>{{ meal.time }}</small>
            </div>
            <p v-if="getMealRecords(meal.type).length === 0">暂无饮食记录，快去添加食物吧～</p>
            <div v-else class="meal-foods">
              <div v-for="r in getMealRecords(meal.type)" :key="r.id" class="meal-food-item">
                <span>{{ r.foodName }}</span>
                <small>{{ r.amount }}g</small>
                <em>{{ r.calories }}kcal</em>
                <i class="del" @click="deleteRecord(r.id)">✕</i>
              </div>
            </div>
            <div class="meal-data">
              🔥 {{ getMealCal(meal.type) }} kcal　
              🥩 {{ getMealNut(meal.type, 'protein') }} g　
              🌾 {{ getMealNut(meal.type, 'carbohydrate') }} g　
              🫒 {{ getMealNut(meal.type, 'fat') }} g
            </div>
          </div>
          <button class="add-btn" @click="openAddDialog(meal.type)">＋ 添加食物</button>
        </div>

        <div class="card history-card">
          <div class="title-row">
            <h3>🍀 最近常吃</h3>
            <span @click="$router.push('/foods')">查看全部 ›</span>
          </div>
          <div class="foods-grid">
            <div v-for="f in frequentFoods" :key="f.name" class="food-item">
              <b>{{ f.icon }}</b>
              <strong>{{ f.name }}</strong>
              <small>{{ f.num }}次</small>
            </div>
          </div>
        </div>
      </div>

      <div class="right">
        <div class="card ai-card">
          <div class="title-row">
            <h3>🤖 AI 今日建议</h3>
            <span @click="refreshAiSuggestion">⟳ 换一换</span>
          </div>
          <div class="ai-food">
            <div class="ai-food-icon">{{ aiSuggestion.icon }}</div>
            <div>
              <label>{{ aiSuggestion.label }}</label>
              <h4>{{ aiSuggestion.title }}</h4>
              <p>{{ aiSuggestion.desc }}</p>
            </div>
          </div>
          <div class="ai-data">
            <span>🔥<small>热量</small><b>{{ aiSuggestion.calories }} kcal</b></span>
            <span>🥩<small>蛋白质</small><b>{{ aiSuggestion.protein }} g</b></span>
            <span>🌾<small>碳水</small><b>{{ aiSuggestion.carb }} g</b></span>
            <span>🫒<small>脂肪</small><b>{{ aiSuggestion.fat }} g</b></span>
          </div>
          <button class="ai-btn" @click="$router.push('/chat')">✨ 让 AI 帮我规划</button>
        </div>

        <div class="card overview-card">
          <div class="title-row">
            <h3>📊 今日营养概览</h3>
            <span @click="$router.push('/nutrition')">更多 ›</span>
          </div>
          <div class="overview-body">
            <div class="chart-ring">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" fill="none" stroke="#e8edf2" stroke-width="8"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#77b8ef" stroke-width="8"
                  :stroke-dasharray="proteinDash" stroke-linecap="round" transform="rotate(-90 50 50)"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#80d1ad" stroke-width="8"
                  :stroke-dasharray="carbDash" stroke-linecap="round" transform="rotate(-90 50 50)"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#ffb56b" stroke-width="8"
                  :stroke-dasharray="fatDash" stroke-linecap="round" transform="rotate(-90 50 50)"/>
              </svg>
              <div class="chart-center">
                <b>{{ overview.totalCalories || 0 }}</b>
                <small>kcal<br>总摄入</small>
              </div>
            </div>
            <div class="nut-list">
              <p>🔵 蛋白质 <b>{{ overview.totalProtein || 0 }} g</b> <small>{{ proteinPercent }}%</small></p>
              <p>🟢 碳水化合物 <b>{{ overview.totalCarbohydrate || 0 }} g</b> <small>{{ carbPercent }}%</small></p>
              <p>🟠 脂肪 <b>{{ overview.totalFat || 0 }} g</b> <small>{{ fatPercent }}%</small></p>
            </div>
          </div>
        </div>

        <div class="card recipes-card">
          <div class="title-row">
            <h3>👨‍🍳 推荐食谱</h3>
            <span @click="$router.push('/foods')">查看全部 ›</span>
          </div>
          <div class="recipe-list">
            <div v-for="r in recipes" :key="r.name" class="recipe-item">
              <b>{{ r.icon }}</b>
              <section>
                <strong>{{ r.name }}</strong>
                <small>{{ r.desc }}</small>
                <em>约 {{ r.time }} 分钟</em>
              </section>
            </div>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="addDialogVisible" :title="`添加食物 - ${currentMealName}`" width="480px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="搜索食物">
          <el-input v-model="dialogSearchKeyword" placeholder="输入食物名称搜索" @input="dialogSearchFood" />
        </el-form-item>
        <el-form-item label="选择食物">
          <el-select v-model="addForm.foodId" filterable placeholder="请选择食物" style="width:100%">
            <el-option v-for="f in dialogSearchResults" :key="f.id" :label="`${f.name} (${f.calories}kcal/100g)`" :value="f.id" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const overview = ref({})
const records = ref([])
const addDialogVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const dialogSearchKeyword = ref('')
const dialogSearchResults = ref([])
const allFoods = ref([])
const addForm = reactive({ foodId: null, amount: 100, mealType: 'breakfast' })

const meals = [
  { type: 'breakfast', name: '早餐', icon: '🍳', time: '07:00 - 09:00' },
  { type: 'lunch', name: '午餐', icon: '🥗', time: '11:00 - 13:00' },
  { type: 'dinner', name: '晚餐', icon: '🍗', time: '17:30 - 19:30' },
  { type: 'snack', name: '加餐', icon: '🍪', time: '15:00 - 16:00' }
]

const currentMealName = computed(() => {
  const m = meals.find(m => m.type === addForm.mealType)
  return m ? m.name : ''
})

const now = new Date()
const todayStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`
const weekDays = ['周日','周一','周二','周三','周四','周五','周六']
const weekDay = weekDays[now.getDay()]
const hour = now.getHours()
const greetingIcon = hour < 12 ? '☀️' : hour < 18 ? '🌤️' : '🌙'
const greetingText = hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'

const nutrients = computed(() => [
  { key: 'protein', name: '蛋白质', icon: '🥩', current: overview.value.totalProtein || 0, target: overview.value.targetProtein || 0, barClass: 'bar-protein' },
  { key: 'carbohydrate', name: '碳水化合物', icon: '🌾', current: overview.value.totalCarbohydrate || 0, target: overview.value.targetCarbohydrate || 0, barClass: 'bar-carb' },
  { key: 'fat', name: '脂肪', icon: '🫒', current: overview.value.totalFat || 0, target: overview.value.targetFat || 0, barClass: 'bar-fat' }
].map(n => ({ ...n, percent: n.target > 0 ? Math.min(Math.round((n.current / n.target) * 100), 100) : 0 })))

const caloriePercent = computed(() => {
  const t = overview.value.targetCalories || 1
  return Math.min(((overview.value.totalCalories || 0) / t) * 100, 100)
})
const calorieDash = computed(() => {
  const circumference = 2 * Math.PI * 52
  const filled = circumference * (caloriePercent.value / 100)
  return `${filled} ${circumference}`
})

const proteinPercent = computed(() => {
  const t = overview.value.targetProtein || 1
  return Math.min(Math.round(((overview.value.totalProtein || 0) / t) * 100), 100)
})
const carbPercent = computed(() => {
  const t = overview.value.targetCarbohydrate || 1
  return Math.min(Math.round(((overview.value.totalCarbohydrate || 0) / t) * 100), 100)
})
const fatPercent = computed(() => {
  const t = overview.value.targetFat || 1
  return Math.min(Math.round(((overview.value.totalFat || 0) / t) * 100), 100)
})

const totalNutrientPercent = computed(() => proteinPercent.value + carbPercent.value + fatPercent.value || 1)
const proteinDash = computed(() => {
  const c = 2 * Math.PI * 40
  const p = (proteinPercent.value / totalNutrientPercent.value) * (proteinPercent.value >= 100 ? 1 : proteinPercent.value / 100)
  return `${c * p} ${c}`
})
const carbDash = computed(() => {
  const c = 2 * Math.PI * 40
  const p = (carbPercent.value / totalNutrientPercent.value) * (carbPercent.value >= 100 ? 1 : carbPercent.value / 100)
  return `${c * p} ${c}`
})
const fatDash = computed(() => {
  const c = 2 * Math.PI * 40
  const p = (fatPercent.value / totalNutrientPercent.value) * (fatPercent.value >= 100 ? 1 : fatPercent.value / 100)
  return `${c * p} ${c}`
})

const frequentFoods = ref([
  { name: '鸡蛋', icon: '🥚', num: 0 },
  { name: '牛奶', icon: '🥛', num: 0 },
  { name: '香蕉', icon: '🍌', num: 0 },
  { name: '鸡胸肉', icon: '🍗', num: 0 },
  { name: '西兰花', icon: '🥦', num: 0 }
])

const aiSuggestions = [
  { icon: '🥣', label: '推荐早餐', title: '燕麦牛奶碗 + 水煮蛋 + 蓝莓', desc: '富含优质蛋白和膳食纤维，帮助控制血糖，提供持久能量。', calories: 420, protein: 24, carb: 52, fat: 12 },
  { icon: '🥗', label: '推荐午餐', title: '鸡胸肉沙拉 + 全麦面包', desc: '高蛋白低脂，搭配丰富蔬菜补充维生素和矿物质。', calories: 520, protein: 35, carb: 55, fat: 15 },
  { icon: '🍲', label: '推荐晚餐', title: '清蒸鱼 + 糙米饭 + 西兰花', desc: '优质蛋白加粗粮，营养均衡易消化，适合晚间食用。', calories: 480, protein: 30, carb: 48, fat: 14 }
]
const aiSuggestion = ref(aiSuggestions[0])
let aiIndex = 0
function refreshAiSuggestion() {
  aiIndex = (aiIndex + 1) % aiSuggestions.length
  aiSuggestion.value = aiSuggestions[aiIndex]
}

const recipes = ref([
  { name: '清蒸鸡胸肉', desc: '高蛋白 · 低脂', time: 15, icon: '🍗' },
  { name: '藜麦蔬菜沙拉', desc: '低卡 · 营养均衡', time: 20, icon: '🥗' },
  { name: '番茄鸡蛋面', desc: '简单 · 快手', time: 15, icon: '🍝' },
  { name: '牛油果三明治', desc: '高纤维 · 健康', time: 10, icon: '🥪' }
])

function getMealRecords(mealType) {
  return records.value.filter(r => r.mealType === mealType)
}

function getMealCal(mealType) {
  return getMealRecords(mealType).reduce((sum, r) => sum + (r.calories || 0), 0)
}

function getMealNut(mealType, nut) {
  return getMealRecords(mealType).reduce((sum, r) => sum + (r[nut] || 0), 0)
}

async function loadData() {
  try {
    overview.value = await api.get('/diet/today')
    records.value = await api.get('/diet/today/records')
    computeFrequentFoods()
  } catch (e) {}
  try {
    allFoods.value = await api.get('/foods/all')
    dialogSearchResults.value = allFoods.value
  } catch (e) {}
}

function computeFrequentFoods() {
  const countMap = {}
  records.value.forEach(r => {
    const name = r.foodName
    if (!name) return
    countMap[name] = (countMap[name] || 0) + 1
  })
  const iconMap = { '鸡蛋': '🥚', '牛奶': '🥛', '香蕉': '🍌', '鸡胸肉': '🍗', '西兰花': '🥦', '米饭': '🍚', '面包': '🍞', '苹果': '🍎', '豆腐': '🧈', '鱼': '🐟' }
  const sorted = Object.entries(countMap).sort((a, b) => b[1] - a[1]).slice(0, 5)
  if (sorted.length > 0) {
    frequentFoods.value = sorted.map(([name, num]) => ({ name, icon: iconMap[name] || '🍽️', num }))
  }
}

function openAddDialog(mealType) {
  addForm.mealType = mealType
  addForm.foodId = null
  addForm.amount = 100
  dialogSearchKeyword.value = ''
  dialogSearchResults.value = allFoods.value
  addDialogVisible.value = true
}

async function searchFood() {
  if (searchKeyword.value.length < 1) { searchResults.value = []; return }
  try { searchResults.value = await api.get('/foods/search', { params: { keyword: searchKeyword.value } }) } catch (e) {}
}

async function dialogSearchFood() {
  if (dialogSearchKeyword.value.length < 1) {
    dialogSearchResults.value = allFoods.value
    return
  }
  const kw = dialogSearchKeyword.value.toLowerCase()
  dialogSearchResults.value = allFoods.value.filter(f => f.name.toLowerCase().includes(kw))
}

function selectFood(food) {
  addForm.foodId = food.id
  addForm.mealType = 'breakfast'
  addForm.amount = 100
  addDialogVisible.value = true
}

async function addDietRecord() {
  if (!addForm.foodId) return ElMessage.warning('请选择食物')
  try {
    await api.post('/diet/today/add', addForm)
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('添加失败')
  }
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
.today-page { min-height: 100vh; background: #f4f7fa; color: #244b6b; font-family: "Microsoft YaHei", Arial, sans-serif; padding: 18px; max-width: 1650px; margin: auto; }
.card { background: #fff; border: 1px solid #e5edf2; border-radius: 13px; box-shadow: 0 3px 15px rgba(49,90,114,0.03); }

.welcome { min-height: 120px; border-radius: 15px; padding: 20px 28px; background: linear-gradient(100deg, #edfaff, #f7fff8, #eff9f4); display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.welcome-text { display: flex; gap: 18px; align-items: center; }
.greeting-icon { font-size: 42px; }
h1 { font-size: 26px; line-height: 1.25; margin: 0; color: #153d67; }
.welcome p { margin: 6px 0 0; color: #718b9c; font-size: 13px; }
.welcome-right { display: flex; gap: 12px; align-items: center; }
.date-card, .target-card { background: #fff; border-radius: 10px; padding: 12px 15px; box-shadow: 0 3px 12px rgba(49,90,114,0.03); font-size: 13px; }
.target-card { min-width: 220px; display: flex; align-items: center; gap: 9px; }
.target-card small { color: #8499a8; }
.target-card b { color: #49ae67; font-size: 19px; }
.target-card i { font-style: normal; font-size: 11px; color: #8499a8; }
.target-card button { margin-left: auto; border: 1px solid #d9eafa; background: #f2f8ff; color: #3287dc; border-radius: 16px; padding: 6px 10px; cursor: pointer; }

.stats { display: grid; grid-template-columns: 1.4fr 1fr 1fr 1fr 2fr; gap: 12px; margin-bottom: 14px; }
.calorie-card { display: flex; align-items: center; justify-content: center; padding: 20px; min-height: 180px; }
.circle-wrap { position: relative; width: 135px; height: 135px; }
.ring { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.circle-inner { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; display: flex; flex-direction: column; align-items: center; }
.circle-inner small { font-size: 10px; color: #8a9eac; margin: 2px; }
.circle-inner b { font-size: 19px; }
.circle-inner i { font-style: normal; font-size: 10px; color: #8a9eac; }

.macro-card { padding: 20px; min-height: 180px; }
.macro-card strong { display: block; font-size: 22px; margin-top: 13px; }
.macro-card i { font-style: normal; font-size: 11px; color: #8499a8; }
.macro-card small { color: #91a2ae; font-size: 10px; }
.progress { height: 7px; background: #eaf0f4; border-radius: 10px; margin-top: 17px; }
.progress span { display: block; height: 100%; border-radius: 10px; transition: width 0.3s; }
.bar-protein { background: #77b8ef; }
.bar-carb { background: #80d1ad; }
.bar-fat { background: #ffb56b; }
.macro-card em { display: block; text-align: right; font-size: 9px; color: #8ea1af; font-style: normal; margin-top: 4px; }

.quick-card { padding: 17px; min-height: 180px; }
.quick-card h3 { font-size: 15px; margin: 0 0 10px; }
.search-bar { height: 38px; border: 1px solid #dce7ef; border-radius: 20px; padding: 0 12px; display: flex; align-items: center; }
.search-bar input { border: 0; outline: 0; flex: 1; font-size: 12px; background: transparent; }
.search-results { margin-top: 8px; max-height: 120px; overflow-y: auto; }
.search-item { padding: 6px 10px; cursor: pointer; border-radius: 6px; font-size: 12px; }
.search-item:hover { background: #edf7ff; }
.search-item small { color: #8499a8; margin-left: 8px; }
.quick-btns { display: grid; grid-template-columns: repeat(4, 1fr); gap: 7px; margin-top: 11px; }
.quick-btns button { height: 55px; border: 1px solid #d7e8f7; background: #f7fbff; border-radius: 9px; color: #3589dc; font-size: 19px; cursor: pointer; }
.quick-btns small { display: block; font-size: 9px; }

.grid { display: grid; grid-template-columns: 1.7fr 0.9fr; gap: 14px; }
.left, .right { display: flex; flex-direction: column; gap: 12px; }

.meal-card { min-height: 135px; padding: 14px; display: flex; align-items: flex-start; gap: 17px; }
.meal-card.breakfast { background: #fffaf0; }
.meal-card.lunch { background: #f2fbf6; }
.meal-card.dinner { background: #f1f8ff; }
.meal-card.snack { background: #fef8f4; }
.meal-img { font-size: 48px; flex-shrink: 0; width: 60px; text-align: center; }
.meal-info { flex: 1; }
.meal-title { display: flex; align-items: center; gap: 10px; }
.meal-title h2 { margin: 0; font-size: 19px; }
.meal-title small { font-size: 11px; color: #8297a6; }
.meal-info p { font-size: 11px; color: #8498a7; margin: 10px 0; }
.meal-foods { margin: 8px 0; }
.meal-food-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 12px; border-bottom: 1px solid #f0f2f5; }
.meal-food-item span { flex: 1; }
.meal-food-item small { color: #8499a8; }
.meal-food-item em { color: #e6a23c; font-style: normal; }
.meal-food-item .del { color: #c0c4cc; cursor: pointer; font-style: normal; }
.meal-food-item .del:hover { color: #f56c6c; }
.meal-data { font-size: 10px; color: #71899b; margin-top: 8px; }
.add-btn { border: 0; background: #288df0; color: #fff; border-radius: 18px; padding: 8px 16px; white-space: nowrap; cursor: pointer; flex-shrink: 0; align-self: center; }

.title-row { display: flex; justify-content: space-between; align-items: center; }
.title-row h3 { margin: 0; font-size: 15px; }
.title-row span { font-size: 10px; color: #718da2; cursor: pointer; }

.history-card, .ai-card, .overview-card, .recipes-card { padding: 16px; }
.foods-grid { display: flex; justify-content: space-between; margin-top: 12px; }
.food-item { display: flex; flex-direction: column; align-items: center; gap: 3px; }
.food-item b { width: 55px; height: 55px; background: #f3f6f8; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: normal; }
.food-item strong { font-size: 10px; }
.food-item small { font-size: 8px; color: #99aab5; }

.ai-food { display: flex; gap: 10px; background: #f0faf8; border-radius: 11px; padding: 9px; margin-top: 10px; }
.ai-food-icon { font-size: 48px; display: flex; align-items: center; justify-content: center; width: 80px; flex-shrink: 0; }
.ai-food label { font-size: 8px; background: #67c899; color: #fff; padding: 3px 7px; border-radius: 8px; }
.ai-food h4 { font-size: 13px; margin: 6px 0; }
.ai-food p { font-size: 9px; color: #8195a1; line-height: 1.5; margin: 0; }
.ai-data { display: grid; grid-template-columns: repeat(4, 1fr); margin: 10px 0; }
.ai-data span { text-align: center; font-size: 14px; border-right: 1px solid #edf1f3; }
.ai-data span:last-child { border: 0; }
.ai-data small, .ai-data b { display: block; font-size: 9px; }
.ai-data small { color: #91a0a9; margin: 3px; }
.ai-btn { width: 100%; height: 38px; border: 0; border-radius: 20px; background: #298df0; color: #fff; cursor: pointer; font-size: 14px; }

.overview-body { display: flex; align-items: center; gap: 20px; margin-top: 12px; }
.chart-ring { position: relative; width: 105px; height: 105px; flex-shrink: 0; }
.chart-ring svg { width: 100%; height: 100%; }
.chart-center { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.chart-center b { font-size: 18px; display: block; }
.chart-center small { font-size: 8px; color: #8195a2; text-align: center; }
.nut-list { flex: 1; }
.nut-list p { font-size: 11px; margin: 10px 0; }
.nut-list b { float: right; margin-right: 10px; }
.nut-list small { float: right; color: #8fa0ac; }

.recipe-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 10px; }
.recipe-item { display: flex; gap: 8px; background: #fafcfd; padding: 8px; border-radius: 9px; }
.recipe-item b { font-size: 28px; font-weight: normal; }
.recipe-item strong, .recipe-item small, .recipe-item em { display: block; }
.recipe-item strong { font-size: 10px; }
.recipe-item small, .recipe-item em { font-size: 8px; color: #7e98a6; margin-top: 3px; }
.recipe-item em { color: #a0adb5; font-style: normal; }

@media (max-width: 1200px) {
  .stats { grid-template-columns: repeat(4, 1fr); }
  .quick-card { grid-column: span 4; }
  .grid { grid-template-columns: 1fr; }
}
@media (max-width: 800px) {
  .welcome-right { display: none; }
  .stats { grid-template-columns: 1fr 1fr; }
  .quick-card { grid-column: span 2; }
}
</style>