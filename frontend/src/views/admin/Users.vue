<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索用户名 / 邮箱 / 姓名"
              clearable
              style="width: 240px"
              @keyup.enter="search"
              @clear="search" />
            <el-button type="primary" @click="search">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" width="110" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="dietGoal" label="饮食目标" width="110" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'admin' ? 'danger' : 'info'">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="info" @click="openReset(row)">重置密码</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        @current-change="loadUsers"
        layout="prev, pager, next, total"
        style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ resetTarget?.username }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="confirmReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../utils/api'

const users = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const loading = ref(false)

const resetVisible = ref(false)
const resetTarget = ref(null)
const newPassword = ref('')
const resetting = ref(false)

function genderText(g) {
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '-'
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function loadUsers() {
  loading.value = true
  try {
    const params = { page: page.value - 1 }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await api.get('/admin/users', { params })
    users.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadUsers()
}

async function toggleStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  await api.put(`/admin/users/${user.id}/status`, { status: newStatus })
  ElMessage.success('操作成功')
  loadUsers()
}

function openReset(user) {
  resetTarget.value = user
  newPassword.value = ''
  resetVisible.value = true
}

async function confirmReset() {
  if (newPassword.value.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  resetting.value = true
  try {
    await api.put(`/admin/users/${resetTarget.value.id}/password`, { password: newPassword.value })
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    resetting.value = false
  }
}

async function remove(user) {
  await ElMessageBox.confirm(`确定删除用户「${user.username}」？该操作为软删除。`, '提示', { type: 'warning' })
  await api.delete(`/admin/users/${user.id}`)
  ElMessage.success('删除成功')
  loadUsers()
}

onMounted(loadUsers)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
