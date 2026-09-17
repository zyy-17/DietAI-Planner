<template>
  <div>
    <el-card>
      <template #header><span>用户管理</span></template>
      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="total" @current-change="loadUsers" layout="prev, pager, next" style="margin-top:16px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../utils/api'

const users = ref([])
const page = ref(1)
const total = ref(0)

async function loadUsers() {
  const data = await api.get('/admin/users', { params: { page: page.value - 1 } })
  users.value = data.content
  total.value = data.totalElements
}

async function toggleStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  await api.put(`/admin/users/${user.id}/status`, { status: newStatus })
  ElMessage.success('操作成功')
  loadUsers()
}

onMounted(loadUsers)
</script>