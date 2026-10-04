<template>
  <div class="ai-chat">
    <el-container class="chat-container">
      <!-- 第一栏：会话列表 -->
      <el-aside width="220px" class="chat-sidebar">
        <div class="sidebar-brand">
          <span class="brand-icon">🤖</span>
          <span class="brand-name">AI饮食助手</span>
        </div>
        <el-button type="primary" class="new-chat-btn" @click="createSession">＋ 新建对话</el-button>
        <div class="session-list">
          <div v-for="s in sessions" :key="s.id" class="session-item" :class="{ active: currentSessionId === s.id }">
            <span class="session-title" @click="loadSession(s.id)">{{ s.title }}</span>
            <el-icon class="session-delete" @click.stop="confirmDeleteSession(s.id)"><Delete /></el-icon>
          </div>
          <div v-if="sessions.length === 0" class="session-empty">暂无历史对话</div>
        </div>
      </el-aside>

      <!-- 第二栏：对话区 -->
      <el-main class="chat-main">
        <div class="messages" ref="messagesRef">
          <!-- 首次进入（新会话）的欢迎卡片 -->
          <div v-if="messages.length === 0 && !thinkingHere" class="welcome">
            <div class="welcome-avatar">🤖</div>
            <div class="welcome-body">
              <div class="welcome-text">
                👋 你好，我是你的 AI 饮食助手。<br />
                我可以根据你的健康档案和饮食记录，帮助你分析营养摄入、调整膳食计划、推荐食物和解决饮食问题。
              </div>
              <div class="welcome-context">
                📎 已自动读取你的健康档案与饮食记录{{ readDays ? `（近 7 天中有 ${readDays} 天数据）` : '' }}
              </div>
              <div class="welcome-chips">
                <button v-for="q in welcomeQuestions" :key="q" class="chip"
                  :disabled="thinkingHere" @click="sendMessage(q)">{{ q }}</button>
              </div>
            </div>
          </div>

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

          <!-- AI正在思考中的提示 -->
          <div v-if="thinkingHere" class="message assistant thinking">
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
        </div>

        <!-- 底部快捷操作 -->
        <div class="quick-actions">
          <button v-for="a in quickActions" :key="a.label" class="qa-btn"
            :disabled="thinkingHere" @click="sendMessage(a.prompt)">
            <span class="qa-icon">{{ a.icon }}</span>
            <span>{{ a.label }}</span>
          </button>
        </div>

        <div class="chat-input">
          <el-input v-model="inputText" :placeholder="inputPlaceholder" @keyup.enter="onEnter" :disabled="thinkingHere">
            <template #append>
              <el-button type="primary" @click="sendMessage()" :loading="thinkingHere">发送</el-button>
            </template>
          </el-input>
        </div>
      </el-main>

      <!-- 第三栏：今日营养数据 -->
      <el-aside width="240px" class="nutrition-panel">
        <div class="np-title">今日营养数据</div>

        <div class="np-item">
          <div class="np-label">今日热量</div>
          <div class="np-value">
            <b>{{ fmtNum(overview?.totalCalories) }}</b>
            <span class="np-sep">/</span>{{ fmtNum(overview?.targetCalories) }} kcal
          </div>
          <el-progress :percentage="pct(overview?.totalCalories, overview?.targetCalories)"
            :show-text="false" :stroke-width="8"
            :color="barColor(overview?.totalCalories, overview?.targetCalories)" />
        </div>

        <div class="np-item">
          <div class="np-label">蛋白质</div>
          <div class="np-value">
            <b>{{ fmtNum(overview?.totalProtein) }}</b>
            <span class="np-sep">/</span>{{ fmtNum(overview?.targetProtein) }} g
          </div>
          <el-progress :percentage="pct(overview?.totalProtein, overview?.targetProtein)"
            :show-text="false" :stroke-width="8" color="#1D9E75" />
        </div>

        <div class="np-item">
          <div class="np-label">碳水</div>
          <div class="np-value">
            <b>{{ fmtNum(overview?.totalCarbohydrate) }}</b>
            <span class="np-sep">/</span>{{ fmtNum(overview?.targetCarbohydrate) }} g
          </div>
          <el-progress :percentage="pct(overview?.totalCarbohydrate, overview?.targetCarbohydrate)"
            :show-text="false" :stroke-width="8" color="#BA7517" />
        </div>

        <div class="np-item">
          <div class="np-label">脂肪</div>
          <div class="np-value">
            <b>{{ fmtNum(overview?.totalFat) }}</b>
            <span class="np-sep">/</span>{{ fmtNum(overview?.targetFat) }} g
          </div>
          <el-progress :percentage="pct(overview?.totalFat, overview?.targetFat)"
            :show-text="false" :stroke-width="8" color="#7F77DD" />
        </div>

        <div class="np-goal">今日目标：<b>{{ dietGoalLabel }}</b></div>
      </el-aside>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, UserFilled } from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import api from '../utils/api'
import { useUserStore } from '../stores/user'

const route = useRoute()
const userStore = useUserStore()

// 单入口 AI 饮食助手：所有提问都走同一个模型，并自动携带当前登录用户的数据
const inputPlaceholder = '输入您的问题...'

// 首次进入的引导问题
const welcomeQuestions = [
  '今天吃什么？',
  '分析我的今日饮食',
  '帮我推荐晚餐',
  '替换今天的某道菜'
]

// 底部常驻快捷操作（label 展示，prompt 实际发给 AI 的内容）
const quickActions = [
  {
    icon: '🍱',
    label: '推荐晚餐',
    prompt: '帮我推荐今天的晚餐。请先看我今天的饮食记录，算出还差多少热量和蛋白质，再给出具体的食物名称和克数。'
  },
  {
    icon: '📊',
    label: '分析今日饮食',
    prompt: '请分析我今天的饮食：已经摄入了多少热量和三大营养素，对比目标还缺什么，给出改进建议。'
  },
  {
    icon: '🔄',
    label: '替换食物',
    prompt: '我想把今天饮食里的一道菜换成更健康的选择，请结合我目前的营养缺口给出替代方案。'
  },
  {
    icon: '🥗',
    label: '推荐健康食物',
    prompt: '根据我目前的营养缺口，推荐几种适合我的健康食物，并说明推荐理由和食用量。'
  }
]

// 新会话在拿到后端 id 之前的哨兵标识
const NEW_SESSION = '__new_session__'

const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref(null)

// 「正在等待 AI 回复」的那个会话：切换会话时据此判断思考态归属，避免状态串台
const pendingSessionId = ref(null)
const currentTag = computed(() => currentSessionId.value ?? NEW_SESSION)
const thinkingHere = computed(() =>
  sending.value && pendingSessionId.value !== null && pendingSessionId.value === currentTag.value
)

// 第三栏：今日营养数据
const overview = ref(null)
const dietGoalLabel = ref('未设置')
// 欢迎卡片展示"AI 已读到多少天数据"，让用户直观感知个性化来源
const readDays = ref(null)
// 饮食目标文案：由后端 /user/options 下发，管理员改文案后这里自动跟随
const goalLabels = ref({ lose: '减脂', maintain: '维持体重', gain: '增肌' })

// 打字机效果、等待时间、取消请求相关状态
const typewriterDisplay = ref('')
const waitingTime = ref(0)
let abortController = null
let timer = null
let typewriterTimer = null
let requestStartTime = null
const fullText = '正在思考中，请稍候...'
let charIndex = 0

// Markdown 渲染函数（带 XSS 防护）
function renderMarkdown(content) {
  if (!content) return ''
  try {
    const html = marked(content)
    return DOMPurify.sanitize(html)
  } catch (e) {
    console.error('Markdown 解析失败:', e)
    return content
  }
}

// 格式化耗时显示
function formatDuration(seconds) {
  if (!seconds && seconds !== 0) return ''
  if (seconds < 60) {
    return `${seconds}秒`
  }
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}分${remainingSeconds}秒`
}

// 营养数据展示辅助
function fmtNum(v) {
  if (v === undefined || v === null || v === '') return '—'
  return Math.round(Number(v))
}

function pct(current, target) {
  const c = Number(current || 0)
  const t = Number(target || 0)
  if (!t) return 0
  return Math.max(0, Math.min(100, Math.round((c / t) * 100)))
}

function barColor(current, target) {
  const c = Number(current || 0)
  const t = Number(target || 0)
  if (t && c > t) return '#E24B4A'
  return '#2589ee'
}

// ==================== LocalStorage 持久化存储（会话耗时）====================

const DURATION_STORAGE_PREFIX = 'ai_chat_duration_'

function saveDurationToStorage(sessionId, messageId, duration) {
  try {
    const key = `${DURATION_STORAGE_PREFIX}${sessionId}`
    const durationMap = JSON.parse(localStorage.getItem(key) || '{}')
    durationMap[messageId] = duration
    localStorage.setItem(key, JSON.stringify(durationMap))
  } catch (e) {
    console.error('[Duration] 保存失败:', e)
  }
}

function mergeDurationsFromStorage(messages, sessionId) {
  try {
    const key = `${DURATION_STORAGE_PREFIX}${sessionId}`
    const durationMap = JSON.parse(localStorage.getItem(key) || '{}')
    return messages.map(msg => {
      if (msg.role === 'assistant' && durationMap[msg.id] !== undefined) {
        return { ...msg, duration: durationMap[msg.id] }
      }
      return msg
    })
  } catch (e) {
    console.error('[Duration] 批量合并失败:', e)
    return messages
  }
}

function clearDurationsForSession(sessionId) {
  try {
    localStorage.removeItem(`${DURATION_STORAGE_PREFIX}${sessionId}`)
  } catch (e) {
    console.error('[Duration] 清理失败:', e)
  }
}

// 打字机效果
function startTypewriter() {
  charIndex = 0
  typewriterDisplay.value = ''
  if (typewriterTimer) clearInterval(typewriterTimer)

  typewriterTimer = setInterval(() => {
    if (charIndex < fullText.length) {
      typewriterDisplay.value += fullText[charIndex]
      charIndex++
    } else {
      setTimeout(() => {
        charIndex = 0
        typewriterDisplay.value = ''
      }, 2000)
    }
  }, 100)
}

function stopTypewriter() {
  if (typewriterTimer) {
    clearInterval(typewriterTimer)
    typewriterTimer = null
  }
  typewriterDisplay.value = ''
}

function startTimer() {
  waitingTime.value = 0
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    waitingTime.value++
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  waitingTime.value = 0
}

function cancelRequest() {
  if (abortController) {
    abortController.abort()
    ElMessage.info('已取消请求')
  }
}

// 回车发送（中文输入法拼字中不触发，避免误发）
function onEnter(e) {
  if (e && (e.isComposing || e.keyCode === 229)) return
  sendMessage()
}

async function loadSessions() {
  try { sessions.value = await api.get('/chat/sessions') } catch (e) {}
}

async function loadNutrition() {
  try {
    overview.value = await api.get('/diet/today')
  } catch (e) {}
  // 先取选项文案，再取档案，保证 dietGoalLabel 能翻译出正确中文
  try {
    const opts = await api.get('/user/options')
    const goals = opts?.diet_goal
    if (Array.isArray(goals) && goals.length) {
      goalLabels.value = Object.fromEntries(goals.map(o => [o.code, o.label]))
    }
  } catch (e) {}
  try {
    const profile = await api.get('/user/profile')
    dietGoalLabel.value = goalLabels.value[profile?.dietGoal] || profile?.dietGoal || '未设置'
  } catch (e) {}
  try {
    const stats = await api.get('/diet/stats?period=week')
    readDays.value = stats?.totalDays ?? null
  } catch (e) {}
}

async function createSession() {
  // 检查是否已存在最新的空会话（避免重复创建）
  if (sessions.value.length > 0 && currentSessionId.value === sessions.value[0].id && messages.value.length === 0) {
    return
  }

  const session = await api.post('/chat/sessions', { title: '新对话' })
  currentSessionId.value = session.id
  messages.value = []
  loadSessions()
}

async function loadSession(sessionId) {
  currentSessionId.value = sessionId
  const loadedMessages = await api.get(`/chat/sessions/${sessionId}/messages`)
  messages.value = mergeDurationsFromStorage(loadedMessages, sessionId)
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
    clearDurationsForSession(sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    loadSessions()
  } catch (e) {}
}

/**
 * 发送消息。
 * 有参调用时使用传入的文案（快捷问题/快捷操作），无参时使用输入框内容。
 */
async function sendMessage(overrideText) {
  const content = typeof overrideText === 'string' ? overrideText : inputText.value
  if (!content || !content.trim() || sending.value) return

  abortController = new AbortController()
  requestStartTime = Date.now()

  // 锁定这条问题属于哪个会话：后续 UI 状态和回复插入都以它为准，
  // 期间用户若切走，回复只能回到原会话，不能插进当前视图
  const targetSessionId = currentSessionId.value
  pendingSessionId.value = targetSessionId ?? NEW_SESSION

  sending.value = true
  inputText.value = ''

  messages.value.push({ id: Date.now(), role: 'user', content })
  await nextTick()
  scrollToBottom()

  startTypewriter()
  startTimer()

  try {
    const res = await api.post('/chat/send', {
      sessionId: targetSessionId,
      content
    }, {
      signal: abortController.signal
    })

    const duration = Math.round((Date.now() - requestStartTime) / 1000)
    const returnedSessionId = res.sessionId ?? targetSessionId

    // 原本是没 id 的新会话，拿到 id 后同步标记，这样中途切回来也能恢复思考态
    if (targetSessionId === null && returnedSessionId) {
      pendingSessionId.value = returnedSessionId
    }

    // 期间用户可能切到了别的会话 / 新建了会话 / 删除了原会话
    const stillHere = targetSessionId !== null
      ? currentSessionId.value === targetSessionId
      : (currentSessionId.value === null || currentSessionId.value === returnedSessionId)

    if (stillHere) {
      currentSessionId.value = returnedSessionId
      pendingSessionId.value = returnedSessionId
      messages.value.push({ ...res, duration })

      if (returnedSessionId && res.id) {
        saveDurationToStorage(returnedSessionId, res.id, duration)
      }

      await nextTick()
      scrollToBottom()
    } else {
      ElMessage.info('上一个问题的回复已生成，请回到原来的会话查看')
    }

    loadSessions()
  } catch (error) {
    if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
      console.log('请求已取消')
    } else {
      console.error('发送消息失败:', error)
    }
  } finally {
    sending.value = false
    pendingSessionId.value = null
    abortController = null
    requestStartTime = null
    stopTypewriter()
    stopTimer()
  }
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

onMounted(async () => {
  await Promise.all([loadSessions(), loadNutrition()])
  const sessionId = route.query.sessionId
  if (sessionId) {
    loadSession(Number(sessionId))
  } else if (sessions.value.length > 0) {
    loadSession(sessions.value[0].id)
  }
})

watch(() => route.query.sessionId, (newId) => {
  if (newId) loadSession(Number(newId))
})

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
.chat-container { height: calc(100vh - 70px); }

/* ============ 第一栏：会话列表 ============ */
.chat-sidebar { background: #fff; padding: 14px 12px; border-right: 1px solid #e4e7ed; overflow-y: auto; display: flex; flex-direction: column; }
.sidebar-brand { display: flex; align-items: center; gap: 8px; padding: 0 4px 14px; font-size: 15px; font-weight: 600; color: #173f63; }
.brand-icon { font-size: 18px; }
.new-chat-btn { width: 100%; margin-bottom: 14px; }
.session-list { flex: 1; overflow-y: auto; }
.session-item { padding: 10px 12px; cursor: pointer; border-radius: 6px; margin-bottom: 4px; font-size: 14px; color: #606266; display: flex; align-items: center; justify-content: space-between; }
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; color: #409eff; }
.session-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-delete { flex-shrink: 0; margin-left: 8px; color: #c0c4cc; cursor: pointer; font-size: 14px; }
.session-delete:hover { color: #f56c6c; }
.session-empty { text-align: center; color: #c0c4cc; font-size: 13px; padding: 20px 0; }

/* ============ 第二栏：对话区 ============ */
.chat-main { display: flex; flex-direction: column; padding: 0; background: #f7f9fc; }
.messages { flex: 1; overflow-y: auto; padding: 16px; }
.message { display: flex; margin-bottom: 16px; align-items: flex-start; gap: 8px; }
.message.assistant { flex-direction: row; }
.message.user { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.msg-avatar-ai { font-size: 28px; flex-shrink: 0; }
.msg-content { max-width: 70%; }
.msg-text { padding: 10px 14px; border-radius: 8px; line-height: 1.6; white-space: pre-wrap; }
.message.user .msg-text { background: #409eff; color: #fff; }
.message.assistant .msg-text { background: #fff; color: #333; }

/* 欢迎卡片 */
.welcome { display: flex; gap: 8px; margin-bottom: 16px; align-items: flex-start; }
.welcome-avatar { font-size: 28px; flex-shrink: 0; }
.welcome-body { background: #fff; border-radius: 8px; padding: 14px 16px; max-width: 78%; }
.welcome-text { line-height: 1.8; color: #333; margin-bottom: 12px; }
.welcome-context { font-size: 12px; color: #8ea1af; margin: -4px 0 12px; }
.welcome-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip { border: 1px solid #b5d4f4; background: #f2f8ff; color: #185fa5; border-radius: 999px; padding: 6px 14px; font-size: 13px; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.chip:hover:not(:disabled) { background: #2589ee; border-color: #2589ee; color: #fff; }
.chip:disabled { opacity: 0.55; cursor: not-allowed; }

/* AI回复消息样式（支持Markdown渲染） */
.ai-response { white-space: normal !important; }
.markdown-body { line-height: 1.7; word-wrap: break-word; }
.markdown-body :deep(p) { margin: 0 0 8px 0; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(strong) { font-weight: 600; color: #1f2328; }
.markdown-body :deep(em) { font-style: italic; color: #1f2328; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 8px 0; padding-left: 24px; }
.markdown-body :deep(li) { margin: 4px 0; line-height: 1.6; }
.markdown-body :deep(h1), .markdown-body :deep(h2),
.markdown-body :deep(h3), .markdown-body :deep(h4) { margin: 12px 0 8px 0; font-weight: 600; color: #1f2328; }
.markdown-body :deep(h1) { font-size: 1.3em; }
.markdown-body :deep(h2) { font-size: 1.2em; }
.markdown-body :deep(h3) { font-size: 1.1em; }
.markdown-body :deep(code) { background: #f6f8fa; padding: 2px 6px; border-radius: 4px; font-family: 'Monaco', 'Menlo', monospace; font-size: 0.9em; color: #e83e8c; }
.markdown-body :deep(pre) { background: #f6f8fa; padding: 12px; border-radius: 6px; overflow-x: auto; margin: 8px 0; }
.markdown-body :deep(pre code) { background: transparent; padding: 0; color: #24292e; }
.markdown-body :deep(blockquote) { border-left: 4px solid #dfe2e5; padding-left: 16px; margin: 8px 0; color: #6a737d; }
.markdown-body :deep(a) { color: #409eff; text-decoration: none; }
.markdown-body :deep(a:hover) { text-decoration: underline; }

/* AI回复耗时显示样式 */
.response-time { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #e4e7ed; font-size: 12px; color: #909399; text-align: right; }

/* 底部快捷操作 */
.quick-actions { display: flex; flex-wrap: wrap; gap: 8px; padding: 10px 16px 0; background: #f7f9fc; }
.qa-btn { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; border-radius: 8px; border: 1px solid #b5d4f4; background: #fff; color: #185fa5; font-size: 13px; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.qa-btn:hover:not(:disabled) { background: #eef7ff; border-color: #2589ee; }
.qa-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.qa-icon { font-size: 14px; }

.chat-input { padding: 12px 16px; border-top: 1px solid #e4e7ed; background: #fff; }

/* AI思考中提示样式 */
.message.thinking { opacity: 0.9; }
.thinking-text { color: #606266 !important; font-style: normal; background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%) !important; border: 1px solid #e4e7ed; }
.thinking-header { display: flex; align-items: center; gap: 2px; margin-bottom: 8px; font-size: 14px; color: #409eff; font-weight: 500; }
.typewriter-text { display: inline; }
.cursor-blink { display: inline-block; animation: cursorBlink 1s infinite; color: #409eff; font-weight: bold; }
@keyframes cursorBlink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }
.thinking-meta { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; padding-top: 8px; border-top: 1px dashed #dcdfe6; }
.waiting-time { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 4px; }
.cancel-btn { font-size: 12px; padding: 4px 12px; }

/* ============ 第三栏：今日营养数据 ============ */
.nutrition-panel { background: #fff; border-left: 1px solid #e4e7ed; padding: 16px 14px; overflow-y: auto; }
.np-title { font-size: 14px; font-weight: 600; color: #173f63; padding-bottom: 12px; margin-bottom: 14px; border-bottom: 1px solid #eef2f6; }
.np-item { margin-bottom: 18px; }
.np-label { font-size: 13px; color: #8ea1af; margin-bottom: 6px; }
.np-value { font-size: 13px; color: #55738d; margin-bottom: 8px; }
.np-value b { font-size: 16px; color: #173f63; font-weight: 600; }
.np-sep { margin: 0 4px; color: #c0c4cc; }
.np-goal { margin-top: 24px; padding-top: 14px; border-top: 1px solid #eef2f6; font-size: 13px; color: #8ea1af; }
.np-goal b { color: #2589ee; font-weight: 600; }
</style>

<style>
/* 第三栏进度条：去掉默认外边距，贴齐容器 */
.nutrition-panel .el-progress-bar__outer { background: #eef2f6; }

html.dark .chat-sidebar { background: #16213e; border-right-color: #2a3a5c; }
html.dark .sidebar-brand { color: #c8d6e5; }
html.dark .session-item { color: #8ea1af; }
html.dark .session-item:hover { background: #1e3a5f; }
html.dark .session-item.active { background: #1e3a5f; color: #74b9ff; }
html.dark .session-delete { color: #636e72; }
html.dark .session-delete:hover { color: #f56c6c; }
html.dark .session-empty { color: #636e72; }
html.dark .chat-main { background: #1a1a2e; }
html.dark .messages { background: #1a1a2e; }
html.dark .message.assistant .msg-text { background: #1a2744; color: #c8d6e5; }
html.dark .welcome-body { background: #1a2744; }
html.dark .welcome-text { color: #c8d6e5; }
html.dark .welcome-context { color: #636e72; }
html.dark .chip { background: #1e3a5f; border-color: #2a3a5c; color: #74b9ff; }
html.dark .chip:hover:not(:disabled) { background: #2589ee; border-color: #2589ee; color: #fff; }
html.dark .chat-input { background: #16213e; border-top-color: #2a3a5c; }
html.dark .quick-actions { background: #1a1a2e; }
html.dark .qa-btn { background: #16213e; border-color: #2a3a5c; color: #74b9ff; }
html.dark .qa-btn:hover:not(:disabled) { background: #1e3a5f; border-color: #74b9ff; }
html.dark .thinking-text { background: linear-gradient(135deg, #1a2744 0%, #16213e 100%) !important; border-color: #2a3a5c; color: #c8d6e5 !important; }
html.dark .thinking-header { color: #74b9ff; }
html.dark .cursor-blink { color: #74b9ff; }
html.dark .waiting-time { color: #636e72; }
html.dark .response-time { border-top-color: #2a3a5c; color: #636e72; }
html.dark .markdown-body :deep(strong) { color: #c8d6e5; }
html.dark .markdown-body :deep(em) { color: #c8d6e5; }
html.dark .markdown-body :deep(h1), html.dark .markdown-body :deep(h2),
html.dark .markdown-body :deep(h3), html.dark .markdown-body :deep(h4) { color: #c8d6e5; }
html.dark .markdown-body :deep(code) { background: #1a2744; color: #f78166; }
html.dark .markdown-body :deep(pre) { background: #1a2744; }
html.dark .markdown-body :deep(pre code) { color: #c8d6e5; }
html.dark .markdown-body :deep(blockquote) { border-left-color: #2a3a5c; color: #8ea1af; }
html.dark .markdown-body :deep(a) { color: #74b9ff; }
html.dark .nutrition-panel { background: #16213e; border-left-color: #2a3a5c; }
html.dark .np-title { color: #c8d6e5; border-bottom-color: #2a3a5c; }
html.dark .np-label { color: #636e72; }
html.dark .np-value { color: #8ea1af; }
html.dark .np-value b { color: #c8d6e5; }
html.dark .np-goal { color: #636e72; border-top-color: #2a3a5c; }
html.dark .nutrition-panel .el-progress-bar__outer { background: #2a3a5c; }
</style>
