<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>食物管理</span>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadFoods" style="width:150px">
            <el-option value="approved" label="已通过" />
            <el-option value="pending" label="待审核" />
            <el-option value="rejected" label="已驳回" />
          </el-select>
        </div>
      </template>
      <el-table :data="foods" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="calories" label="热量(kcal/100g)" width="140" />
        <el-table-column prop="source" label="来源" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'approved' ? 'success' : row.status === 'pending' ? 'warning' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" size="small" type="success" @click="updateStatus(row.id, 'approved')">通过</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="danger" @click="updateStatus(row.id, 'rejected')">驳回</el-button>
            <el-button size="small" type="danger" @click="deleteFood(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="total" @current-change="loadFoods" layout="prev, pager, next" style="margin-top:16px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../utils/api'

const foods = ref([])
const page = ref(1)
const total = ref(0)
const statusFilter = ref(null)

async function loadFoods() {
  const params = { page: page.value - 1 }
  if (statusFilter.value) params.status = statusFilter.value
  const data = await api.get('/admin/foods', { params })
  foods.value = data.content
  total.value = data.totalElements
}

async function updateStatus(id, status) {
  await api.put(`/admin/foods/${id}/status`, { status })
  ElMessage.success('操作成功')
  loadFoods()
}

async function deleteFood(id) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await api.delete(`/admin/foods/${id}`)
  ElMessage.success('删除成功')
  loadFoods()
}

onMounted(loadFoods)
</script>