import dayjs from 'dayjs'

export function formatDate(value, fallback = '未设置') {
  return value ? dayjs(value).format('YYYY-MM-DD') : fallback
}

export function formatDateTime(value, fallback = '未记录') {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : fallback
}

export function formatSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export const projectStatusMap = {
  PLANNING: { label: '筹备中', tone: 'slate' },
  IN_PROGRESS: { label: '进行中', tone: 'teal' },
  COMPLETED: { label: '已结项', tone: 'blue' },
  PAUSED: { label: '已暂停', tone: 'amber' }
}

export const taskStatusMap = {
  TODO: { label: '待处理', tone: 'slate' },
  IN_PROGRESS: { label: '进行中', tone: 'teal' },
  DONE: { label: '已完成', tone: 'blue' },
  BLOCKED: { label: '已阻塞', tone: 'red' }
}

export const priorityMap = {
  LOW: { label: '低', tone: 'slate' },
  MEDIUM: { label: '中', tone: 'amber' },
  HIGH: { label: '高', tone: 'red' }
}

export const paperStatusMap = {
  DRAFT: { label: '草稿', tone: 'slate' },
  UNDER_REVIEW: { label: '审稿中', tone: 'amber' },
  PUBLISHED: { label: '已发表', tone: 'teal' },
  ACCEPTED: { label: '已录用', tone: 'blue' }
}

export const roleMap = {
  ADMIN: '平台管理员',
  LEADER: '项目负责人',
  MANAGER: '项目管理员',
  MEMBER: '项目成员'
}
