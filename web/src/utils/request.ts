import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'
import { clearAuth, getToken } from './auth'

/** 业务异常：HTTP 200 但 code 非 200 时抛出 */
export class BusinessError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'BusinessError'
    this.code = code
  }
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

/** 请求头 X-Silent: true 时不弹出全局错误提示（由调用方自行处理） */
function isSilent(config?: InternalAxiosRequestConfig | AxiosRequestConfig): boolean {
  const headers = config?.headers as Record<string, unknown> | undefined
  return headers?.['X-Silent'] === true || headers?.['X-Silent'] === 'true'
}

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

service.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const payload = response.data as ApiResult<unknown> | undefined
    if (!payload || typeof payload.code !== 'number') {
      return response
    }
    if (payload.code !== 200) {
      if (!isSilent(response.config)) {
        ElMessage.error(payload.message || '请求失败')
      }
      return Promise.reject(new BusinessError(payload.code, payload.message))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const serverMessage = error.response?.data?.message

    if (status === 401) {
      clearAuth()
      if (!isSilent(error.config)) {
        ElMessage.error(serverMessage ?? '登录已过期，请重新登录')
      }
      // P2 引入登录页后改为跳转 /login；当前只有工程自检页
      if (window.location.pathname !== '/') {
        window.location.replace('/')
      }
      return Promise.reject(new BusinessError(401, serverMessage ?? '未登录'))
    }

    let message: string
    if (!error.response) {
      message =
        error.code === 'ECONNABORTED'
          ? '请求超时，请稍后重试'
          : '网络异常，请确认后端服务已启动'
    } else if (status === 403) {
      message = serverMessage ?? '没有访问权限'
    } else if (status === 404) {
      message = serverMessage ?? '接口不存在'
    } else if (status && status >= 500) {
      message = serverMessage ?? '服务器内部错误'
    } else {
      message = serverMessage ?? `请求失败（${status}）`
    }

    if (!isSilent(error.config)) {
      ElMessage.error(message)
    }
    return Promise.reject(new BusinessError(status ?? 0, message))
  }
)

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await service.get<ApiResult<T>>(url, { params })
  return response.data.data
}

export async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await service.post<ApiResult<T>>(url, data, config)
  return response.data.data
}

export async function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await service.put<ApiResult<T>>(url, data, config)
  return response.data.data
}

export async function del<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await service.delete<ApiResult<T>>(url, { params })
  return response.data.data
}

export default service
