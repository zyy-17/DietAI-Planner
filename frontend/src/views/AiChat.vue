<template>
  <div class="ai-chat">
    <el-container style="height:calc(100vh - 100px)">
      <el-aside width="260px" class="chat-sidebar">
        <el-button type="primary" style="width:100%;margin-bottom:12px" @click="createSession">{{ presetTitle || '新对话' }}</el-button>
        <div class="preset-hint" v-if="presetTitle">
          <small>当前模式：{{ presetTitle }}</small>
        </div>
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
            <p>{{ emptyChatText }}</p>
          </div>
        </div>
        <div class="chat-input">
          <el-input v-model="inputText" :placeholder="inputPlaceholder" @keyup.enter="sendMessage" :disabled="sending">
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, UserFilled } from '@element-plus/icons-vue'
import api from '../utils/api'
import { useUserStore } from '../stores/user'

const route = useRoute()
const userStore = useUserStore()

const preset = computed(() => route.meta.preset || '')

const presetMap = {
  'diet-plan': { title: '膳食规划', greeting: '🤖 您好！我是您的膳食规划助手，请告诉我您的饮食偏好和目标，我来为您定制专属膳食方案。', placeholder: '描述您的饮食目标和偏好...' },
  'fat-loss': { title: '减脂方案', greeting: '🤖 您好！我是您的减脂顾问，请告诉我您的当前体重、目标体重和日常活动情况，我来为您制定科学减脂方案。', placeholder: '描述您的减脂需求...' },
  'muscle-gain': { title: '增肌方案', greeting: '🤖 您好！我是您的增肌顾问，请告诉我您的训练计划和营养需求，我来为您制定增肌饮食方案。', placeholder: '描述您的增肌需求...' },
  'consult': { title: '饮食咨询', greeting: '🤖 您好！我是您的饮食健康顾问，有任何关于饮食营养、食物搭配、特殊饮食需求的问题都可以问我。', placeholder: '输入您的饮食问题...' }
}

const presetTitle = computed(() => presetMap[preset.value]?.title || '')
const emptyChatText = computed(() => presetMap[preset.value]?.greeting || '🤖 您好！我是您的智能膳食助手，有什么可以帮您的？')
const inputPlaceholder = computed(() => presetMap[preset.value]?.placeholder || '输入您的问题...')

const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref(null)

async function loadSessions() {
  try { sessions.value = await api.get('/chat/sessions') } catch (e) {}
}

async function createSession() {
  const title = presetTitle.value || '新对话'
  const session = await api.post('/chat/sessions', { title, preset: preset.value })
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
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => deleteSession(sessionId)).catch(() => {})
}

async function deleteSession(sessionId) {
  try {
    await api.delete(`/chat/sessions/${sessionId}`)
    ElMessage.success('会话已删除')
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    loadSessions()
  } catch (e) {}
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
    const res = await api.post('/chat/send', { sessionId: currentSessionId.value, content: text, preset: preset.value })
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

onMounted(() => {
  loadSessions()
  const sessionId = route.query.sessionId
  if (sessionId) {
    loadSession(sessionId)
  }
})
</script>

<style scoped>
.chat-sidebar { background: #fff; padding: 12px; border-right: 1px solid #e4e7ed; overflow-y: auto; }
.preset-hint { margin-bottom: 10px; text-align: center; }
.preset-hint small { color: #409eff; background: #ecf5ff; padding: 3px 8px; border-radius: 4px; }
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