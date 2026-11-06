<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Admin@123' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill(username: string, password: string): void {
  form.username = username
  form.password = password
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="intro">
      <div class="intro-brand">
        <span class="brand-mark">智</span>
        <div>
          <h1>智能耗</h1>
          <p>工业企业能耗画像与节能诊断平台</p>
        </div>
      </div>

      <div class="intro-copy">
        <h2>用数据看清每一度能耗</h2>
        <p>从设备采集、数据治理到动态基线、异常诊断、整改闭环与节能量核算，形成可追溯的能源管理闭环。</p>
        <ul class="feature-list">
          <li><span class="feature-bar"></span>工况驱动的动态能耗基线</li>
          <li><span class="feature-bar"></span>异常识别与诊断报告</li>
          <li><span class="feature-bar"></span>整改任务与节能量核算</li>
        </ul>
      </div>

      <div class="intro-foot">
        <span>工业互联网 · 能源数字化</span>
        <span>SIMULATOR 演示环境</span>
      </div>
    </section>

    <section class="form-side">
      <div class="login-panel">
        <div class="panel-head">
          <h2>账号登录</h2>
          <p>请使用平台分配的账号进入系统</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleSubmit">
          <el-form-item prop="username" label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="handleSubmit">
            登 录
          </el-button>
        </el-form>

        <div class="demo">
          <div class="demo-head">
            <span>演示账号</span>
            <span class="demo-tip">点击自动填充</span>
          </div>
          <button type="button" class="demo-item" @click="fill('admin', 'Admin@123')">
            <span class="demo-role">平台管理员</span>
            <span class="demo-account">admin / Admin@123</span>
          </button>
          <button type="button" class="demo-item" @click="fill('engineer', 'Engineer@123')">
            <span class="demo-role">工程师</span>
            <span class="demo-account">engineer / Engineer@123</span>
          </button>
          <button type="button" class="demo-item" @click="fill('demo_engineer', 'Engineer@123')">
            <span class="demo-role">联调工程师</span>
            <span class="demo-account">demo_engineer / Engineer@123</span>
          </button>
        </div>
      </div>

      <p class="copyright">智能耗 · 工业能耗管理平台</p>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(360px, 46%) 1fr;
  height: 100%;
  background: var(--smarten-bg);
}

.intro {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  color: #dbe7f3;
  background-color: #0b2237;
  background-image:
    linear-gradient(rgba(148, 190, 230, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 190, 230, 0.06) 1px, transparent 1px);
  background-size: 44px 44px;
}

.intro-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #1d4ed8;
  color: #fff;
  font-size: 21px;
  font-weight: 700;
}

.intro-brand h1 {
  margin: 0;
  font-size: 21px;
  letter-spacing: 2px;
  color: #fff;
}

.intro-brand p {
  margin: 3px 0 0;
  font-size: 12px;
  color: #8fa9c2;
}

.intro-copy {
  max-width: 460px;
}

.intro-copy h2 {
  margin: 0 0 14px;
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 1px;
  line-height: 1.35;
  color: #f1f7ff;
}

.intro-copy > p {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: #9db5cc;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 30px 0 0;
  padding: 0;
  list-style: none;
  font-size: 13.5px;
  color: #c4d6e7;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 10px;
}

.feature-bar {
  width: 14px;
  height: 2px;
  background: #3b82f6;
}

.intro-foot {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #6f88a1;
  letter-spacing: 0.4px;
}

.form-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 22px;
  padding: 32px;
}

.login-panel {
  width: 100%;
  max-width: 396px;
  padding: 34px 34px 28px;
  background: #fff;
  border: 1px solid var(--smarten-border);
  border-radius: var(--smarten-radius-lg);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
}

.panel-head {
  margin-bottom: 24px;
}

.panel-head h2 {
  margin: 0;
  font-size: 21px;
  font-weight: 600;
  color: var(--smarten-text-1);
}

.panel-head p {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--smarten-text-3);
}

.submit {
  width: 100%;
  height: 42px;
  margin-top: 6px;
  font-size: 14px;
  letter-spacing: 4px;
}

.demo {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--smarten-border);
}

.demo-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--smarten-text-2);
  font-weight: 500;
}

.demo-tip {
  color: var(--smarten-text-3);
  font-weight: 400;
}

.demo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 9px 10px;
  margin-bottom: 6px;
  background: var(--smarten-surface-muted);
  border: 1px solid transparent;
  border-radius: var(--smarten-radius-sm);
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.demo-item:hover {
  background: var(--smarten-primary-soft);
  border-color: var(--smarten-primary-border);
}

.demo-role {
  color: var(--smarten-text-1);
  font-weight: 500;
}

.demo-account {
  color: var(--smarten-text-3);
  font-variant-numeric: tabular-nums;
}

.copyright {
  margin: 0;
  font-size: 11px;
  color: var(--smarten-text-3);
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .intro {
    display: none;
  }

  .form-side {
    padding: 20px;
  }
}
</style>
