<template>
  <div class="ai-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🕘 历史对话</span>
          <el-input v-model="keyword" placeholder="搜索对话标题..." clearable style="width:250px" @input="filterSessions" />
        </div>
      </template>

      <el-empty v-if="filteredSessions.length === 0" description="暂无历史对话" />

      <div v-else class="session-list">
        <div v-for="s in filteredSessions" :key="s.id" class="session-card" @click="openSession(s)">
          <div class="session-icon">🤖</div>
          <div class="session-info">
            <h4>{{ s.title || '未命名对话' }}</h4>
            <p>{{ s.lastMessage || '暂无消息' }}</p>
            <small>{{ s.createdAt }}</small>
          </div>
          <el-button type="danger" text size="small" @click.stop="deleteSession(s.id)">删除</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const sessions = ref([])
const keyword = ref('')

const filteredSessions = computed(() => {
  if (!keyword.value) return sessions.value
  const kw = keyword.value.toLowerCase()
  return sessions.value.filter(s => (s.title || '').toLowerCase().includes(kw))
})

async function loadSessions() {
  try {
    sessions.value = await api.get('/ai/sessions')
  } catch (e) {}
}

function filterSessions() {}

async function openSession(s) {
  router.push({ path: '/chat', query: { sessionId: s.id } })
}

async function deleteSession(id) {
  await ElMessageBox.confirm('确定删除此对话？', '提示', { type: 'warning' })
  try {
    await api.delete(`/ai/sessions/${id}`)
    ElMessage.success('删除成功')
    loadSessions()
  } catch (e) {}
}

onMounted(loadSessions)
</script>

<style scoped>
.ai-history { padding: 18px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.session-list { display: flex; flex-direction: column; gap: 10px; }
.session-card { display: flex; align-items: center; gap: 12px; padding: 14px; border: 1px solid #e5edf2; border-radius: 10px; cursor: pointer; transition: all 0.2s; }
.session-card:hover { background: #f5f9fc; border-color: #c6e2ff; }
.session-icon { font-size: 28px; flex-shrink: 0; }
.session-info { flex: 1; }
.session-info h4 { margin: 0 0 4px; font-size: 14px; color: #244b6b; }
.session-info p { margin: 0 0 4px; font-size: 12px; color: #8ea1af; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-info small { font-size: 11px; color: #b0b8c4; }
</style>