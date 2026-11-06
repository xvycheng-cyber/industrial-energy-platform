export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: string
  traceId: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
  orderBy?: string
  asc?: boolean
}

export interface HealthVO {
  status: string
  application: string
  version: string
  profiles: string[]
  javaVersion: string
  timeZone: string
  serverTime: string
}
