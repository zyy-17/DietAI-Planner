<template>
  <div class="food-favorites">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>⭐ 我的收藏</span>
          <el-button type="primary" @click="$router.push('/foods')">浏览食物库</el-button>
        </div>
      </template>

      <el-empty v-if="favorites.length === 0" description="暂无收藏食物，去食物库收藏吧～" />

      <el-row v-else :gutter="16">
        <el-col :span="6" v-for="food in favorites" :key="food.id">
          <el-card shadow="hover" class="food-card" @click="showDetail(food)">
            <div class="food-name">{{ food.name }}</div>
            <div class="food-cal">{{ food.calories }} kcal/100g</div>
            <div class="food-macros">
              蛋白{{ food.protein || 0 }}g · 碳水{{ food.carbohydrate || 0 }}g · 脂肪{{ food.fat || 0 }}g
            </div>
            <el-button type="warning" text size="small" @click.stop="removeFavorite(food.id)">取消收藏</el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="detailVisible" :title="currentFood?.name" width="400px">
      <el-descriptions :column="1" border v-if="currentFood">
        <el-descriptions-item label="热量">{{ currentFood.calories }} kcal/100g</el-descriptions-item>
        <el-descriptions-item label="蛋白质">{{ currentFood.protein }} g/100g</el-descriptions-item>
        <el-descriptions-item label="碳水化合物">{{ currentFood.carbohydrate }} g/100g</el-descriptions-item>
        <el-descriptions-item label="脂肪">{{ currentFood.fat }} g/100g</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const favorites = ref([])
const detailVisible = ref(false)
const currentFood = ref(null)

async function loadFavorites() {
  try {
    favorites.value = await api.get('/foods/favorites')
  } catch (e) {
    favorites.value = []
  }
}

function showDetail(food) {
  currentFood.value = food
  detailVisible.value = true
}

async function removeFavorite(foodId) {
  try {
    await api.delete(`/foods/favorites/${foodId}`)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch (e) {}
}

onMounted(loadFavorites)
</script>

<style scoped>
.food-favorites { padding: 18px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.food-card { cursor: pointer; margin-bottom: 12px; text-align: center; }
.food-name { font-size: 15px; font-weight: 600; color: #244b6b; margin-bottom: 6px; }
.food-cal { font-size: 13px; color: #e6a23c; margin-bottom: 4px; }
.food-macros { font-size: 11px; color: #8ea1af; }
</style>