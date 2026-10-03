import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('research_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('research_user') || 'null'))
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))

  function persist() {
    if (token.value) localStorage.setItem('research_token', token.value)
    else localStorage.removeItem('research_token')
    if (user.value) localStorage.setItem('research_user', JSON.stringify(user.value))
    else localStorage.removeItem('research_user')
  }

  async function login(credentials) {
    loading.value = true
    try {
      const result = await authApi.login(credentials)
      token.value = result.token
      user.value = result.user
      persist()
      return result
    } finally {
      loading.value = false
    }
  }

  async function register(payload) {
    loading.value = true
    try {
      const result = await authApi.register(payload)
      token.value = result.token
      user.value = result.user
      persist()
      return result
    } finally {
      loading.value = false
    }
  }

  async function refreshProfile() {
    if (!token.value) return null
    const profile = await authApi.me()
    user.value = profile
    persist()
    return profile
  }

  function logout() {
    token.value = ''
    user.value = null
    persist()
  }

  return { token, user, loading, isAuthenticated, login, register, refreshProfile, logout }
})
