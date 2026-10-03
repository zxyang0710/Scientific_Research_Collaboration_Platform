<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Eye, EyeOff, LockKeyhole, UserRound } from 'lucide-vue-next'
import { ElMessage } from '../utils/message'
import { useAuthStore } from '../stores/auth'
import AppLogo from '../components/AppLogo.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const mode = ref('login')
const showPassword = ref(false)
const departments = [
  '计算机科学与技术学院',
  '人工智能研究院',
  '软件学院',
  '信息工程学院',
  '生命科学学院',
  '材料科学与工程学院',
  '经济管理学院',
  '其他'
]
const form = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  department: '',
  title: ''
})

const isLogin = computed(() => mode.value === 'login')

function switchMode(value) {
  mode.value = value
  if (value === 'register') {
    form.username = ''
    form.password = ''
  }
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  try {
    if (isLogin.value) {
      await auth.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
    } else {
      if (!form.realName || !form.department || !form.title) {
        ElMessage.warning('请完整填写姓名、所属院系和身份')
        return
      }
      await auth.register({
        username: form.username,
        password: form.password,
        realName: form.realName,
        email: form.email,
        department: form.department,
        title: form.title
      })
      ElMessage.success('注册成功')
    }
    router.replace(route.query.redirect || '/dashboard')
  } catch {
    // The HTTP layer already displays the server message.
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-scene">
      <div class="auth-scene__top"><AppLogo light /></div>
      <div class="auth-scene__brand">
        <p class="auth-scene__english">CAMPUS RESEARCH<br />COLLABORATION</p>
        <h1>校园科研协作平台</h1>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-card__head">
          <h2>{{ isLogin ? '登录工作台' : '创建科研账号' }}</h2>
          <p>{{ isLogin ? '使用校园科研账号继续' : '注册后即可创建或加入项目' }}</p>
        </div>

        <div class="auth-tabs">
          <button type="button" :class="{ active: isLogin }" @click="switchMode('login')">登录</button>
          <button type="button" :class="{ active: !isLogin }" @click="switchMode('register')">注册</button>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <div v-if="!isLogin" class="field">
            <label for="realName">真实姓名</label>
            <div class="input-icon">
              <UserRound :size="17" />
              <input id="realName" v-model.trim="form.realName" class="input" placeholder="请输入真实姓名" />
            </div>
          </div>

          <div class="field">
            <label for="username">用户名</label>
            <div class="input-icon">
              <UserRound :size="17" />
              <input id="username" v-model.trim="form.username" class="input" autocomplete="username" placeholder="请输入用户名" />
            </div>
          </div>

          <div class="field">
            <label for="password">密码</label>
            <div class="input-icon">
              <LockKeyhole :size="17" />
              <input id="password" v-model="form.password" class="input" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" />
              <button class="password-toggle" type="button" title="显示或隐藏密码" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" :size="17" />
                <Eye v-else :size="17" />
              </button>
            </div>
          </div>

          <template v-if="!isLogin">
            <div class="field">
              <label for="email">邮箱</label>
              <input id="email" v-model.trim="form.email" class="input" type="email" placeholder="可选，如 name@campus.edu.cn" />
            </div>
            <div class="field">
              <label for="department">所属院系</label>
              <select id="department" v-model="form.department" class="select" required>
                <option value="">请选择所属院系</option>
                <option v-for="department in departments" :key="department" :value="department">{{ department }}</option>
              </select>
            </div>
            <div class="field">
              <label for="title">身份</label>
              <select id="title" v-model="form.title" class="select" required>
                <option value="">请选择身份</option>
                <option value="教师">教师</option>
                <option value="学生">学生</option>
              </select>
            </div>
          </template>

          <button class="btn btn-primary auth-submit" type="submit" :disabled="auth.loading">
            {{ auth.loading ? '正在处理...' : isLogin ? '进入科研工作台' : '注册并进入' }}
            <ArrowRight v-if="!auth.loading" :size="17" />
          </button>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page { display: grid; grid-template-columns: minmax(460px, 1.08fr) minmax(420px, 0.92fr); min-height: 100vh; background: white; }
.auth-scene { position: relative; display: flex; flex-direction: column; overflow: hidden; padding: 34px 48px 48px; background: var(--ink-950); color: white; }
.auth-scene::before, .auth-scene::after { position: absolute; content: ""; border: 1px solid rgba(255,255,255,.08); border-radius: 50%; }
.auth-scene::before { top: -210px; right: -170px; width: 520px; height: 520px; }
.auth-scene::after { right: -80px; bottom: -280px; width: 470px; height: 470px; }
.auth-scene__top, .auth-scene__brand { position: relative; z-index: 1; }
.auth-scene__brand { margin: auto 0; }
.auth-scene__english { margin-bottom: 18px; color: #73cfc2; font-size: clamp(24px, 2.6vw, 38px); font-weight: 700; letter-spacing: .04em; line-height: 1.15; }
.auth-scene h1 { font-size: clamp(32px, 3.5vw, 50px); line-height: 1.28; }
.auth-panel { display: grid; place-items: center; padding: 48px; background: var(--canvas); }
.auth-card { width: min(420px, 100%); padding: 34px; border: 1px solid var(--ink-200); border-radius: 10px; background: var(--paper); box-shadow: var(--shadow-md); }
.auth-card__head h2 { color: var(--ink-950); font-size: 28px; }
.auth-card__head p { margin-top: 8px; color: var(--ink-500); }
.auth-tabs { display: flex; gap: 24px; margin-top: 32px; border-bottom: 1px solid var(--ink-200); }
.auth-tabs button { position: relative; padding: 0 0 12px; background: transparent; color: var(--ink-500); font-size: 14px; font-weight: 700; }
.auth-tabs button.active { color: var(--ink-950); }
.auth-tabs button.active::after { position: absolute; right: 0; bottom: -1px; left: 0; height: 2px; background: var(--teal-600); content: ""; }
.auth-form { display: grid; gap: 18px; margin-top: 26px; }
.field { display: grid; gap: 8px; }
.field label { color: var(--ink-700); font-size: 13px; font-weight: 700; }
.input-icon { position: relative; }
.input-icon > svg { position: absolute; top: 50%; left: 13px; color: var(--ink-600); transform: translateY(-50%); }
.input-icon .input { padding-left: 40px; }
.input, .select { width: 100%; min-height: 42px; border: 1px solid var(--ink-700); border-radius: 6px; background: white; color: var(--ink-950); font: inherit; outline: none; padding: 10px 12px; }
.input::placeholder { color: var(--ink-500); }
.input:focus, .select:focus { border-color: var(--teal-600); box-shadow: 0 0 0 3px rgba(21,129,124,.14); }
.password-toggle { position: absolute; top: 50%; right: 10px; display: grid; width: 28px; height: 28px; place-items: center; background: transparent; color: var(--ink-600); transform: translateY(-50%); }
.auth-submit { display: inline-flex; width: 100%; justify-content: center; gap: 8px; margin-top: 6px; }
@media (max-width: 860px) {
  .auth-page { display: block; }
  .auth-scene { min-height: 280px; padding: 30px 28px 36px; }
  .auth-scene h1 { font-size: 34px; }
  .auth-panel { padding: 36px 24px 48px; }
}
</style>
