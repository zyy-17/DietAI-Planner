<template>
  <div class="admin-meal-plans">
    <div class="head">
      <h2>食谱审核</h2>
      <p class="sub">用户发布到食谱广场的食谱需要审核通过才会上架</p>
    </div>

    <el-radio-group v-model="status" size="small" @change="load(0)">
      <el-radio-button value="pending">待审核</el-radio-button>
      <el-radio-button value="approved">已上架</el-radio-button>
      <el-radio-button value="rejected">未通过</el-radio-button>
      <el-radio-button value="none">未发布</el-radio-button>
    </el-radio-group>

    <el-table :data="list" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column prop="name" label="食谱名" min-width="200" show-overflow-tags />
      <el-table-column prop="authorName" label="作者" width="110" />
      <el-table-column label="周期" width="90">
        <template #default="{ row }">{{ row.days }}天 · {{ row.goalLabel }}</template>
      </el-table-column>
      <el-table-column label="内容" width="120">
        <template #default="{ row }">{{ row.itemCount }} 条食物 / {{ row.plannedDailyCalories }} kcal</template>
      </el-table-column>
      <el-table-column prop="publishStatusLabel" label="状态" width="90" />
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ (row.updatedAt || row.createdAt || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="view(row)">查看</el-button>
          <el-button size="small" type="success" v-if="row.publishStatus === 'pending'"
            @click="approve(row)">通过</el-button>
          <el-button size="small" type="danger" v-if="row.publishStatus === 'pending'"
            @click="reject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total > size" style="margin-top: 16px; justify-content: flex-end"
      layout="prev, pager, next, total" :total="total" :page-size="size"
      :current-page="page + 1" @current-change="onPage" />

    <RecipeSheet v-model="sheetVisible" :plan-id="sheetPlanId" mode="admin" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../utils/api'
import RecipeSheet from '../../components/RecipeSheet.vue'

const list = ref([])
const loading = ref(false)
const status = ref('pending')
const page = ref(0)
const size = ref(20)
const total = ref(0)

const sheetVisible = ref(false)
const sheetPlanId = ref(null)

async function load(toPage) {
  if (typeof toPage === 'number') page.value = toPage
  loading.value = true
  try {
    const res = await api.get('/admin/meal-plans', {
      params: { status: status.value, page: page.value, size: size.value }
    })
    list.value = res?.content || []
    total.value = res?.totalElements || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  load(p - 1)
}

function view(row) {
  sheetPlanId.value = row.id
  sheetVisible.value = true
}

async function approve(row) {
  try {
    await api.post(`/admin/meal-plans/${row.id}/approve`)
    ElMessage.success('已通过，食谱已上架')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

async function reject(row) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt('填写驳回原因（会展示给作者）', '驳回食谱', {
      inputPlaceholder: '如：内容与标题不符 / 缺少份量',
      inputValidator: (v) => (v && v.trim() ? true : '请填写原因')
    })
    reason = res.value
  } catch (e) {
    return
  }
  try {
    await api.post(`/admin/meal-plans/${row.id}/reject`, { reason })
    ElMessage.success('已驳回')
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(() => load(0))
</script>

<style scoped>
.head h2 { margin: 0; font-size: 18px; color: #2f4152; }
.head .sub { margin: 4px 0 14px; font-size: 12px; color: #8b98a5; }
html.dark .head h2 { color: #d8e2ea; }
</style>
