import { get, post } from '@/utils/request'
import type { LoginResult, MenuItem, UserProfile } from '@/types/models'

export interface LoginPayload {
  username: string
  password: string
}

export function login(payload: LoginPayload): Promise<LoginResult> {
  return post<LoginResult>('/auth/login', payload)
}

export function fetchProfile(): Promise<UserProfile> {
  return get<UserProfile>('/auth/me')
}

export function fetchMenus(): Promise<MenuItem[]> {
  return get<MenuItem[]>('/auth/menus')
}

export function refreshToken(refreshTokenValue: string): Promise<LoginResult> {
  return post<LoginResult>('/auth/refresh', { refreshToken: refreshTokenValue })
}
