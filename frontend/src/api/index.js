import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  me: () => http.get('/auth/me')
}

export const dashboardApi = {
  get: () => http.get('/dashboard')
}

export const projectApi = {
  list: (params) => http.get('/projects', { params }),
  detail: (id) => http.get(`/projects/${id}`),
  create: (data) => http.post('/projects', data),
  update: (id, data) => http.put(`/projects/${id}`, data),
  remove: (id) => http.delete(`/projects/${id}`),
  members: (id) => http.get(`/projects/${id}/members`),
  addMember: (id, data) => http.post(`/projects/${id}/members`, data),
  updateMember: (id, memberId, data) => http.put(`/projects/${id}/members/${memberId}`, data),
  removeMember: (id, memberId) => http.delete(`/projects/${id}/members/${memberId}`)
}

export const taskApi = {
  list: (params) => http.get('/tasks', { params }),
  get: (id) => http.get(`/tasks/${id}`),
  create: (data) => http.post('/tasks', data),
  update: (id, data) => http.put(`/tasks/${id}`, data),
  updateProgress: (id, data) => http.put(`/tasks/${id}/progress`, data),
  remove: (id) => http.delete(`/tasks/${id}`)
}

export const paperApi = {
  list: (params) => http.get('/papers', { params }),
  create: (data) => http.post('/papers', data),
  update: (id, data) => http.put(`/papers/${id}`, data),
  remove: (id) => http.delete(`/papers/${id}`)
}

export const discussionApi = {
  list: (params) => http.get('/discussions', { params }),
  create: (data) => http.post('/discussions', data),
  remove: (id) => http.delete(`/discussions/${id}`)
}

export const fileApi = {
  list: (params) => http.get('/files', { params }),
  upload: (formData, onUploadProgress) => http.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  }),
  remove: (id) => http.delete(`/files/${id}`),
  downloadUrl: (id) => `/api/files/${id}/download`
}

export const notificationApi = {
  list: (params) => http.get('/notifications', { params }),
  unreadCount: () => http.get('/notifications/unread-count'),
  markRead: (id) => http.put(`/notifications/${id}/read`),
  markAllRead: () => http.put('/notifications/read-all'),
  remove: (id) => http.delete(`/notifications/${id}`)
}

export const userApi = {
  list: (params) => http.get('/users', { params })
}
