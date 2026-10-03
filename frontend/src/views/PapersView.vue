<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  BookOpen,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Plus,
  Search
} from 'lucide-vue-next'
import { paperApi, projectApi } from '../api'
import PaperFormModal from '../components/PaperFormModal.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { ElMessage } from '../utils/message'
import { formatDate, paperStatusMap } from '../utils/format'

const loading = ref(true)
const papers = ref([])
const projects = ref([])
const total = ref(0)
const pages = ref(0)
const modalOpen = ref(false)
const filters = reactive({
  projectId: '',
  status: '',
  keyword: '',
  page: 1,
  size: 10
})

async function load() {
  loading.value = true
  try {
    const [paperResult, projectResult] = await Promise.all([
      paperApi.list({
        projectId: filters.projectId || undefined,
        status: filters.status || undefined,
        keyword: filters.keyword || undefined,
        page: filters.page,
        size: filters.size
      }),
      projectApi.list({ page: 1, size: 100 })
    ])
    papers.value = paperResult.records
    total.value = paperResult.total
    pages.value = paperResult.pages
    projects.value = projectResult.records
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

async function remove(paper) {
  if (!window.confirm(`确定删除论文“${paper.title}”吗？`)) return
  await paperApi.remove(paper.id)
  ElMessage.success('论文已删除')
  load()
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
        <h2 class="page-title">论文成果</h2>
        <p class="page-subtitle">集中维护项目论文的投稿、录用与发表状态，沉淀团队研究成果。</p>
      </div>
      <button class="btn btn-primary" type="button" :disabled="!projects.length" @click="modalOpen = true">
        <Plus :size="16" />
        登记论文
      </button>
    </header>

    <section class="surface paper-toolbar">
      <div class="input-icon">
        <Search :size="17" />
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索论文标题、作者或期刊" @keyup.enter="search" />
      </div>
      <select v-model="filters.projectId" class="select" @change="search">
        <option value="">全部项目</option>
        <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.name }}</option>
      </select>
      <select v-model="filters.status" class="select" @change="search">
        <option value="">全部状态</option>
        <option value="DRAFT">草稿</option>
        <option value="UNDER_REVIEW">审稿中</option>
        <option value="ACCEPTED">已录用</option>
        <option value="PUBLISHED">已发表</option>
      </select>
      <button class="btn btn-secondary" type="button" @click="search">查询</button>
    </section>

    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在加载论文成果...
    </div>

    <template v-else>
      <section v-if="papers.length" class="paper-grid">
        <article v-for="paper in papers" :key="paper.id" class="paper-card surface">
          <div class="paper-card__head">
            <span class="paper-card__icon"><BookOpen :size="19" /></span>
            <StatusBadge :value="paper.status" :map="paperStatusMap" />
          </div>
          <h3>{{ paper.title }}</h3>
          <p class="paper-card__authors">{{ paper.authors || '作者未填写' }}</p>
          <p class="paper-card__abstract line-clamp-2">{{ paper.abstractText || '暂无摘要' }}</p>
          <dl class="paper-card__meta">
            <div><dt>期刊 / 会议</dt><dd>{{ paper.journal || '未填写' }}</dd></div>
            <div><dt>所属项目</dt><dd>{{ paper.projectName }}</dd></div>
            <div><dt>发表日期</dt><dd>{{ formatDate(paper.publicationDate, '尚未发表') }}</dd></div>
          </dl>
          <div class="paper-card__foot">
            <span>登记人 {{ paper.creatorName }}</span>
            <div>
              <a v-if="paper.url" :href="paper.url" target="_blank" class="btn btn-ghost" rel="noreferrer">
                查看原文 <ExternalLink :size="14" />
              </a>
              <button class="btn btn-ghost text-danger" type="button" @click="remove(paper)">删除</button>
            </div>
          </div>
        </article>
      </section>
      <div v-else class="surface empty-state">
        <BookOpen :size="38" />
        <strong>暂无论文成果</strong>
        <span>登记项目论文后可以在这里跟踪投稿与发表状态</span>
      </div>

      <div v-if="pages > 1" class="pagination surface" style="margin-top: 16px">
        <span class="pagination-info">共 {{ total }} 篇论文，第 {{ filters.page }}/{{ pages }} 页</span>
        <button class="icon-btn" type="button" title="上一页" :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">
          <ChevronLeft :size="17" />
        </button>
        <button class="icon-btn" type="button" title="下一页" :disabled="filters.page >= pages" @click="changePage(filters.page + 1)">
          <ChevronRight :size="17" />
        </button>
      </div>
    </template>

    <PaperFormModal
      v-if="modalOpen"
      :project-id="filters.projectId || projects[0]?.id"
      @close="modalOpen = false"
      @saved="modalOpen = false; load()"
    />
  </div>
</template>

<style scoped>
.paper-toolbar {
  display: grid;
  grid-template-columns: minmax(250px, 1fr) 230px 160px auto;
  gap: 10px;
  padding: 14px;
  margin-bottom: 16px;
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.paper-card {
  display: flex;
  flex-direction: column;
  min-height: 335px;
  padding: 19px;
}

.paper-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.paper-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 7px;
  background: var(--blue-100);
  color: var(--blue-700);
}

.paper-card h3 {
  margin-top: 14px;
  color: var(--ink-950);
  font-size: 15px;
  line-height: 1.6;
}

.paper-card__authors {
  margin-top: 5px;
  color: var(--teal-700);
  font-size: 11px;
  font-weight: 650;
}

.paper-card__abstract {
  margin-top: 11px;
  color: var(--ink-600);
  font-size: 11px;
  line-height: 1.7;
}

.paper-card__meta {
  display: grid;
  gap: 8px;
  margin: 15px 0 0;
  padding-top: 13px;
  border-top: 1px solid var(--ink-100);
}

.paper-card__meta div {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 8px;
}

.paper-card__meta dt {
  color: var(--ink-500);
  font-size: 10px;
}

.paper-card__meta dd {
  margin: 0;
  overflow: hidden;
  color: var(--ink-700);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.paper-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 13px;
}

.paper-card__foot > span {
  color: var(--ink-500);
  font-size: 10px;
}

.paper-card__foot > div {
  display: flex;
  align-items: center;
}

@media (max-width: 900px) {
  .paper-grid {
    grid-template-columns: 1fr;
  }

  .paper-toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .paper-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
