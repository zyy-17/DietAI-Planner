import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../utils/api'

function applyTheme(theme) {
  if (theme === 'dark') {
    document.documentElement.classList.add('dark')
  } else if (theme === 'auto') {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    document.documentElement.classList.toggle('dark', prefersDark)
  } else {
    document.documentElement.classList.remove('dark')
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')
  const theme = ref(localStorage.getItem('theme') || 'light')

  function setTheme(t) {
    theme.value = t
    localStorage.setItem('theme', t)
    applyTheme(t)
  }

  function setLogin(data) {
    token.value = data.token
    username.value = data.username
    role.value = data.role
    userId.value = data.userId
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    localStorage.setItem('userId', data.userId)
    fetchProfile()
    fetchAndApplyTheme()
  }

  function setAvatar(url) {
    avatarUrl.value = url
    localStorage.setItem('avatarUrl', url)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''
    avatarUrl.value = ''
    theme.value = 'light'
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('avatarUrl')
    localStorage.removeItem('theme')
    applyTheme('light')
  }

  async function fetchProfile() {
    try {
      const data = await api.get('/user/profile')
      if (data) {
        if (data.username) username.value = data.username
        if (data.role) role.value = data.role
        if (data.avatarUrl) {
          avatarUrl.value = data.avatarUrl
          localStorage.setItem('avatarUrl', data.avatarUrl)
        }
      }
      return data
    } catch (e) {
      return null
    }
  }

  async function fetchAndApplyTheme() {
    try {
      const data = await api.get('/user/settings')
      if (data && data.theme) {
        setTheme(data.theme)
      }
    } catch (e) {}
  }

  return { token, username, role, userId, avatarUrl, theme, setLogin, setAvatar, setTheme, logout, fetchProfile, fetchAndApplyTheme }
})