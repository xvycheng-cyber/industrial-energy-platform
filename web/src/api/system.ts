import { get } from '@/utils/request'
import type { HealthVO } from '@/types/api'

/** 系统健康检查（后端 /api/system/health） */
export function fetchHealth(): Promise<HealthVO> {
  return get<HealthVO>('/system/health', undefined)
}
