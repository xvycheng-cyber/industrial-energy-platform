import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 应用级状态：标题、侧边栏折叠、全局加载计数。
 */
export const useAppStore = defineStore('app', () => {
  const title = ref<string>(import.meta.env.VITE_APP_TITLE || '智能耗')
  const sidebarCollapsed = ref(false)
  const loadingCount = ref(0)

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function startLoading(): void {
    loadingCount.value += 1
  }

  function stopLoading(): void {
    loadingCount.value = Math.max(0, loadingCount.value - 1)
  }

  return { title, sidebarCollapsed, loadingCount, toggleSidebar, startLoading, stopLoading }
})
