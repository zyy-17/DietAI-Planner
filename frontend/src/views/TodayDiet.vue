<template>
  <div class="today-page">
    <section class="welcome">
      <div class="welcome-text">
        <span class="greeting-icon">{{ greetingIcon }}</span>
        <div>
          <h1>{{ greetingText }}，{{ userName }}，<br>今天也要好好吃饭！</h1>
          <p>合理膳食 · 科学营养 · 健康生活</p>
        </div>
      </div>
      <div class="welcome-right">
        <div class="date-card">📅 {{ todayStr }}　{{ weekDay }}</div>
        <div class="target-card">
          🎯 <small>今日目标</small>
          <b>{{ overview.targetCalories || 0 }} <i>kcal</i></b>
          <button @click="openTargetDialog">修改目标</button>
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

    <el-dialog v-model="addDialogVisible" :title="`添加食物 - ${currentMealName}`" width="720px" top="6vh">
      <div class="multi-add-header">
        🔍 搜索并选择多种食物，一次性添加到{{ currentMealName }}
      </div>
      <el-input v-model="dialogSearchKeyword" placeholder="输入食物名称搜索，如：鸡蛋、牛奶、米饭..." @input="dialogSearchFood" clearable style="margin-bottom:12px" />

      <div class="custom-toggle-row">
        <el-button text type="primary" size="small" @click="toggleCustomForm">
          {{ showCustomForm ? '收起自定义食物' : '＋ 找不到？添加自定义食物' }}
        </el-button>
        <span v-if="!showCustomForm" class="custom-tip">自己填写营养数据，仅你本人可见，不会进入公共食物库</span>
      </div>

      <div v-if="showCustomForm" class="custom-food-form">
        <div class="custom-form-title">🙋 自定义食物 <small>仅自己可见</small></div>
        <div class="custom-grid">
          <label class="cf-field">
            <span>食物名称</span>
            <el-input v-model="customFood.foodName" placeholder="如：妈妈牌红烧肉" size="small" />
          </label>
          <label class="cf-field">
            <span>分类（可选）</span>
            <el-select v-model="customFood.categoryId" placeholder="默认归入「其他」" clearable size="small" style="width:100%">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </label>
        </div>
        <div class="custom-form-tip">以下为每 100g 的营养数值，可参考包装上的营养成分表</div>
        <div class="custom-grid custom-grid-4">
          <label class="cf-field">
            <span>热量(kcal)</span>
            <el-input-number v-model="customFood.calories" :min="0" :max="2000" :precision="1" size="small" controls-position="right" style="width:100%" />
          </label>
          <label class="cf-field">
            <span>蛋白质(g)</span>
            <el-input-number v-model="customFood.protein" :min="0" :max="200" :precision="1" size="small" controls-position="right" style="width:100%" />
          </label>
          <label class="cf-field">
            <span>碳水(g)</span>
            <el-input-number v-model="customFood.carbohydrate" :min="0" :max="200" :precision="1" size="small" controls-position="right" style="width:100%" />
          </label>
          <label class="cf-field">
            <span>脂肪(g)</span>
            <el-input-number v-model="customFood.fat" :min="0" :max="200" :precision="1" size="small" controls-position="right" style="width:100%" />
          </label>
        </div>
        <div class="custom-grid">
          <label class="cf-field">
            <span>常用单位（可选）</span>
            <el-select v-model="customFood.unitName" placeholder="如：个 / 份 / 盒" clearable filterable allow-create size="small" style="width:100%">
              <el-option v-for="u in COMMON_UNIT_NAMES" :key="u" :label="u" :value="u" />
            </el-select>
          </label>
          <div class="custom-preview">🔥 本次约摄入 <b>{{ customPreviewCal }}</b> kcal</div>
        </div>
        <div class="custom-amount-row">
          <span class="cf-field-label">本次食用量</span>
          <FoodQuantity :item="customFood" :calories="Number(customFood.calories) || 0" />
        </div>
        <div class="custom-form-tip">设置了常用单位后，以后记录这种食物就能直接按「个数 / 份数」填写，例如 1 个 ≈ 50g</div>
        <div class="custom-form-footer">
          <el-button size="small" @click="showCustomForm = false">取消</el-button>
          <el-button size="small" type="primary" :loading="customSubmitting" @click="submitCustomFood">
            添加并记录到{{ currentMealName }}
          </el-button>
        </div>
      </div>

      <div class="food-select-list" v-if="dialogSearchResults.length">
        <div v-for="f in dialogSearchResults" :key="f.key" class="food-select-item" :class="{ chosen: isChosen(f.key) }" @click="toggleFood(f)">
          <div class="food-select-row">
            <div class="food-select-left">
              <span class="check-box">{{ isChosen(f.key) ? '✅' : '⬜' }}</span>
              <span class="food-select-name">{{ f.name }}</span>
              <el-tag v-if="f.custom" size="small" type="warning" effect="plain">自定义</el-tag>
              <small class="food-select-cal">{{ f.calories }} kcal/100g</small>
              <small v-if="f.unitName && f.unitWeight" class="food-select-unit">1{{ f.unitName }} ≈ {{ f.unitWeight }}g</small>
            </div>
            <small v-if="!isChosen(f.key)" class="food-select-hint">点击选择</small>
          </div>
          <div v-if="isChosen(f.key)" class="food-select-panel" @click.stop>
            <FoodQuantity :item="getChosenItem(f.key)" :calories="Number(f.calories) || 0" />
          </div>
        </div>
      </div>
      <el-empty v-else description="未找到食物，试试其他关键词" :image-size="50" />

      <div v-if="chosenFoods.length" class="chosen-summary">
        <div class="chosen-title">已选择 {{ chosenFoods.length }} 种食物</div>
        <div class="chosen-preview">
          <div v-for="c in chosenFoods" :key="c.key" class="chosen-item">
            <span>{{ c.foodName }}<em v-if="c.foodSource === 'user'" class="custom-flag">自定义</em></span>
            <small>{{ formatPortion(c) }}</small>
            <em>{{ ((c.calories || 0) * c.amount / 100).toFixed(0) }} kcal</em>
            <i @click="removeChosen(c.key)">✕</i>
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

    <el-dialog v-model="showTargetDialog" width="480px" top="10vh">
      <template #header>
        <div class="target-dialog-header">
          <div class="target-dialog-title">🎯 修改今日饮食目标</div>
          <div class="target-dialog-slogan">科学设定目标，让每一餐都更有方向 ✨</div>
        </div>
      </template>

      <div class="target-dialog-body">
        <div class="target-field">
          <div class="target-label">🔥 每日热量目标 <small>kcal</small></div>
          <div class="target-input-row">
            <el-button circle size="small" @click="targetForm.calories = Math.max(800, targetForm.calories - 100)">－</el-button>
            <el-input-number v-model="targetForm.calories" :min="800" :max="5000" :step="50" controls-position="right" style="flex:1" />
            <el-button circle size="small" @click="targetForm.calories = Math.min(5000, targetForm.calories + 100)">＋</el-button>
          </div>
        </div>

        <div class="target-field">
          <div class="target-label">🥩 蛋白质目标 <small>g</small></div>
          <div class="target-input-row">
            <el-button circle size="small" @click="targetForm.protein = Math.max(20, targetForm.protein - 5)">－</el-button>
            <el-input-number v-model="targetForm.protein" :min="20" :max="300" :step="5" controls-position="right" style="flex:1" />
            <el-button circle size="small" @click="targetForm.protein = Math.min(300, targetForm.protein + 5)">＋</el-button>
          </div>
        </div>

        <div class="target-field">
          <div class="target-label">🌾 碳水化合物目标 <small>g</small></div>
          <div class="target-input-row">
            <el-button circle size="small" @click="targetForm.carbohydrate = Math.max(50, targetForm.carbohydrate - 10)">－</el-button>
            <el-input-number v-model="targetForm.carbohydrate" :min="50" :max="500" :step="10" controls-position="right" style="flex:1" />
            <el-button circle size="small" @click="targetForm.carbohydrate = Math.min(500, targetForm.carbohydrate + 10)">＋</el-button>
          </div>
        </div>

        <div class="target-field">
          <div class="target-label">🫒 脂肪目标 <small>g</small></div>
          <div class="target-input-row">
            <el-button circle size="small" @click="targetForm.fat = Math.max(15, targetForm.fat - 5)">－</el-button>
            <el-input-number v-model="targetForm.fat" :min="15" :max="150" :step="5" controls-position="right" style="flex:1" />
            <el-button circle size="small" @click="targetForm.fat = Math.min(150, targetForm.fat + 5)">＋</el-button>
          </div>
        </div>

        <div class="target-tips">
          💡 常见参考：减脂期 1200-1600 kcal · 维持期 1800-2200 kcal · 增肌期 2500-3000 kcal
        </div>
      </div>

      <template #footer>
        <el-button @click="showTargetDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTarget">保存目标</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import FoodQuantity from '../components/FoodQuantity.vue'
import { resolveUnit, formatPortion, COMMON_UNIT_NAMES } from '../utils/foodUnits'

const overview = ref({})
const records = ref([])
const addDialogVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const dialogSearchKeyword = ref('')
const dialogSearchResults = ref([])
const allFoods = ref([])
const addForm = reactive({ foodId: null, amount: 100, mealType: 'breakfast' })
const chosenFoods = ref([])
const showTargetDialog = ref(false)
const targetForm = reactive({ calories: 2000, protein: 65, carbohydrate: 250, fat: 55 })

// 自定义食物（仅本人可见）
const categories = ref([])
const showCustomForm = ref(false)
const customSubmitting = ref(false)
const customFood = reactive({
  foodName: '',
  categoryId: null,
  calories: 0,
  protein: 0,
  carbohydrate: 0,
  fat: 0,
  // 计量：mode=gram 按克数 / unit 按个数（份数）
  mode: 'gram',
  count: 1,
  unitName: '',
  unitWeight: 100,
  amount: 100
})

const customPreviewCal = computed(() => ((customFood.calories || 0) * (customFood.amount || 0) / 100).toFixed(1))

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
const userName = ref('')

async function loadGreeting() {
  try {
    const data = await api.get('/user/greeting')
    userName.value = data.name || ''
  } catch (e) {}
}

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
    const foods = await api.get('/foods/all')
    allFoods.value = (foods || []).map(toPickerItem)
    dialogSearchResults.value = allFoods.value
  } catch (e) {}
  if (categories.value.length === 0) {
    try { categories.value = await api.get('/categories') } catch (e) {}
  }
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
  dialogSearchKeyword.value = ''
  dialogSearchResults.value = allFoods.value
  chosenFoods.value = []
  showCustomForm.value = false
  resetCustomFood()
  addDialogVisible.value = true
}

function toggleCustomForm() {
  showCustomForm.value = !showCustomForm.value
}

function resetCustomFood() {
  customFood.foodName = ''
  customFood.categoryId = null
  customFood.calories = 0
  customFood.protein = 0
  customFood.carbohydrate = 0
  customFood.fat = 0
  customFood.mode = 'gram'
  customFood.count = 1
  customFood.unitName = ''
  customFood.unitWeight = 100
  customFood.amount = 100
}

/** 提交自定义食物：创建仅本人可见的私有食物并直接计入当前餐次 */
async function submitCustomFood() {
  const foodName = (customFood.foodName || '').trim()
  if (!foodName) return ElMessage.warning('请填写食物名称')
  if (customFood.calories === null || customFood.calories === undefined) return ElMessage.warning('请填写每100g热量')
  customSubmitting.value = true
  try {
    await api.post('/diet/today/add-custom', {
      foodName,
      categoryId: customFood.categoryId || null,
      calories: customFood.calories,
      protein: customFood.protein,
      carbohydrate: customFood.carbohydrate,
      fat: customFood.fat,
      amount: customFood.amount,
      unitName: customFood.unitName || null,
      unitWeight: customFood.unitName ? customFood.unitWeight : null,
      mealType: addForm.mealType
    })
    ElMessage.success(`已添加自定义食物「${foodName}」到${currentMealName.value}`)
    showCustomForm.value = false
    resetCustomFood()
    addDialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('添加失败，请重试')
  } finally {
    customSubmitting.value = false
  }
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
  addForm.mealType = 'breakfast'
  dialogSearchKeyword.value = ''
  dialogSearchResults.value = allFoods.value
  chosenFoods.value = []
  showCustomForm.value = false
  resetCustomFood()
  toggleFood(food)
  addDialogVisible.value = true
}

function isChosen(key) {
  return chosenFoods.value.some(c => c.key === key)
}

function getChosenItem(key) {
  return chosenFoods.value.find(c => c.key === key)
}

/** 统一食物选择项：系统食物与自定义食物分属两张表、ID 会重复，用 key 区分；同时补上计量单位 */
function toPickerItem(f) {
  const source = f.foodSource || (f.custom ? 'user' : 'system')
  const { unitName, unitWeight } = resolveUnit(f)
  return { ...f, foodSource: source, unitName, unitWeight, key: f.key || (source === 'user' ? 'u' : 's') + f.id }
}

function toggleFood(food) {
  const item = toPickerItem(food)
  const idx = chosenFoods.value.findIndex(c => c.key === item.key)
  if (idx >= 0) {
    chosenFoods.value.splice(idx, 1)
  } else {
    chosenFoods.value.push({
      key: item.key,
      foodId: item.id,
      foodSource: item.foodSource,
      foodName: item.name,
      mode: 'gram',
      count: 1,
      unitName: item.unitName || '',
      unitWeight: item.unitWeight || 100,
      amount: 100,
      calories: item.calories || 0,
      protein: item.protein || 0,
      carbohydrate: item.carbohydrate || 0,
      fat: item.fat || 0
    })
  }
}

function removeChosen(key) {
  const idx = chosenFoods.value.findIndex(c => c.key === key)
  if (idx >= 0) chosenFoods.value.splice(idx, 1)
}

const chosenTotalCal = computed(() => chosenFoods.value.reduce((s, c) => s + (c.calories || 0) * c.amount / 100, 0).toFixed(0))
const chosenTotalProtein = computed(() => chosenFoods.value.reduce((s, c) => s + (c.protein || 0) * c.amount / 100, 0).toFixed(1))
const chosenTotalCarb = computed(() => chosenFoods.value.reduce((s, c) => s + (c.carbohydrate || 0) * c.amount / 100, 0).toFixed(1))
const chosenTotalFat = computed(() => chosenFoods.value.reduce((s, c) => s + (c.fat || 0) * c.amount / 100, 0).toFixed(1))

async function addDietRecords() {
  if (chosenFoods.value.length === 0) return ElMessage.warning('请至少选择一种食物')
  try {
    const payload = chosenFoods.value.map(c => ({
      foodId: c.foodId,
      foodSource: c.foodSource,
      amount: c.amount,
      mealType: addForm.mealType
    }))
    await api.post('/diet/today/add-batch', payload)
    ElMessage.success(`成功添加 ${chosenFoods.value.length} 种食物`)
    addDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

function openTargetDialog() {
  targetForm.calories = overview.value.targetCalories || 2000
  targetForm.protein = overview.value.targetProtein || 65
  targetForm.carbohydrate = overview.value.targetCarbohydrate || 250
  targetForm.fat = overview.value.targetFat || 55
  showTargetDialog.value = true
}

async function saveTarget() {
  try {
    await api.put('/diet/today/target', targetForm)
    ElMessage.success('目标已更新')
    showTargetDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
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

onMounted(() => {
  loadData()
  loadGreeting()
})
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

.multi-add-header { font-size: 13px; color: #55738d; margin-bottom: 12px; padding: 8px 12px; background: #f0f7ff; border-radius: 8px; }
.custom-toggle-row { display: flex; align-items: center; gap: 10px; margin: -4px 0 10px; }
.custom-tip { font-size: 12px; color: #8ea1af; }
.custom-food-form { border: 1px dashed #a9d5fb; background: #f8fbff; border-radius: 10px; padding: 14px; margin-bottom: 14px; }
.custom-form-title { font-size: 13px; font-weight: 600; color: #2589ee; margin-bottom: 12px; }
.custom-form-title small { font-weight: normal; color: #8ea1af; margin-left: 6px; }
.custom-form-tip { font-size: 12px; color: #8ea1af; margin-bottom: 8px; }
.custom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; margin-bottom: 10px; align-items: end; }
.custom-grid-4 { grid-template-columns: repeat(4, 1fr); }
.cf-field { display: flex; flex-direction: column; gap: 4px; }
.cf-field > span { font-size: 12px; color: #55738d; }
.custom-preview { font-size: 12px; color: #8499a8; padding-bottom: 6px; }
.custom-preview b { color: #e6a23c; font-size: 16px; margin: 0 3px; }
.custom-form-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px; }
.food-select-list { max-height: 320px; overflow-y: auto; border: 1px solid #eef2f6; border-radius: 8px; }
.food-select-item { padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #f5f7fa; transition: background 0.15s; }
.food-select-item:hover { background: #f5f9ff; }
.food-select-item.chosen { background: #eef7ff; cursor: default; }
.food-select-row { display: flex; align-items: center; justify-content: space-between; }
.food-select-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.food-select-hint { font-size: 11px; color: #b3c2cc; white-space: nowrap; }
.check-box { font-size: 16px; }
.food-select-name { font-size: 14px; color: #244b6b; }
.food-select-cal { font-size: 12px; color: #8ea1af; margin-left: 6px; }
.food-select-unit { font-size: 12px; color: #4aa86d; background: #eefaf2; border-radius: 8px; padding: 1px 7px; }
.food-select-panel { margin-top: 8px; }
.custom-amount-row { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 10px; }
.custom-amount-row .cf-field-label { font-size: 12px; color: #55738d; padding-top: 9px; white-space: nowrap; }
.custom-amount-row :deep(.fq) { flex: 1; }
.chosen-summary { margin-top: 14px; border: 1px solid #d9eafa; border-radius: 10px; padding: 12px; background: #f8fbff; }
.chosen-title { font-size: 13px; font-weight: 600; color: #2589ee; margin-bottom: 8px; }
.chosen-preview { max-height: 140px; overflow-y: auto; }
.chosen-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; border-bottom: 1px solid #eef2f6; }
.chosen-item span { flex: 1; color: #244b6b; }
.chosen-item small { color: #8499a8; }
.chosen-item em { color: #e6a23c; font-style: normal; }
.chosen-item em.custom-flag { color: #d48806; background: #fff5e6; border-radius: 4px; padding: 0 4px; font-size: 11px; margin-left: 6px; }
.chosen-item i { color: #c0c4cc; cursor: pointer; font-style: normal; }
.chosen-item i:hover { color: #f56c6c; }
.chosen-total { margin-top: 8px; font-size: 12px; color: #55738d; padding-top: 8px; border-top: 1px solid #eef2f6; }
.chosen-total b { color: #244b6b; }

.target-dialog-header { text-align: center; }
.target-dialog-title { font-size: 18px; font-weight: 700; color: #153d67; margin-bottom: 6px; }
.target-dialog-slogan { font-size: 13px; color: #55738d; }
.target-dialog-body { padding: 8px 0; }
.target-field { margin-bottom: 18px; }
.target-label { font-size: 14px; color: #244b6b; margin-bottom: 8px; font-weight: 600; }
.target-label small { color: #8ea1af; font-weight: normal; }
.target-input-row { display: flex; align-items: center; gap: 8px; }
.target-tips { font-size: 12px; color: #8ea1af; padding: 10px 12px; background: #f8fbff; border-radius: 8px; margin-top: 4px; line-height: 1.6; }
</style>