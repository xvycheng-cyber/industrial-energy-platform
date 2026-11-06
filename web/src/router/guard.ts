import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由守卫：标题、登录态、权限码校验。
 */
export function setupRouterGuard(router: Router): void {
  router.beforeEach(async (to) => {
    const appTitle = import.meta.env.VITE_APP_TITLE || '智能耗'
    document.title = to.meta.title ? `${to.meta.title} · ${appTitle}` : appTitle

    const userStore = useUserStore()

    if (to.meta.public === true) {
      if (to.path === '/login' && userStore.isLoggedIn) {
        return { path: '/dashboard' }
      }
      return true
    }

    if (!userStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (!userStore.loaded) {
      try {
        await userStore.loadProfile()
      } catch {
        userStore.logout()
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }

    const required = to.meta.permissions
    if (required?.length && !required.every((code) => userStore.hasPermission(code))) {
      return { path: '/403', query: { from: to.fullPath } }
    }

    return true
  })
}
