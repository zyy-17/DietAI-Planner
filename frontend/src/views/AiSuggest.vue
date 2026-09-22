<template>
  <div class="ai-suggest">
    <div class="suggest-header">
      <h1>🤖 AI 今日建议</h1>
      <p>基于您的身体数据和饮食目标，AI为您定制今日膳食方案</p>
      <el-button type="primary" @click="refreshSuggestion" :loading="loading">⟳ 换一换</el-button>
    </div>

    <!-- 优化二：算法推荐食物卡片 -->
    <el-card v-if="recommendation" class="rec-card" shadow="hover">
      <template #header>
        <div class="rec-header">
          <span>🎯 智能推荐食物（算法评分 Top5）</span>
          <el-tag type="info" size="small">剩余 {{ recommendation.remainingCalories }} kcal · 蛋白质缺口 {{ recommendation.proteinGap }}g</el-tag>
        </div>
      </template>

      <div class="rec-foods">
        <div v-for="food in recommendation.recommendations" :key="food.foodId" class="rec-food-item">
          <div class="food-rank" :class="getRankClass(food.score)">{{ food.foodName }}</div>
          <div class="food-score">
            <el-progress :percentage="food.score" :stroke-width="8" :color="getScoreColor(food.score)" :format="() => food.score + '分'" :show-text="true" />
          </div>
          <div class="food-nutrients">
            <span>🔥 {{ food.calories }} kcal</span>
            <span>🥩 {{ food.protein }}g</span>
          </div>
        </div>
      </div>

      <div class="rec-context" v-if="recommendation.candidateFoods && recommendation.candidateFoods.length">
        <el-divider content-position="left">算法筛选过程</el-divider>
        <p class="rec-desc">
          系统根据您的<strong>饮食目标（{{ goalLabel }}）</strong>、
          <strong>营养缺口</strong>和<strong>用户偏好</strong>，
          对数据库中候选食物进行多因素评分排序：
        </p>
        <div class="rec-weights">
          <el-tag size="small" type="primary">热量匹配 40%</el-tag>
          <el-tag size="small" type="success">蛋白质匹配 30%</el-tag>
          <el-tag size="small" type="warning">用户偏好 15%</el-tag>
          <el-tag size="small" type="info">饮食目标 15%</el-tag>
        </div>
        <p class="rec-candidates">候选食物：{{ recommendation.candidateFoods.join('、') }}</p>
      </div>
    </el-card>

    <!-- 优化三：结构化AI膳食规划 -->
    <el-card v-if="structuredPlan" class="plan-card" shadow="hover" style="margin-top:16px">
      <template #header>
        <div class="plan-header">
          <span>📋 AI 结构化膳食方案</span>
          <el-button size="small" @click="loadStructuredPlan" :loading="planLoading">重新生成</el-button>
        </div>
      </template>

      <el-alert v-if="structuredPlan.summary" :title="structuredPlan.summary" type="info" :closable="false" show-icon style="margin-bottom:12px" />

      <div v-if="structuredPlan.nutritionAnalysis" class="plan-nutrition">
        <el-descriptions title="营养缺口分析" :column="3" size="small" border>
          <el-descriptions-item label="剩余热量">{{ structuredPlan.nutritionAnalysis.caloriesRemaining }} kcal</el-descriptions-item>
          <el-descriptions-item label="剩余蛋白质">{{ structuredPlan.nutritionAnalysis.proteinRemaining }}g</el-descriptions-item>
          <el-descriptions-item label="剩余脂肪">{{ structuredPlan.nutritionAnalysis.fatRemaining }}g</el-descriptions-item>
        </el-descriptions>
      </div>

      <div v-if="structuredPlan.mealPlan" class="plan-meals">
        <el-divider content-position="left">膳食方案</el-divider>
        <el-row :gutter="16">
          <el-col :span="8" v-if="structuredPlan.mealPlan.breakfast && structuredPlan.mealPlan.breakfast.length">
            <div class="meal-section">
              <h4>🥣 早餐</h4>
              <div v-for="item in structuredPlan.mealPlan.breakfast" :key="item.food" class="meal-item">
                <span class="meal-food">{{ item.food }}</span>
                <span class="meal-amount">{{ item.amount }}g</span>
                <span class="meal-cal">{{ item.calories }}kcal</span>
              </div>
            </div>
          </el-col>
          <el-col :span="8" v-if="structuredPlan.mealPlan.lunch && structuredPlan.mealPlan.lunch.length">
            <div class="meal-section">
              <h4>🥗 午餐</h4>
              <div v-for="item in structuredPlan.mealPlan.lunch" :key="item.food" class="meal-item">
                <span class="meal-food">{{ item.food }}</span>
                <span class="meal-amount">{{ item.amount }}g</span>
                <span class="meal-cal">{{ item.calories }}kcal</span>
              </div>
            </div>
          </el-col>
          <el-col :span="8" v-if="structuredPlan.mealPlan.dinner && structuredPlan.mealPlan.dinner.length">
            <div class="meal-section">
              <h4>🍲 晚餐</h4>
              <div v-for="item in structuredPlan.mealPlan.dinner" :key="item.food" class="meal-item">
                <span class="meal-food">{{ item.food }}</span>
                <span class="meal-amount">{{ item.amount }}g</span>
                <span class="meal-cal">{{ item.calories }}kcal</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <div v-if="structuredPlan.suggestions && structuredPlan.suggestions.length" class="plan-suggestions">
        <el-divider content-position="left">💡 建议</el-divider>
        <ul>
          <li v-for="s in structuredPlan.suggestions" :key="s">{{ s }}</li>
        </ul>
      </div>
    </el-card>

    <!-- 原有静态建议卡片 -->
    <div class="suggest-cards" style="margin-top:16px">
      <div v-for="s in suggestions" :key="s.label" class="suggest-card" :class="s.type">
        <div class="card-icon">{{ s.icon }}</div>
        <div class="card-body">
          <label>{{ s.label }}</label>
          <h3>{{ s.title }}</h3>
          <p>{{ s.desc }}</p>
          <div class="card-nutrients">
            <span>🔥 {{ s.calories }} kcal</span>
            <span>🥩 {{ s.protein }} g</span>
            <span>🌾 {{ s.carb }} g</span>
            <span>🫒 {{ s.fat }} g</span>
          </div>
        </div>
        <el-button type="primary" text @click="applySuggestion(s)" :loading="applying">应用到今日饮食 ›</el-button>
      </div>
    </div>

    <el-card class="tips-card">
      <template #header><span>💡 健康小贴士</span></template>
      <ul>
        <li>每餐间隔4-6小时，避免暴饮暴食</li>
        <li>晚餐建议在19:00前完成，避免睡前3小时进食</li>
        <li>每日饮水量建议1500-2000ml</li>
        <li>细嚼慢咽，每口食物咀嚼20次以上</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const planLoading = ref(false)
const applying = ref(false)
const recommendation = ref(null)
const structuredPlan = ref(null)

const goalLabel = computed(() => {
  if (!recommendation.value) return ''
  const goal = recommendation.value.dietGoal
  if (goal === 'lose') return '减脂'
  if (goal === 'gain') return '增肌'
  return '维持'
})

function getRankClass(score) {
  if (score >= 90) return 'rank-gold'
  if (score >= 80) return 'rank-silver'
  return 'rank-bronze'
}

function getScoreColor(score) {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#409eff'
  if (score >= 70) return '#e6a23c'
  return '#f56c6c'
}

async function loadRecommendation() {
  try {
    recommendation.value = await api.get('/recommendation/foods', { params: { topN: 5 } })
  } catch (e) {
    console.warn('加载推荐食物失败', e)
  }
}

async function loadStructuredPlan() {
  if (!recommendation.value) return
  planLoading.value = true
  try {
    const userId = JSON.parse(localStorage.getItem('user') || '{}').id || 1
    const res = await axios.post('/ai-api/diet-plan/structured', {
      user_id: userId,
      remaining_calories: recommendation.value.remainingCalories || 0,
      target_calories: 2000,
      diet_goal: recommendation.value.dietGoal || 'maintain',
      candidate_foods: recommendation.value.candidateFoods || []
    }, { timeout: 120000 })
    structuredPlan.value = res.data
  } catch (e) {
    console.warn('加载结构化膳食方案失败', e)
    ElMessage.warning('AI服务暂不可用，请确保Python服务已启动')
  } finally {
    planLoading.value = false
  }
}

const allSuggestions = [
  [
    { type: 'breakfast', icon: '🥣', label: '推荐早餐', title: '燕麦牛奶碗 + 水煮蛋 + 蓝莓', desc: '富含优质蛋白和膳食纤维，帮助控制血糖，提供持久能量。', calories: 420, protein: 24, carb: 52, fat: 12, foods: [{ foodName: '燕麦片', amount: 40 }, { foodName: '牛奶', amount: 250 }, { foodName: '鸡蛋', amount: 100 }, { foodName: '蓝莓', amount: 50 }] },
    { type: 'lunch', icon: '🥗', label: '推荐午餐', title: '鸡胸肉沙拉 + 全麦面包 + 酸奶', desc: '高蛋白低脂，搭配丰富蔬菜补充维生素和矿物质。', calories: 520, protein: 35, carb: 55, fat: 15, foods: [{ foodName: '鸡胸肉', amount: 150 }, { foodName: '全麦面包', amount: 80 }, { foodName: '酸奶', amount: 200 }, { foodName: '番茄', amount: 100 }] },
    { type: 'dinner', icon: '🍲', label: '推荐晚餐', title: '清蒸鱼 + 糙米饭 + 西兰花', desc: '优质蛋白加粗粮，营养均衡易消化，适合晚间食用。', calories: 480, protein: 30, carb: 48, fat: 14, foods: [{ foodName: '清蒸鱼', amount: 150 }, { foodName: '糙米饭', amount: 200 }, { foodName: '西兰花', amount: 100 }] }
  ],
  [
    { type: 'breakfast', icon: '🥞', label: '推荐早餐', title: '全麦吐司 + 牛油果 + 煎蛋', desc: '健康脂肪与优质蛋白组合，提供上午所需能量。', calories: 380, protein: 18, carb: 35, fat: 18, foods: [{ foodName: '全麦面包', amount: 80 }, { foodName: '鸡蛋', amount: 100 }, { foodName: '牛奶', amount: 250 }] },
    { type: 'lunch', icon: '🍱', label: '推荐午餐', title: '牛肉西兰花 + 紫薯 + 豆腐汤', desc: '补铁增肌，粗粮替代精米，膳食纤维丰富。', calories: 550, protein: 32, carb: 60, fat: 16, foods: [{ foodName: '牛肉', amount: 150 }, { foodName: '西兰花', amount: 100 }, { foodName: '红薯', amount: 200 }, { foodName: '豆腐', amount: 100 }] },
    { type: 'dinner', icon: '🥘', label: '推荐晚餐', title: '番茄鸡蛋面 + 凉拌黄瓜', desc: '清淡易消化，番茄红素抗氧化，适合晚间。', calories: 400, protein: 15, carb: 55, fat: 10, foods: [{ foodName: '面条(煮)', amount: 200 }, { foodName: '鸡蛋', amount: 100 }, { foodName: '番茄', amount: 100 }, { foodName: '黄瓜', amount: 100 }] }
  ],
  [
    { type: 'breakfast', icon: '🥛', label: '推荐早餐', title: '豆浆 + 杂粮馒头 + 水煮蛋', desc: '植物蛋白与粗粮搭配，低脂高纤维，稳定血糖。', calories: 360, protein: 20, carb: 42, fat: 8, foods: [{ foodName: '豆浆', amount: 300 }, { foodName: '鸡蛋', amount: 100 }] },
    { type: 'lunch', icon: '🍛', label: '推荐午餐', title: '虾仁炒饭 + 海带汤 + 橙子', desc: '海鲜优质蛋白，海带补碘，橙子补充维C。', calories: 580, protein: 28, carb: 65, fat: 18, foods: [{ foodName: '虾', amount: 150 }, { foodName: '白米饭', amount: 200 }, { foodName: '橙子', amount: 200 }] },
    { type: 'dinner', icon: '🥬', label: '推荐晚餐', title: '白灼虾 + 蒸南瓜 + 小米粥', desc: '低脂高蛋白，南瓜富含β胡萝卜素，小米养胃。', calories: 350, protein: 22, carb: 40, fat: 6, foods: [{ foodName: '虾', amount: 150 }, { foodName: '红薯', amount: 150 }] }
  ],
  [
    { type: 'breakfast', icon: '🫐', label: '推荐早餐', title: '希腊酸奶 + 坚果麦片 + 香蕉', desc: '高蛋白酸奶搭配坚果，提供持久饱腹感和优质脂肪。', calories: 410, protein: 22, carb: 48, fat: 14, foods: [{ foodName: '酸奶', amount: 200 }, { foodName: '香蕉', amount: 100 }] },
    { type: 'lunch', icon: '🥙', label: '推荐午餐', title: '三文鱼饭团 + 味噌汤 + 毛豆', desc: 'Omega-3丰富，味噌发酵食品益肠道，毛豆补植物蛋白。', calories: 530, protein: 30, carb: 58, fat: 16, foods: [{ foodName: '三文鱼', amount: 150 }, { foodName: '白米饭', amount: 200 }] },
    { type: 'dinner', icon: '🍜', label: '推荐晚餐', title: '鸡丝凉面 + 蒜蓉菠菜 + 蘑菇汤', desc: '清淡爽口，菠菜补铁，蘑菇增强免疫力。', calories: 420, protein: 20, carb: 52, fat: 12, foods: [{ foodName: '鸡胸肉', amount: 100 }, { foodName: '面条(煮)', amount: 200 }, { foodName: '菠菜', amount: 100 }] }
  ]
]

let currentIndex = 0
const suggestions = ref(allSuggestions[0])

function refreshSuggestion() {
  loading.value = true
  setTimeout(() => {
    currentIndex = (currentIndex + 1) % allSuggestions.length
    suggestions.value = allSuggestions[currentIndex]
    loading.value = false
  }, 500)
}

async function applySuggestion(s) {
  if (!s.foods || s.foods.length === 0) {
    ElMessage.warning('该方案暂无可应用的食物数据')
    router.push(`/today/${s.type}`)
    return
  }
  applying.value = true
  try {
    const res = await api.post('/diet/today/apply-suggestion', {
      mealType: s.type,
      foodItems: s.foods
    })
    const count = res.length || 0
    ElMessage.success(`已将「${s.title}」方案应用到${s.type === 'breakfast' ? '早餐' : s.type === 'lunch' ? '午餐' : s.type === 'dinner' ? '晚餐' : '加餐'}，共${count}种食物已记录`)
    router.push('/today')
  } catch (e) {
    ElMessage.error('应用失败，请手动添加食物')
    router.push(`/today/${s.type}`)
  } finally {
    applying.value = false
  }
}

onMounted(async () => {
  await loadRecommendation()
  if (recommendation.value) {
    loadStructuredPlan()
  }
})
</script>

<style scoped>
.ai-suggest { padding: 18px; }
.suggest-header { background: linear-gradient(100deg, #f0faf8, #edfaff, #f7fff8); border-radius: 15px; padding: 24px; margin-bottom: 16px; }
.suggest-header h1 { margin: 0 0 8px; font-size: 22px; color: #153d67; }
.suggest-header p { margin: 0 0 16px; color: #718b9c; font-size: 13px; }
.suggest-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 16px; }
.suggest-card { background: #fff; border: 1px solid #e5edf2; border-radius: 13px; padding: 18px; box-shadow: 0 3px 15px rgba(49,90,114,0.03); }
.suggest-card.breakfast { border-top: 3px solid #ffb56b; }
.suggest-card.lunch { border-top: 3px solid #80d1ad; }
.suggest-card.dinner { border-top: 3px solid #77b8ef; }
.card-icon { font-size: 36px; margin-bottom: 10px; }
.card-body label { font-size: 10px; background: #67c899; color: #fff; padding: 3px 8px; border-radius: 8px; display: inline-block; }
.card-body h3 { font-size: 15px; margin: 8px 0 6px; color: #153d67; }
.card-body p { font-size: 12px; color: #8195a1; line-height: 1.6; margin: 0 0 10px; }
.card-nutrients { display: flex; gap: 12px; font-size: 11px; color: #55738d; }
.tips-card ul { padding-left: 18px; line-height: 2; color: #606266; font-size: 13px; }

.rec-card { border-left: 4px solid #67c23a; margin-bottom: 16px; }
.rec-header { display: flex; justify-content: space-between; align-items: center; }
.rec-foods { display: flex; flex-direction: column; gap: 12px; }
.rec-food-item { display: flex; align-items: center; gap: 16px; }
.food-rank { min-width: 80px; font-size: 15px; font-weight: bold; padding: 4px 12px; border-radius: 8px; text-align: center; }
.rank-gold { background: linear-gradient(135deg, #fff7e6, #ffe7ba); color: #d48806; }
.rank-silver { background: linear-gradient(135deg, #e6f7ff, #bae7ff); color: #096dd9; }
.rank-bronze { background: linear-gradient(135deg, #f6ffed, #d9f7be); color: #389e0d; }
.food-score { flex: 1; }
.food-nutrients { display: flex; gap: 12px; font-size: 12px; color: #8c8c8c; min-width: 140px; justify-content: flex-end; }
.rec-context { margin-top: 8px; }
.rec-desc { font-size: 13px; color: #606266; line-height: 1.8; }
.rec-weights { display: flex; gap: 8px; margin: 8px 0; flex-wrap: wrap; }
.rec-candidates { font-size: 12px; color: #909399; margin-top: 8px; }

.plan-card { border-left: 4px solid #409eff; }
.plan-header { display: flex; justify-content: space-between; align-items: center; }
.plan-nutrition { margin-bottom: 12px; }
.plan-meals { margin-top: 8px; }
.meal-section { background: #fafafa; border-radius: 8px; padding: 12px; }
.meal-section h4 { margin: 0 0 8px; font-size: 14px; color: #303133; }
.meal-item { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; border-bottom: 1px dashed #eee; }
.meal-item:last-child { border-bottom: none; }
.meal-food { color: #303133; flex: 1; }
.meal-amount { color: #909399; margin: 0 8px; }
.meal-cal { color: #67c23a; font-weight: 500; }
.plan-suggestions ul { padding-left: 18px; line-height: 2; color: #606266; font-size: 13px; }

@media (max-width: 900px) { .suggest-cards { grid-template-columns: 1fr; } }
</style>