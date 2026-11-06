import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import type { LoginPayload } from '@/api/auth'
import type { MenuItem, UserProfile } from '@/types/models'
import { clearAuth, getRefreshToken, getToken, setRefreshToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const refreshToken = ref<string>(getRefreshToken())
  const profile = ref<UserProfile | null>(null)
  const menus = ref<MenuItem[]>([])
  const loaded = ref(false)

  const isLoggedIn = computed(() => token.value.length > 0)
  const permissions = computed(() => profile.value?.permissions ?? [])
  const roles = computed(() => profile.value?.roles ?? [])

  function hasPermission(code: string): boolean {
    return permissions.value.includes('*') || permissions.value.includes(code)
  }

  function applySession(accessToken: string, refresh: string, user: UserProfile): void {
    token.value = accessToken
    refreshToken.value = refresh
    profile.value = user
    setToken(accessToken)
    setRefreshToken(refresh)
  }

  async function login(payload: LoginPayload): Promise<void> {
    const result = await authApi.login(payload)
    applySession(result.accessToken, result.refreshToken, result.user)
    menus.value = await authApi.fetchMenus()
    loaded.value = true
  }

  async function loadProfile(): Promise<void> {
    const [user, menuList] = await Promise.all([authApi.fetchProfile(), authApi.fetchMenus()])
    profile.value = user
    menus.value = menuList
    loaded.value = true
  }

  function logout(): void {
    token.value = ''
    refreshToken.value = ''
    profile.value = null
    menus.value = []
    loaded.value = false
    clearAuth()
  }

  return { token, refreshToken, profile, menus, loaded, isLoggedIn, permissions, roles, hasPermission, login, loadProfile, logout }
})
