<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHealth } from '@/api/system'
import { useUserStore } from '@/stores/user'
import type { HealthVO } from '@/types/api'

const userStore = useUserStore()
const router = useRouter()

const health = ref<HealthVO | null>(null)
const healthError = ref('')
const healthLoading = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const roleNames: Record<string, string> = {
  PLATFORM_ADMIN: '平台管理员',
  ENERGY_ADMIN: '能源管理员',
  ENGINEER: '工程师',
  VIEWER: '企业管理者'
}

const roles = computed(() => (userStore.profile?.roles ?? []).map((role) => roleNames[role] ?? role))

const shortcuts = computed(() => {
  const items = [
    { title: '能源驾驶舱', desc: '企业能耗总览', path: '/cockpit', perm: 'profile:cockpit:view', icon: 'DataLine' },
    { title: '三维工厂', desc: '设备风险透视', path: '/factory', perm: 'factory:view', icon: 'Monitor' },
    { title: '能耗画像', desc: '企业与设备画像', path: '/profile/enterprise', perm: 'profile:enterprise:view', icon: 'Histogram' },
    { title: '动态基线', desc: '工况模型与偏差', path: '/baseline/models', perm: 'baseline:view', icon: 'TrendCharts' },
    { title: '异常诊断', desc: '规则识别与报告', path: '/diagnosis/overview', perm: 'diagnosis:view', icon: 'Warning' },
    { title: '整改任务', desc: '闭环处置与审核', path: '/rectification/tasks', perm: 'rectify:view', icon: 'Tools' },
    { title: '节能核算', desc: '节能量与金额', path: '/saving/results', perm: 'saving:view', icon: 'Money' },
    { title: '设备管理', desc: '主数据维护', path: '/master/devices', perm: 'master:device:list', icon: 'Cpu' }
  ]
  return items.filter((item) => userStore.hasPermission(item.perm))
})

async function loadHealth(): Promise<void> {
  healthLoading.value = true
  healthError.value = ''
  try {
    health.value = await fetchHealth()
  } catch (exception) {
    health.value = null
    healthError.value = exception instanceof Error ? exception.message : '无法连接后端'
  } finally {
    healthLoading.value = false
  }
}

onMounted(loadHealth)
</script>

<template>
  <div class="page dashboard">
    <section class="welcome">
      <div class="welcome-main">
        <p class="eyebrow">工作台</p>
        <h2>{{ greeting }}，{{ userStore.profile?.realName || userStore.profile?.username }}</h2>
        <p class="sub">
          {{ userStore.profile?.enterpriseName || '平台级账号' }}
          <el-tag v-for="role in roles" :key="role" size="small" class="role-tag">{{ role }}</el-tag>
        </p>
      </div>
      <div class="health" v-loading="healthLoading">
        <template v-if="healthError">
          <el-tag type="danger" effect="plain">后端不可用</el-tag>
          <el-button text size="small" @click="loadHealth">重试</el-button>
        </template>
        <template v-else-if="health">
          <span class="health-dot" :class="{ down: health.status !== 'UP' }"></span>
          <div class="health-meta">
            <span class="health-status">{{ health.status === 'UP' ? '数据服务正常' : health.status }}</span>
            <span class="health-text">{{ health.application }} · JDK {{ health.javaVersion }}</span>
          </div>
        </template>
      </div>
    </section>

    <section>
      <div class="section-head">
        <span class="section-title">常用功能</span>
        <span class="muted">按当前账号权限展示</span>
      </div>
      <el-empty v-if="shortcuts.length === 0" description="当前账号没有可访问的业务页面" />
      <div v-else class="shortcut-grid">
        <button v-for="item in shortcuts" :key="item.path" type="button" class="shortcut" @click="router.push(item.path)">
          <span class="shortcut-icon"><el-icon :size="19"><component :is="item.icon" /></el-icon></span>
          <span class="shortcut-copy">
            <span class="shortcut-title">{{ item.title }}</span>
            <span class="shortcut-desc">{{ item.desc }}</span>
          </span>
          <el-icon class="shortcut-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="info-grid">
      <div class="info-card">
        <span class="info-label">数据口径</span>
        <p>业务页面数据均来自后端接口；演示环境中的采集与生产数据标记为 SIMULATOR，不作为真实计量依据。</p>
      </div>
      <div class="info-card">
        <span class="info-label">分析链路</span>
        <p>采集 → 数据湖 → 画像 → 动态基线 → 异常诊断 → 整改闭环 → 节能核算，全流程可追溯。</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  gap: 20px;
}

.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  padding: 22px 24px;
  background: #fff;
  border: 1px solid var(--smarten-border);
  border-radius: var(--smarten-radius-lg);
  box-shadow: var(--smarten-shadow-card);
}

.eyebrow {
  margin: 0 0 4px;
  font-size: 11px;
  letter-spacing: 1.6px;
  color: var(--smarten-text-3);
  text-transform: uppercase;
}

.welcome-main h2 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 600;
  color: var(--smarten-text-1);
}

.sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  color: var(--smarten-text-3);
  font-size: 13px;
}

.role-tag {
  margin-left: 2px;
}

.health {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 190px;
  padding: 10px 14px;
  background: var(--smarten-surface-muted);
  border: 1px solid var(--smarten-border);
  border-radius: var(--smarten-radius);
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--smarten-success);
}

.health-dot.down {
  background: var(--smarten-danger);
}

.health-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.health-status {
  font-size: 13px;
  font-weight: 500;
  color: var(--smarten-text-1);
}

.health-text {
  font-size: 11px;
  color: var(--smarten-text-3);
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 12px;
}

.shortcut {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 15px 16px;
  text-align: left;
  background: #fff;
  border: 1px solid var(--smarten-border);
  border-radius: var(--smarten-radius);
  box-shadow: var(--smarten-shadow-card);
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.shortcut:hover {
  border-color: var(--smarten-primary-border);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
}

.shortcut-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--smarten-primary-soft);
  color: var(--smarten-primary);
}

.shortcut-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.shortcut-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--smarten-text-1);
}

.shortcut-desc {
  font-size: 11.5px;
  color: var(--smarten-text-3);
}

.shortcut-arrow {
  color: var(--smarten-text-3);
  font-size: 13px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
}

.info-card {
  padding: 15px 18px;
  background: var(--smarten-surface-muted);
  border: 1px solid var(--smarten-border);
  border-radius: var(--smarten-radius);
}

.info-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--smarten-text-2);
}

.info-card p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--smarten-text-3);
}

@media (max-width: 720px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
