<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  FolderKanban,
  Plus,
  Search,
  UsersRound
} from 'lucide-vue-next'
import { projectApi } from '../api'
import ProjectFormModal from '../components/ProjectFormModal.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate, projectStatusMap } from '../utils/format'

const router = useRouter()
const loading = ref(true)
const projects = ref([])
const total = ref(0)
const pages = ref(0)
const modalOpen = ref(false)
const filters = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 9
})

async function load() {
  loading.value = true
  try {
    const result = await projectApi.list(filters)
    projects.value = result.records
    total.value = result.total
    pages.value = result.pages
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

function changePage(page) {
  if (page < 1 || page > pages.value || page === filters.page) return
  filters.page = page
  load()
}

function openCreated(project) {
  modalOpen.value = false
  router.push(`/projects/${project.id}`)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">科研项目</h2>
        <p class="page-subtitle">查看您参与的全部科研项目，进入项目后可进行任务、文件与讨论协作。</p>
      </div>
      <button class="btn btn-primary" type="button" @click="modalOpen = true">
        <Plus :size="17" />
        创建项目
      </button>
    </header>

    <section class="surface project-filter">
      <div class="input-icon">
        <Search :size="17" />
        <input
          v-model.trim="filters.keyword"
          class="input"
          placeholder="搜索项目名称或编号"
          @keyup.enter="search"
        />
      </div>
      <select v-model="filters.status" class="select filter-status" @change="search">
        <option value="">全部状态</option>
        <option value="PLANNING">筹备中</option>
        <option value="IN_PROGRESS">进行中</option>
        <option value="PAUSED">已暂停</option>
        <option value="COMPLETED">已结项</option>
      </select>
      <button class="btn btn-secondary" type="button" @click="search">查询</button>
    </section>

    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在加载项目...
    </div>

    <template v-else>
      <section v-if="projects.length" class="project-grid">
        <article
          v-for="project in projects"
          :key="project.id"
          class="project-card surface"
          @click="router.push(`/projects/${project.id}`)"
        >
          <div class="project-card__head">
            <span class="project-card__category">{{ project.category || '综合科研' }}</span>
            <StatusBadge :value="project.status" :map="projectStatusMap" />
          </div>
          <h3>{{ project.name }}</h3>
          <p class="project-card__code">{{ project.code }} · {{ project.source || '项目来源未填写' }}</p>
          <p class="project-card__desc line-clamp-2">{{ project.description || '暂无项目简介' }}</p>

          <div class="project-card__progress">
            <div class="project-card__progress-copy">
              <span>推进进度</span>
              <strong>{{ project.progress || 0 }}%</strong>
            </div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: `${project.progress || 0}%` }" />
            </div>
          </div>

          <div class="project-card__meta">
            <span><UsersRound :size="14" /> {{ project.memberCount }} 位成员</span>
            <span><FolderKanban :size="14" /> {{ project.completedTaskCount }}/{{ project.taskCount }} 任务</span>
            <span><CalendarDays :size="14" /> {{ formatDate(project.endDate) }}</span>
          </div>
        </article>
      </section>

      <section v-else class="surface empty-state">
        <FolderKanban :size="38" />
        <strong>没有找到符合条件的项目</strong>
        <span>调整筛选条件，或创建新的科研项目</span>
        <button class="btn btn-primary" type="button" style="margin-top: 16px" @click="modalOpen = true">
          <Plus :size="16" />
          创建项目
        </button>
      </section>

      <div v-if="pages > 1" class="pagination project-pagination">
        <span class="pagination-info">共 {{ total }} 个项目，第 {{ filters.page }}/{{ pages }} 页</span>
        <button class="icon-btn" type="button" title="上一页" :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">
          <ChevronLeft :size="17" />
        </button>
        <button class="icon-btn" type="button" title="下一页" :disabled="filters.page >= pages" @click="changePage(filters.page + 1)">
          <ChevronRight :size="17" />
        </button>
      </div>
    </template>

    <ProjectFormModal v-if="modalOpen" @close="modalOpen = false" @saved="openCreated" />
  </div>
</template>

<style scoped>
.project-filter {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px auto;
  gap: 10px;
  padding: 14px;
  margin-bottom: 16px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.project-card {
  position: relative;
  min-height: 284px;
  padding: 18px;
  cursor: pointer;
  transition: border-color 0.16s, box-shadow 0.16s, transform 0.16s;
}

.project-card:hover {
  border-color: var(--ink-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.project-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.project-card__category {
  color: var(--teal-700);
  font-size: 11px;
  font-weight: 700;
}

.project-card h3 {
  min-height: 48px;
  margin-top: 14px;
  color: var(--ink-950);
  font-size: 16px;
  line-height: 1.5;
}

.project-card__code {
  margin-top: 5px;
  color: var(--ink-500);
  font-size: 11px;
}

.project-card__desc {
  min-height: 40px;
  margin-top: 12px;
  color: var(--ink-600);
  font-size: 12px;
  line-height: 1.65;
}

.project-card__progress {
  margin-top: 18px;
}

.project-card__progress-copy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
  color: var(--ink-500);
  font-size: 11px;
}

.project-card__progress-copy strong {
  color: var(--ink-800);
}

.project-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--ink-100);
  color: var(--ink-500);
  font-size: 10px;
}

.project-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.project-pagination {
  margin-top: 16px;
  border: 1px solid var(--ink-200);
  border-radius: var(--radius-md);
  background: white;
}

@media (max-width: 1100px) {
  .project-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .project-filter {
    grid-template-columns: 1fr;
  }

  .filter-status {
    width: 100%;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
