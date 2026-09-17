<template>
  <div>
    <el-card>
      <template #header><span>AI生成记录</span></template>
      <el-table :data="logs" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="inputSummary" label="输入摘要" show-overflow-tooltip />
        <el-table-column prop="modelName" label="模型" width="100" />
        <el-table-column prop="isAbnormal" label="异常" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isAbnormal === 1 ? 'danger' : 'success'">{{ row.isAbnormal === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.isAbnormal === 0" size="small" type="danger" @click="markAbnormal(row.id)">标记异常</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="total" @current-change="loadLogs" layout="prev, pager, next" style="margin-top:16px" />
    </el-card>

    <el-dialog v-model="detailVisible" title="记录详情" width="600px">
      <el-descriptions :column="1" border v-if="currentLog">
        <el-descriptions-item label="输入摘要">{{ currentLog.inputSummary }}</el-descriptions-item>
        <el-descriptions-item label="AI输出"><div style="white-space:pre-wrap;max-height:300px;overflow:auto">{{ currentLog.outputContent }}</div></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../../utils/api'

const logs = ref([])
const page = ref(1)
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref(null)

async function loadLogs() {
  const data = await api.get('/admin/ai-logs', { params: { page: page.value - 1 } })
  logs.value = data.content
  total.value = data.totalElements
}

function viewDetail(log) {
  currentLog.value = log
  detailVisible.value = true
}

async function markAbnormal(id) {
  await api.put(`/admin/ai-logs/${id}/abnormal`)
  ElMessage.success('已标记为异常')
  loadLogs()
}

onMounted(loadLogs)
</script>