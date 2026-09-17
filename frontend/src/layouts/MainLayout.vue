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
          <div class="aux-group" v-for="group in auxGroups" :key="group.key">
            <div class="aux-title" @click="toggleGroup(group.key)">
              {{ group.icon }} {{ group.name }}
              <span class="aux-arrow" :class="{ open: expandedGroups[group.key] }">›</span>
            </div>
            <transition name="slide">
              <div v-show="expandedGroups[group.key]" class="aux-items">
                <div v-for="m in group.children" :key="m.path"
                  class="side-item sub"
                  :class="{ selected: isSideActive(m.path) }"
                  @click="router.push(m.path)">
                  {{ m.icon }}　{{ m.name }}
                </div>
              </div>
            </transition>
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
import { computed, onMounted, reactive } from 'vue'
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
    { name: '饮食咨询', path: '/chat/consult', icon: '🍎' },
    { name: '历史对话', path: '/chat/history', icon: '🕘' }
  ]
}

const moduleTitleMap = {
  today: '🏠 今日饮食',
  records: '📋 饮食记录',
  nutrition: '📊 营养分析',
  chat: '🤖 AI对话'
}

const auxGroups = [
  {
    key: 'foods',
    name: '食物库',
    icon: '🍱',
    children: [
      { name: '全部食物', path: '/foods', icon: '🍎' },
      { name: '食物搜索', path: '/foods/search', icon: '🔍' },
      { name: '食物分类', path: '/foods/categories', icon: '🥩' },
      { name: '我的收藏', path: '/foods/favorites', icon: '⭐' },
      { name: '添加食物', path: '/foods/add', icon: '➕' }
    ]
  },
  {
    key: 'profile',
    name: '个人中心',
    icon: '👤',
    children: [
      { name: '基本资料', path: '/profile', icon: '👤' },
      { name: '身体数据', path: '/profile/body', icon: '📏' },
      { name: '饮食目标', path: '/profile/goal', icon: '🎯' },
      { name: '饮食偏好', path: '/profile/preference', icon: '🥗' },
      { name: '忌口设置', path: '/profile/allergy', icon: '🚫' },
      { name: '活动水平', path: '/profile/activity', icon: '🏃' },
      { name: '健康信息', path: '/profile/health', icon: '❤️' }
    ]
  },
  {
    key: 'settings',
    name: '系统设置',
    icon: '⚙️',
    children: [
      { name: '通知设置', path: '/settings', icon: '🔔' },
      { name: '界面设置', path: '/settings/appearance', icon: '🎨' },
      { name: '隐私设置', path: '/settings/privacy', icon: '🔐' },
      { name: '修改密码', path: '/settings/password', icon: '🔑' }
    ]
  }
]

const expandedGroups = reactive({
  foods: false,
  profile: false,
  settings: false
})

const activeTopModule = computed(() => {
  const path = route.path
  if (path.startsWith('/today')) return 'today'
  if (path.startsWith('/records')) return 'records'
  if (path.startsWith('/nutrition')) return 'nutrition'
  if (path.startsWith('/chat')) return 'chat'
  if (path.startsWith('/foods')) return 'today'
  if (path.startsWith('/profile')) return 'today'
  if (path.startsWith('/settings')) return 'today'
  return 'today'
})

const currentModuleTitle = computed(() => moduleTitleMap[activeTopModule.value] || '🏠 今日饮食')

const currentSideMenus = computed(() => sideMenusMap[activeTopModule.value] || [])

function isSideActive(path) {
  return route.path === path
}

function toggleGroup(key) {
  expandedGroups[key] = !expandedGroups[key]
}

onMounted(() => {
  if (userStore.token && !userStore.avatarUrl) {
    userStore.fetchProfile()
  }
  const path = route.path
  auxGroups.forEach(g => {
    if (g.children.some(c => path === c.path || path.startsWith(c.path + '/'))) {
      expandedGroups[g.key] = true
    }
  })
})

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    expandedGroups.profile = true
    router.push('/profile')
  } else if (command === 'settings') {
    expandedGroups.settings = true
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
.side-item.sub { height: 36px; font-size: 12px; padding-left: 22px; }

.side-aux { margin-top: auto; padding-top: 12px; border-top: 1px solid #eef2f6; }
.aux-group { margin-bottom: 2px; }
.aux-title { display: flex; align-items: center; justify-content: space-between; padding: 8px 15px; font-size: 13px; color: #55738d; cursor: pointer; border-radius: 8px; transition: all 0.2s; }
.aux-title:hover { background: #f5f9fc; color: #2789ed; }
.aux-arrow { font-size: 16px; transition: transform 0.2s; display: inline-block; }
.aux-arrow.open { transform: rotate(90deg); }
.aux-items { overflow: hidden; }

.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; }
.slide-enter-to, .slide-leave-from { opacity: 1; max-height: 500px; }

.side-bottom { padding: 16px 15px 8px; color: #69beb6; line-height: 1.8; font-size: 12px; }
.main-content { flex: 1; overflow-y: auto; }

@media (max-width: 800px) {
  aside { width: 65px; padding: 10px 6px; }
  .side-item { justify-content: center; padding: 0; font-size: 0; }
  .side-item.sub { padding-left: 0; }
  .section-title, .aux-title, .side-bottom, .aux-arrow { display: none; }
  .aux-items { display: flex; flex-direction: column; }
  .logo { width: 100px; }
  nav span { min-width: 75px; font-size: 12px; }
}
</style>