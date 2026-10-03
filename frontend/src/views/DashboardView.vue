<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertTriangle,
  ArrowRight,
  BookOpen,
  CalendarClock,
  CheckCircle2,
  FolderKanban,
  ListTodo,
  MessageSquareText
} from 'lucide-vue-next'
import { dashboardApi } from '../api'
import EChart from '../components/EChart.vue'
import StatusBadge from '../components/StatusBadge.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notification'
import { formatDate, priorityMap, projectStatusMap, taskStatusMap } from '../utils/format'

const router = useRouter()
const auth = useAuthStore()
const notificationStore = useNotificationStore()
const loading = ref(true)
const dashboard = ref({
  projectCount: 0,
  activeProjectCount: 0,
  taskCount: 0,
  completedTaskCount: 0,
  overdueTaskCount: 0,
  paperCount: 0,
  unreadNotificationCount: 0,
  recentProjects: [],
  myTasks: [],
  activities: [],
  taskDistribution: [],
  projectProgress: []
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const statCards = computed(() => [
  { label: '参与项目', value: dashboard.value.projectCount, note: `${dashboard.value.activeProjectCount} 个进行中`, icon: FolderKanban, tone: 'teal' },
  { label: '协作任务', value: dashboard.value.taskCount, note: `${dashboard.value.completedTaskCount} 个已完成`, icon: ListTodo, tone: 'blue' },
  { label: '逾期任务', value: dashboard.value.overdueTaskCount, note: dashboard.value.overdueTaskCount ? '需要尽快处理' : '当前无逾期', icon: AlertTriangle, tone: dashboard.value.overdueTaskCount ? 'red' : 'slate' },
  { label: '论文成果', value: dashboard.value.paperCount, note: `${dashboard.value.unreadNotificationCount} 条未读消息`, icon: BookOpen, tone: 'amber' }
])

const taskChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  color: ['#78929a', '#15817c', '#315b9a', '#aa3e3e'],
  legend: { bottom: 0, icon: 'circle', textStyle: { color: '#5a7a85', fontSize: 11 } },
  series: [{
    type: 'pie',
    radius: ['48%', '70%'],
    center: ['50%', '43%'],
    avoidLabelOverlap: true,
    itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 3 },
    label: { show: false },
    data: dashboard.value.taskDistribution.map((item) => ({ name: item.name, value: item.value }))
  }]
}))

const projectChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { top: 10, right: 20, bottom: 14, left: 118 },
  xAxis: {
    type: 'value',
    max: 100,
    axisLabel: { formatter: '{value}%', color: '#78929a', fontSize: 10 },
    splitLine: { lineStyle: { color: '#e9f0f1' } }
  },
  yAxis: {
    type: 'category',
    inverse: true,
    data: dashboard.value.projectProgress.map((item) => item.name),
    axisLabel: { width: 104, overflow: 'truncate', color: '#3b6270', fontSize: 11 },
    axisLine: { show: false },
    axisTick: { show: false }
  },
  series: [{
    type: 'bar',
    barWidth: 13,
    data: dashboard.value.projectProgress.map((item) => item.value),
    itemStyle: { color: '#15817c', borderRadius: [0, 4, 4, 0] },
    label: { show: true, position: 'right', formatter: '{c}%', color: '#17323c', fontSize: 10 }
  }]
}))

const activityIcon = {
  DISCUSSION: MessageSquareText,
  PAPER: BookOpen,
  FILE: FolderKanban
}

async function load() {
  loading.value = true
  try {
    dashboard.value = await dashboardApi.get()
    notificationStore.unreadCount = dashboard.value.unreadNotificationCount
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page dashboard-page">
    <header class="dashboard-hero">
      <div>
        <p class="dashboard-hero__eyebrow">RESEARCH OVERVIEW</p>
        <h2>{{ greeting }}，{{ auth.user?.realName }}</h2>
        <p>这里是您参与的科研协作进展，优先关注临近截止与逾期任务。</p>
      </div>
      <button class="btn btn-primary" type="button" @click="router.push('/projects')">
        查看科研项目
        <ArrowRight :size="16" />
      </button>
    </header>

    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在汇总科研数据...
    </div>

    <template v-else>
      <section class="grid-4 stats-grid">
        <article v-for="card in statCards" :key="card.label" class="stat-card surface">
          <div class="stat-card__top">
            <span>本轮统计</span>
            <span class="stat-card__icon" :class="`stat-card__icon--${card.tone}`">
              <component :is="card.icon" :size="18" />
            </span>
          </div>
          <strong class="stat-card__value">{{ card.value }}</strong>
          <div class="stat-card__foot">
            <span>{{ card.label }}</span>
            <small :class="{ 'text-danger': card.tone === 'red' }">{{ card.note }}</small>
          </div>
        </article>
      </section>

      <section class="dashboard-main">
        <div class="surface recent-projects">
          <div class="surface-header">
            <h3 class="surface-title">近期项目</h3>
            <button class="btn btn-ghost" type="button" @click="router.push('/projects')">
              全部项目 <ArrowRight :size="15" />
            </button>
          </div>
          <div v-if="dashboard.recentProjects.length" class="project-list">
            <article
              v-for="project in dashboard.recentProjects"
              :key="project.id"
              class="project-row"
              @click="router.push(`/projects/${project.id}`)"
            >
              <div class="project-row__head">
                <div class="project-row__title">
                  <strong>{{ project.name }}</strong>
                  <span>{{ project.code }} · {{ project.category || '未分类' }}</span>
                </div>
                <StatusBadge :value="project.status" :map="projectStatusMap" />
              </div>
              <div class="project-row__progress">
                <div class="progress-track">
                  <div class="progress-fill" :style="{ width: `${project.progress || 0}%` }" />
                </div>
                <span>{{ project.progress || 0 }}%</span>
              </div>
              <div class="project-row__meta">
                <span>负责人 {{ project.leaderName }}</span>
                <span>{{ project.memberCount }} 位成员</span>
                <span>{{ project.completedTaskCount }}/{{ project.taskCount }} 项任务完成</span>
                <span>截止 {{ formatDate(project.endDate) }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-state">
            <FolderKanban :size="34" />
            <strong>还没有参与项目</strong>
            <span>进入项目列表创建科研项目</span>
          </div>
        </div>

        <div class="surface my-tasks">
          <div class="surface-header">
            <h3 class="surface-title">我的待办</h3>
            <button class="btn btn-ghost" type="button" @click="router.push('/tasks')">
              任务中心 <ArrowRight :size="15" />
            </button>
          </div>
          <div v-if="dashboard.myTasks.length" class="task-list">
            <article
              v-for="task in dashboard.myTasks"
              :key="task.id"
              class="task-row"
              @click="router.push('/tasks')"
            >
              <span class="task-row__check" :class="{ done: task.status === 'DONE' }">
                <CheckCircle2 :size="17" />
              </span>
              <div class="task-row__copy">
                <strong>{{ task.title }}</strong>
                <span>{{ task.projectName }}</span>
              </div>
              <div class="task-row__aside">
                <StatusBadge :value="task.priority" :map="priorityMap" />
                <small :class="{ 'text-danger': task.deadline && new Date(task.deadline) < new Date() }">
                  {{ formatDate(task.deadline) }}
                </small>
              </div>
            </article>
          </div>
          <div v-else class="empty-state">
            <CheckCircle2 :size="34" />
            <strong>当前没有待办</strong>
            <span>已分配任务会显示在这里</span>
          </div>
        </div>
      </section>

      <section class="dashboard-charts">
        <div class="surface">
          <div class="surface-header">
            <h3 class="surface-title">任务状态分布</h3>
          </div>
          <div class="surface-body">
            <EChart :option="taskChartOption" height="250px" />
          </div>
        </div>
        <div class="surface">
          <div class="surface-header">
            <h3 class="surface-title">项目推进度</h3>
          </div>
          <div class="surface-body">
            <EChart :option="projectChartOption" height="250px" />
          </div>
        </div>
        <div class="surface activity-panel">
          <div class="surface-header">
            <h3 class="surface-title">最新动态</h3>
          </div>
          <div class="activity-list">
            <article v-for="(activity, index) in dashboard.activities" :key="`${activity.type}-${index}`" class="activity-row">
              <span class="activity-row__icon">
                <component :is="activityIcon[activity.type] || CalendarClock" :size="16" />
              </span>
              <div>
                <strong>{{ activity.title }}</strong>
                <p>{{ activity.description }}</p>
                <small>{{ activity.actorName }} · {{ formatDate(activity.date) }}</small>
              </div>
            </article>
            <div v-if="!dashboard.activities.length" class="empty-state">
              <MessageSquareText :size="32" />
              <strong>暂无协作动态</strong>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 25px;
  margin: -24px -28px 24px;
  padding: 28px 28px 30px;
  background: var(--ink-950);
  color: white;
}

.dashboard-hero__eyebrow {
  margin-bottom: 8px;
  color: #73cfc2;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.13em;
}

.dashboard-hero h2 {
  font-size: 26px;
}

.dashboard-hero p:last-child {
  margin-top: 7px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
}

.dashboard-hero .btn-primary {
  background: white;
  color: var(--ink-950);
}

.stats-grid {
  margin-bottom: 16px;
}

.stat-card {
  padding: 16px 17px 17px;
}

.stat-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--ink-500);
  font-size: 11px;
}

.stat-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
}

.stat-card__icon--teal { background: var(--teal-100); color: var(--teal-700); }
.stat-card__icon--blue { background: var(--blue-100); color: var(--blue-700); }
.stat-card__icon--amber { background: var(--amber-100); color: var(--amber-700); }
.stat-card__icon--red { background: var(--red-100); color: var(--red-700); }
.stat-card__icon--slate { background: var(--ink-100); color: var(--ink-700); }

.stat-card__value {
  display: block;
  margin: 13px 0 8px;
  color: var(--ink-950);
  font-size: 30px;
  line-height: 1;
}

.stat-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stat-card__foot > span {
  color: var(--ink-700);
  font-weight: 650;
}

.stat-card__foot small {
  color: var(--ink-500);
  font-size: 10px;
}

.dashboard-main {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(300px, 0.85fr);
  gap: 16px;
  margin-bottom: 16px;
}

.project-list {
  padding: 0 18px 8px;
}

.project-row {
  padding: 15px 2px;
  border-bottom: 1px solid var(--ink-100);
  cursor: pointer;
}

.project-row:last-child {
  border-bottom: 0;
}

.project-row:hover .project-row__title strong {
  color: var(--teal-700);
}

.project-row__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 15px;
}

.project-row__title {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.project-row__title strong {
  overflow: hidden;
  color: var(--ink-900);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.15s;
}

.project-row__title span {
  margin-top: 3px;
  color: var(--ink-500);
  font-size: 11px;
}

.project-row__progress {
  display: grid;
  grid-template-columns: 1fr 36px;
  align-items: center;
  gap: 10px;
  margin-top: 13px;
}

.project-row__progress span {
  color: var(--ink-700);
  font-size: 11px;
  font-weight: 700;
  text-align: right;
}

.project-row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 10px;
  color: var(--ink-500);
  font-size: 10px;
}

.task-list {
  padding: 0 16px 6px;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 2px;
  border-bottom: 1px solid var(--ink-100);
  cursor: pointer;
}

.task-row:last-child {
  border-bottom: 0;
}

.task-row__check {
  color: var(--ink-300);
}

.task-row__check.done {
  color: var(--teal-600);
}

.task-row__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.task-row__copy strong {
  overflow: hidden;
  color: var(--ink-900);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-row__copy span {
  margin-top: 2px;
  overflow: hidden;
  color: var(--ink-500);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-row__aside {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 4px;
}

.task-row__aside small {
  color: var(--ink-500);
  font-size: 9px;
}

.dashboard-charts {
  display: grid;
  grid-template-columns: minmax(250px, 0.75fr) minmax(320px, 1fr) minmax(320px, 1.05fr);
  gap: 16px;
}

.activity-list {
  max-height: 320px;
  padding: 4px 16px 8px;
  overflow-y: auto;
}

.activity-row {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--ink-100);
}

.activity-row:last-child {
  border-bottom: 0;
}

.activity-row__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 29px;
  height: 29px;
  border-radius: 6px;
  background: var(--ink-100);
  color: var(--ink-700);
}

.activity-row div {
  min-width: 0;
}

.activity-row strong {
  display: block;
  color: var(--ink-800);
  font-size: 11px;
}

.activity-row p {
  display: -webkit-box;
  margin-top: 2px;
  overflow: hidden;
  color: var(--ink-600);
  font-size: 11px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.activity-row small {
  display: block;
  margin-top: 3px;
  color: var(--ink-500);
  font-size: 9px;
}

@media (max-width: 1250px) {
  .dashboard-charts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .activity-panel {
    grid-column: span 2;
  }
}

@media (max-width: 1000px) {
  .dashboard-main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .dashboard-hero {
    margin: -20px -16px 20px;
    padding: 24px 16px;
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-charts {
    grid-template-columns: 1fr;
  }

  .activity-panel {
    grid-column: span 1;
  }

  .dashboard-hero h2 {
    font-size: 22px;
  }
}
</style>
