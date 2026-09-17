<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="admin-logo">🔧 管理后台</div>
      <el-menu :default-active="currentRoute" router class="admin-menu">
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/foods">
          <el-icon><Bowl /></el-icon>
          <span>食物管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Menu /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/ai-logs">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI记录</span>
        </el-menu-item>
      </el-menu>
      <div class="back-to-user">
        <el-button text @click="$router.push('/')">← 返回用户端</el-button>
      </div>
    </el-aside>
    <el-main class="admin-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentRoute = computed(() => route.path)
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}
.aside {
  background: #304156;
  color: #fff;
}
.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}
.admin-menu {
  border-right: none;
  background: #304156;
}
.admin-menu .el-menu-item {
  color: #bfcbd9;
}
.admin-menu .el-menu-item.is-active {
  background: #263445;
  color: #409eff;
}
.back-to-user {
  position: absolute;
  bottom: 20px;
  width: 100%;
  text-align: center;
}
.admin-main {
  background: #f5f7fa;
}
</style>