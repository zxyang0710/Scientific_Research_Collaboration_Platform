<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { taskApi } from '../api'
import { ElMessage } from '../utils/message'
import BaseModal from './BaseModal.vue'

const props = defineProps({
  task: { type: Object, default: null },
  projectId: { type: [Number, String], required: true },
  members: { type: Array, default: () => [] }
})

const emit = defineEmits(['saved', 'close'])
const saving = ref(false)
const form = reactive({
  projectId: Number(props.projectId),
  title: '',
  description: '',
  assigneeId: null,
  priority: 'MEDIUM',
  status: 'TODO',
  deadline: ''
})

const title = computed(() => props.task ? '编辑科研任务' : '新建科研任务')

watch(() => props.task, (value) => {
  if (value) {
    Object.assign(form, {
      projectId: value.projectId,
      title: value.title,
      description: value.description,
      assigneeId: value.assigneeId,
      priority: value.priority,
      status: value.status,
      deadline: value.deadline || ''
    })
  }
}, { immediate: true })

async function submit() {
  if (!form.title) {
    ElMessage.warning('请输入任务标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      projectId: Number(form.projectId),
      assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
      deadline: form.deadline || null
    }
    const result = props.task
      ? await taskApi.update(props.task.id, payload)
      : await taskApi.create(payload)
    ElMessage.success(props.task ? '任务更新成功' : '任务创建成功')
    emit('saved', result)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :title="title" @close="$emit('close')">
    <form id="task-form" class="form-grid" @submit.prevent="submit">
      <div class="field form-span-2">
        <label class="required" for="task-title">任务标题</label>
        <input id="task-title" v-model.trim="form.title" class="input" placeholder="清晰描述需要完成的工作" />
      </div>
      <div class="field">
        <label for="task-assignee">负责人</label>
        <select id="task-assignee" v-model="form.assigneeId" class="select">
          <option :value="null">暂不指定</option>
          <option v-for="member in members" :key="member.userId" :value="member.userId">
            {{ member.realName }} · {{ member.department || member.identity }}
          </option>
        </select>
      </div>
      <div class="field">
        <label for="task-priority">优先级</label>
        <select id="task-priority" v-model="form.priority" class="select">
          <option value="LOW">低</option>
          <option value="MEDIUM">中</option>
          <option value="HIGH">高</option>
        </select>
      </div>
      <div class="field">
        <label for="task-status">任务状态</label>
        <select id="task-status" v-model="form.status" class="select">
          <option value="TODO">待处理</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="BLOCKED">已阻塞</option>
          <option value="DONE">已完成</option>
        </select>
      </div>
      <div class="field">
        <label for="task-deadline">截止日期</label>
        <input id="task-deadline" v-model="form.deadline" class="input" type="date" />
      </div>
      <div class="field form-span-2">
        <label for="task-description">任务说明</label>
        <textarea id="task-description" v-model.trim="form.description" class="textarea" placeholder="补充交付标准、相关材料或注意事项" />
      </div>
    </form>
    <template #footer>
      <button class="btn btn-secondary" type="button" @click="$emit('close')">取消</button>
      <button class="btn btn-primary" type="submit" form="task-form" :disabled="saving">
        {{ saving ? '保存中...' : '保存任务' }}
      </button>
    </template>
  </BaseModal>
</template>
