import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'

const LoginView = () => import('../views/LoginView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const ProjectsView = () => import('../views/ProjectsView.vue')
const ProjectDetailView = () => import('../views/ProjectDetailView.vue')
const TasksView = () => import('../views/TasksView.vue')
const PapersView = () => import('../views/PapersView.vue')
const FilesView = () => import('../views/FilesView.vue')
const NotificationsView = () => import('../views/NotificationsView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '科研总览' } },
        { path: 'projects', name: 'projects', component: ProjectsView, meta: { title: '科研项目' } },
        { path: 'projects/:id', name: 'project-detail', component: ProjectDetailView, meta: { title: '项目详情' } },
        { path: 'tasks', name: 'tasks', component: TasksView, meta: { title: '科研任务' } },
        { path: 'papers', name: 'papers', component: PapersView, meta: { title: '论文成果' } },
        { path: 'files', name: 'files', component: FilesView, meta: { title: '项目文件' } },
        { path: 'notifications', name: 'notifications', component: NotificationsView, meta: { title: '消息通知' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('research_token')
  if (!to.meta.public && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && token) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
