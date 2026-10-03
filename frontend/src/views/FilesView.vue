<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  Download,
  FileArchive,
  FileText,
  FolderOpen,
  Search,
  Trash2,
  Upload
} from 'lucide-vue-next'
import { fileApi, projectApi } from '../api'
import { ElMessage } from '../utils/message'
import { formatDateTime, formatSize } from '../utils/format'

const loading = ref(true)
const files = ref([])
const projects = ref([])
const total = ref(0)
const pages = ref(0)
const filters = reactive({
  projectId: '',
  category: '',
  keyword: '',
  page: 1,
  size: 10
})

const selectedSize = computed(() => files.value.reduce((sum, file) => sum + (file.fileSize || 0), 0))

const categoryLabels = {
  DOCUMENT: '研究文档',
  DATA: '实验数据',
  REPORT: '阶段报告',
  PRESENTATION: '汇报材料',
  OTHER: '其他'
}

async function load() {
  loading.value = true
  try {
    const [fileResult, projectResult] = await Promise.all([
      fileApi.list({
        projectId: filters.projectId || undefined,
        category: filters.category || undefined,
        keyword: filters.keyword || undefined,
        page: filters.page,
        size: filters.size
      }),
      projectApi.list({ page: 1, size: 100 })
    ])
    files.value = fileResult.records
    total.value = fileResult.total
    pages.value = fileResult.pages
    projects.value = projectResult.records
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

async function remove(file) {
  if (!window.confirm(`确定删除文件“${file.originalName}”吗？`)) return
  await fileApi.remove(file.id)
  ElMessage.success('文件已删除')
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
        <h2 class="page-title">项目文件</h2>
        <p class="page-subtitle">汇总项目研究方案、实验数据、汇报材料和阶段报告。</p>
      </div>
      <button class="btn btn-primary" type="button" :disabled="!projects.length" @click="$router.push(`/projects/${filters.projectId || projects[0]?.id}`)">
        <Upload :size="16" />
        上传文件
      </button>
    </header>

    <section class="grid-3 file-stats">
      <article class="surface file-stat">
        <span><FolderOpen :size="18" /></span>
        <div><strong>{{ total }}</strong><small>文件总数</small></div>
      </article>
      <article class="surface file-stat">
        <span><FileArchive :size="18" /></span>
        <div><strong>{{ formatSize(selectedSize) }}</strong><small>当前页容量</small></div>
      </article>
      <article class="surface file-stat">
        <span><FileText :size="18" /></span>
        <div><strong>{{ projects.length }}</strong><small>可访问项目</small></div>
      </article>
    </section>

    <section class="surface file-toolbar">
      <div class="input-icon">
        <Search :size="17" />
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索文件名称" @keyup.enter="search" />
      </div>
      <select v-model="filters.projectId" class="select" @change="search">
        <option value="">全部项目</option>
        <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.name }}</option>
      </select>
      <select v-model="filters.category" class="select" @change="search">
        <option value="">全部分类</option>
        <option v-for="(label, key) in categoryLabels" :key="key" :value="key">{{ label }}</option>
      </select>
      <button class="btn btn-secondary" type="button" @click="search">查询</button>
    </section>

    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在加载文件...
    </div>

    <template v-else>
      <section class="surface">
        <div v-if="files.length" class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>文件名称</th>
                <th>所属项目</th>
                <th>分类</th>
                <th>大小</th>
                <th>上传人</th>
                <th>上传时间</th>
                <th style="width: 100px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="file in files" :key="file.id">
                <td>
                  <div class="file-name">
                    <span><FileText :size="17" /></span>
                    <div>
                      <div class="table-primary">{{ file.originalName }}</div>
                      <div class="table-secondary">{{ file.description || '暂无说明' }}</div>
                    </div>
                  </div>
                </td>
                <td>{{ projects.find((project) => project.id === file.projectId)?.name || `项目 #${file.projectId}` }}</td>
                <td>{{ categoryLabels[file.category] || file.category }}</td>
                <td>{{ formatSize(file.fileSize) }}</td>
                <td>{{ file.uploaderName }}</td>
                <td>{{ formatDateTime(file.createdAt) }}</td>
                <td>
                  <div class="row-actions">
                    <a class="icon-btn" :href="fileApi.downloadUrl(file.id)" title="下载文件">
                      <Download :size="16" />
                    </a>
                    <button class="icon-btn danger" type="button" title="删除文件" @click="remove(file)">
                      <Trash2 :size="16" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <FolderOpen :size="38" />
          <strong>没有找到文件</strong>
          <span>进入项目详情即可上传研究文件</span>
        </div>
        <div v-if="pages > 1" class="pagination">
          <span class="pagination-info">共 {{ total }} 个文件，第 {{ filters.page }}/{{ pages }} 页</span>
          <button class="icon-btn" type="button" title="上一页" :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">
            <ChevronLeft :size="17" />
          </button>
          <button class="icon-btn" type="button" title="下一页" :disabled="filters.page >= pages" @click="changePage(filters.page + 1)">
            <ChevronRight :size="17" />
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.file-stats {
  margin-bottom: 16px;
}

.file-stat {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 16px;
}

.file-stat > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 7px;
  background: var(--teal-100);
  color: var(--teal-700);
}

.file-stat > div {
  display: flex;
  flex-direction: column;
}

.file-stat strong {
  color: var(--ink-950);
  font-size: 20px;
  line-height: 1.2;
}

.file-stat small {
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 10px;
}

.file-toolbar {
  display: grid;
  grid-template-columns: minmax(250px, 1fr) 220px 160px auto;
  gap: 10px;
  padding: 14px;
  margin-bottom: 16px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-name > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--blue-100);
  color: var(--blue-700);
}

.row-actions {
  display: flex;
  gap: 2px;
}

.icon-btn.danger:hover {
  background: var(--red-100);
  color: var(--red-700);
}

@media (max-width: 900px) {
  .file-toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .file-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
