import { reactive, ref, type Ref } from 'vue'
import type { PageResult } from '@/types/api'

export interface ListState<T> {
  loading: Ref<boolean>
  error: Ref<string>
  records: Ref<T[]>
  total: Ref<number>
  pageNum: Ref<number>
  pageSize: Ref<number>
  /** 查询条件容器：字段随页面而异，绑定到表单控件 */
  query: Record<string, any>
  load: () => Promise<void>
  search: () => void
  reset: () => void
  refresh: () => void
  handlePageChange: (page: number) => void
  handleSizeChange: (size: number) => void
}

/**
 * 列表页通用逻辑：分页、加载态、错误态、查询与重置。
 */
export function usePageList<T>(
  fetcher: (params: Record<string, any>) => Promise<PageResult<T>>,
  initialQuery: Record<string, any> = {}
): ListState<T> {
  const loading = ref(false)
  const error = ref('')
  const records = ref([]) as Ref<T[]>
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const query = reactive<Record<string, any>>({ ...initialQuery })

  async function load(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      const result = await fetcher({ ...query, pageNum: pageNum.value, pageSize: pageSize.value })
      records.value = result.records ?? []
      total.value = result.total ?? 0
    } catch (exception) {
      error.value = exception instanceof Error ? exception.message : '加载失败，请重试'
      records.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  function search(): void {
    pageNum.value = 1
    void load()
  }

  function reset(): void {
    Object.assign(query, initialQuery)
    pageNum.value = 1
    void load()
  }

  function refresh(): void {
    void load()
  }

  function handlePageChange(page: number): void {
    pageNum.value = page
    void load()
  }

  function handleSizeChange(size: number): void {
    pageSize.value = size
    pageNum.value = 1
    void load()
  }

  return { loading, error, records, total, pageNum, pageSize, query, load, search, reset, refresh, handlePageChange, handleSizeChange }
}
