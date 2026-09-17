<template>
  <div class="food-library">
    <el-row :gutter="20">
      <el-col :span="4" v-if="showCategory">
        <el-card class="category-card">
          <div class="cat-item" :class="{ active: !selectedCategory }" @click="selectedCategory = null; loadFoods()">全部分类</div>
          <div v-for="cat in categories" :key="cat.id" class="cat-item" :class="{ active: selectedCategory === cat.id }" @click="selectedCategory = cat.id; loadFoods()">
            {{ cat.name }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="showCategory ? 20 : 24">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-input v-model="keyword" :placeholder="searchPlaceholder" @input="loadFoods" clearable style="width:300px" />
              <el-button type="primary" @click="addFoodDialogVisible = true">添加新食物</el-button>
            </div>
          </template>
          <el-row :gutter="16">
            <el-col :span="6" v-for="food in foods" :key="food.id">
              <el-card shadow="hover" class="food-card" @click="showFoodDetail(food)">
                <div class="food-name">{{ food.name }}</div>
                <div class="food-cal">{{ food.calories }} kcal/100g</div>
                <div class="food-macros">
                  蛋白{{ food.protein || 0 }}g · 碳水{{ food.carbohydrate || 0 }}g · 脂肪{{ food.fat || 0 }}g
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-pagination v-model:current-page="page" :page-size="20" :total="total" @current-change="loadFoods" layout="prev, pager, next" style="margin-top:16px" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="foodDetailVisible" :title="currentFood?.name" width="400px">
      <el-descriptions :column="1" border v-if="currentFood">
        <el-descriptions-item label="热量">{{ currentFood.calories }} kcal/100g</el-descriptions-item>
        <el-descriptions-item label="蛋白质">{{ currentFood.protein }} g/100g</el-descriptions-item>
        <el-descriptions-item label="碳水化合物">{{ currentFood.carbohydrate }} g/100g</el-descriptions-item>
        <el-descriptions-item label="脂肪">{{ currentFood.fat }} g/100g</el-descriptions-item>
        <el-descriptions-item label="膳食纤维">{{ currentFood.fiber }} g/100g</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="addFoodDialogVisible" title="添加新食物" width="500px">
      <el-form :model="newFood" label-width="80px">
        <el-form-item label="名称"><el-input v-model="newFood.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newFood.categoryId" style="width:100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="热量"><el-input-number v-model="newFood.calories" :precision="2" /></el-form-item>
        <el-form-item label="蛋白质"><el-input-number v-model="newFood.protein" :precision="2" /></el-form-item>
        <el-form-item label="碳水"><el-input-number v-model="newFood.carbohydrate" :precision="2" /></el-form-item>
        <el-form-item label="脂肪"><el-input-number v-model="newFood.fat" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addFoodDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addFood">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const route = useRoute()
const mode = computed(() => route.meta.mode || 'default')

const showCategory = computed(() => mode.value !== 'search')
const searchPlaceholder = computed(() => mode.value === 'search' ? '输入食物名称搜索...' : '搜索食物...')

const categories = ref([])
const foods = ref([])
const keyword = ref('')
const selectedCategory = ref(null)
const page = ref(1)
const total = ref(0)
const foodDetailVisible = ref(false)
const addFoodDialogVisible = ref(false)
const currentFood = ref(null)
const newFood = reactive({ name: '', categoryId: null, calories: 0, protein: 0, carbohydrate: 0, fat: 0 })

async function loadCategories() {
  try { categories.value = await api.get('/categories') } catch (e) {}
}

async function loadFoods() {
  try {
    const params = { page: page.value - 1, size: 20 }
    if (keyword.value) params.keyword = keyword.value
    if (selectedCategory.value) params.categoryId = selectedCategory.value
    const data = await api.get('/foods', { params })
    foods.value = data.content || []
    total.value = data.totalElements || 0
  } catch (e) {}
}

function showFoodDetail(food) {
  currentFood.value = food
  foodDetailVisible.value = true
}

async function addFood() {
  await api.post('/foods', newFood)
  ElMessage.success('食物已提交，等待审核')
  addFoodDialogVisible.value = false
  loadFoods()
}

onMounted(() => {
  loadCategories()
  loadFoods()
  if (mode.value === 'add') {
    addFoodDialogVisible.value = true
  }
})
</script>

<style scoped>
.food-library { padding: 18px; }
.category-card { min-height: 400px; }
.cat-item { padding: 8px 12px; cursor: pointer; border-radius: 4px; margin-bottom: 4px; }
.cat-item:hover { background: #f5f7fa; }
.cat-item.active { background: #ecf5ff; color: #409eff; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.food-card { margin-bottom: 16px; cursor: pointer; text-align: center; }
.food-name { font-weight: bold; margin-bottom: 4px; }
.food-cal { color: #e6a23c; font-size: 14px; }
.food-macros { color: #909399; font-size: 12px; margin-top: 4px; }
</style>