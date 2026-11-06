<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

const activePath = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string | undefined) ?? '工作台')
const displayName = computed(() => userStore.profile?.realName || userStore.profile?.username || '未登录')
const roleText = computed(() => userStore.profile?.roles?.join(' / ') ?? '-')

onMounted(() => {
  if (window.innerWidth < 1100) {
    collapsed.value = true
  }
})

async function handleLogout(): Promise<void> {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '232px'" class="aside">
      <div class="logo">
        <span class="logo-mark">智</span>
        <div v-show="!collapsed" class="logo-copy">
          <span class="logo-text">智能耗</span>
          <span class="logo-sub">工业能耗管理平台</span>
        </div>
      </div>

      <el-menu
        :default-active="activePath"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        background-color="#0b1f33"
        text-color="#9fb3c8"
        active-text-color="#ffffff"
        class="menu"
      >
        <template v-for="menu in userStore.menus" :key="menu.key">
          <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.key">
            <template #title>
              <el-icon><component :is="menu.icon || 'Menu'" /></el-icon>
              <span>{{ menu.title }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.key" :index="child.path">
              <el-icon><component :is="child.icon || 'Menu'" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="menu.icon || 'Menu'" /></el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div v-show="!collapsed" class="aside-foot">
        <span class="foot-dot"></span>
        <span>数据服务正常</span>
      </div>
    </el-aside>

    <el-container class="body">
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <component :is="collapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">
              <span class="crumb-home"><el-icon><HomeFilled /></el-icon> 首页</span>
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <el-dropdown trigger="click">
          <div class="user-box">
            <el-avatar :size="30" class="avatar">{{ displayName.slice(0, 1) }}</el-avatar>
            <div class="user-meta">
              <span class="user-name">{{ displayName }}</span>
              <span class="user-role">{{ roleText }}</span>
            </div>
            <el-icon class="user-arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                {{ userStore.profile?.enterpriseName || '平台级账号' }}
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  display: flex;
  flex-direction: column;
  background: #0b1f33;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  transition: width 0.18s ease;
  overflow-x: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 58px;
  padding: 0 16px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.logo-mark {
  display: grid;
  place-items: center;
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #1d4ed8;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.logo-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1.5px;
  white-space: nowrap;
}

.logo-sub {
  margin-top: 2px;
  font-size: 10px;
  letter-spacing: 0.5px;
  color: #6f88a1;
  white-space: nowrap;
}

.menu {
  flex: 1;
  border-right: none;
  padding: 10px 8px;
  overflow-y: auto;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  height: 42px;
  margin: 2px 0;
  border-radius: 7px;
  font-size: 13.5px;
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(37, 99, 235, 0.24);
  box-shadow: inset 3px 0 0 #3b82f6;
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.menu :deep(.el-sub-menu .el-menu-item) {
  min-width: 0;
  height: 38px;
  padding-left: 44px !important;
  font-size: 13px;
}

.menu :deep(.el-menu--collapse .el-sub-menu .el-menu-item) {
  padding-left: 16px !important;
}

.aside-foot {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 42px;
  padding: 0 18px;
  font-size: 11px;
  color: #6f88a1;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

.foot-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
}

.body {
  background: var(--smarten-bg);
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 58px;
  padding: 0 22px;
  background: #fff;
  border-bottom: 1px solid var(--smarten-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.collapse-btn {
  cursor: pointer;
  font-size: 18px;
  color: var(--smarten-text-2);
}

.collapse-btn:hover {
  color: var(--smarten-primary);
}

.crumb-home {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 6px;
  border-radius: 8px;
  cursor: pointer;
}

.user-box:hover {
  background: var(--smarten-surface-muted);
}

.avatar {
  background: var(--smarten-primary);
  color: #fff;
  font-size: 13px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--smarten-text-1);
}

.user-role {
  font-size: 10.5px;
  color: var(--smarten-text-3);
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  color: var(--smarten-text-3);
  font-size: 12px;
}

.main {
  padding: 18px 20px 24px;
  overflow-y: auto;
}
</style>
