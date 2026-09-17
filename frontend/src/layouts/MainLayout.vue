    <template>
  <div class="app">
    <header class="header">
      <div class="logo">🌿 <b>智慧膳食</b></div>
      <nav>
        <span v-for="m in topMenus" :key="m.path"
          :class="{ active: currentRoute === m.path }"
          @click="router.push(m.path)">
          {{ m.name }}
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
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="userStore.role === 'admin'" command="admin">管理后台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="layout">
      <aside>
        <div v-for="m in sideMenus" :key="m.path"
          class="side-item"
          :class="{ selected: currentRoute === m.path }"
          @click="router.push(m.path)">
          {{ m.icon }}　{{ m.name }}
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

const currentRoute = computed(() => route.path)

const topMenus = [
  { name: '今日饮食', path: '/today' },
  { name: '饮食记录', path: '/records' },
  { name: '营养分析', path: '/nutrition' },
  { name: 'AI对话', path: '/chat' }
]

const sideMenus = [
  { name: '今日饮食', path: '/today', icon: '🏠' },
  { name: '饮食记录', path: '/records', icon: '📋' },
  { name: '营养分析', path: '/nutrition', icon: '📊' },
  { name: 'AI对话', path: '/chat', icon: '🤖' },
  { name: '食物库', path: '/foods', icon: '🍎' },
  { name: '个人中心', path: '/profile', icon: '👤' }
]

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
aside { width: 190px; min-height: calc(100vh - 70px); background: #fff; padding: 20px 12px; position: relative; border-right: 1px solid #e8edf2; }
.side-item { height: 50px; border-radius: 10px; padding: 0 15px; display: flex; align-items: center; color: #55738d; cursor: pointer; margin-bottom: 8px; font-size: 14px; transition: all 0.2s; }
.side-item:hover, .side-item.selected { background: #edf7ff; color: #2789ed; }
.side-bottom { position: absolute; bottom: 35px; left: 25px; color: #69beb6; line-height: 1.8; font-size: 13px; }
.main-content { flex: 1; overflow-y: auto; }

@media (max-width: 800px) {
  aside { width: 65px; }
  .side-item { justify-content: center; padding: 0; font-size: 0; }
  .logo { width: 100px; }
  nav span { min-width: 75px; font-size: 12px; }
}
</style>