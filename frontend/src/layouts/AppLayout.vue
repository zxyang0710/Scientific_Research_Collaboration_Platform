<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  BookOpen,
  ChevronDown,
  Files,
  FolderKanban,
  LayoutDashboard,
  ListTodo,
  LogOut,
  Menu,
  UserRound,
  X
} from 'lucide-vue-next'
import AppLogo from '../components/AppLogo.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notification'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()
const sidebarOpen = ref(false)
const profileOpen = ref(false)

const navItems = [
  { to: '/dashboard', label: '科研总览', icon: LayoutDashboard },
  { to: '/projects', label: '科研项目', icon: FolderKanban },
  { to: '/tasks', label: '科研任务', icon: ListTodo },
  { to: '/papers', label: '论文成果', icon: BookOpen },
  { to: '/files', label: '项目文件', icon: Files }
]

const pageTitle = computed(() => route.meta.title || '研协')

function closePanels() {
  sidebarOpen.value = false
  profileOpen.value = false
}

function logout() {
  auth.logout()
  router.replace('/login')
}

function handleDocumentClick() {
  profileOpen.value = false
}

function toggleProfile(event) {
  event.stopPropagation()
  profileOpen.value = !profileOpen.value
}

onMounted(() => {
  notifications.refreshUnread()
  document.addEventListener('click', handleDocumentClick)
})

onUnmounted(() => document.removeEventListener('click', handleDocumentClick))
</script>

<template>
  <div class="app-shell">
    <div v-if="sidebarOpen" class="mobile-overlay" @click="sidebarOpen = false" />
    <aside class="sidebar" :class="{ 'sidebar--open': sidebarOpen }">
      <div class="sidebar__head">
        <AppLogo light />
        <button class="sidebar__close" type="button" title="关闭导航" @click="sidebarOpen = false">
          <X :size="20" />
        </button>
      </div>

      <nav class="sidebar__nav">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ 'nav-link--active': route.path === item.to || (item.to !== '/dashboard' && route.path.startsWith(item.to)) }"
          @click="closePanels"
        >
          <component :is="item.icon" :size="18" :stroke-width="2" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar__foot">
        <div class="sidebar__hint">
          <span class="sidebar__hint-dot" />
          <div>
            <strong>协作空间运行中</strong>
            <small>数据已同步</small>
          </div>
        </div>
      </div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div class="topbar__left">
          <button class="topbar__menu" type="button" title="打开导航" @click="sidebarOpen = true">
            <Menu :size="21" />
          </button>
          <div>
            <div class="topbar__crumb">校园科研协作平台</div>
            <h1 class="topbar__title">{{ pageTitle }}</h1>
          </div>
        </div>

        <div class="topbar__right">
          <router-link to="/notifications" class="notification-button" title="消息通知">
            <Bell :size="20" />
            <span v-if="notifications.unreadCount" class="notification-badge">
              {{ notifications.unreadCount > 99 ? '99+' : notifications.unreadCount }}
            </span>
          </router-link>

          <div class="profile">
            <button class="profile__button" type="button" @click="toggleProfile">
              <UserAvatar :name="auth.user?.realName" :src="auth.user?.avatar" />
              <span class="profile__copy">
                <strong>{{ auth.user?.realName }}</strong>
                <small>{{ auth.user?.department || '校园科研用户' }}</small>
              </span>
              <ChevronDown :size="15" />
            </button>
            <div v-if="profileOpen" class="profile__menu" @click.stop>
              <div class="profile__menu-head">
                <UserRound :size="17" />
                <div>
                  <strong>{{ auth.user?.realName }}</strong>
                  <small>{{ auth.user?.identity || auth.user?.role }}</small>
                </div>
              </div>
              <button class="profile__menu-item" type="button" @click="logout">
                <LogOut :size="16" />
                退出登录
              </button>
            </div>
          </div>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  z-index: 100;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  background: var(--ink-950);
  color: white;
}

.sidebar__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 74px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar__close {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: rgba(255, 255, 255, 0.7);
  background: transparent;
  cursor: pointer;
}

.sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 20px 12px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 42px;
  padding: 0 12px;
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.62);
  font-weight: 600;
  transition: background 0.15s, color 0.15s;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.07);
  color: white;
}

.nav-link--active {
  background: rgba(255, 255, 255, 0.12);
  color: white;
}

.sidebar__foot {
  margin-top: auto;
  padding: 16px 14px 20px;
}

.sidebar__hint {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.04);
}

.sidebar__hint-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #63cfb9;
  box-shadow: 0 0 0 4px rgba(99, 207, 185, 0.12);
}

.sidebar__hint div {
  display: flex;
  flex-direction: column;
}

.sidebar__hint strong {
  font-size: 12px;
}

.sidebar__hint small {
  margin-top: 1px;
  color: rgba(255, 255, 255, 0.46);
  font-size: 10px;
}

.workspace {
  min-height: 100vh;
  margin-left: var(--sidebar-width);
}

.topbar {
  position: sticky;
  z-index: 50;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 74px;
  padding: 0 28px;
  border-bottom: 1px solid var(--ink-200);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
}

.topbar__left,
.topbar__right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.topbar__menu {
  display: none;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 6px;
  color: var(--ink-700);
  background: var(--ink-50);
  cursor: pointer;
}

.topbar__crumb {
  margin-bottom: 2px;
  color: var(--ink-500);
  font-size: 11px;
}

.topbar__title {
  color: var(--ink-950);
  font-size: 19px;
  line-height: 1.2;
}

.notification-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 6px;
  color: var(--ink-700);
}

.notification-button:hover {
  background: var(--ink-100);
}

.notification-badge {
  position: absolute;
  top: 2px;
  right: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border: 2px solid white;
  border-radius: 999px;
  background: var(--red-700);
  color: white;
  font-size: 9px;
  font-weight: 700;
  line-height: 14px;
  text-align: center;
}

.profile {
  position: relative;
}

.profile__button {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 7px 4px 5px;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-500);
  cursor: pointer;
}

.profile__button:hover {
  background: var(--ink-50);
}

.profile__copy {
  display: flex;
  flex-direction: column;
  min-width: 90px;
  max-width: 150px;
  text-align: left;
}

.profile__copy strong {
  color: var(--ink-900);
  font-size: 13px;
  line-height: 1.2;
}

.profile__copy small {
  margin-top: 2px;
  overflow: hidden;
  color: var(--ink-500);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile__menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 230px;
  padding: 7px;
  border: 1px solid var(--ink-200);
  border-radius: 8px;
  background: white;
  box-shadow: var(--shadow-md);
}

.profile__menu-head {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px;
  margin-bottom: 5px;
  border-bottom: 1px solid var(--ink-100);
  color: var(--ink-600);
}

.profile__menu-head div {
  display: flex;
  flex-direction: column;
}

.profile__menu-head strong {
  color: var(--ink-900);
  font-size: 13px;
}

.profile__menu-head small {
  color: var(--ink-500);
  font-size: 11px;
}

.profile__menu-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border-radius: 5px;
  background: transparent;
  color: var(--red-700);
  cursor: pointer;
}

.profile__menu-item:hover {
  background: var(--red-100);
}

.content {
  padding: 24px 28px 40px;
}

.mobile-overlay {
  display: none;
}

@media (max-width: 900px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.22s ease;
  }

  .sidebar--open {
    transform: translateX(0);
  }

  .sidebar__close {
    display: inline-flex;
  }

  .workspace {
    margin-left: 0;
  }

  .topbar {
    padding: 0 16px;
  }

  .topbar__menu {
    display: inline-flex;
  }

  .mobile-overlay {
    position: fixed;
    z-index: 90;
    inset: 0;
    display: block;
    background: rgba(18, 36, 44, 0.42);
  }

  .content {
    padding: 20px 16px 34px;
  }
}

@media (max-width: 560px) {
  .topbar__crumb,
  .profile__copy,
  .profile__button > svg {
    display: none;
  }

  .topbar__right {
    gap: 5px;
  }

  .topbar__title {
    font-size: 16px;
  }
}
</style>
