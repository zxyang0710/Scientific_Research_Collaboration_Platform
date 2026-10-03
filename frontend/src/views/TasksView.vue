<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CalendarClock,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  CircleDashed,
  Edit3,
  ListTodo,
  Search
} from 'lucide-vue-next'
import { projectApi, taskApi } from '../api'
import StatusBadge from '../components/StatusBadge.vue'
import TaskFormModal from '../components/TaskFormModal.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { useAuthStore } from '../stores/auth'
import { formatDate, priorityMap, taskStatusMap } from '../utils/format'

const auth = useAuthStore()
const loading = ref(true)
const tasks = ref([])
const projects = ref([])
const total = ref(0)
const pages = ref(0)
const selectedProject = ref(null)
const modalOpen = ref(false)
const editingTask = ref(null)
const members = ref([])
const filters = reactive({
  projectId: '',
  status: '',
  priority: '',
  mine: false,
  keyword: '',
  page: 1,
  size: 12
})

const filteredTasks = computed(() => {
  if (!filters.keyword) return tasks.value
  const keyword = filters.keyword.toLowerCase()
  return tasks.value.filter((task) =>
    task.title.toLowerCase().includes(keyword)
    || task.projectName.toLowerCase().includes(keyword)
    || (task.assigneeName || '').toLowerCase().includes(keyword)
  )
})

const columns = [
  { key: 'TODO', label: '待处理', icon: CircleDashed },
  { key: 'IN_PROGRESS', label: '进行中', icon: CalendarClock },
  { key: 'BLOCKED', label: '已阻塞', icon: ListTodo },
  { key: 'DONE', label: '已完成', icon: CheckCircle2 }
]

async function load() {
  loading.value = true
  try {
    const [taskResult, projectResult] = await Promise.all([
      taskApi.list({
        projectId: filters.projectId || undefined,
        status: filters.status || undefined,
        priority: filters.priority || undefined,
        mine: filters.mine || undefined,
        page: filters.page,
        size: filters.size
      }),
      projectApi.list({ page: 1, size: 100 })
    ])
    tasks.value = taskResult.records
    total.value = taskResult.total
    pages.value = taskResult.pages
    projects.value = projectResult.records
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

function canEditTask(task) {
  if (auth.user?.role === 'ADMIN') return true
  const project = projects.value.find((item) => Number(item.id) === Number(task.projectId))
  return project?.leaderId === auth.user?.id
    || project?.projectRole === 'LEADER'
    || project?.projectRole === 'MANAGER'
}

async function openCreate(task = null) {
  editingTask.value = task
  const projectId = task?.projectId || filters.projectId || projects.value[0]?.id
  if (!projectId) return
  const detail = await projectApi.detail(projectId)
  members.value = detail.members
  selectedProject.value = detail.project
  modalOpen.value = true
}

async function changeProject(value) {
  filters.projectId = value
  filters.page = 1
  await load()
}

async function changeStatus(task, status) {
  await taskApi.updateStatus(task.id, { status })
  load()
}

async function remove(task) {
  if (!window.confirm(`确定删除任务“${task.title}”吗？`)) return
  await taskApi.remove(task.id)
  load()
}

function columnTasks(status) {
  return filteredTasks.value.filter((task) => task.status === status)
}

function changePage(page) {
  filters.page = page
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">科研任务</h2>
        <p class="page-subtitle">统一查看项目任务、个人待办和推进状态，保持协作节奏。</p>
      </div>
    </header>

    <section class="surface task-toolbar">
      <div class="input-icon">
        <Search :size="17" />
        <input v-model.trim="filters.keyword" class="input" placeholder="在当前结果中查找任务、项目或负责人" />
      </div>
      <select :value="filters.projectId" class="select" @change="changeProject($event.target.value)">
        <option value="">全部项目</option>
        <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.name }}</option>
      </select>
      <select v-model="filters.status" class="select" @change="search">
        <option value="">全部状态</option>
        <option value="TODO">待处理</option>
        <option value="IN_PROGRESS">进行中</option>
        <option value="BLOCKED">已阻塞</option>
        <option value="DONE">已完成</option>
      </select>
      <select v-model="filters.priority" class="select" @change="search">
        <option value="">全部优先级</option>
        <option value="HIGH">高优先级</option>
        <option value="MEDIUM">中优先级</option>
        <option value="LOW">低优先级</option>
      </select>
      <label class="mine-toggle">
        <input v-model="filters.mine" type="checkbox" @change="search" />
        只看我的任务
      </label>
    </section>

    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在加载任务...
    </div>

    <template v-else>
      <section class="kanban">
        <div v-for="column in columns" :key="column.key" class="kanban-column">
          <header class="kanban-column__head">
            <div>
              <component :is="column.icon" :size="17" />
              <strong>{{ column.label }}</strong>
            </div>
            <span>{{ columnTasks(column.key).length }}</span>
          </header>
          <div class="kanban-column__body">
            <article
              v-for="task in columnTasks(column.key)"
              :key="task.id"
              class="task-card surface"
            >
              <div class="task-card__head">
                <StatusBadge :value="task.priority" :map="priorityMap" />
                <button
                  v-if="canEditTask(task)"
                  class="icon-btn task-card__edit"
                  type="button"
                  title="编辑任务"
                  @click.stop="openCreate(task)"
                >
                  <Edit3 :size="14" />
                </button>
              </div>
              <h3>{{ task.title }}</h3>
              <p>{{ task.projectName }}</p>
              <div class="task-card__foot">
                <div class="user-cell">
                  <UserAvatar :name="task.assigneeName" :src="task.assigneeAvatar" size="sm" />
                  <span>{{ task.assigneeName }}</span>
                </div>
                <span :class="{ 'text-danger': task.deadline && new Date(task.deadline) < new Date() && task.status !== 'DONE' }">
                  {{ formatDate(task.deadline) }}
                </span>
              </div>
              <div class="task-card__actions">
                <select :value="task.status" class="select" @change="changeStatus(task, $event.target.value)">
                  <option value="TODO">待处理</option>
                  <option value="IN_PROGRESS">进行中</option>
                  <option value="BLOCKED">已阻塞</option>
                  <option value="DONE">已完成</option>
                </select>
              </div>
            </article>
            <div v-if="!columnTasks(column.key).length" class="kanban-empty">
              当前没有{{ column.label }}任务
            </div>
          </div>
        </div>
      </section>

      <div v-if="pages > 1" class="pagination surface">
        <span class="pagination-info">共 {{ total }} 项任务，第 {{ filters.page }}/{{ pages }} 页</span>
        <button class="icon-btn" type="button" title="上一页" :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">
          <ChevronLeft :size="17" />
        </button>
        <button class="icon-btn" type="button" title="下一页" :disabled="filters.page >= pages" @click="changePage(filters.page + 1)">
          <ChevronRight :size="17" />
        </button>
      </div>
    </template>

    <TaskFormModal
      v-if="modalOpen"
      :task="editingTask"
      :project-id="editingTask?.projectId || filters.projectId || projects[0]?.id"
      :members="members"
      @close="modalOpen = false"
      @saved="modalOpen = false; load()"
    />
  </div>
</template>

<style scoped>
.task-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 210px 145px 145px auto;
  gap: 10px;
  padding: 14px;
  margin-bottom: 16px;
}

.mine-toggle {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--ink-200);
  border-radius: var(--radius-sm);
  background: white;
  color: var(--ink-700);
  white-space: nowrap;
  cursor: pointer;
  font-size: 12px;
}

.mine-toggle input {
  accent-color: var(--teal-700);
}

.kanban {
  display: grid;
  grid-template-columns: repeat(4, minmax(230px, 1fr));
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 5px;
}

.kanban-column {
  min-width: 230px;
  border-radius: 8px;
  background: #e9eeed;
}

.kanban-column__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 13px;
  color: var(--ink-700);
}

.kanban-column__head > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kanban-column__head strong {
  font-size: 12px;
}

.kanban-column__head > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 23px;
  height: 23px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--ink-600);
  font-size: 10px;
  font-weight: 700;
}

.kanban-column__body {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 350px;
  padding: 0 9px 10px;
}

.task-card {
  padding: 13px;
  cursor: default;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.task-card:hover {
  border-color: var(--ink-300);
  box-shadow: 0 5px 18px rgba(18, 36, 44, 0.08);
}

.task-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.task-card__edit {
  padding: 2px 0;
  background: transparent;
  color: var(--ink-500);
  cursor: pointer;
  font-size: 10px;
}

.task-card__edit:hover {
  color: var(--teal-700);
}

.task-card h3 {
  margin-top: 11px;
  color: var(--ink-900);
  font-size: 13px;
  line-height: 1.55;
}

.task-card > p {
  margin-top: 4px;
  overflow: hidden;
  color: var(--ink-500);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 13px;
  padding-top: 11px;
  border-top: 1px solid var(--ink-100);
}

.task-card__foot .user-cell {
  min-width: 0;
  font-size: 10px;
}

.task-card__foot .user-cell span {
  max-width: 75px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card__foot > span {
  color: var(--ink-500);
  font-size: 9px;
  white-space: nowrap;
}

.task-card__actions {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 5px;
  margin-top: 11px;
}

.task-card__actions .select {
  height: 31px;
  font-size: 10px;
}

.task-card__actions .btn {
  height: 31px;
  font-size: 10px;
}

.kanban-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 96px;
  padding: 15px;
  border: 1px dashed var(--ink-300);
  border-radius: 7px;
  color: var(--ink-500);
  font-size: 10px;
  text-align: center;
}

.pagination {
  margin-top: 15px;
}

@media (max-width: 1200px) {
  .task-toolbar {
    grid-template-columns: minmax(220px, 1fr) repeat(2, 145px);
  }

  .mine-toggle {
    justify-content: center;
  }
}

@media (max-width: 760px) {
  .task-toolbar {
    grid-template-columns: 1fr;
  }

  .kanban {
    grid-template-columns: repeat(4, 265px);
    margin-right: -16px;
    padding-right: 16px;
  }
}
</style>
