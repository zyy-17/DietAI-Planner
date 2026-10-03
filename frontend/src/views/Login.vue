<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">🍎 智慧膳食</h1>
      <p class="subtitle">记录每一餐，管理每一天</p>

      <!-- 身份选择：登录 / 注册 共用 -->
      <div class="identity-block">
        <div class="identity-label">{{ isRegister ? '注册身份' : '登录身份' }}</div>
        <el-segmented v-model="identity" :options="identityOptions" block size="large" />
        <p class="identity-tip">
          <template v-if="identity === 'admin'">🛡️ 管理员可管理用户、食物库与营养标准</template>
          <template v-else>👤 记录饮食、查看营养分析与 AI 膳食建议</template>
        </p>
      </div>

      <el-form v-if="!isRegister" ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="用户名 / 邮箱" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width:100%" @click="handleLogin">登 录</el-button>
      </el-form>

      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" @submit.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱（选填）" prefix-icon="Message" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码（6位以上）" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width:100%" @click="handleRegister">注 册</el-button>
      </el-form>

      <p class="switch-text">
        <span v-if="!isRegister">没有账号？<el-link type="primary" @click="switchMode(true)">立即注册</el-link></span>
        <span v-else>已有账号？<el-link type="primary" @click="switchMode(false)">返回登录</el-link></span>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const isRegister = ref(false)

/** 当前选择的身份：user-普通用户 / admin-管理员 */
const identity = ref('user')
const identityOptions = [
  { label: '👤 普通用户', value: 'user' },
  { label: '🛡️ 管理员', value: 'admin' }
]

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', email: '', password: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '至少6位', trigger: 'blur' }
  ]
}

const loginFormRef = ref()
const registerFormRef = ref()

// 切换身份后清掉上一个身份的校验提示
watch(identity, () => {
  nextTick(() => {
    loginFormRef.value?.clearValidate()
    registerFormRef.value?.clearValidate()
  })
})

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const data = await api.post('/auth/login', { ...loginForm, role: identity.value })
    userStore.setLogin(data)
    ElMessage.success(identity.value === 'admin' ? '管理员登录成功' : '登录成功')
    redirectAfterLogin(data.role)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    const data = await api.post('/auth/register', { ...registerForm, role: identity.value })
    userStore.setLogin(data)
    ElMessage.success(data.role === 'admin' ? '管理员账号注册成功' : '注册成功')
    redirectAfterLogin(data.role)
  } finally {
    loading.value = false
  }
}

/** 登录 / 注册模式切换，顺带清掉校验状态 */
function switchMode(toRegister) {
  isRegister.value = toRegister
  nextTick(() => {
    loginFormRef.value?.clearValidate()
    registerFormRef.value?.clearValidate()
  })
}

/** 管理员登录后直接进入管理后台，普通用户进入用户端首页 */
function redirectAfterLogin(role) {
  if (role === 'admin') {
    router.push('/admin/dashboard')
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.title {
  text-align: center;
  font-size: 28px;
  color: #409eff;
  margin-bottom: 8px;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 24px;
}
.identity-block {
  margin-bottom: 20px;
}
.identity-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.identity-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.switch-text {
  text-align: center;
  margin-top: 16px;
  color: #909399;
}
</style>
