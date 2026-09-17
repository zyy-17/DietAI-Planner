<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-left">
        <span class="logo">🍎 智慧膳食</span>
        <el-menu mode="horizontal" :default-active="currentRoute" router class="nav-menu">
          <el-menu-item index="/today">今日饮食</el-menu-item>
          <el-menu-item index="/records">饮食记录</el-menu-item>
          <el-menu-item index="/nutrition">营养分析</el-menu-item>
          <el-menu-item index="/chat">AI对话</el-menu-item>
          <el-menu-item index="/foods">食物库</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="28" :src="userStore.avatarUrl || undefined" :icon="UserFilled" />
            {{ userStore.username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="userStore.role === 'admin'" command="admin">管理后台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentRoute = computed(() => route.path)

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
  } else if (command === 'admin') {
    router.push('/admin')
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 0 24px;
  height: 60px;
}
.header-left {
  display: flex;
  align-items: center;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  margin-right: 32px;
  white-space: nowrap;
}
.nav-menu {
  border-bottom: none;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
}
.main-content {
  padding: 20px;
  background: #f5f7fa;
}
</style>