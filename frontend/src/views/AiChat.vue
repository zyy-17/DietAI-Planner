<template>
  <div class="ai-chat">
    <el-container style="height:calc(100vh - 100px)">
      <el-aside width="260px" class="chat-sidebar">
        <el-button type="primary" style="width:100%;margin-bottom:12px" @click="createSession">新对话</el-button>
        <div v-for="s in sessions" :key="s.id" class="session-item" :class="{ active: currentSessionId === s.id }">
          <span class="session-title" @click="loadSession(s.id)">{{ s.title }}</span>
          <el-icon class="session-delete" @click.stop="confirmDeleteSession(s.id)"><Delete /></el-icon>
        </div>
      </el-aside>
      <el-main class="chat-main">
        <div class="messages" ref="messagesRef">
          <div v-for="msg in messages" :key="msg.id" class="message" :class="msg.role">
            <el-avatar v-if="msg.role === 'user'" :size="32" :src="userStore.avatarUrl || undefined" :icon="UserFilled" class="msg-avatar" />
            <div v-else class="msg-avatar-ai">🤖</div>
            <div class="msg-content">
              <div class="msg-text">{{ msg.content }}</div>
            </div>
          </div>
          <div v-if="messages.length === 0" class="empty-chat">
            <p>🤖 您好！我是您的智能膳食助手，有什么可以帮您的？</p>
          </div>
        </div>
        <div class="chat-input">
          <el-input v-model="inputText" placeholder="输入您的问题..." @keyup.enter="sendMessage" :disabled="sending">
            <template #append>
              <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
            </template>
          </el-input>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, UserFilled } from '@element-plus/icons-vue'
import api from '../utils/api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref(null)

async function loadSessions() {
  sessions.value = await api.get('/chat/sessions')
}

async function createSession() {
  const session = await api.post('/chat/sessions', { title: '新对话' })
  currentSessionId.value = session.id
  messages.value = []
  loadSessions()
}

async function loadSession(sessionId) {
  currentSessionId.value = sessionId
  messages.value = await api.get(`/chat/sessions/${sessionId}/messages`)
  await nextTick()
  scrollToBottom()
}

function confirmDeleteSession(sessionId) {
  ElMessageBox.confirm('确定要删除该会话吗？删除后无法恢复。', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteSession(sessionId)
  }).catch(() => {})
}

async function deleteSession(sessionId) {
  try {
    await api.delete(`/chat/sessions/${sessionId}`)
    ElMessage.success('会话已删除')
    if (currentSessionId === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    loadSessions()
  } catch (e) {
    console.error(e)
  }
}

async function sendMessage() {
  if (!inputText.value.trim()) return
  sending.value = true
  const text = inputText.value
  inputText.value = ''

  messages.value.push({ id: Date.now(), role: 'user', content: text })
  await nextTick()
  scrollToBottom()

  try {
    const res = await api.post('/chat/send', { sessionId: currentSessionId.value, content: text })
    if (!currentSessionId.value) {
      currentSessionId.value = res.sessionId
    }
    messages.value.push(res)
    await nextTick()
    scrollToBottom()
    loadSessions()
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

onMounted(loadSessions)
</script>

<style scoped>
.chat-sidebar { background: #fff; padding: 12px; border-right: 1px solid #e4e7ed; overflow-y: auto; }
.session-item { padding: 10px 12px; cursor: pointer; border-radius: 6px; margin-bottom: 4px; font-size: 14px; color: #606266; display: flex; align-items: center; justify-content: space-between; }
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; color: #409eff; }
.session-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-delete { flex-shrink: 0; margin-left: 8px; color: #c0c4cc; cursor: pointer; font-size: 14px; }
.session-delete:hover { color: #f56c6c; }
.chat-main { display: flex; flex-direction: column; padding: 0; }
.messages { flex: 1; overflow-y: auto; padding: 16px; }
.message { display: flex; margin-bottom: 16px; align-items: flex-start; gap: 8px; }
.message.assistant { flex-direction: row; }
.message.user { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.msg-avatar-ai { font-size: 28px; flex-shrink: 0; }
.msg-content { max-width: 70%; }
.msg-text { padding: 10px 14px; border-radius: 8px; line-height: 1.6; white-space: pre-wrap; }
.message.user .msg-text { background: #409eff; color: #fff; }
.message.assistant .msg-text { background: #f4f4f5; color: #333; }
.empty-chat { text-align: center; padding: 60px 0; color: #909399; font-size: 16px; }
.chat-input { padding: 12px 16px; border-top: 1px solid #e4e7ed; background: #fff; }
</style>