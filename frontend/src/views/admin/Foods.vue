<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>食物管理</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索食物名称"
              clearable
              style="width: 200px"
              @keyup.enter="search"
              @clear="search" />
            <el-button type="primary" @click="openDialog()">➕ 添加食物</el-button>
          </div>
        </div>
      </template>

      <el-table :data="foods" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.name }}
            <el-tag v-if="row.unitName && row.unitWeight" size="small" type="success" effect="plain" style="margin-left:6px">
              1{{ row.unitName }}≈{{ row.unitWeight }}g
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="calories" label="热量(kcal/100g)" width="140" />
        <el-table-column prop="protein" label="蛋白质(g)" width="100" />
        <el-table-column prop="carbohydrate" label="碳水(g)" width="100" />
        <el-table-column prop="fat" label="脂肪(g)" width="90" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteFood(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        @current-change="loadFoods"
        layout="prev, pager, next, total"
        style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑食物' : '添加食物'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="食物名称" prop="name">
          <el-input v-model="form.name" placeholder="如：鸡胸肉" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="热量(kcal)" prop="calories">
          <el-input-number v-model="form.calories" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="蛋白质(g)">
          <el-input-number v-model="form.protein" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="碳水(g)">
          <el-input-number v-model="form.carbohydrate" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="脂肪(g)">
          <el-input-number v-model="form.fat" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="膳食纤维(g)">
          <el-input-number v-model="form.fiber" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计量单位">
          <div class="unit-row">
            <el-select v-model="form.unitName" placeholder="不设置则只能按克数记录" clearable filterable allow-create style="width: 170px">
              <el-option v-for="u in COMMON_UNIT_NAMES" :key="u" :label="u" :value="u" />
            </el-select>
            <span class="unit-label">每 1 个/份约</span>
            <el-input-number v-model="form.unitWeight" :min="1" :max="2000" :step="5" :disabled="!form.unitName" style="width: 140px" />
            <span class="unit-label">g</span>
          </div>
          <div class="unit-tip">设置后用户在记录饮食时可按「个数 / 份数」填写，例如鸡蛋：个 ≈ 50g</div>
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.imageUrl" placeholder="选填" />
        </el-form-item>
        <el-alert
          v-if="!editingId"
          type="info"
          :closable="false"
          show-icon
          title="新增的食物将进入公开食物库，所有用户可见。" />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFood">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../utils/api'
import { COMMON_UNIT_NAMES } from '../../utils/foodUnits'

const foods = ref([])
const categories = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()

const emptyForm = () => ({
  name: '',
  categoryId: null,
  calories: 0,
  protein: 0,
  carbohydrate: 0,
  fat: 0,
  fiber: 0,
  imageUrl: '',
  unitName: '',
  unitWeight: 100
})
const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入食物名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  calories: [{ required: true, message: '请输入热量', trigger: 'blur' }]
}

function categoryName(id) {
  const hit = categories.value.find((c) => c.id === id)
  return hit ? hit.name : '-'
}

async function loadCategories() {
  categories.value = await api.get('/categories')
}

async function loadFoods() {
  loading.value = true
  try {
    const params = { page: page.value - 1 }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await api.get('/admin/foods', { params })
    foods.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadFoods()
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      name: row.name,
      categoryId: row.categoryId,
      calories: Number(row.calories ?? 0),
      protein: Number(row.protein ?? 0),
      carbohydrate: Number(row.carbohydrate ?? 0),
      fat: Number(row.fat ?? 0),
      fiber: Number(row.fiber ?? 0),
      imageUrl: row.imageUrl || '',
      unitName: row.unitName || '',
      unitWeight: Number(row.unitWeight) > 0 ? Number(row.unitWeight) : 100
    })
  } else {
    editingId.value = null
    Object.assign(form, emptyForm())
  }
  dialogVisible.value = true
}

async function saveFood() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      ...form,
      unitName: form.unitName || null,
      unitWeight: form.unitName ? form.unitWeight : null
    }
    if (editingId.value) {
      await api.put(`/admin/foods/${editingId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      await api.post('/admin/foods', payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadFoods()
  } finally {
    saving.value = false
  }
}

async function deleteFood(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？删除后不可恢复。`, '提示', { type: 'warning' })
  await api.delete(`/admin/foods/${row.id}`)
  ElMessage.success('删除成功')
  loadFoods()
}

onMounted(async () => {
  await loadCategories()
  await loadFoods()
})
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
.unit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.unit-label {
  font-size: 13px;
  color: #606266;
}
.unit-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 2px;
}
</style>
