<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>食物分类管理</span>
          <el-button type="primary" @click="openDialog()">➕ 新增分类</el-button>
        </div>
      </template>

      <el-table :data="categories" stripe v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="分类名称" min-width="140" />
        <el-table-column prop="parentId" label="父分类ID" width="100">
          <template #default="{ row }">{{ row.parentId || 0 }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="icon" label="图标" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteCat(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：谷薯类" />
        </el-form-item>
        <el-form-item label="父分类ID">
          <el-input-number v-model="form.parentId" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji 或图标名，选填" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../utils/api'

const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const form = reactive({ name: '', parentId: 0, sortOrder: 0, icon: '', status: 1 })

async function loadCategories() {
  loading.value = true
  try {
    categories.value = await api.get('/admin/categories')
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      name: row.name,
      parentId: row.parentId ?? 0,
      sortOrder: row.sortOrder ?? 0,
      icon: row.icon || '',
      status: row.status ?? 1
    })
  } else {
    editingId.value = null
    Object.assign(form, { name: '', parentId: 0, sortOrder: 0, icon: '', status: 1 })
  }
  dialogVisible.value = true
}

async function saveCategory() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/admin/categories/${editingId.value}`, form)
    } else {
      await api.post('/admin/categories', form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadCategories()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = { ...row, status: row.status === 1 ? 0 : 1 }
  await api.put(`/admin/categories/${row.id}`, next)
  ElMessage.success('操作成功')
  loadCategories()
}

async function deleteCat(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '提示', { type: 'warning' })
  await api.delete(`/admin/categories/${row.id}`)
  ElMessage.success('删除成功')
  loadCategories()
}

onMounted(loadCategories)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
