<template>
  <div class="admin-meal-plans">
    <div class="head">
      <h2>食谱审核</h2>
      <p class="sub">用户发布到食谱广场的食谱需要审核通过才会上架</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" :class="{ active: status === 'pending' }" @click="switchStatus('pending')">
        <div class="stat-num">{{ pendingCount }}</div>
        <div class="stat-label">待审核</div>
      </div>
      <div class="stat-card" :class="{ active: status === 'approved' }" @click="switchStatus('approved')">
        <div class="stat-num">{{ approvedCount }}</div>
        <div class="stat-label">已上架</div>
      </div>
      <div class="stat-card" :class="{ active: status === 'rejected' }" @click="switchStatus('rejected')">
        <div class="stat-num">{{ rejectedCount }}</div>
        <div class="stat-label">未通过</div>
      </div>
      <div class="stat-card" :class="{ active: status === 'none' }" @click="switchStatus('none')">
        <div class="stat-num">—</div>
        <div class="stat-label">未发布</div>
      </div>
    </div>

    <div class="toolbar">
      <el-radio-group v-model="status" size="small" @change="load(0)">
        <el-radio-button value="pending">待审核</el-radio-button>
        <el-radio-button value="approved">已上架</el-radio-button>
        <el-radio-button value="rejected">未通过</el-radio-button>
        <el-radio-button value="none">未发布</el-radio-button>
      </el-radio-group>
      <div class="toolbar-right">
        <el-input v-model="keyword" size="small" placeholder="搜索食谱名" clearable
          style="width: 180px" @keyup.enter="load(0)" @clear="load(0)">
          <template #append><el-button @click="load(0)">搜</el-button></template>
        </el-input>
        <el-button size="small" type="success" v-if="status === 'pending' && selected.length"
          @click="batchApprove">批量通过 ({{ selected.length }})</el-button>
        <el-button size="small" type="danger" v-if="status === 'pending' && selected.length"
          @click="batchReject">批量驳回 ({{ selected.length }})</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="margin-top: 12px"
      @selection-change="onSelectionChange" ref="tableRef">
      <el-table-column type="selection" width="40" v-if="status === 'pending'" />
      <el-table-column prop="name" label="食谱名" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="plan-name">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.difficulty" size="small" type="info" style="margin-left: 6px">{{ row.difficulty }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="100" />
      <el-table-column label="目标" width="80">
        <template #default="{ row }">{{ row.goalLabel }}</template>
      </el-table-column>
      <el-table-column label="周期" width="70">
        <template #default="{ row }">{{ row.days }}天</template>
      </el-table-column>
      <el-table-column label="每日热量" width="100">
        <template #default="{ row }">{{ row.plannedDailyCalories || '—' }} kcal</template>
      </el-table-column>
      <el-table-column label="食物条目" width="80">
        <template #default="{ row }">{{ row.itemCount || 0 }} 条</template>
      </el-table-column>
      <el-table-column label="热度" width="90">
        <template #default="{ row }">
          <span v-if="row.usageCount">🔥 {{ row.usageCount }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="publishStatusLabel" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusType(row.publishStatus)" size="small">{{ row.publishStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="150">
        <template #default="{ row }">{{ (row.updatedAt || row.createdAt || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="view(row)">查看</el-button>
          <el-button size="small" type="success" v-if="row.publishStatus === 'pending'"
            @click="approve(row)">通过</el-button>
          <el-button size="small" type="danger" v-if="row.publishStatus === 'pending'"
            @click="reject(row)">驳回</el-button>
          <el-button size="small" type="warning" v-if="row.publishStatus === 'approved'"
            @click="takeDown(row)">下架</el-button>
          <el-button size="small" type="info" v-if="row.publishStatus === 'rejected'"
            @click="viewReason(row)">原因</el-button>
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
const keyword = ref('')
const selected = ref([])
const tableRef = ref(null)

const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)

const sheetVisible = ref(false)
const sheetPlanId = ref(null)

function statusType(s) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', none: 'info' }[s] || 'info'
}

function switchStatus(s) {
  status.value = s
  load(0)
}

async function loadStats() {
  try {
    const [p, a, r] = await Promise.all([
      api.get('/admin/meal-plans', { params: { status: 'pending', page: 0, size: 1 } }),
      api.get('/admin/meal-plans', { params: { status: 'approved', page: 0, size: 1 } }),
      api.get('/admin/meal-plans', { params: { status: 'rejected', page: 0, size: 1 } })
    ])
    pendingCount.value = p?.totalElements || 0
    approvedCount.value = a?.totalElements || 0
    rejectedCount.value = r?.totalElements || 0
  } catch (e) {}
}

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

function onSelectionChange(rows) {
  selected.value = rows
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
    loadStats()
  } catch (e) {}
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
    loadStats()
  } catch (e) {}
}

async function takeDown(row) {
  try {
    await ElMessageBox.confirm(`确定将「${row.name}」从广场下架？收藏该食谱的用户将无法再查看。`, '下架确认', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await api.post(`/admin/meal-plans/${row.id}/takedown`)
    ElMessage.success('已下架')
    load()
    loadStats()
  } catch (e) {}
}

function viewReason(row) {
  ElMessageBox.alert(row.rejectReason || '无驳回原因', '驳回原因', { type: 'warning' })
}

async function batchApprove() {
  try {
    await ElMessageBox.confirm(`确定批量通过 ${selected.value.length} 份食谱？`, '批量审核', { type: 'success' })
  } catch (e) {
    return
  }
  let ok = 0
  for (const row of selected.value) {
    try {
      await api.post(`/admin/meal-plans/${row.id}/approve`)
      ok++
    } catch (e) {}
  }
  ElMessage.success(`已通过 ${ok} 份食谱`)
  load()
  loadStats()
}

async function batchReject() {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt(`批量驳回 ${selected.value.length} 份食谱，填写原因`, '批量驳回', {
      inputPlaceholder: '如：内容质量不达标',
      inputValidator: (v) => (v && v.trim() ? true : '请填写原因')
    })
    reason = res.value
  } catch (e) {
    return
  }
  let ok = 0
  for (const row of selected.value) {
    try {
      await api.post(`/admin/meal-plans/${row.id}/reject`, { reason })
      ok++
    } catch (e) {}
  }
  ElMessage.success(`已驳回 ${ok} 份食谱`)
  load()
  loadStats()
}

onMounted(() => {
  load(0)
  loadStats()
})
</script>

<style scoped>
.head h2 { margin: 0; font-size: 18px; color: #2f4152; }
.head .sub { margin: 4px 0 14px; font-size: 12px; color: #8b98a5; }

.stats-row {
  display: flex; gap: 16px; margin-bottom: 16px;
}
.stat-card {
  flex: 1; background: #f5f8f6; border-radius: 12px; padding: 16px 20px;
  text-align: center; cursor: pointer; transition: all .2s;
  border: 2px solid transparent;
}
.stat-card:hover { background: #eef7f1; }
.stat-card.active { border-color: #4caf7d; background: #eef7f1; }
.stat-num { font-size: 28px; font-weight: 700; color: #3fae74; line-height: 1.2; }
.stat-label { margin-top: 6px; font-size: 13px; color: #8b98a5; }

.toolbar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
}
.toolbar-right {
  display: flex; align-items: center; gap: 8px;
}

.plan-name { display: flex; align-items: center; }

html.dark .head h2 { color: #d8e2ea; }
html.dark .stat-card { background: #232b34; }
html.dark .stat-card:hover { background: #2a333d; }
html.dark .stat-card.active { background: #24322b; border-color: #4caf7d; }
</style>