import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from '../api'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function refreshUnread() {
    try {
      unreadCount.value = await notificationApi.unreadCount()
    } catch {
      unreadCount.value = 0
    }
  }

  return { unreadCount, refreshUnread }
})
