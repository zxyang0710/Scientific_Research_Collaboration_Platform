<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  BellRing,
  BookOpen,
  CheckCheck,
  FileText,
  FolderKanban,
  MessageSquareText,
  Trash2
} from 'lucide-vue-next'
import { notificationApi } from '../api'
import { ElMessage } from '../utils/message'
import { useNotificationStore } from '../stores/notification'
import { formatDateTime } from '../utils/format'

const router = useRouter()
const notificationStore = useNotificationStore()
const loading = ref(true)
const notifications = ref([])
const unreadOnly = ref(false)

const icons = {
  TASK: CheckCheck,
  PROJECT: FolderKanban,
  PAPER: BookOpen,
  FILE: FileText,
  DISCUSSION: MessageSquareText,
  SYSTEM: Bell
}

async function load() {
  loading.value = true
  try {
    const result = await notificationApi.list({ page: 1, size: 100, unreadOnly: unreadOnly.value || undefined })
    notifications.value = result.records
    notificationStore.refreshUnread()
  } finally {
    loading.value = false
  }
}

async function open(item) {
  if (!item.isRead) {
    await notificationApi.markRead(item.id)
    item.isRead = 1
    notificationStore.refreshUnread()
  }
  if (item.link && item.link !== '/notifications') {
    router.push(item.link)
  }
}

async function markAll() {
  await notificationApi.markAllRead()
  ElMessage.success('全部消息已标记为已读')
  load()
}

async function remove(item) {
  await notificationApi.remove(item.id)
  ElMessage.success('消息已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page notification-page">
    <header class="page-header">
      <div>
        <h2 class="page-title">消息通知</h2>
        <p class="page-subtitle">集中查看任务分配、项目变更、论文登记和讨论动态。</p>
      </div>
      <button class="btn btn-secondary" type="button" @click="markAll">
        <CheckCheck :size="16" />
        全部标为已读
      </button>
    </header>

    <section class="surface notification-panel">
      <div class="notification-toolbar">
        <div class="segmented">
          <button type="button" :class="{ active: !unreadOnly }" @click="unreadOnly = false; load()">全部消息</button>
          <button type="button" :class="{ active: unreadOnly }" @click="unreadOnly = true; load()">
            仅未读
            <span v-if="notificationStore.unreadCount">（{{ notificationStore.unreadCount }}）</span>
          </button>
        </div>
        <span>{{ notifications.length }} 条消息</span>
      </div>

      <div v-if="loading" class="loading-state">
        <span class="spinner" />
        正在加载消息...
      </div>

      <div v-else-if="notifications.length" class="notification-list">
        <article
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.isRead }"
          @click="open(item)"
        >
          <span class="notification-item__icon">
            <component :is="icons[item.type] || Bell" :size="18" />
          </span>
          <div class="notification-item__copy">
            <div class="notification-item__title">
              <strong>{{ item.title }}</strong>
              <span v-if="!item.isRead" class="unread-dot" />
            </div>
            <p>{{ item.content }}</p>
            <small>{{ formatDateTime(item.createdAt) }} · {{ item.type }}</small>
          </div>
          <button class="icon-btn danger" type="button" title="删除消息" @click.stop="remove(item)">
            <Trash2 :size="15" />
          </button>
        </article>
      </div>

      <div v-else class="empty-state">
        <BellRing :size="38" />
        <strong>{{ unreadOnly ? '没有未读消息' : '暂无消息通知' }}</strong>
        <span>协作动态会及时出现在这里</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.notification-panel {
  width: min(900px, 100%);
}

.notification-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--ink-100);
}

.notification-toolbar > span {
  color: var(--ink-500);
  font-size: 11px;
}

.notification-list {
  padding: 0 16px 6px;
}

.notification-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 5px;
  border-bottom: 1px solid var(--ink-100);
  cursor: pointer;
}

.notification-item:last-child {
  border-bottom: 0;
}

.notification-item.unread {
  background: #f7fbfa;
}

.notification-item:hover .notification-item__title strong {
  color: var(--teal-700);
}

.notification-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border-radius: 7px;
  background: var(--ink-100);
  color: var(--ink-700);
}

.notification-item.unread .notification-item__icon {
  background: var(--teal-100);
  color: var(--teal-700);
}

.notification-item__copy {
  flex: 1;
  min-width: 0;
}

.notification-item__title {
  display: flex;
  align-items: center;
  gap: 7px;
}

.notification-item__title strong {
  color: var(--ink-900);
  font-size: 13px;
  transition: color 0.15s;
}

.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--red-700);
}

.notification-item__copy p {
  margin-top: 5px;
  color: var(--ink-600);
  font-size: 12px;
  line-height: 1.65;
}

.notification-item__copy small {
  display: block;
  margin-top: 7px;
  color: var(--ink-500);
  font-size: 9px;
}

.icon-btn.danger:hover {
  background: var(--red-100);
  color: var(--red-700);
}

@media (max-width: 560px) {
  .notification-toolbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .segmented {
    width: 100%;
  }

  .segmented button {
    flex: 1;
  }
}
</style>
