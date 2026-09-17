import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../utils/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')

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
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('avatarUrl')
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

  return { token, username, role, userId, avatarUrl, setLogin, setAvatar, logout, fetchProfile }
})