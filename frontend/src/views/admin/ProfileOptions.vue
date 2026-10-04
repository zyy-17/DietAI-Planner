<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>📋 档案选项配置</span>
          <el-button type="primary" @click="openDialog()">➕ 新增选项</el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="这里配置的是「个人中心」里让用户选择的选项：饮食目标、活动水平、饮食偏好、忌口食物、慢性疾病。改动保存后，用户刷新页面即可生效。"
        style="margin-bottom: 12px" />

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="选项值(code)是存进用户档案的真实数据，一旦有用户用过就不要再改；只想暂时下线请用「停用」。已停用的选项用户端不再显示，但已有用户的数据仍能正常显示。"
        style="margin-bottom: 16px" />

      <el-tabs v-model="activeType" @tab-change="loadOptions">
        <el-tab-pane
          v-for="t in types"
          :key="t.value"
          :label="`${t.label} (${countOf(t.value)})`"
          :name="t.value" />
      </el-tabs>

      <el-table :data="options" stripe v-loading="loading" empty-text="该分组下暂无选项">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="选项值 code" width="180">
          <template #default="{ row }">
            <code class="code-cell">{{ row.optionCode }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="optionLabel" label="显示文案" width="200" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑选项' : '新增选项'"
      width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="选项分组">
          <el-select v-model="form.optionType" :disabled="!!editingId" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="选项值 code">
          <el-input v-model="form.optionCode" :disabled="!!editingId" placeholder="存进用户档案的值，如 lose" />
          <div class="tip">已有用户使用过的值不要修改，否则会影响他们的档案</div>
        </el-form-item>
        <el-form-item label="显示文案" prop="optionLabel">
          <el-input v-model="form.optionLabel" placeholder="给用户看的文案，如 减脂" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
          <div class="tip">数字越小越靠前</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="仅后台可见，如该选项的适用说明" />
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

const FALLBACK_TYPES = [
  { value: 'diet_goal', label: '饮食目标' },
  { value: 'activity_level', label: '活动水平' },
  { value: 'diet_preference', label: '饮食偏好' },
  { value: 'allergy', label: '忌口食物' },
  { value: 'disease', label: '慢性疾病' }
]

const types = ref(FALLBACK_TYPES)
const counts = ref({})
const activeType = ref('diet_goal')
const options = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()

const emptyForm = () => ({
  optionType: 'diet_goal',
  optionCode: '',
  optionLabel: '',
  sortOrder: 0,
  status: 1,
  remark: ''
})
const form = reactive(emptyForm())

const rules = {
  optionLabel: [{ required: true, message: '请输入显示文案', trigger: 'blur' }],
  optionCode: [{ required: true, message: '请输入选项值', trigger: 'blur' }]
}

function countOf(type) {
  return counts.value[type] ?? 0
}

async function loadTypes() {
  try {
    const list = await api.get('/admin/profile-options/types')
    if (Array.isArray(list) && list.length) types.value = list
  } catch (e) {
    // 接口不可用时用兜底分组，页面仍可浏览
  }
}

async function loadOptions() {
  loading.value = true
  try {
    const list = await api.get('/admin/profile-options', { params: { type: activeType.value } })
    options.value = list || []
    counts.value = { ...counts.value, [activeType.value]: options.value.length }
  } finally {
    loading.value = false
  }
}

/** 切到某个分组时把新选项的排序值预填为末尾+1，省得用户自己算 */
function onTabChange(type) {
  activeType.value = type
  loadOptions()
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      optionType: row.optionType,
      optionCode: row.optionCode,
      optionLabel: row.optionLabel,
      sortOrder: row.sortOrder,
      status: row.status,
      remark: row.remark || ''
    })
  } else {
    editingId.value = null
    const maxSort = options.value.reduce((m, o) => Math.max(m, o.sortOrder || 0), 0)
    Object.assign(form, emptyForm())
    form.optionType = activeType.value
    form.sortOrder = maxSort + 1
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/admin/profile-options/${editingId.value}`, form)
      ElMessage.success('保存成功')
    } else {
      await api.post('/admin/profile-options', form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadOptions()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(
    next === 1
      ? `确定启用「${row.optionLabel}」？用户端将重新显示该选项。`
      : `确定停用「${row.optionLabel}」？停用后新用户选不到，已有用户的数据仍能正常显示。`,
    `${action}确认`,
    { type: 'warning' }
  )
  await api.put(`/admin/profile-options/${row.id}`, { ...row, status: next })
  ElMessage.success(`${action}成功`)
  loadOptions()
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除「${row.optionLabel}」？若已有用户使用该值，需要改为「停用」而非删除。`,
    '提示',
    { type: 'warning' }
  )
  await api.delete(`/admin/profile-options/${row.id}`)
  ElMessage.success('删除成功')
  loadOptions()
}

onMounted(async () => {
  await loadTypes()
  await loadOptions()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.code-cell {
  background: #f4f6f8;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  color: #55738d;
}
.tip {
  font-size: 12px;
  color: #999;
  line-height: 1.6;
  margin-top: 2px;
}
</style>
