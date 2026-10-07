<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>营养标准管理</span>
          <el-button type="primary" @click="openDialog()">➕ 新增标准</el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="营养标准用于客户端「营养分析」模块，按性别 + 年龄段给出每日推荐摄入量。"
        style="margin-bottom: 16px" />

      <el-table :data="standards" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="性别" width="90">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="年龄段" width="130">
          <template #default="{ row }">{{ row.ageMin }} - {{ row.ageMax }} 岁</template>
        </el-table-column>
        <el-table-column prop="caloriesKcal" label="推荐热量(kcal)" width="150" />
        <el-table-column prop="proteinG" label="蛋白质(g)" width="110" />
        <el-table-column prop="carbG" label="碳水(g)" width="110" />
        <el-table-column prop="fatG" label="脂肪(g)" width="110" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑营养标准' : '新增营养标准'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" style="width: 100%">
            <el-option :value="1" label="男" />
            <el-option :value="2" label="女" />
            <el-option :value="0" label="未知 / 通用" />
          </el-select>
        </el-form-item>
        <el-form-item label="起始年龄" prop="ageMin">
          <el-input-number v-model="form.ageMin" :min="0" :max="150" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束年龄" prop="ageMax">
          <el-input-number v-model="form.ageMax" :min="0" :max="150" style="width: 100%" />
        </el-form-item>
        <el-form-item label="推荐热量(kcal)" prop="caloriesKcal">
          <el-input-number v-model="form.caloriesKcal" :min="0" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="蛋白质(g)">
          <el-input-number v-model="form.proteinG" :min="0" :precision="2" :step="5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="碳水(g)">
          <el-input-number v-model="form.carbG" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="脂肪(g)">
          <el-input-number v-model="form.fatG" :min="0" :precision="2" :step="5" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../utils/api'

const standards = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()

const emptyForm = () => ({ gender: 1, ageMin: 18, ageMax: 30, caloriesKcal: 2000, proteinG: 60, carbG: 250, fatG: 60 })
const form = reactive(emptyForm())

const rules = {
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  ageMin: [{ required: true, message: '请输入起始年龄', trigger: 'blur' }],
  ageMax: [{ required: true, message: '请输入结束年龄', trigger: 'blur' }],
  caloriesKcal: [{ required: true, message: '请输入推荐热量', trigger: 'blur' }]
}

function genderText(g) {
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '通用'
}

async function loadStandards() {
  loading.value = true
  try {
    standards.value = await api.get('/admin/nutrition-standards')
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      gender: row.gender,
      ageMin: row.ageMin,
      ageMax: row.ageMax,
      caloriesKcal: row.caloriesKcal,
      proteinG: row.proteinG != null ? Number(row.proteinG) : 0,
      carbG: row.carbG != null ? Number(row.carbG) : 0,
      fatG: row.fatG != null ? Number(row.fatG) : 0
    })
  } else {
    editingId.value = null
    Object.assign(form, emptyForm())
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  if (form.ageMin > form.ageMax) {
    ElMessage.warning('起始年龄不能大于结束年龄')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/admin/nutrition-standards/${editingId.value}`, form)
      ElMessage.success('保存成功')
    } else {
      await api.post('/admin/nutrition-standards', form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadStandards()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该条营养标准？', '提示', { type: 'warning' })
  await api.delete(`/admin/nutrition-standards/${row.id}`)
  ElMessage.success('删除成功')
  loadStandards()
}

onMounted(loadStandards)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
