<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  BookOpen,
  CheckCircle2,
  Eye,
  EyeOff,
  FolderKanban,
  LockKeyhole,
  UserRound,
  UsersRound
} from 'lucide-vue-next'
import AppLogo from '../components/AppLogo.vue'
import { ElMessage } from '../utils/message'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const mode = ref('login')
const showPassword = ref(false)
const form = reactive({
  username: 'zhangwei',
  password: '123456',
  realName: '',
  email: '',
  department: '',
  title: ''
})

const isLogin = computed(() => mode.value === 'login')

function switchMode(value) {
  mode.value = value
  if (value === 'register' && form.username === 'zhangwei') {
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
      if (!form.realName) {
        ElMessage.warning('请输入真实姓名')
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

function useDemo(username) {
  mode.value = 'login'
  form.username = username
  form.password = '123456'
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-scene">
      <div class="auth-scene__top">
        <AppLogo light />
      </div>

      <div class="auth-scene__content">
        <p class="auth-scene__eyebrow">CAMPUS RESEARCH WORKSPACE</p>
        <h1>让科研协作的每一步<br />都清晰、可追踪、可沉淀</h1>
        <p class="auth-scene__lead">
          从项目立项、成员分工到任务推进、论文与文件沉淀，为校园科研团队提供同一套协作工作台。
        </p>

        <div class="auth-features">
          <div class="auth-feature">
            <FolderKanban :size="20" />
            <div>
              <strong>项目全过程管理</strong>
              <span>立项、成员、里程碑与进度统一维护</span>
            </div>
          </div>
          <div class="auth-feature">
            <UsersRound :size="20" />
            <div>
              <strong>团队任务协同</strong>
              <span>责任人、截止时间与进度状态一目了然</span>
            </div>
          </div>
          <div class="auth-feature">
            <BookOpen :size="20" />
            <div>
              <strong>成果与资料沉淀</strong>
              <span>论文、项目文件和研究讨论集中归档</span>
            </div>
          </div>
        </div>
      </div>

      <div class="auth-scene__foot">
        <CheckCircle2 :size="16" />
        数据本地部署，适合校内科研团队使用
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
              <input
                id="username"
                v-model.trim="form.username"
                class="input"
                autocomplete="username"
                placeholder="请输入用户名"
              />
            </div>
          </div>

          <div class="field">
            <label for="password">密码</label>
            <div class="input-icon">
              <LockKeyhole :size="17" />
              <input
                id="password"
                v-model="form.password"
                class="input"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="请输入密码"
              />
              <button class="password-toggle" type="button" title="显示或隐藏密码" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" :size="17" />
                <Eye v-else :size="17" />
              </button>
            </div>
          </div>

          <template v-if="!isLogin">
            <div class="field">
              <label for="email">邮箱</label>
              <input id="email" v-model.trim="form.email" class="input" type="email" placeholder="name@campus.edu.cn" />
            </div>
            <div class="field">
              <label for="department">所属院系</label>
              <input id="department" v-model.trim="form.department" class="input" placeholder="如：计算机科学与技术学院" />
            </div>
            <div class="field">
              <label for="title">职称 / 身份</label>
              <input id="title" v-model.trim="form.title" class="input" placeholder="如：教授、讲师、研究生" />
            </div>
          </template>

          <button class="btn btn-primary auth-submit" type="submit" :disabled="auth.loading">
            {{ auth.loading ? '正在处理...' : isLogin ? '进入科研工作台' : '注册并进入' }}
            <ArrowRight v-if="!auth.loading" :size="17" />
          </button>
        </form>

        <div v-if="isLogin" class="demo-accounts">
          <span>演示账号</span>
          <button type="button" @click="useDemo('zhangwei')">张伟 · 项目负责人</button>
          <button type="button" @click="useDemo('chenyu')">陈宇 · 研究生成员</button>
          <button type="button" @click="useDemo('admin')">系统管理员</button>
          <small>统一密码：123456</small>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(460px, 1.08fr) minmax(420px, 0.92fr);
  min-height: 100vh;
  background: white;
}

.auth-scene {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 34px 48px 28px;
  background: var(--ink-950);
  color: white;
}

.auth-scene::before,
.auth-scene::after {
  position: absolute;
  content: "";
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.auth-scene::before {
  top: -210px;
  right: -170px;
  width: 520px;
  height: 520px;
}

.auth-scene::after {
  right: -80px;
  bottom: -280px;
  width: 470px;
  height: 470px;
}

.auth-scene__top,
.auth-scene__content,
.auth-scene__foot {
  position: relative;
  z-index: 1;
}

.auth-scene__content {
  width: min(580px, 100%);
  margin: auto 0;
  padding: 70px 0;
}

.auth-scene__eyebrow {
  margin-bottom: 18px;
  color: #73cfc2;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.auth-scene h1 {
  font-size: clamp(32px, 3.5vw, 50px);
  line-height: 1.28;
  letter-spacing: -0.02em;
}

.auth-scene__lead {
  max-width: 530px;
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 15px;
  line-height: 1.9;
}

.auth-features {
  display: grid;
  gap: 12px;
  margin-top: 46px;
}

.auth-feature {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px 15px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.auth-feature > svg {
  flex: 0 0 auto;
  color: #73cfc2;
}

.auth-feature div {
  display: flex;
  flex-direction: column;
}

.auth-feature strong {
  font-size: 13px;
}

.auth-feature span {
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.52);
  font-size: 11px;
}

.auth-scene__foot {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.48);
  font-size: 11px;
}

.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #f7f9f8;
}

.auth-card {
  width: min(420px, 100%);
}

.auth-card__head h2 {
  color: var(--ink-950);
  font-size: 27px;
}

.auth-card__head p {
  margin-top: 6px;
  color: var(--ink-500);
  font-size: 13px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-top: 28px;
  padding: 4px;
  border: 1px solid var(--ink-200);
  border-radius: 7px;
  background: white;
}

.auth-tabs button {
  height: 36px;
  border-radius: 5px;
  background: transparent;
  color: var(--ink-500);
  cursor: pointer;
  font-weight: 600;
}

.auth-tabs button.active {
  background: var(--ink-950);
  color: white;
}

.auth-form {
  display: grid;
  gap: 16px;
  margin-top: 24px;
}

.password-toggle {
  position: absolute;
  right: 4px;
  top: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: var(--ink-500);
  background: transparent;
  cursor: pointer;
  transform: translateY(-50%);
}

.auth-form .input-icon .input {
  padding-right: 40px;
}

.auth-submit {
  height: 43px;
  margin-top: 6px;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid var(--ink-200);
}

.demo-accounts > span {
  width: 100%;
  margin-bottom: 2px;
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.demo-accounts button {
  padding: 5px 9px;
  border: 1px solid var(--ink-200);
  border-radius: 5px;
  background: white;
  color: var(--ink-700);
  cursor: pointer;
  font-size: 11px;
}

.demo-accounts button:hover {
  border-color: var(--teal-600);
  color: var(--teal-700);
}

.demo-accounts small {
  width: 100%;
  margin-top: 3px;
  color: var(--ink-500);
}

@media (max-width: 1000px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-scene {
    min-height: 430px;
    padding: 28px;
  }

  .auth-scene__content {
    padding: 55px 0;
  }

  .auth-features {
    display: none;
  }

  .auth-scene h1 {
    font-size: 34px;
  }

  .auth-panel {
    padding: 42px 20px 60px;
  }
}

@media (max-width: 560px) {
  .auth-scene {
    min-height: 360px;
    padding: 22px;
  }

  .auth-scene__content {
    padding: 38px 0 24px;
  }

  .auth-scene h1 {
    font-size: 27px;
  }

  .auth-scene__lead {
    margin-top: 14px;
    font-size: 13px;
  }

  .auth-scene__foot {
    display: none;
  }
}
</style>
