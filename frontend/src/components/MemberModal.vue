<script setup>
import { onMounted, reactive, ref } from 'vue'
import { projectApi, userApi } from '../api'
import { ElMessage } from '../utils/message'
import BaseModal from './BaseModal.vue'

const props = defineProps({
  projectId: { type: [Number, String], required: true },
  members: { type: Array, default: () => [] }
})

const emit = defineEmits(['saved', 'close'])
const saving = ref(false)
const users = ref([])
const form = reactive({
  userId: null,
  projectRole: 'MEMBER'
})

async function submit() {
  if (!form.userId) {
    ElMessage.warning('请选择要加入的成员')
    return
  }
  saving.value = true
  try {
    await projectApi.addMember(props.projectId, {
      userId: Number(form.userId),
      projectRole: form.projectRole
    })
    ElMessage.success('成员添加成功')
    emit('saved')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const existing = new Set(props.members.map((member) => Number(member.userId)))
  users.value = (await userApi.list()).filter((user) => !existing.has(Number(user.id)))
})
</script>

<template>
  <BaseModal title="添加项目成员" @close="$emit('close')">
    <form id="member-form" class="form-grid" @submit.prevent="submit">
      <div class="field form-span-2">
        <label class="required" for="member-user">选择用户</label>
        <select id="member-user" v-model="form.userId" class="select">
          <option :value="null">请选择用户</option>
          <option v-for="user in users" :key="user.id" :value="user.id">
            {{ user.realName }} · {{ user.department || '院系未填写' }} · {{ user.identity || user.role }}
          </option>
        </select>
      </div>
      <div class="field form-span-2">
        <label for="member-role">项目角色</label>
        <select id="member-role" v-model="form.projectRole" class="select">
          <option value="MEMBER">项目成员</option>
          <option value="MANAGER">项目管理员</option>
          <option value="LEADER">项目负责人</option>
        </select>
      </div>
      <p v-if="!users.length" class="form-span-2 text-muted">所有系统用户都已加入该项目。</p>
    </form>
    <template #footer>
      <button class="btn btn-secondary" type="button" @click="$emit('close')">取消</button>
      <button class="btn btn-primary" type="submit" form="member-form" :disabled="saving || !users.length">
        {{ saving ? '添加中...' : '添加成员' }}
      </button>
    </template>
  </BaseModal>
</template>
