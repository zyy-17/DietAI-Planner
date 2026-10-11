<template>
  <div class="food-library">
    <!-- 顶部三栏 Tab：基础食材 / 成品菜肴 / 自定义食物 -->
    <div class="page-tabs">
      <div class="tab-item" :class="{ active: currentTab === 'ingredient' }" @click="switchTab('ingredient')">
        🥦 基础食材
      </div>
      <div class="tab-item" :class="{ active: currentTab === 'dish' }" @click="switchTab('dish')">
        🍱 成品菜肴
      </div>
      <div class="tab-item" :class="{ active: currentTab === 'custom' }" @click="switchTab('custom')">
        🍳 自定义食物
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧分类：仅「基础食材」Tab 显示，且只列食材分类（排除成品菜肴） -->
      <el-col :span="4" v-if="currentTab === 'ingredient'">
        <el-card class="category-card">
          <div class="cat-item" :class="{ active: !selectedCategory }" @click="selectedCategory = null; loadFoods()">全部分类</div>
          <div v-for="cat in ingredientCategories" :key="cat.id" class="cat-item" :class="{ active: selectedCategory === cat.id }" @click="selectedCategory = cat.id; loadFoods()">
            {{ cat.name }}
          </div>
        </el-card>
      </el-col>

      <el-col :span="currentTab === 'ingredient' ? 20 : 24">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-input v-model="keyword" :placeholder="searchPlaceholder" @input="handleSearch" clearable style="width:260px" />
                <el-button v-if="currentTab === 'custom'" type="primary" size="small" @click="openAddMyFood">＋ 添加我的食物</el-button>
              </div>
              <span class="header-tip">{{ headerTip }}</span>
            </div>
          </template>

          <!-- 自定义食物 Tab：展示我的自定义食物 -->
          <template v-if="currentTab === 'custom'">
            <div class="section-title">
              <span>我的自定义食物</span>
              <small>仅本人可见，不对外共享</small>
            </div>
            <el-row :gutter="16">
              <el-col :span="6" v-for="food in filteredMyFoods" :key="'my-' + food.id">
                <el-card shadow="hover" class="food-card my-food-card" @click="openMyFoodDetail(food)">
                  <div class="food-name">
                    {{ food.name }}
                    <el-tag size="small" type="success" effect="light" style="margin-left:4px">我的</el-tag>
                    <el-tag v-if="food.recipe" size="small" type="warning" effect="plain" style="margin-left:4px">组合食谱</el-tag>
                  </div>
                  <div class="food-cal">{{ food.calories }} kcal/100g</div>
                  <div class="food-macros">
                    蛋白{{ food.protein || 0 }}g · 碳水{{ food.carbohydrate || 0 }}g · 脂肪{{ food.fat || 0 }}g
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <el-empty v-if="!filteredMyFoods.length" description="还没有自定义食物，点击右上角「＋ 添加我的食物」创建" />
          </template>

          <!-- 基础食材 / 成品菜肴 Tab：展示平台公共食物 -->
          <template v-else>
            <div class="section-title">
              <span>{{ currentTab === 'dish' ? '平台成品菜肴' : '平台基础食材' }}</span>
              <small>{{ currentTab === 'dish' ? '行业平均估算值，仅供参考' : '平台公共食物，全员共享' }}</small>
            </div>
            <el-row :gutter="16">
              <el-col :span="6" v-for="food in foods" :key="food.id">
                <el-card shadow="hover" class="food-card" @click="showFoodDetail(food)">
                  <div class="food-name">
                    {{ food.name }}
                    <el-tag v-if="food.foodType === 'dish'" size="small" type="warning" effect="plain" style="margin-left:4px">成品菜</el-tag>
                  </div>
                  <div class="food-cal">{{ food.calories }} kcal/100g</div>
                  <div class="food-macros">
                    蛋白{{ food.protein || 0 }}g · 碳水{{ food.carbohydrate || 0 }}g · 脂肪{{ food.fat || 0 }}g
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <el-empty v-if="!foods.length" description="暂无数据" />
            <el-pagination v-if="total > 20" v-model:current-page="page" :page-size="20" :total="total" @current-change="loadFoods" layout="prev, pager, next" style="margin-top:16px" />
          </template>
        </el-card>
      </el-col>
    </el-row>

    <!-- 公共食物详情弹窗 -->
    <el-dialog v-model="foodDetailVisible" :title="currentFood?.name" width="400px">
      <el-descriptions :column="1" border v-if="currentFood">
        <el-descriptions-item label="热量">{{ currentFood.calories }} kcal/100g</el-descriptions-item>
        <el-descriptions-item label="蛋白质">{{ currentFood.protein }} g/100g</el-descriptions-item>
        <el-descriptions-item label="碳水化合物">{{ currentFood.carbohydrate }} g/100g</el-descriptions-item>
        <el-descriptions-item label="脂肪">{{ currentFood.fat }} g/100g</el-descriptions-item>
        <el-descriptions-item label="膳食纤维">{{ currentFood.fiber }} g/100g</el-descriptions-item>
        <el-descriptions-item v-if="currentFood.unitName" label="常用单位">1{{ currentFood.unitName }} ≈ {{ currentFood.unitWeight }}g</el-descriptions-item>
      </el-descriptions>
      <el-alert v-if="currentFood?.foodType === 'dish'" type="warning" :closable="false" show-icon
        title="成品菜肴为行业平均估算值，仅供参考" style="margin-top:12px" />
      <div style="margin-top:16px">
        <el-row :gutter="8" align="middle">
          <el-col :span="8"><span style="line-height:32px">食用量(g)：</span></el-col>
          <el-col :span="16"><el-input-number v-model="addAmount" :min="1" :max="2000" :step="50" style="width:100%" /></el-col>
        </el-row>
      </div>
      <template #footer>
        <el-button @click="foodDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="addToDiet">添加到今日饮食</el-button>
      </template>
    </el-dialog>

    <!-- 我的自定义食物：详情/编辑/删除弹窗 -->
    <el-dialog v-model="myFoodDialogVisible" :title="myFoodForm.id ? '编辑我的食物' : '添加我的食物'" width="460px">
      <el-alert type="info" :closable="false" show-icon title="仅本人可见，不对外共享" style="margin-bottom:12px" />
      <div v-if="myFoodForm.recipe" style="margin-bottom:12px">
        <el-alert type="warning" :closable="false" show-icon title="这是组合食谱，营养由食材自动累加，不可手动修改" />
      </div>
      <div class="form-grid">
        <label class="f-field">
          <span>食物名称</span>
          <el-input v-model="myFoodForm.name" placeholder="如：妈妈牌红烧肉" />
        </label>
        <label class="f-field">
          <span>分类（可选）</span>
          <el-select v-model="myFoodForm.categoryId" placeholder="默认归入「其他」" clearable style="width:100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </label>
        <template v-if="!myFoodForm.recipe">
          <label class="f-field">
            <span>热量(kcal/100g)</span>
            <el-input-number v-model="myFoodForm.calories" :min="0" :max="2000" :precision="1" controls-position="right" style="width:100%" />
          </label>
          <label class="f-field">
            <span>蛋白质(g/100g)</span>
            <el-input-number v-model="myFoodForm.protein" :min="0" :max="200" :precision="1" controls-position="right" style="width:100%" />
          </label>
          <label class="f-field">
            <span>碳水(g/100g)</span>
            <el-input-number v-model="myFoodForm.carbohydrate" :min="0" :max="200" :precision="1" controls-position="right" style="width:100%" />
          </label>
          <label class="f-field">
            <span>脂肪(g/100g)</span>
            <el-input-number v-model="myFoodForm.fat" :min="0" :max="200" :precision="1" controls-position="right" style="width:100%" />
          </label>
        </template>
      </div>
      <template #footer>
        <el-button v-if="myFoodForm.id" type="danger" plain @click="deleteMyFood">删除</el-button>
        <el-button @click="myFoodDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="myFoodSubmitting" @click="saveMyFood">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const currentTab = ref('ingredient')
const categories = ref([])
const foods = ref([])
const myFoods = ref([])
const keyword = ref('')
const selectedCategory = ref(null)
const page = ref(1)
const total = ref(0)
const foodDetailVisible = ref(false)
const currentFood = ref(null)
const addAmount = ref(100)

// 我的自定义食物弹窗
const myFoodDialogVisible = ref(false)
const myFoodSubmitting = ref(false)
const myFoodForm = ref({
  id: null,
  name: '',
  categoryId: null,
  calories: 0,
  protein: 0,
  carbohydrate: 0,
  fat: 0,
  recipe: false
})

// 食材分类（排除「成品菜肴」）
const ingredientCategories = computed(() => categories.value.filter(c => c.name !== '成品菜肴'))

const searchPlaceholder = computed(() =>
  currentTab.value === 'custom' ? '搜索我的食物...' : '搜索食物...'
)
const headerTip = computed(() => {
  if (currentTab.value === 'custom') return '💡 自定义食物仅本人可见，不对外共享'
  if (currentTab.value === 'dish') return '💡 成品菜肴为行业平均估算值，仅供参考'
  return '💡 平台基础食材，营养数据精准可靠'
})

// 自定义食物按关键词前端过滤
const filteredMyFoods = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return myFoods.value
  return myFoods.value.filter(f => (f.name || '').includes(kw))
})

function switchTab(tab) {
  currentTab.value = tab
  selectedCategory.value = null
  keyword.value = ''
  page.value = 1
  if (tab === 'custom') {
    loadMyFoods()
  } else {
    loadFoods()
  }
}

function handleSearch() {
  if (currentTab.value === 'custom') {
    // 自定义食物走前端过滤，无需请求
    return
  }
  page.value = 1
  loadFoods()
}

async function loadCategories() {
  try { categories.value = await api.get('/categories') } catch (e) {}
}

async function loadMyFoods() {
  try { myFoods.value = await api.get('/foods/my') || [] } catch (e) { myFoods.value = [] }
}

async function loadFoods() {
  try {
    const params = { page: page.value - 1, size: 20 }
    if (keyword.value) params.keyword = keyword.value
    else {
      // 成品菜肴 Tab 下无分类筛选；基础食材 Tab 下可按食材分类筛
      if (currentTab.value === 'dish') {
        params.foodType = 'dish'
      } else {
        params.foodType = 'ingredient'
        if (selectedCategory.value) params.categoryId = selectedCategory.value
      }
    }
    const data = await api.get('/foods', { params })
    foods.value = data.content || []
    total.value = data.totalElements || 0
  } catch (e) {}
}

function showFoodDetail(food) {
  currentFood.value = food
  if (food.foodType === 'dish' && food.unitWeight) {
    addAmount.value = food.unitWeight
  } else {
    addAmount.value = 100
  }
  foodDetailVisible.value = true
}

async function addToDiet() {
  if (!currentFood.value) return
  try {
    await api.post('/diet/today/add', {
      foodId: currentFood.value.id,
      amount: addAmount.value,
      mealType: 'snack'
    })
    ElMessage.success(`已添加 ${addAmount.value}g ${currentFood.value.name} 到今日饮食`)
    foodDetailVisible.value = false
  } catch (e) {
    ElMessage.error('添加失败，请重试')
  }
}

// ── 我的自定义食物：新增/编辑/删除 ─────────────────────────────

function openAddMyFood() {
  myFoodForm.value = { id: null, name: '', categoryId: null, calories: 0, protein: 0, carbohydrate: 0, fat: 0, recipe: false }
  myFoodDialogVisible.value = true
}

function openMyFoodDetail(food) {
  myFoodForm.value = {
    id: food.id,
    name: food.name,
    categoryId: food.categoryId || null,
    calories: food.calories || 0,
    protein: food.protein || 0,
    carbohydrate: food.carbohydrate || 0,
    fat: food.fat || 0,
    recipe: !!food.recipe
  }
  myFoodDialogVisible.value = true
}

async function saveMyFood() {
  const name = (myFoodForm.value.name || '').trim()
  if (!name) return ElMessage.warning('请填写食物名称')
  if (!myFoodForm.value.recipe && (myFoodForm.value.calories === null || myFoodForm.value.calories === undefined)) {
    return ElMessage.warning('请填写热量')
  }
  myFoodSubmitting.value = true
  try {
    if (myFoodForm.value.id) {
      // 编辑
      await api.put(`/foods/my/${myFoodForm.value.id}`, {
        name,
        categoryId: myFoodForm.value.categoryId || null,
        calories: myFoodForm.value.calories,
        protein: myFoodForm.value.protein,
        carbohydrate: myFoodForm.value.carbohydrate,
        fat: myFoodForm.value.fat
      })
      ElMessage.success('已更新')
    } else {
      // 新增：纯创建自定义食物，不记录饮食
      await api.post('/foods/my', {
        name,
        categoryId: myFoodForm.value.categoryId || null,
        calories: myFoodForm.value.calories,
        protein: myFoodForm.value.protein,
        carbohydrate: myFoodForm.value.carbohydrate,
        fat: myFoodForm.value.fat
      })
      ElMessage.success('已添加我的食物')
    }
    myFoodDialogVisible.value = false
    await loadMyFoods()
  } catch (e) {
    ElMessage.error('保存失败，请重试')
  } finally {
    myFoodSubmitting.value = false
  }
}

async function deleteMyFood() {
  try {
    await ElMessageBox.confirm(`确定删除「${myFoodForm.value.name}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await api.delete(`/foods/my/${myFoodForm.value.id}`)
    ElMessage.success('已删除')
    myFoodDialogVisible.value = false
    await loadMyFoods()
  } catch (e) {
    ElMessage.error('删除失败，请重试')
  }
}

onMounted(() => {
  loadCategories()
  loadFoods()
  loadMyFoods()
})
</script>

<style scoped>
.food-library { padding: 18px; }
.page-tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 2px solid #eef2f6; padding-bottom: 0; }
.tab-item { padding: 8px 16px; font-size: 13px; color: #55738d; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; white-space: nowrap; }
.tab-item:hover { color: #2789ed; }
.tab-item.active { color: #2589ee; border-bottom-color: #2589ee; font-weight: 600; }
.category-card { min-height: 400px; }
.cat-item { padding: 8px 12px; cursor: pointer; border-radius: 4px; margin-bottom: 4px; }
.cat-item:hover { background: #f5f7fa; }
.cat-item.active { background: #ecf5ff; color: #409eff; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
.header-left { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.header-tip { font-size: 12px; color: #8499a8; text-align: right; line-height: 1.5; max-width: 420px; }
.section-title { display: flex; align-items: baseline; gap: 10px; margin: 4px 0 14px; }
.section-title > span { font-size: 15px; font-weight: 600; color: #244b6b; }
.section-title > small { font-size: 12px; color: #8ea1af; }
.my-food-section { margin-bottom: 4px; }
.my-food-card { background: #f6fbf7; border-color: #d9f0df; }
.food-card { margin-bottom: 16px; cursor: pointer; text-align: center; }
.food-name { font-weight: bold; margin-bottom: 4px; }
.food-cal { color: #e6a23c; font-size: 14px; }
.food-macros { color: #909399; font-size: 12px; margin-top: 4px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 14px; }
.f-field { display: flex; flex-direction: column; gap: 4px; }
.f-field > span { font-size: 12px; color: #55738d; }
</style>
