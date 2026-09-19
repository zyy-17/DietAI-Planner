<template>
  <div class="settings-page">
    <div class="page-tabs">
      <div v-for="tab in tabs" :key="tab.path"
        class="tab-item" :class="{ active: isTabActive(tab) }"
        @click="router.push(tab.path)">
        {{ tab.icon }} {{ tab.name }}
      </div>
    </div>

    <el-card style="max-width:650px;margin:0 auto" v-loading="loading">
      <template #header>
        <span>⚙️ {{ sectionTitle }}</span>
      </template>

      <div v-if="section === 'default'">
        <el-form label-width="100px">
          <el-form-item label="饮食提醒">
            <el-switch v-model="form.dietReminder" />
            <span class="hint">每日定时提醒记录饮食</span>
          </el-form-item>
          <el-form-item label="提醒时间">
            <el-time-picker v-model="form.reminderTime" format="HH:mm" placeholder="选择提醒时间" :disabled="!form.dietReminder" />
          </el-form-item>
          <el-form-item label="目标提醒">
            <el-switch v-model="form.goalReminder" />
            <span class="hint">营养目标未达成时提醒</span>
          </el-form-item>
          <el-form-item label="AI建议推送">
            <el-switch v-model="form.aiSuggestion" />
            <span class="hint">每日推送AI膳食建议</span>
          </el-form-item>
        </el-form>
      </div>

      <div v-else-if="section === 'appearance'">
        <el-form label-width="100px">
          <el-form-item label="主题模式">
            <el-radio-group v-model="form.theme">
              <el-radio value="light">浅色</el-radio>
              <el-radio value="dark">深色</el-radio>
              <el-radio value="auto">跟随系统</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="语言">
            <el-select v-model="form.language" style="width:200px">
              <el-option value="zh-CN" label="简体中文" />
              <el-option value="en-US" label="English" />
            </el-select>
          </el-form-item>
          <el-form-item label="侧边栏">
            <el-switch v-model="form.collapsedSidebar" active-text="收起" inactive-text="展开" />
          </el-form-item>
        </el-form>
      </div>

      <div v-else-if="section === 'privacy'">
        <el-form label-width="100px">
          <el-form-item label="数据共享">
            <el-switch v-model="form.dataSharing" />
            <span class="hint">允许匿名数据用于改善AI建议</span>
          </el-form-item>
          <el-form-item label="饮食记录公开">
            <el-switch v-model="form.publicRecords" />
            <span class="hint">其他用户可查看您的饮食记录</span>
          </el-form-item>
        </el-form>
      </div>

      <div v-else-if="section === 'password'">
        <el-form :model="passwordForm" label-width="100px">
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
        </el-form>
      </div>

      <div style="text-align:right;margin-top:20px">
        <el-button type="primary" @click="saveSettings" :loading="saving">保存设置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const section = computed(() => route.meta.section || 'default')

const tabs = [
  { name: '通知设置', path: '/settings', icon: '🔔' },
  { name: '界面设置', path: '/settings/appearance', icon: '🎨' },
  { name: '隐私设置', path: '/settings/privacy', icon: '🔐' },
  { name: '修改密码', path: '/settings/password', icon: '🔑' }
]

function isTabActive(tab) {
  return route.path === tab.path
}

const sectionTitles = {
  default: '通知设置',
  appearance: '界面设置',
  privacy: '隐私设置',
  password: '修改密码'
}
const sectionTitle = computed(() => sectionTitles[section.value] || '系统设置')

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  dietReminder: true,
  reminderTime: null,
  goalReminder: true,
  aiSuggestion: true,
  theme: 'light',
  language: 'zh-CN',
  collapsedSidebar: false,
  dataSharing: false,
  publicRecords: false
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

function parseReminderTime(timeStr) {
  if (!timeStr) return null
  const [h, m] = timeStr.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m, 0, 0)
  return d
}

function formatReminderTime(date) {
  if (!date) return null
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

async function loadSettings() {
  loading.value = true
  try {
    const data = await api.get('/user/settings')
    if (data) {
      form.dietReminder = data.dietReminder ?? true
      form.reminderTime = parseReminderTime(data.reminderTime)
      form.goalReminder = data.goalReminder ?? true
      form.aiSuggestion = data.aiSuggestion ?? true
      form.theme = data.theme || 'light'
      form.language = data.language || 'zh-CN'
      form.collapsedSidebar = data.collapsedSidebar ?? false
      form.dataSharing = data.dataSharing ?? false
      form.publicRecords = data.publicRecords ?? false
    }
  } catch (e) {
    console.warn('加载设置失败，使用默认值')
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    if (section.value === 'password') {
      if (!passwordForm.oldPassword || !passwordForm.newPassword) {
        return ElMessage.warning('请填写完整密码信息')
      }
      if (passwordForm.newPassword.length < 6) {
        return ElMessage.warning('新密码长度不能少于6位')
      }
      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        return ElMessage.warning('两次密码输入不一致')
      }
      await api.put('/user/password', passwordForm)
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      return
    }

    const payload = {
      dietReminder: form.dietReminder,
      reminderTime: formatReminderTime(form.reminderTime),
      goalReminder: form.goalReminder,
      aiSuggestion: form.aiSuggestion,
      theme: form.theme,
      language: form.language,
      collapsedSidebar: form.collapsedSidebar,
      dataSharing: form.dataSharing,
      publicRecords: form.publicRecords
    }
    await api.put('/user/settings', payload)
    if (payload.theme) {
      userStore.setTheme(payload.theme)
    }
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.settings-page { padding: 18px; }
.page-tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 2px solid #eef2f6; padding-bottom: 0; }
.tab-item { padding: 8px 16px; font-size: 13px; color: #55738d; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; white-space: nowrap; }
.tab-item:hover { color: #2789ed; }
.tab-item.active { color: #2589ee; border-bottom-color: #2589ee; font-weight: 600; }
.hint { font-size: 12px; color: #8ea1af; margin-left: 10px; }
</style>