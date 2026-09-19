<template>
  <div class="app">
    <header class="header">
      <div class="logo">🌿 <b>智慧膳食</b></div>
      <nav>
        <span v-for="m in topMenus" :key="m.key"
          :class="{ active: activeTopModule === m.key }"
          @click="router.push(m.path)">
          {{ m.icon }} {{ m.name }}
        </span>
      </nav>
      <div class="user-area">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            🔔
            <el-avatar :size="28" :src="userStore.avatarUrl || undefined" :icon="UserFilled" />
            {{ userStore.username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">👤 个人中心</el-dropdown-item>
              <el-dropdown-item command="settings">⚙️ 系统设置</el-dropdown-item>
              <el-dropdown-item v-if="userStore.role === 'admin'" command="admin">🛠️ 管理后台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>🚪 退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="layout">
      <aside>
        <div class="side-section">
          <div class="section-title">{{ currentModuleTitle }}</div>
          <div v-for="m in currentSideMenus" :key="m.path"
            class="side-item"
            :class="{ selected: isSideActive(m.path) }"
            @click="router.push(m.path)">
            {{ m.icon }}　{{ m.name }}
          </div>
        </div>

        <div class="side-aux">
          <div v-for="item in auxLinks" :key="item.path"
            class="side-item aux-item"
            :class="{ selected: isAuxActive(item.key) }"
            @click="router.push(item.path)">
            {{ item.icon }}　{{ item.name }}
          </div>
        </div>

        <div class="side-bottom">
          健康饮食<br>从今天开始 🌱
        </div>
      </aside>

      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const topMenus = [
  { key: 'today', name: '今日饮食', icon: '🏠', path: '/today' },
  { key: 'records', name: '饮食记录', icon: '📋', path: '/records' },
  { key: 'nutrition', name: '营养分析', icon: '📊', path: '/nutrition' },
  { key: 'chat', name: 'AI对话', icon: '🤖', path: '/chat' }
]

const sideMenusMap = {
  today: [
    { name: '今日概览', path: '/today', icon: '🏠' },
    { name: '早餐', path: '/today/breakfast', icon: '🍳' },
    { name: '午餐', path: '/today/lunch', icon: '🥗' },
    { name: '晚餐', path: '/today/dinner', icon: '🍗' },
    { name: '加餐', path: '/today/snack', icon: '🍎' },
    { name: 'AI今日建议', path: '/today/ai-suggest', icon: '🤖' }
  ],
  records: [
    { name: '历史记录', path: '/records', icon: '📅' },
    { name: '周记录', path: '/records/week', icon: '📆' },
    { name: '月记录', path: '/records/month', icon: '🗓️' },
    { name: '条件查询', path: '/records/search', icon: '🔍' },
    { name: '饮食统计', path: '/records/stats', icon: '📊' }
  ],
  nutrition: [
    { name: '今日营养', path: '/nutrition', icon: '📊' },
    { name: '营养趋势', path: '/nutrition/trend', icon: '📈' },
    { name: '营养素分析', path: '/nutrition/nutrients', icon: '🥩' },
    { name: '热量分析', path: '/nutrition/calorie', icon: '🔥' },
    { name: '目标完成度', path: '/nutrition/goal', icon: '🎯' },
    { name: '营养报告', path: '/nutrition/report', icon: '📄' }
  ],
  chat: [
    { name: '新建对话', path: '/chat', icon: '💬' },
    { name: '膳食规划', path: '/chat/diet-plan', icon: '🥗' },
    { name: '减脂方案', path: '/chat/fat-loss', icon: '🔥' },
    { name: '增肌方案', path: '/chat/muscle-gain', icon: '💪' },
    { name: '饮食咨询', path: '/chat/consult', icon: '🍎' }
  ]
}

const moduleTitleMap = {
  today: '🏠 今日饮食',
  records: '📋 饮食记录',
  nutrition: '📊 营养分析',
  chat: '🤖 AI对话'
}

const auxLinks = [
  { key: 'foods', name: '食物库', icon: '🍱', path: '/foods' },
  { key: 'profile', name: '个人中心', icon: '👤', path: '/profile' },
  { key: 'settings', name: '系统设置', icon: '⚙️', path: '/settings' }
]

const activeTopModule = computed(() => {
  const path = route.path
  if (path.startsWith('/today')) return 'today'
  if (path.startsWith('/records')) return 'records'
  if (path.startsWith('/nutrition')) return 'nutrition'
  if (path.startsWith('/chat')) return 'chat'
  return 'today'
})

const currentModuleTitle = computed(() => moduleTitleMap[activeTopModule.value] || '🏠 今日饮食')

const currentSideMenus = computed(() => sideMenusMap[activeTopModule.value] || [])

function isSideActive(path) {
  return route.path === path
}

function isAuxActive(key) {
  const path = route.path
  if (key === 'foods') return path.startsWith('/foods')
  if (key === 'profile') return path.startsWith('/profile')
  if (key === 'settings') return path.startsWith('/settings')
  return false
}

onMounted(() => {
  if (userStore.token && !userStore.avatarUrl) {
    userStore.fetchProfile()
  }
})

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'admin') {
    router.push('/admin')
  }
}
</script>

<style scoped>
.app { min-height: 100vh; background: #f4f7fa; color: #244b6b; font-family: "Microsoft YaHei", Arial, sans-serif; }
.header { height: 70px; background: #fff; border-bottom: 1px solid #e8edf2; display: flex; align-items: center; padding: 0 28px; }
.logo { width: 190px; font-size: 22px; color: #173f63; white-space: nowrap; }
.logo b { color: #2589ee; }
nav { height: 100%; display: flex; }
nav span { min-width: 105px; height: 100%; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 14px; color: #55738d; transition: all 0.2s; }
nav span:hover { color: #2789ed; }
nav span.active { color: #2589ee; background: #eef7ff; border-bottom: 3px solid #3292f3; }
.user-area { margin-left: auto; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #55738d; font-size: 14px; }
.layout { display: flex; }
aside { width: 200px; min-height: calc(100vh - 70px); background: #fff; padding: 16px 10px; position: relative; border-right: 1px solid #e8edf2; display: flex; flex-direction: column; overflow-y: auto; }

.side-section { margin-bottom: 12px; }
.section-title { font-size: 12px; color: #8ea1af; padding: 0 15px 8px; font-weight: 600; letter-spacing: 1px; }

.side-item { height: 42px; border-radius: 8px; padding: 0 15px; display: flex; align-items: center; color: #55738d; cursor: pointer; margin-bottom: 4px; font-size: 13px; transition: all 0.2s; white-space: nowrap; }
.side-item:hover { background: #edf7ff; color: #2789ed; }
.side-item.selected { background: #e6f3ff; color: #2589ee; font-weight: 600; }

.side-aux { margin-top: auto; padding-top: 12px; border-top: 1px solid #eef2f6; }
.aux-item { height: 38px; font-size: 13px; }

.side-bottom { padding: 16px 15px 8px; color: #69beb6; line-height: 1.8; font-size: 12px; }
.main-content { flex: 1; overflow-y: auto; }

@media (max-width: 800px) {
  aside { width: 65px; padding: 10px 6px; }
  .side-item { justify-content: center; padding: 0; font-size: 0; }
  .section-title, .side-bottom { display: none; }
  .logo { width: 100px; }
  nav span { min-width: 75px; font-size: 12px; }
}
</style>