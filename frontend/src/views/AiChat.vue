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
          <!-- AI正在思考中的提示（增强版） -->
          <div v-if="sending" class="message assistant thinking">
            <div class="msg-avatar-ai">🤖</div>
            <div class="msg-content">
              <div class="msg-text thinking-text">
                <div class="thinking-header">
                  <span class="typewriter-text">{{ typewriterDisplay }}</span>
                  <span class="cursor-blink">|</span>
                </div>
                <div class="thinking-meta">
                  <span class="waiting-time">⏱️ 已等待 {{ waitingTime }}秒</span>
                  <el-button type="danger" size="small" @click="cancelRequest" class="cancel-btn">
                    取消请求
                  </el-button>
                </div>
              </div>
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
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

// 新增：打字机效果、等待时间、取消请求相关状态
const typewriterDisplay = ref('')
const waitingTime = ref(0)
let abortController = null
let timer = null
let typewriterTimer = null
const fullText = '正在思考中，请稍候...'
let charIndex = 0

// 打字机效果函数
function startTypewriter() {
  charIndex = 0
  typewriterDisplay.value = ''
  if (typewriterTimer) clearInterval(typewriterTimer)

  typewriterTimer = setInterval(() => {
    if (charIndex < fullText.length) {
      typewriterDisplay.value += fullText[charIndex]
      charIndex++
    } else {
      // 循环播放：完成后重新开始
      setTimeout(() => {
        charIndex = 0
        typewriterDisplay.value = ''
      }, 2000)
    }
  }, 100)
}

// 停止打字机效果
function stopTypewriter() {
  if (typewriterTimer) {
    clearInterval(typewriterTimer)
    typewriterTimer = null
  }
  typewriterDisplay.value = ''
}

// 开始计时
function startTimer() {
  waitingTime.value = 0
  if (timer) clearInterval(timer)

  timer = setInterval(() => {
    waitingTime.value++
  }, 1000)
}

// 停止计时
function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  waitingTime.value = 0
}

// 取消请求
function cancelRequest() {
  if (abortController) {
    abortController.abort()
    ElMessage.info('已取消请求')
  }
}

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
  if (!inputText.value.trim() || sending.value) return

  // 创建AbortController用于取消请求
  abortController = new AbortController()

  sending.value = true
  const text = inputText.value
  inputText.value = ''

  messages.value.push({ id: Date.now(), role: 'user', content: text })
  await nextTick()
  scrollToBottom()

  // 启动打字机效果和计时器
  startTypewriter()
  startTimer()

  try {
    const res = await api.post('/chat/send', {
      sessionId: currentSessionId.value,
      content: text,
      preset: preset.value
    }, {
      signal: abortController.signal  // 传递取消信号
    })

    if (!currentSessionId.value) {
      currentSessionId.value = res.sessionId
    }
    messages.value.push(res)
    await nextTick()
    scrollToBottom()
    loadSessions()
  } catch (error) {
    // 如果是用户主动取消，不显示错误信息
    if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
      console.log('请求已取消')
      // 可选：添加一条系统消息提示用户
      // messages.value.push({ id: Date.now(), role: 'assistant', content: '❌ 请求已取消' })
    } else {
      console.error('发送消息失败:', error)
    }
  } finally {
    sending.value = false
    abortController = null
    // 停止打字机效果和计时器
    stopTypewriter()
    stopTimer()
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

// 组件卸载时清理定时器
onUnmounted(() => {
  stopTypewriter()
  stopTimer()
  if (abortController) {
    abortController.abort()
    abortController = null
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

/* AI思考中提示样式（增强版） */
.message.thinking { opacity: 0.9; }
.thinking-text {
  color: #606266 !important;
  font-style: normal;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border: 1px solid #e4e7ed;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.typewriter-text {
  display: inline;
}

.cursor-blink {
  display: inline-block;
  animation: cursorBlink 1s infinite;
  color: #409eff;
  font-weight: bold;
}

@keyframes cursorBlink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.thinking-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #dcdfe6;
}

.waiting-time {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.cancel-btn {
  font-size: 12px;
  padding: 4px 12px;
}
</style>