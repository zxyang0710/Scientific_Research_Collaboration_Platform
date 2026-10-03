<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { paperApi } from '../api'
import { ElMessage } from '../utils/message'
import BaseModal from './BaseModal.vue'

const props = defineProps({
  paper: { type: Object, default: null },
  projectId: { type: [Number, String], required: true }
})

const emit = defineEmits(['saved', 'close'])
const saving = ref(false)
const form = reactive({
  projectId: Number(props.projectId),
  title: '',
  authors: '',
  journal: '',
  publicationDate: '',
  doi: '',
  url: '',
  status: 'DRAFT',
  abstractText: ''
})

const title = computed(() => props.paper ? '编辑论文成果' : '登记论文成果')

watch(() => props.paper, (value) => {
  if (value) {
    Object.assign(form, {
      projectId: value.projectId,
      title: value.title,
      authors: value.authors,
      journal: value.journal,
      publicationDate: value.publicationDate || '',
      doi: value.doi,
      url: value.url,
      status: value.status,
      abstractText: value.abstractText
    })
  }
}, { immediate: true })

async function submit() {
  if (!form.title) {
    ElMessage.warning('请输入论文标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      projectId: Number(form.projectId),
      publicationDate: form.publicationDate || null
    }
    const result = props.paper ? await paperApi.update(props.paper.id, payload) : await paperApi.create(payload)
    ElMessage.success(props.paper ? '论文更新成功' : '论文登记成功')
    emit('saved', result)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :title="title" wide @close="$emit('close')">
    <form id="paper-form" class="form-grid" @submit.prevent="submit">
      <div class="field form-span-2">
        <label class="required" for="paper-title">论文标题</label>
        <input id="paper-title" v-model.trim="form.title" class="input" placeholder="请输入论文完整标题" />
      </div>
      <div class="field form-span-2">
        <label for="paper-authors">作者</label>
        <input id="paper-authors" v-model.trim="form.authors" class="input" placeholder="多位作者使用逗号分隔" />
      </div>
      <div class="field">
        <label for="paper-journal">期刊 / 会议</label>
        <input id="paper-journal" v-model.trim="form.journal" class="input" placeholder="期刊或会议名称" />
      </div>
      <div class="field">
        <label for="paper-status">成果状态</label>
        <select id="paper-status" v-model="form.status" class="select">
          <option value="DRAFT">草稿</option>
          <option value="UNDER_REVIEW">审稿中</option>
          <option value="ACCEPTED">已录用</option>
          <option value="PUBLISHED">已发表</option>
        </select>
      </div>
      <div class="field">
        <label for="paper-date">发表日期</label>
        <input id="paper-date" v-model="form.publicationDate" class="input" type="date" />
      </div>
      <div class="field">
        <label for="paper-doi">DOI</label>
        <input id="paper-doi" v-model.trim="form.doi" class="input" placeholder="10.xxxx/xxxxx" />
      </div>
      <div class="field form-span-2">
        <label for="paper-url">成果链接</label>
        <input id="paper-url" v-model.trim="form.url" class="input" type="url" placeholder="https://" />
      </div>
      <div class="field form-span-2">
        <label for="paper-abstract">摘要</label>
        <textarea id="paper-abstract" v-model.trim="form.abstractText" class="textarea" placeholder="简要说明研究问题、方法与结论" />
      </div>
    </form>
    <template #footer>
      <button class="btn btn-secondary" type="button" @click="$emit('close')">取消</button>
      <button class="btn btn-primary" type="submit" form="paper-form" :disabled="saving">
        {{ saving ? '保存中...' : '保存论文' }}
      </button>
    </template>
  </BaseModal>
</template>
