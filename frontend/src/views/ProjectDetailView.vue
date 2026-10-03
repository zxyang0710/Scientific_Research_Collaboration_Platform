<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Edit3,
  FileText,
  FolderOpen,
  MessageSquareText,
  Plus,
  Trash2,
  Upload,
  UserPlus,
  UsersRound
} from 'lucide-vue-next'
import { fileApi, paperApi, projectApi, taskApi } from '../api'
import DiscussionPanel from '../components/DiscussionPanel.vue'
import MemberModal from '../components/MemberModal.vue'
import PaperFormModal from '../components/PaperFormModal.vue'
import ProjectFormModal from '../components/ProjectFormModal.vue'
import StatusBadge from '../components/StatusBadge.vue'
import TaskFormModal from '../components/TaskFormModal.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { ElMessage } from '../utils/message'
import { formatDate, formatSize, paperStatusMap, priorityMap, projectStatusMap, roleMap, taskStatusMap } from '../utils/format'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const detail = ref(null)
const activeTab = ref('overview')
const taskModalOpen = ref(false)
const editingTask = ref(null)
const memberModalOpen = ref(false)
const paperModalOpen = ref(false)
const editingPaper = ref(null)
const projectModalOpen = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const selectedFile = ref(null)
const uploadCategory = ref('DOCUMENT')
const uploadDescription = ref('')

const projectId = computed(() => Number(route.params.id))
const project = computed(() => detail.value?.project)
const isManager = computed(() =>
  auth.user?.role === 'ADMIN'
  || project.value?.leaderId === auth.user?.id
  || project.value?.projectRole === 'LEADER'
  || project.value?.projectRole === 'MANAGER'
)

const tabs = [
  { key: 'overview', label: '项目概况', icon: FolderOpen },
  { key: 'tasks', label: '科研任务', icon: CheckCircle2, count: () => detail.value?.tasks?.length || 0 },
  { key: 'members', label: '项目成员', icon: UsersRound, count: () => detail.value?.members?.length || 0 },
  { key: 'papers', label: '论文成果', icon: BookOpen, count: () => detail.value?.papers?.length || 0 },
  { key: 'files', label: '项目文件', icon: FileText, count: () => detail.value?.fileCount || 0 },
  { key: 'discussions', label: '项目讨论', icon: MessageSquareText, count: () => detail.value?.discussionCount || 0 }
]

async function load() {
  loading.value = true
  try {
    detail.value = await projectApi.detail(projectId.value)
  } catch {
    router.replace('/projects')
  } finally {
    loading.value = false
  }
}

function openTask(task = null) {
  editingTask.value = task
  taskModalOpen.value = true
}

async function removeTask(task) {
  if (!window.confirm(`确定删除任务“${task.title}”吗？`)) return
  await taskApi.remove(task.id)
  ElMessage.success('任务已删除')
  load()
}

function openPaper(paper = null) {
  editingPaper.value = paper
  paperModalOpen.value = true
}

async function removePaper(paper) {
  if (!window.confirm(`确定删除论文“${paper.title}”吗？`)) return
  await paperApi.remove(paper.id)
  ElMessage.success('论文已删除')
  load()
}

async function removeMember(member) {
  if (!window.confirm(`确定将 ${member.realName} 移出项目吗？`)) return
  await projectApi.removeMember(projectId.value, member.id)
  ElMessage.success('成员已移出项目')
  load()
}

async function removeFile(file) {
  if (!window.confirm(`确定删除文件“${file.originalName}”吗？`)) return
  await fileApi.remove(file.id)
  ElMessage.success('文件已删除')
  await loadFiles()
}

function chooseFile(event) {
  selectedFile.value = event.target.files[0] || null
}

async function uploadFile() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  uploadProgress.value = 0
  try {
    const formData = new FormData()
    formData.append('projectId', projectId.value)
    formData.append('file', selectedFile.value)
    formData.append('category', uploadCategory.value)
    formData.append('description', uploadDescription.value)
    await fileApi.upload(formData, (event) => {
      uploadProgress.value = event.total ? Math.round((event.loaded / event.total) * 100) : 0
    })
    ElMessage.success('文件上传成功')
    selectedFile.value = null
    uploadDescription.value = ''
    await loadFiles()
  } finally {
    uploading.value = false
  }
}

async function loadFiles() {
  const result = await fileApi.list({ projectId: projectId.value, page: 1, size: 100 })
  detail.value.files = result.records
}

async function openProjectFile(file) {
  const blob = await fileApi.view(file.id)
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank', 'noopener,noreferrer')
  window.setTimeout(() => URL.revokeObjectURL(url), 60000)
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'files' && !detail.value.files) loadFiles()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在加载项目工作区...
    </div>

    <template v-else-if="detail && project">
      <button class="back-link" type="button" @click="router.push('/projects')">
        <ArrowLeft :size="16" />
        返回项目列表
      </button>

      <header class="project-head">
        <div class="project-head__main">
          <div class="project-head__badges">
            <StatusBadge :value="project.status" :map="projectStatusMap" />
            <span>{{ project.category || '综合科研' }}</span>
          </div>
          <h2>{{ project.name }}</h2>
          <p>{{ project.code }} · {{ project.source || '来源未填写' }}</p>
        </div>
        <div class="project-head__actions">
          <button v-if="isManager" class="btn btn-secondary" type="button" @click="projectModalOpen = true">
            <Edit3 :size="16" />
            编辑项目
          </button>
          <button v-if="isManager" class="btn btn-primary" type="button" @click="openTask()">
            <Plus :size="16" />
            新建任务
          </button>
        </div>
      </header>

      <section class="project-summary surface">
        <div class="summary-progress">
          <div class="summary-progress__top">
            <span>项目总体进度</span>
            <strong>{{ project.progress || 0 }}%</strong>
          </div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: `${project.progress || 0}%` }" />
          </div>
        </div>
        <div class="summary-item">
          <CalendarDays :size="18" />
          <div>
            <span>项目周期</span>
            <strong>{{ formatDate(project.startDate) }} 至 {{ formatDate(project.endDate) }}</strong>
          </div>
        </div>
        <div class="summary-item">
          <UsersRound :size="18" />
          <div>
            <span>团队成员</span>
            <strong>{{ detail.members.length }} 人 · 负责人 {{ project.leaderName }}</strong>
          </div>
        </div>
        <div class="summary-item">
          <CheckCircle2 :size="18" />
          <div>
            <span>任务完成</span>
            <strong>{{ project.completedTaskCount }}/{{ project.taskCount }} 项</strong>
          </div>
        </div>
      </section>

      <nav class="project-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          <component :is="tab.icon" :size="16" />
          {{ tab.label }}
          <span v-if="tab.count" class="tab-count">{{ tab.count() }}</span>
        </button>
      </nav>

      <section v-if="activeTab === 'overview'" class="tab-content">
        <div class="grid-2">
          <article class="surface">
            <div class="surface-header">
              <h3 class="surface-title">项目简介</h3>
            </div>
            <div class="surface-body project-description">
              {{ project.description || '暂无项目简介。' }}
            </div>
          </article>
          <article class="surface">
            <div class="surface-header">
              <h3 class="surface-title">项目信息</h3>
            </div>
            <div class="surface-body">
              <dl class="detail-list">
                <dt>项目编号</dt><dd>{{ project.code }}</dd>
                <dt>项目负责人</dt><dd>{{ project.leaderName }}</dd>
                <dt>研究领域</dt><dd>{{ project.category || '未填写' }}</dd>
                <dt>项目来源</dt><dd>{{ project.source || '未填写' }}</dd>
                <dt>预算</dt><dd>{{ project.budget ? `¥ ${Number(project.budget).toLocaleString()}` : '未填写' }}</dd>
                <dt>最近更新</dt><dd>{{ formatDate(project.updatedAt) }}</dd>
              </dl>
            </div>
          </article>
        </div>

        <article class="surface overview-tasks">
          <div class="surface-header">
            <h3 class="surface-title">重点任务</h3>
            <button class="btn btn-ghost" type="button" @click="switchTab('tasks')">查看全部</button>
          </div>
          <div class="compact-task-list">
            <article v-for="task in detail.tasks.slice(0, 5)" :key="task.id" class="compact-task">
              <span class="compact-task__status" :class="`compact-task__status--${task.status}`" />
              <div>
                <strong>{{ task.title }}</strong>
                <span>{{ task.assigneeName }} · 截止 {{ formatDate(task.deadline) }}</span>
              </div>
              <StatusBadge :value="task.status" :map="taskStatusMap" />
            </article>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'tasks'" class="tab-content">
        <div class="surface">
          <div class="surface-header">
            <div>
              <h3 class="surface-title">任务清单</h3>
              <p class="surface-hint">更新任务状态即可同步协作进展</p>
            </div>
            <button v-if="isManager" class="btn btn-primary" type="button" @click="openTask()">
              <Plus :size="16" /> 新建任务
            </button>
          </div>
          <div v-if="detail.tasks.length" class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>任务</th>
                  <th>负责人</th>
                  <th>优先级</th>
                  <th>截止日期</th>
                  <th>状态</th>
                  <th style="width: 100px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in detail.tasks" :key="task.id">
                  <td>
                    <div class="table-primary">{{ task.title }}</div>
                    <div class="table-secondary line-clamp-2">{{ task.description || '暂无任务说明' }}</div>
                  </td>
                  <td>
                    <div class="user-cell">
                      <UserAvatar :name="task.assigneeName" :src="task.assigneeAvatar" size="sm" />
                      <span>{{ task.assigneeName }}</span>
                    </div>
                  </td>
                  <td><StatusBadge :value="task.priority" :map="priorityMap" /></td>
                  <td :class="{ 'text-danger': task.deadline && new Date(task.deadline) < new Date() && task.status !== 'DONE' }">
                    {{ formatDate(task.deadline) }}
                  </td>
                  <td><StatusBadge :value="task.status" :map="taskStatusMap" /></td>
                  <td>
                    <div class="row-actions">
                      <button v-if="isManager" class="icon-btn danger" type="button" title="删除任务" @click="removeTask(task)">
                        <Trash2 :size="15" />
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <CheckCircle2 :size="36" />
            <strong>暂无科研任务</strong>
            <button v-if="isManager" class="btn btn-primary" type="button" style="margin-top: 14px" @click="openTask()">新建任务</button>
          </div>
        </div>
      </section>

      <section v-else-if="activeTab === 'members'" class="tab-content">
        <div class="surface">
          <div class="surface-header">
            <div>
              <h3 class="surface-title">项目成员</h3>
              <p class="surface-hint">项目负责人和管理员可以维护成员角色</p>
            </div>
            <button v-if="isManager" class="btn btn-primary" type="button" @click="memberModalOpen = true">
              <UserPlus :size="16" /> 添加成员
            </button>
          </div>
          <div class="member-grid">
            <article v-for="member in detail.members" :key="member.id" class="member-card">
              <UserAvatar :name="member.realName" :src="member.avatar" size="lg" />
              <div class="member-card__copy">
                <strong>{{ member.realName }}</strong>
                <span>{{ member.identity || '科研成员' }} · {{ member.department || '院系未填写' }}</span>
                <small>{{ roleMap[member.projectRole] || member.projectRole }}</small>
              </div>
              <button
                v-if="isManager && member.userId !== project.leaderId"
                class="icon-btn danger"
                type="button"
                title="移出项目"
                @click="removeMember(member)"
              >
                <Trash2 :size="15" />
              </button>
            </article>
          </div>
        </div>
      </section>

      <section v-else-if="activeTab === 'papers'" class="tab-content">
        <div class="surface">
          <div class="surface-header">
            <div>
              <h3 class="surface-title">论文成果</h3>
              <p class="surface-hint">记录项目产出的论文、会议报告与投稿进展</p>
            </div>
            <button class="btn btn-primary" type="button" @click="openPaper()">
              <Plus :size="16" /> 登记论文
            </button>
          </div>
          <div v-if="detail.papers.length" class="paper-list">
            <article v-for="paper in detail.papers" :key="paper.id" class="paper-row">
              <span class="paper-row__icon"><BookOpen :size="19" /></span>
              <div class="paper-row__copy">
                <div class="paper-row__title">
                  <strong>{{ paper.title }}</strong>
                  <StatusBadge :value="paper.status" :map="paperStatusMap" />
                </div>
                <p>{{ paper.authors || '作者未填写' }} · {{ paper.journal || '期刊/会议未填写' }}</p>
                <div class="paper-row__meta">
                  <span>{{ paper.publicationDate ? `发表日期 ${formatDate(paper.publicationDate)}` : '尚未发表' }}</span>
                  <span v-if="paper.doi">DOI {{ paper.doi }}</span>
                  <span>登记人 {{ paper.creatorName }}</span>
                </div>
              </div>
              <div class="row-actions">
                <button class="icon-btn" type="button" title="编辑论文" @click="openPaper(paper)">
                  <Edit3 :size="15" />
                </button>
                <button class="icon-btn danger" type="button" title="删除论文" @click="removePaper(paper)">
                  <Trash2 :size="15" />
                </button>
              </div>
            </article>
          </div>
          <div v-else class="empty-state">
            <BookOpen :size="36" />
            <strong>暂无论文成果</strong>
          </div>
        </div>
      </section>

      <section v-else-if="activeTab === 'files'" class="tab-content files-layout">
        <article class="surface upload-panel">
          <div class="surface-header">
            <h3 class="surface-title">上传项目文件</h3>
          </div>
          <div class="surface-body">
            <label class="file-drop">
              <Upload :size="28" />
              <strong>{{ selectedFile ? selectedFile.name : '选择文件上传' }}</strong>
              <span>{{ selectedFile ? formatSize(selectedFile.size) : '支持单个不超过 20 MB 的文件' }}</span>
              <input type="file" @change="chooseFile" />
            </label>
            <div class="field" style="margin-top: 14px">
              <label for="upload-category">文件分类</label>
              <select id="upload-category" v-model="uploadCategory" class="select">
                <option value="DOCUMENT">研究文档</option>
                <option value="DATA">实验数据</option>
                <option value="REPORT">阶段报告</option>
                <option value="PRESENTATION">汇报材料</option>
                <option value="OTHER">其他</option>
              </select>
            </div>
            <div class="field" style="margin-top: 14px">
              <label for="upload-description">文件说明</label>
              <input id="upload-description" v-model.trim="uploadDescription" class="input" placeholder="简要说明文件用途" />
            </div>
            <div v-if="uploading" class="upload-progress">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: `${uploadProgress}%` }" />
              </div>
              <span>{{ uploadProgress }}%</span>
            </div>
            <button class="btn btn-primary upload-button" type="button" :disabled="uploading || !selectedFile" @click="uploadFile">
              <Upload :size="16" />
              {{ uploading ? '上传中...' : '上传文件' }}
            </button>
          </div>
        </article>

        <article class="surface">
          <div class="surface-header">
            <h3 class="surface-title">文件列表</h3>
          </div>
          <div v-if="detail.files?.length" class="file-list">
            <article v-for="file in detail.files" :key="file.id" class="file-row">
              <span class="file-row__icon"><FileText :size="18" /></span>
              <div>
                <button type="button" class="file-row__name" @click="openProjectFile(file)">{{ file.originalName }}</button>
                <p>{{ file.description || '暂无文件说明' }}</p>
                <small>{{ formatSize(file.fileSize) }} · {{ file.uploaderName }} · {{ formatDate(file.createdAt) }}</small>
              </div>
              <button class="icon-btn danger" type="button" title="删除文件" @click="removeFile(file)">
                <Trash2 :size="15" />
              </button>
            </article>
          </div>
          <div v-else class="empty-state">
            <FolderOpen :size="36" />
            <strong>暂无项目文件</strong>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'discussions'" class="tab-content">
        <article class="surface">
          <div class="surface-header">
            <div>
              <h3 class="surface-title">项目讨论</h3>
              <p class="surface-hint">集中记录问题、决策和研究进展</p>
            </div>
          </div>
          <div class="surface-body">
            <DiscussionPanel :project-id="projectId" />
          </div>
        </article>
      </section>
    </template>

    <TaskFormModal
      v-if="taskModalOpen"
      :task="editingTask"
      :project-id="projectId"
      :members="detail.members"
      @close="taskModalOpen = false"
      @saved="taskModalOpen = false; load()"
    />
    <MemberModal
      v-if="memberModalOpen"
      :project-id="projectId"
      :members="detail.members"
      @close="memberModalOpen = false"
      @saved="memberModalOpen = false; load()"
    />
    <PaperFormModal
      v-if="paperModalOpen"
      :paper="editingPaper"
      :project-id="projectId"
      @close="paperModalOpen = false"
      @saved="paperModalOpen = false; load()"
    />
    <ProjectFormModal
      v-if="projectModalOpen"
      :project="project"
      @close="projectModalOpen = false"
      @saved="projectModalOpen = false; load()"
    />
  </div>
</template>

<style scoped>
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 15px;
  padding: 4px 0;
  background: transparent;
  color: var(--ink-500);
  cursor: pointer;
  font-size: 12px;
}

.back-link:hover {
  color: var(--teal-700);
}

.project-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 25px;
  margin-bottom: 18px;
}

.project-head__badges {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 10px;
}

.project-head__badges > span:last-child {
  color: var(--teal-700);
  font-size: 11px;
  font-weight: 700;
}

.project-head h2 {
  max-width: 850px;
  color: var(--ink-950);
  font-size: 25px;
  line-height: 1.4;
}

.project-head__main p {
  margin-top: 6px;
  color: var(--ink-500);
  font-size: 12px;
}

.project-head__actions {
  display: flex;
  gap: 9px;
}

.project-summary {
  display: grid;
  grid-template-columns: minmax(240px, 1.4fr) repeat(3, minmax(170px, 1fr));
  align-items: center;
  gap: 20px;
  padding: 18px 20px;
  margin-bottom: 16px;
}

.summary-progress__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 9px;
  color: var(--ink-600);
  font-size: 12px;
}

.summary-progress__top strong {
  color: var(--ink-950);
  font-size: 18px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: var(--ink-600);
}

.summary-item > svg {
  flex: 0 0 auto;
  color: var(--teal-700);
}

.summary-item div {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.summary-item span {
  color: var(--ink-500);
  font-size: 10px;
}

.summary-item strong {
  margin-top: 2px;
  overflow: hidden;
  color: var(--ink-800);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 16px;
  padding: 4px;
  border: 1px solid var(--ink-200);
  border-radius: 8px;
  background: white;
  overflow-x: auto;
}

.project-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
  height: 36px;
  padding: 0 13px;
  border-radius: 5px;
  background: transparent;
  color: var(--ink-600);
  cursor: pointer;
  white-space: nowrap;
}

.project-tabs button:hover {
  background: var(--ink-50);
}

.project-tabs button.active {
  background: var(--ink-950);
  color: white;
}

.tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--ink-100);
  color: var(--ink-700);
  font-size: 9px;
  line-height: 18px;
  text-align: center;
}

.project-tabs button.active .tab-count {
  background: rgba(255, 255, 255, 0.16);
  color: white;
}

.tab-content {
  animation: page-in 0.2s ease-out;
}

.project-description {
  min-height: 190px;
  color: var(--ink-700);
  font-size: 13px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.overview-tasks {
  margin-top: 16px;
}

.compact-task-list {
  padding: 0 18px 8px;
}

.compact-task {
  display: grid;
  grid-template-columns: 4px minmax(0, 1fr) auto 48px;
  align-items: center;
  gap: 13px;
  padding: 13px 0;
  border-bottom: 1px solid var(--ink-100);
}

.compact-task:last-child {
  border-bottom: 0;
}

.compact-task__status {
  width: 4px;
  height: 34px;
  border-radius: 999px;
  background: var(--ink-300);
}

.compact-task__status--IN_PROGRESS { background: var(--teal-600); }
.compact-task__status--DONE { background: var(--blue-700); }
.compact-task__status--BLOCKED { background: var(--red-700); }

.compact-task > div {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.compact-task strong {
  overflow: hidden;
  color: var(--ink-800);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-task div span {
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 10px;
}

.surface-hint {
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 10px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.icon-btn.danger:hover {
  background: var(--red-100);
  color: var(--red-700);
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
  padding: 0 20px 8px;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 82px;
  border-bottom: 1px solid var(--ink-100);
}

.member-card__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.member-card__copy strong {
  color: var(--ink-900);
  font-size: 13px;
}

.member-card__copy span {
  margin-top: 2px;
  overflow: hidden;
  color: var(--ink-500);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-card__copy small {
  margin-top: 5px;
  color: var(--teal-700);
  font-size: 10px;
  font-weight: 700;
}

.paper-list {
  padding: 0 20px 8px;
}

.paper-row {
  display: flex;
  align-items: flex-start;
  gap: 13px;
  padding: 17px 0;
  border-bottom: 1px solid var(--ink-100);
}

.paper-row__icon,
.file-row__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border-radius: 7px;
  background: var(--blue-100);
  color: var(--blue-700);
}

.paper-row__copy {
  flex: 1;
  min-width: 0;
}

.paper-row__title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.paper-row__title strong {
  color: var(--ink-900);
  font-size: 13px;
  line-height: 1.55;
}

.paper-row__copy > p {
  margin-top: 5px;
  color: var(--ink-600);
  font-size: 11px;
}

.paper-row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin-top: 7px;
  color: var(--ink-500);
  font-size: 10px;
}

.files-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
}

.file-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 155px;
  padding: 18px;
  border: 1px dashed var(--ink-300);
  border-radius: 7px;
  background: var(--ink-50);
  color: var(--ink-500);
  cursor: pointer;
  text-align: center;
}

.file-drop:hover {
  border-color: var(--teal-600);
  background: #f2faf9;
}

.file-drop svg {
  margin-bottom: 10px;
  color: var(--teal-700);
}

.file-drop strong {
  max-width: 100%;
  overflow: hidden;
  color: var(--ink-800);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-drop span {
  margin-top: 4px;
  font-size: 10px;
}

.file-drop input {
  display: none;
}

.upload-progress {
  display: grid;
  grid-template-columns: 1fr 36px;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
}

.upload-progress span {
  color: var(--ink-700);
  font-size: 10px;
}

.upload-button {
  width: 100%;
  margin-top: 18px;
}

.file-list {
  padding: 0 18px 8px;
}

.file-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px 0;
  border-bottom: 1px solid var(--ink-100);
}

.file-row > div {
  flex: 1;
  min-width: 0;
}

.file-row__name {
  display: block;
  width: 100%;
  overflow: hidden;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink-900);
  font-size: 12px;
  font-weight: 700;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.file-row__name:hover {
  color: var(--teal-700);
}

.file-row p {
  margin-top: 3px;
  color: var(--ink-600);
  font-size: 11px;
}

.file-row small {
  display: block;
  margin-top: 5px;
  color: var(--ink-500);
  font-size: 9px;
}

@media (max-width: 1100px) {
  .project-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .files-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 800px) {
  .project-head {
    flex-direction: column;
  }

  .project-head__actions {
    width: 100%;
  }

  .project-head__actions .btn {
    flex: 1;
  }

  .project-summary,
  .member-grid {
    grid-template-columns: 1fr;
  }

  .compact-task {
    grid-template-columns: 4px minmax(0, 1fr) auto;
  }

}
</style>
