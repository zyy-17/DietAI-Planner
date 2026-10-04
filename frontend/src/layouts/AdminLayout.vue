<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="admin-logo">🛠️ 管理后台</div>
      <el-menu :default-active="currentRoute" router class="admin-menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <span class="menu-icon">{{ item.icon }}</span>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div class="admin-user">👤 {{ userStore.username || '未登录' }}</div>
        <el-button text class="footer-btn" @click="$router.push('/')">← 返回用户端</el-button>
        <el-button text class="footer-btn" @click="handleLogout">退出登录</el-button>
      </div>
    </el-aside>
    <el-main class="admin-main">
      <div class="admin-header">
        <span class="page-title">{{ pageTitle }}</span>
        <span class="header-time">{{ today }}</span>
      </div>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { path: '/admin/dashboard', icon: '📊', title: '数据看板' },
  { path: '/admin/users', icon: '👥', title: '用户管理' },
  { path: '/admin/foods', icon: '🍎', title: '食物管理' },
  { path: '/admin/categories', icon: '🗂️', title: '分类管理' },
  { path: '/admin/diet-records', icon: '🍽️', title: '饮食记录' },
  { path: '/admin/nutrition-standards', icon: '📐', title: '营养标准' },
  { path: '/admin/profile-options', icon: '📋', title: '档案选项' },
  { path: '/admin/ai-logs', icon: '🤖', title: 'AI记录' }
]

const currentRoute = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '管理后台')
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}
.aside {
  background: #304156;
  color: #fff;
  position: relative;
  padding-bottom: 130px;
  box-sizing: border-box;
}
.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  border-bottom: 1px solid #263445;
}
.admin-menu {
  border-right: none;
  background: #304156;
}
.admin-menu .el-menu-item {
  color: #bfcbd9;
}
.admin-menu .el-menu-item:hover {
  background: #263445;
  color: #fff;
}
.admin-menu .el-menu-item.is-active {
  background: #263445;
  color: #409eff;
}
.menu-icon {
  margin-right: 10px;
  font-size: 16px;
}
.aside-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 12px;
  box-sizing: border-box;
  border-top: 1px solid #263445;
}
.admin-user {
  color: #bfcbd9;
  font-size: 13px;
  margin-bottom: 8px;
  padding-left: 8px;
}
.footer-btn {
  color: #bfcbd9;
  width: 100%;
  justify-content: flex-start;
}
.footer-btn:hover {
  color: #409eff;
}
.admin-main {
  background: #f5f7fa;
  padding: 16px 20px;
}
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #244b6b;
}
.header-time {
  color: #909399;
  font-size: 13px;
}
</style>
