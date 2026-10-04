<template>
  <el-dialog v-model="visible" :width="'900px'" :show-close="false" class="recipe-sheet"
    :close-on-click-modal="false" top="3vh" destroy-on-close>
    <div class="sheet" v-loading="loading">
      <!-- 顶部：封面 + 标题 + 来源 -->
      <div class="hero" :style="heroStyle">
        <div class="hero-mask"></div>
        <div class="hero-body">
          <div class="hero-title">
            <span class="hero-icon">👨‍🍳</span>
            <span class="hero-name">{{ plan?.name || '食谱' }}</span>
          </div>
          <div class="hero-meta">
            <template v-if="mode !== 'mine'">
              <span>🔥 {{ plan?.usageCount || 0 }}+ 人使用中</span>
              <span class="dot">·</span>
              <span>由{{ plan?.authorName || '匿名用户' }}创建并投稿</span>
              <span class="dot" v-if="mode === 'admin'">·</span>
              <span v-if="mode === 'admin'">{{ publishText }}</span>
            </template>
            <template v-else>
              <span>{{ plan?.statusLabel || '未执行' }}</span>
              <span class="dot">·</span>
              <span>{{ publishText }}</span>
            </template>
          </div>
        </div>
        <button class="hero-close" @click="close">✕</button>
      </div>

      <div class="sheet-body">
        <!-- 四个数据块 -->
        <div class="stats">
          <div class="stat">
            <div class="stat-value">{{ plan?.days || '-' }}</div>
            <div class="stat-label">计划天数</div>
          </div>
          <div class="stat">
            <div class="stat-value">{{ plan?.goalLabel || '-' }}</div>
            <div class="stat-label">目标</div>
          </div>
          <div class="stat">
            <div class="stat-value">{{ plan?.difficulty || '-' }}</div>
            <div class="stat-label">难度</div>
          </div>
          <div class="stat">
            <div class="stat-value">{{ plan?.expectedLoss || '-' }}</div>
            <div class="stat-label">减脂 Kg</div>
          </div>
        </div>

        <!-- 标签 -->
        <div class="tags" v-if="tags.length">
          <span v-for="(tag, i) in tags" :key="tag" class="tag" :class="'tag-' + (i % 5)">{{ tag }}</span>
        </div>

        <!-- 心得 -->
        <div class="desc" v-if="plan?.description">
          <span class="desc-icon">❤️</span>
          <span class="desc-text" :class="{ folded: !descExpanded }">{{ plan.description }}</span>
          <a class="desc-toggle" @click="descExpanded = !descExpanded">
            {{ descExpanded ? '收起' : '展开' }}
          </a>
        </div>

        <!-- 日期页签 -->
        <div class="day-tabs">
          <span v-for="day in dayList" :key="day.dayIndex" class="day-tab"
            :class="{ active: day.dayIndex === activeDay }" @click="activeDay = day.dayIndex">
            第{{ day.dayIndex }}天
          </span>
        </div>

        <!-- 当日营养条 -->
        <div class="macro" v-if="activeDetail">
          <div class="bar">
            <div class="seg carb" :style="{ width: macro.carbPct + '%' }"></div>
            <div class="seg protein" :style="{ width: macro.proteinPct + '%' }"></div>
            <div class="seg fat" :style="{ width: macro.fatPct + '%' }"></div>
          </div>
          <div class="macro-labels">
            <span class="carb">碳水 {{ fmt(activeDetail.carbohydrate) }}g {{ macro.carbPct }}%</span>
            <span class="protein">蛋白质 {{ fmt(activeDetail.protein) }}g {{ macro.proteinPct }}%</span>
            <span class="fat">脂肪 {{ fmt(activeDetail.fat) }}g {{ macro.fatPct }}%</span>
          </div>
        </div>

        <!-- 每一餐 -->
        <div class="meals">
          <div class="meal" v-for="group in activeDetail?.meals || []" :key="group.mealType">
            <div class="meal-head">
              <span class="meal-icon">{{ mealIcon(group.mealType) }}</span>
              <span class="meal-name">{{ group.mealLabel }}</span>
              <span class="meal-kcal">{{ fmt(group.calories) }} 千卡</span>
              <span class="meal-add" v-if="editable" @click="openAdd(group.mealType)">＋</span>
            </div>
            <div class="food-list" v-if="group.items.length">
              <div class="food" v-for="item in group.items" :key="item.id">
                <img class="food-img" :src="item.imageUrl || foodFallback" alt="" />
                <div class="food-main">
                  <div class="food-name">{{ item.foodName }}</div>
                  <div class="food-sub">
                    <span class="amount-chip">🥄 {{ item.amountText }}</span>
                    <span class="food-kcal">+{{ fmt(item.calories) }} 千卡</span>
                  </div>
                </div>
                <div class="food-actions" v-if="editable">
                  <button class="act act-swap" @click="openReplace(item)">替换</button>
                  <button class="act act-remove" @click="removeItem(item)">移除</button>
                </div>
              </div>
            </div>
            <div class="food-empty" v-else>这一餐还没安排，点右上角「＋」添加</div>
          </div>
        </div>

        <!-- 工具行 -->
        <div class="tools" v-if="editable">
          <button class="tool" @click="copyDialog = true">复制到其他日期</button>
          <button class="tool" @click="changeDaysDialog = true">修改计划天数</button>
        </div>

        <div class="tip" v-if="plan?.rejectReason">审核未通过：{{ plan.rejectReason }}</div>
      </div>

      <!-- 底部固定条 -->
      <div class="sheet-footer">
        <div class="total">{{ fmt(activeDetail?.calories) }}<small>千卡/天{{ activeDay ? '（第' + activeDay + '天）' : '' }}</small></div>
        <div class="footer-actions">
          <template v-if="mode === 'square'">
            <button class="btn-ghost" @click="toggleFavorite">
              {{ plan?.favorited ? '★ 已收藏' : '☆ 收藏' }}
            </button>
            <button class="btn-primary" @click="$emit('copy')">保存为我的食谱</button>
          </template>
          <template v-else-if="mode === 'admin'">
            <button class="btn-ghost" @click="close">关闭</button>
          </template>
          <template v-else>
            <button class="btn-ghost" @click="close">完成</button>
            <button class="btn-primary" @click="applyPlan" v-if="plan?.status !== 'active'">开始执行这份食谱</button>
          </template>
        </div>
      </div>
    </div>

    <!-- 添加食物 -->
    <el-dialog v-model="addDialog" title="添加食物" width="460px" append-to-body>
      <el-form label-width="72px">
        <el-form-item label="食物">
          <el-select v-model="addForm.foodKey" filterable placeholder="搜索食物库" style="width: 100%">
            <el-option v-for="f in foodOptions" :key="f.key" :label="f.name" :value="f.key">
              <span>{{ f.name }}</span>
              <span style="float: right; color: #9aa7b4; font-size: 12px">{{ f.calories }} kcal/100g</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="克数">
          <el-input-number v-model="addForm.amount" :min="1" :max="2000" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdd">添加</el-button>
      </template>
    </el-dialog>

    <!-- 替换 -->
    <el-dialog v-model="replaceDialog" title="换一种食物" width="560px" append-to-body>
      <div class="swap-head">
        <span>把「{{ replaceTarget?.foodName }} {{ replaceTarget?.amountText }}」换成：</span>
        <el-button size="small" :loading="aiLoading" @click="loadAlternatives">
          ✨ AI 智能推荐
        </el-button>
      </div>
      <div class="swap-tip" v-if="aiNote">{{ aiNote }}</div>
      <div class="swap-list" v-loading="swapLoading">
        <div class="swap-item" v-for="c in candidates" :key="c.foodSource + ':' + c.foodId">
          <img :src="c.imageUrl || foodFallback" alt="" />
          <div class="swap-main">
            <div class="swap-name">{{ c.foodName }}</div>
            <div class="swap-sub">
              <span>{{ c.amountText }}</span>
              <span>{{ fmt(c.calories) }} 千卡</span>
            </div>
            <div class="swap-reason" v-if="c.reason">{{ c.reason }}</div>
          </div>
          <el-button type="primary" size="small" :loading="saving" @click="submitReplace(c)">选它</el-button>
        </div>
        <div class="swap-empty" v-if="!candidates.length && !swapLoading">没有找到合适的替代食物</div>
      </div>
    </el-dialog>

    <!-- 复制到其他日期 -->
    <el-dialog v-model="copyDialog" title="复制到其他日期" width="420px" append-to-body>
      <p class="dlg-tip">把第 {{ activeDay }} 天的安排复制到：</p>
      <el-checkbox-group v-model="copyForm.days">
        <el-checkbox v-for="d in copyCandidates" :key="d" :value="d">第 {{ d }} 天</el-checkbox>
      </el-checkbox-group>
      <el-checkbox v-model="copyForm.overwrite" style="margin-top: 12px">覆盖目标日期已有的安排</el-checkbox>
      <template #footer>
        <el-button @click="copyDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCopy">确定复制</el-button>
      </template>
    </el-dialog>

    <!-- 修改计划天数 -->
    <el-dialog v-model="changeDaysDialog" title="修改计划天数" width="380px" append-to-body>
      <el-radio-group v-model="newDays">
        <el-radio-button v-for="d in [3, 7, 14]" :key="d" :value="d">{{ d }}天</el-radio-button>
      </el-radio-group>
      <p class="dlg-tip" style="margin-top: 12px">
        缩短天数不会删除已有安排，改回来还能看到；超出的部分暂时不计入统计。
      </p>
      <template #footer>
        <el-button @click="changeDaysDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitDays">保存</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  planId: { type: [Number, String], default: null },
  /** mine = 我自己的食谱（可编辑）；square = 广场上的（只读 + 收藏/保存） */
  mode: { type: String, default: 'mine' }
})
const emit = defineEmits(['update:modelValue', 'changed', 'copy', 'applied'])

const foodFallback = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56"><rect width="56" height="56" rx="10" fill="%23f0f4f2"/><text x="28" y="34" font-size="20" text-anchor="middle">🍽️</text></svg>'

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const saving = ref(false)
const plan = ref(null)
const planDays = ref([])
const activeDay = ref(1)
const descExpanded = ref(false)
const foodOptions = ref([])

const addDialog = ref(false)
const addForm = reactive({ mealType: 'breakfast', foodKey: '', amount: 100 })
const replaceDialog = ref(false)
const replaceTarget = ref(null)
const candidates = ref([])
const aiLoading = ref(false)
const aiNote = ref('')
const swapLoading = ref(false)
const copyDialog = ref(false)
const copyForm = reactive({ days: [], overwrite: false })
const changeDaysDialog = ref(false)
const newDays = ref(7)

const editable = computed(() => props.mode === 'mine' && plan.value && plan.value.status !== 'archived')
const tags = computed(() => plan.value?.tags || [])
const dayList = computed(() => planDays.value.length
  ? planDays.value
  : Array.from({ length: plan.value?.days || 0 }, (_, i) => ({ dayIndex: i + 1, meals: [] })))
const activeDetail = computed(() => planDays.value.find(d => d.dayIndex === activeDay.value) || null)
const copyCandidates = computed(() => {
  const total = plan.value?.days || 0
  return Array.from({ length: total }, (_, i) => i + 1).filter(d => d !== activeDay.value)
})
const heroStyle = computed(() => plan.value?.coverUrl
  ? { backgroundImage: `url(${plan.value.coverUrl})` }
  : { background: 'linear-gradient(120deg, #7fc79a, #55b47c)' })
const publishText = computed(() => {
  const map = { none: '未发布', pending: '待审核', approved: '已上架广场', rejected: '审核未通过' }
  return map[plan.value?.publishStatus] || '未发布'
})
const macro = computed(() => {
  const d = activeDetail.value
  if (!d) return { carbPct: 0, proteinPct: 0, fatPct: 0 }
  const c = (Number(d.carbohydrate) || 0) * 4
  const p = (Number(d.protein) || 0) * 4
  const f = (Number(d.fat) || 0) * 9
  const total = c + p + f
  if (!total) return { carbPct: 0, proteinPct: 0, fatPct: 0 }
  const r = (v) => Math.round(v / total * 100)
  let carb = r(c), protein = r(p), fat = r(f)
  if (carb + protein + fat !== 100) carb += 100 - (carb + protein + fat)
  return { carbPct: carb, proteinPct: protein, fatPct: fat }
})

watch(() => [props.modelValue, props.planId], ([open]) => {
  if (open && props.planId) load()
})

function fmt(v) {
  const n = Number(v)
  if (!n || Number.isNaN(n)) return '0'
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

function mealIcon(type) {
  return { breakfast: '🌅', lunch: '☀️', dinner: '🌙', snack: '🍎' }[type] || '🍽️'
}

async function load() {
  loading.value = true
  try {
    const url = props.mode === 'square'
      ? `/meal-plan/square/${props.planId}`
      : (props.mode === 'admin' ? `/admin/meal-plans/${props.planId}` : `/meal-plan/${props.planId}`)
    const data = await api.get(url)
    plan.value = data.plan
    planDays.value = data.planDays || []
    newDays.value = data.plan?.days || 7
    activeDay.value = data.plan?.currentDayIndex || 1
    if (!planDays.value.some(d => d.dayIndex === activeDay.value)) activeDay.value = 1
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function reload(keepDay = true) {
  const day = activeDay.value
  await load()
  if (keepDay) activeDay.value = day
  emit('changed')
}

function close() {
  visible.value = false
}

async function loadFoodOptions() {
  if (foodOptions.value.length) return
  try {
    const list = await api.get('/foods/all')
    foodOptions.value = (list || []).map(f => ({
      key: `${f.custom ? 'user' : 'system'}:${f.id}`,
      id: f.id,
      source: f.custom ? 'user' : 'system',
      name: f.name,
      calories: f.calories
    }))
  } catch (e) {
    foodOptions.value = []
  }
}

function openAdd(mealType) {
  loadFoodOptions()
  addForm.mealType = mealType
  addForm.foodKey = ''
  addForm.amount = 100
  addDialog.value = true
}

async function submitAdd() {
  if (!addForm.foodKey) {
    ElMessage.warning('请先选食物')
    return
  }
  const [source, id] = addForm.foodKey.split(':')
  saving.value = true
  try {
    await api.post(`/meal-plan/${props.planId}/items`, {
      dayIndex: activeDay.value,
      mealType: addForm.mealType,
      foodId: Number(id),
      foodSource: source,
      amount: addForm.amount
    })
    addDialog.value = false
    ElMessage.success('已添加')
    await reload()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function removeItem(item) {
  try {
    await ElMessageBox.confirm(`确定移除「${item.foodName}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await api.delete(`/meal-plan/${props.planId}/item/${item.id}`)
    ElMessage.success('已移除')
    await reload()
  } catch (e) {
    // 拦截器已提示
  }
}

async function openReplace(item) {
  replaceTarget.value = item
  candidates.value = []
  aiNote.value = ''
  replaceDialog.value = true
  swapLoading.value = true
  try {
    const list = await api.get(`/meal-plan/${props.planId}/item/${item.id}/candidates`)
    candidates.value = list || []
  } catch (e) {
    candidates.value = []
  } finally {
    swapLoading.value = false
  }
}

async function loadAlternatives() {
  aiLoading.value = true
  aiNote.value = ''
  try {
    const res = await api.get(`/meal-plan/${props.planId}/item/${replaceTarget.value.id}/alternatives`)
    candidates.value = res.candidates || []
    aiNote.value = res.aiUsed ? '以下由 AI 按你的目标与忌口排序' : (res.note || '')
  } catch (e) {
    // 拦截器已提示
  } finally {
    aiLoading.value = false
  }
}

async function submitReplace(candidate) {
  saving.value = true
  try {
    await api.put(`/meal-plan/${props.planId}/item/${replaceTarget.value.id}`, {
      foodId: candidate.foodId,
      foodSource: candidate.foodSource,
      amount: candidate.amount
    })
    replaceDialog.value = false
    ElMessage.success('已替换')
    await reload()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function submitCopy() {
  if (!copyForm.days.length) {
    ElMessage.warning('请选择要复制到哪几天')
    return
  }
  saving.value = true
  try {
    await api.post(`/meal-plan/${props.planId}/copy-day`, {
      fromDayIndex: activeDay.value,
      toDayIndexes: copyForm.days,
      overwrite: copyForm.overwrite
    })
    copyDialog.value = false
    copyForm.days = []
    copyForm.overwrite = false
    ElMessage.success('已复制')
    await reload()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function submitDays() {
  saving.value = true
  try {
    await api.put(`/meal-plan/${props.planId}`, { days: newDays.value })
    changeDaysDialog.value = false
    ElMessage.success('已更新')
    await reload(false)
    if (activeDay.value > newDays.value) activeDay.value = 1
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function toggleFavorite() {
  const on = plan.value?.favorited
  try {
    await api({
      url: `/meal-plan/square/${props.planId}/favorite`,
      method: on ? 'delete' : 'post'
    })
    plan.value.favorited = !on
    plan.value.favoriteCount = Math.max(0, (plan.value.favoriteCount || 0) + (on ? -1 : 1))
    ElMessage.success(on ? '已取消收藏' : '已收藏')
  } catch (e) {
    // 拦截器已提示
  }
}

async function applyPlan() {
  try {
    await api.post(`/meal-plan/${props.planId}/apply`)
    ElMessage.success('已开始执行')
    emit('applied')
    close()
  } catch (e) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.sheet { border-radius: 14px; overflow: hidden; background: #fff; }
.hero { position: relative; height: 168px; background-size: cover; background-position: center; }
.hero-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.05), rgba(0,0,0,.45)); }
.hero-body { position: absolute; left: 22px; bottom: 16px; right: 56px; color: #fff; }
.hero-title { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 700; }
.hero-meta { margin-top: 6px; font-size: 12px; opacity: .92; display: flex; gap: 6px; align-items: center; }
.hero-close { position: absolute; right: 14px; top: 12px; width: 30px; height: 30px; border-radius: 50%;
  border: none; background: rgba(255,255,255,.85); cursor: pointer; font-size: 14px; color: #445; }

.sheet-body { padding: 18px 22px 12px; max-height: 62vh; overflow-y: auto; }

.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat { background: #f5f8f6; border-radius: 12px; padding: 14px 8px; text-align: center; }
.stat-value { font-size: 20px; font-weight: 700; color: #3fae74; line-height: 1.2; }
.stat-label { margin-top: 6px; font-size: 12px; color: #8b98a5; }

.tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.tag { padding: 5px 12px; border-radius: 6px; font-size: 12px; color: #fff; }
.tag-0 { background: #4caf7d; } .tag-1 { background: #b3c96a; }
.tag-2 { background: #6fa8dc; } .tag-3 { background: #f08baf; } .tag-4 { background: #9aa7b4; }

.desc { margin-top: 14px; font-size: 13px; color: #5b6b7a; line-height: 1.7; }
.desc-icon { margin-right: 4px; }
.desc-text.folded { display: -webkit-inline-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; vertical-align: bottom; }
.desc-toggle { color: #3fae74; margin-left: 6px; cursor: pointer; white-space: nowrap; }

.day-tabs { display: flex; gap: 4px; margin: 16px 0 14px; border: 1px solid #e6efe9;
  border-radius: 10px; padding: 6px; overflow-x: auto; }
.day-tab { flex: none; padding: 8px 14px; border-radius: 8px; font-size: 13px; color: #7b8a97; cursor: pointer; }
.day-tab.active { background: #4caf7d; color: #fff; font-weight: 600; }

.macro { margin-bottom: 14px; }
.bar { display: flex; height: 8px; border-radius: 4px; overflow: hidden; background: #eef3f0; }
.seg.carb { background: #5b9bd5; } .seg.protein { background: #4caf7d; } .seg.fat { background: #f0a04b; }
.macro-labels { display: flex; justify-content: space-between; margin-top: 8px; font-size: 12px; }
.macro-labels .carb { color: #5b9bd5; } .macro-labels .protein { color: #4caf7d; }
.macro-labels .fat { color: #f0a04b; }

.meal { margin-bottom: 18px; }
.meal-head { display: flex; align-items: center; gap: 8px; padding: 6px 2px; }
.meal-name { font-size: 15px; font-weight: 700; color: #3fae74; }
.meal-kcal { margin-left: auto; font-size: 13px; color: #8b98a5; }
.meal-add { color: #3fae74; font-size: 20px; cursor: pointer; line-height: 1; margin-left: 10px; }

.food-list { background: #fff; }
.food { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-bottom: 1px solid #f2f6f3; }
.food:last-child { border-bottom: none; }
.food-img { width: 48px; height: 48px; border-radius: 10px; object-fit: cover; background: #f3f6f4; flex: none; }
.food-main { min-width: 0; flex: 1; }
.food-name { font-size: 15px; color: #33414f; font-weight: 600; }
.food-sub { display: flex; align-items: center; gap: 10px; margin-top: 5px; }
.amount-chip { background: #4caf7d; color: #fff; border-radius: 6px; padding: 2px 8px; font-size: 12px; }
.food-kcal { color: #4caf7d; font-size: 13px; }
.food-actions { display: flex; gap: 8px; flex: none; }
.act { border: none; border-radius: 8px; padding: 7px 12px; font-size: 12px; cursor: pointer; }
.act-swap { background: #e6f6ec; color: #3fae74; }
.act-remove { background: #fdeceb; color: #e06055; }
.food-empty { padding: 12px; color: #a9b6c1; font-size: 12px; background: #fafcfb; border-radius: 8px; }

.tools { display: flex; gap: 12px; justify-content: center; margin: 18px 0 6px; }
.tool { border: none; background: #eef7f1; color: #3fae74; border-radius: 16px; padding: 8px 18px;
  font-size: 13px; cursor: pointer; }
.tip { margin-top: 10px; font-size: 12px; color: #e06055; }

.sheet-footer { display: flex; align-items: center; gap: 14px; padding: 12px 22px 18px;
  border-top: 1px solid #f0f4f1; background: #fff; }
.total { font-size: 22px; font-weight: 700; color: #3fae74; }
.total small { font-size: 12px; color: #9aa7b4; font-weight: 400; margin-left: 6px; }
.footer-actions { margin-left: auto; display: flex; gap: 10px; }
.btn-ghost { border: 1px solid #dfe8e3; background: #fff; border-radius: 22px; padding: 10px 20px;
  font-size: 14px; cursor: pointer; color: #5b6b7a; }
.btn-primary { border: none; background: #4caf7d; color: #fff; border-radius: 22px; padding: 10px 26px;
  font-size: 14px; cursor: pointer; font-weight: 600; }

.swap-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.swap-tip { font-size: 12px; color: #8b98a5; margin-bottom: 8px; }
.swap-list { max-height: 46vh; overflow-y: auto; }
.swap-item { display: flex; align-items: center; gap: 12px; padding: 10px 4px; border-bottom: 1px solid #f2f6f3; }
.swap-item img { width: 44px; height: 44px; border-radius: 10px; object-fit: cover; background: #f3f6f4; }
.swap-main { flex: 1; min-width: 0; }
.swap-name { font-size: 14px; font-weight: 600; color: #33414f; }
.swap-sub { display: flex; gap: 10px; font-size: 12px; color: #8b98a5; margin-top: 4px; }
.swap-reason { font-size: 12px; color: #3fae74; margin-top: 4px; }
.swap-empty { text-align: center; color: #a9b6c1; font-size: 13px; padding: 20px 0; }
.dlg-tip { font-size: 13px; color: #5b6b7a; margin: 0 0 10px; }

html.dark .sheet { background: #1b2129; }
html.dark .stat { background: #232b34; }
html.dark .stat-label, html.dark .meal-kcal, html.dark .food-kcal { color: #8ea0b0; }
html.dark .food, html.dark .swap-item { border-bottom-color: #2a333d; }
html.dark .food-name, html.dark .swap-name { color: #d8e2ea; }
html.dark .desc { color: #9aabba; }
html.dark .day-tabs { border-color: #2a333d; }
html.dark .sheet-footer { background: #1b2129; border-top-color: #2a333d; }
html.dark .btn-ghost { background: #232b34; border-color: #333e49; color: #c8d6e5; }
html.dark .tool { background: #24322b; }
html.dark .food-empty { background: #202730; }
</style>

<style>
.recipe-sheet .el-dialog__header { display: none; }
.recipe-sheet .el-dialog__body { padding: 0; }
.recipe-sheet .el-dialog { border-radius: 14px; overflow: hidden; }
</style>
