<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { projectApi, userApi } from '../api'
import { ElMessage } from '../utils/message'
import BaseModal from './BaseModal.vue'

const props = defineProps({
  project: { type: Object, default: null }
})

const emit = defineEmits(['saved', 'close'])
const saving = ref(false)
const users = ref([])
const form = reactive({
  name: '',
  code: '',
  description: '',
  category: '',
  source: '',
  leaderId: null,
  status: 'PLANNING',
  startDate: '',
  endDate: '',
  budget: null
})

const title = computed(() => props.project ? '编辑科研项目' : '创建科研项目')

watch(() => props.project, (value) => {
  if (value) {
    Object.assign(form, {
      name: value.name,
      code: value.code,
      description: value.description,
      category: value.category,
      source: value.source,
      leaderId: value.leaderId,
      status: value.status,
      startDate: value.startDate || '',
      endDate: value.endDate || '',
      budget: value.budget
    })
  }
}, { immediate: true })

async function submit() {
  if (!form.name || !form.code) {
    ElMessage.warning('请填写项目名称和项目编号')
    return
  }
  if (form.startDate && form.endDate && new Date(form.startDate) > new Date(form.endDate)) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      budget: form.budget === '' || form.budget == null ? null : Number(form.budget),
      startDate: form.startDate || null,
      endDate: form.endDate || null
    }
    const result = props.project
      ? await projectApi.update(props.project.id, payload)
      : await projectApi.create(payload)
    ElMessage.success(props.project ? '项目更新成功' : '项目创建成功')
    emit('saved', result)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  users.value = await userApi.list()
})
</script>

<template>
  <BaseModal :title="title" wide @close="$emit('close')">
    <form id="project-form" class="form-grid" @submit.prevent="submit">
      <div class="field form-span-2">
        <label class="required" for="project-name">项目名称</label>
        <input id="project-name" v-model.trim="form.name" class="input" placeholder="请输入科研项目名称" />
      </div>
      <div class="field">
        <label class="required" for="project-code">项目编号</label>
        <input id="project-code" v-model.trim="form.code" class="input" placeholder="如 KY-2026-010" />
      </div>
      <div class="field">
        <label for="project-leader">项目负责人</label>
        <select id="project-leader" v-model="form.leaderId" class="select">
          <option :value="null">当前用户</option>
          <option v-for="user in users" :key="user.id" :value="user.id">
            {{ user.realName }} · {{ user.department || user.identity }}
          </option>
        </select>
      </div>
      <div class="field">
        <label for="project-category">研究领域</label>
        <input id="project-category" v-model.trim="form.category" class="input" placeholder="如 人工智能" />
      </div>
      <div class="field">
        <label for="project-source">项目来源</label>
        <input id="project-source" v-model.trim="form.source" class="input" placeholder="如 国家自然科学基金" />
      </div>
      <div class="field">
        <label for="project-status">项目状态</label>
        <select id="project-status" v-model="form.status" class="select">
          <option value="PLANNING">筹备中</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="PAUSED">已暂停</option>
          <option value="COMPLETED">已结项</option>
        </select>
      </div>
      <div class="field">
        <label for="project-start">开始日期</label>
        <input id="project-start" v-model="form.startDate" class="input" type="date" />
      </div>
      <div class="field">
        <label for="project-end">结束日期</label>
        <input id="project-end" v-model="form.endDate" class="input" type="date" />
      </div>
      <div class="field">
        <label for="project-budget">预算（元）</label>
        <input id="project-budget" v-model="form.budget" class="input" type="number" min="0" step="0.01" placeholder="0.00" />
      </div>
      <div class="field form-span-2">
        <label for="project-description">项目简介</label>
        <textarea id="project-description" v-model.trim="form.description" class="textarea" placeholder="说明研究目标、主要内容与预期成果" />
      </div>
    </form>
    <template #footer>
      <button class="btn btn-secondary" type="button" @click="$emit('close')">取消</button>
      <button class="btn btn-primary" type="submit" form="project-form" :disabled="saving">
        {{ saving ? '保存中...' : '保存项目' }}
      </button>
    </template>
  </BaseModal>
</template>
