import axios from 'axios'
import { ElMessage } from '../utils/message'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('research_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code !== 200) {
        ElMessage.error(payload.message || '请求失败')
        return Promise.reject(new Error(payload.message || '请求失败'))
      }
      return payload.data
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    if (status === 401) {
      localStorage.removeItem('research_token')
      localStorage.removeItem('research_user')
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
    }
    ElMessage.error(message || error.message || '网络请求失败')
    return Promise.reject(error)
  }
)

export default http
