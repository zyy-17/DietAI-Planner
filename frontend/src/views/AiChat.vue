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
              <!-- 用户消息：纯文本显示 -->
              <div v-if="msg.role === 'user'" class="msg-text">{{ msg.content }}</div>
              <!-- AI回复：Markdown渲染 + 耗时显示 -->
              <div v-else class="msg-text ai-response">
                <div class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
                <div v-if="msg.duration !== undefined" class="response-time">
                  ⏱️ 回复耗时：{{ formatDuration(msg.duration) }}
                </div>
              </div>
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
import { marked } from 'marked'
import DOMPurify from 'dompurify'
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
let requestStartTime = null  // 记录请求开始时间
const fullText = '正在思考中，请稍候...'
let charIndex = 0

// Markdown 渲染函数（带 XSS 防护）
function renderMarkdown(content) {
  if (!content) return ''
  try {
    // 使用 marked 解析 Markdown，再用 DOMPurify 清理 HTML 防止 XSS 攻击
    const html = marked(content)
    return DOMPurify.sanitize(html)
  } catch (e) {
    console.error('Markdown 解析失败:', e)
    return content  // 解析失败时返回原始文本
  }
}

// 格式化耗时显示（超过1分钟显示"X分X秒"，否则"X秒"）
function formatDuration(seconds) {
  if (!seconds && seconds !== 0) return ''
  if (seconds < 60) {
    return `${seconds}秒`
  } else {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes}分${remainingSeconds}秒`
  }
}

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

  // 记录请求开始时间
  requestStartTime = Date.now()

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

    // 计算AI回复耗时（秒）
    const duration = Math.round((Date.now() - requestStartTime) / 1000)

    if (!currentSessionId.value) {
      currentSessionId.value = res.sessionId
    }

    // 将耗时信息添加到AI回复消息中
    messages.value.push({
      ...res,
      duration: duration  // 添加耗时字段
    })

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
    requestStartTime = null  // 清空开始时间
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

/* AI回复消息样式（支持Markdown渲染） */
.ai-response {
  white-space: normal !important;  /* 覆盖父级的 pre-wrap，允许Markdown正常渲染 */
}

.markdown-body {
  line-height: 1.7;
  word-wrap: break-word;
}

/* Markdown 渲染后的元素样式 */
.markdown-body :deep(p) {
  margin: 0 0 8px 0;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(strong) {
  font-weight: 600;
  color: #1f2328;
}

.markdown-body :deep(em) {
  font-style: italic;
  color: #1f2328;
}

.markdown-body :deep(ul), .markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
  line-height: 1.6;
}

.markdown-body :deep(h1), .markdown-body :deep(h2),
.markdown-body :deep(h3), .markdown-body :deep(h4) {
  margin: 12px 0 8px 0;
  font-weight: 600;
  color: #1f2328;
}

.markdown-body :deep(h1) { font-size: 1.3em; }
.markdown-body :deep(h2) { font-size: 1.2em; }
.markdown-body :deep(h3) { font-size: 1.1em; }

.markdown-body :deep(code) {
  background: #f6f8fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.markdown-body :deep(pre) {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #24292e;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #dfe2e5;
  padding-left: 16px;
  margin: 8px 0;
  color: #6a737d;
}

.markdown-body :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

/* AI回复耗时显示样式 */
.response-time {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
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