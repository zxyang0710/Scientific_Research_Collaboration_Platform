<script setup>
import { onMounted, ref } from 'vue'
import { CornerDownRight, MessageSquareText, Send, Trash2 } from 'lucide-vue-next'
import { discussionApi } from '../api'
import { ElMessage } from '../utils/message'
import UserAvatar from './UserAvatar.vue'
import { formatDateTime } from '../utils/format'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  projectId: { type: [Number, String], required: true }
})

const auth = useAuthStore()
const loading = ref(true)
const submitting = ref(false)
const discussions = ref([])
const total = ref(0)
const page = ref(1)
const content = ref('')
const replyTo = ref(null)
const replyContent = ref('')

async function load() {
  loading.value = true
  try {
    const result = await discussionApi.list({ projectId: props.projectId, page: page.value, size: 8 })
    discussions.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!content.value.trim()) return
  submitting.value = true
  try {
    await discussionApi.create({
      projectId: Number(props.projectId),
      parentId: null,
      content: content.value.trim()
    })
    content.value = ''
    ElMessage.success('讨论发布成功')
    await load()
  } finally {
    submitting.value = false
  }
}

async function submitReply(parent) {
  if (!replyContent.value.trim()) return
  submitting.value = true
  try {
    await discussionApi.create({
      projectId: Number(props.projectId),
      parentId: parent.id,
      content: replyContent.value.trim()
    })
    replyContent.value = ''
    replyTo.value = null
    ElMessage.success('回复成功')
    await load()
  } finally {
    submitting.value = false
  }
}

async function remove(id) {
  await discussionApi.remove(id)
  ElMessage.success('讨论已删除')
  load()
}

function changePage(value) {
  page.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div class="discussion-wrap">
    <div class="discussion-compose">
      <UserAvatar :name="auth.user?.realName" :src="auth.user?.avatar" />
      <div>
        <textarea
          v-model="content"
          class="textarea"
          placeholder="发布研究进展、讨论问题或同步协作信息..."
          @keydown.ctrl.enter="submit"
        />
        <div class="discussion-compose__foot">
          <span>Ctrl + Enter 快捷发布</span>
          <button class="btn btn-primary" type="button" :disabled="submitting || !content.trim()" @click="submit">
            <Send :size="15" />
            发布讨论
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在加载讨论...
    </div>

    <div v-else-if="discussions.length" class="discussion-list">
      <article v-for="discussion in discussions" :key="discussion.id" class="discussion-item">
        <UserAvatar :name="discussion.authorName" :src="discussion.authorAvatar" size="lg" />
        <div class="discussion-item__body">
          <div class="discussion-item__head">
            <div>
              <strong>{{ discussion.authorName }}</strong>
              <span>{{ formatDateTime(discussion.createdAt) }}</span>
            </div>
            <button
              v-if="discussion.authorId === auth.user?.id || auth.user?.role === 'ADMIN'"
              class="icon-btn"
              type="button"
              title="删除讨论"
              @click="remove(discussion.id)"
            >
              <Trash2 :size="15" />
            </button>
          </div>
          <p class="discussion-item__content">{{ discussion.content }}</p>

          <div v-if="discussion.replies?.length" class="reply-list">
            <div v-for="reply in discussion.replies" :key="reply.id" class="reply-item">
              <UserAvatar :name="reply.authorName" :src="reply.authorAvatar" size="sm" />
              <div>
                <div class="reply-item__head">
                  <strong>{{ reply.authorName }}</strong>
                  <span>{{ formatDateTime(reply.createdAt) }}</span>
                </div>
                <p>{{ reply.content }}</p>
              </div>
            </div>
          </div>

          <button class="reply-trigger" type="button" @click="replyTo = replyTo === discussion.id ? null : discussion.id">
            <CornerDownRight :size="14" />
            回复
          </button>

          <div v-if="replyTo === discussion.id" class="reply-compose">
            <input
              v-model="replyContent"
              class="input"
              :placeholder="`回复 ${discussion.authorName}`"
              @keyup.enter="submitReply(discussion)"
            />
            <button class="btn btn-primary" type="button" :disabled="submitting || !replyContent.trim()" @click="submitReply(discussion)">
              发送
            </button>
          </div>
        </div>
      </article>
    </div>

    <div v-else class="empty-state">
      <MessageSquareText :size="36" />
      <strong>还没有项目讨论</strong>
      <span>发布第一条研究动态，让协作信息沉淀下来</span>
    </div>

    <div v-if="total > 8" class="discussion-pagination">
      <button class="btn btn-secondary" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / 8) }} 页</span>
      <button class="btn btn-secondary" type="button" :disabled="page * 8 >= total" @click="changePage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.discussion-compose {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--ink-100);
}

.discussion-compose > div {
  flex: 1;
}

.discussion-compose .textarea {
  min-height: 82px;
}

.discussion-compose__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.discussion-compose__foot span {
  color: var(--ink-500);
  font-size: 10px;
}

.discussion-list {
  display: grid;
}

.discussion-item {
  display: flex;
  gap: 13px;
  padding: 20px 0;
  border-bottom: 1px solid var(--ink-100);
}

.discussion-item__body {
  flex: 1;
  min-width: 0;
}

.discussion-item__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.discussion-item__head > div {
  display: flex;
  align-items: baseline;
  gap: 9px;
}

.discussion-item__head strong {
  color: var(--ink-900);
  font-size: 13px;
}

.discussion-item__head span,
.reply-item__head span {
  color: var(--ink-500);
  font-size: 10px;
}

.discussion-item__content {
  margin-top: 7px;
  color: var(--ink-700);
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.reply-list {
  display: grid;
  gap: 9px;
  margin-top: 13px;
  padding: 11px;
  border-radius: 7px;
  background: var(--ink-50);
}

.reply-item {
  display: flex;
  gap: 8px;
}

.reply-item > div {
  flex: 1;
}

.reply-item__head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.reply-item__head strong {
  color: var(--ink-800);
  font-size: 11px;
}

.reply-item p {
  margin-top: 3px;
  color: var(--ink-700);
  font-size: 12px;
  line-height: 1.65;
}

.reply-trigger {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 9px;
  padding: 3px 0;
  background: transparent;
  color: var(--ink-500);
  cursor: pointer;
  font-size: 11px;
}

.reply-trigger:hover {
  color: var(--teal-700);
}

.reply-compose {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  margin-top: 10px;
}

.discussion-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding-top: 18px;
}

.discussion-pagination span {
  color: var(--ink-500);
  font-size: 11px;
}
</style>
