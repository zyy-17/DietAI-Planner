<template>
  <div class="ai-suggest">
    <div class="suggest-header">
      <h1>🤖 AI 今日建议</h1>
      <p>基于您的身体数据和饮食目标，AI为您定制今日膳食方案</p>
      <el-button type="primary" @click="refreshSuggestion" :loading="loading">⟳ 换一换</el-button>
    </div>

    <div class="suggest-cards">
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
        <el-button type="primary" text @click="applySuggestion(s)">应用到今日饮食 ›</el-button>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const allSuggestions = [
  [
    { type: 'breakfast', icon: '🥣', label: '推荐早餐', title: '燕麦牛奶碗 + 水煮蛋 + 蓝莓', desc: '富含优质蛋白和膳食纤维，帮助控制血糖，提供持久能量。', calories: 420, protein: 24, carb: 52, fat: 12 },
    { type: 'lunch', icon: '🥗', label: '推荐午餐', title: '鸡胸肉沙拉 + 全麦面包 + 酸奶', desc: '高蛋白低脂，搭配丰富蔬菜补充维生素和矿物质。', calories: 520, protein: 35, carb: 55, fat: 15 },
    { type: 'dinner', icon: '🍲', label: '推荐晚餐', title: '清蒸鱼 + 糙米饭 + 西兰花', desc: '优质蛋白加粗粮，营养均衡易消化，适合晚间食用。', calories: 480, protein: 30, carb: 48, fat: 14 }
  ],
  [
    { type: 'breakfast', icon: '🥞', label: '推荐早餐', title: '全麦吐司 + 牛油果 + 煎蛋', desc: '健康脂肪与优质蛋白组合，提供上午所需能量。', calories: 380, protein: 18, carb: 35, fat: 18 },
    { type: 'lunch', icon: '🍱', label: '推荐午餐', title: '牛肉西兰花 + 紫薯 + 豆腐汤', desc: '补铁增肌，粗粮替代精米，膳食纤维丰富。', calories: 550, protein: 32, carb: 60, fat: 16 },
    { type: 'dinner', icon: '🥘', label: '推荐晚餐', title: '番茄鸡蛋面 + 凉拌黄瓜', desc: '清淡易消化，番茄红素抗氧化，适合晚间。', calories: 400, protein: 15, carb: 55, fat: 10 }
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

function applySuggestion(s) {
  ElMessage.success(`已将「${s.title}」方案记录，快去添加食物吧！`)
  router.push(`/today/${s.type}`)
}
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
@media (max-width: 900px) { .suggest-cards { grid-template-columns: 1fr; } }
</style>