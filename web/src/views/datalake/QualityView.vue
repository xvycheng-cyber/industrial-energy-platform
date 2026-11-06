<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { backfillGap, detectGaps, pageFailures, pageGaps, qualitySummary } from '@/api/datalake'
import type { PageParams } from '@/api/datalake'
import type { FailureItem, GapItem, QualitySummary } from '@/types/datalake'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const date = ref(new Date().toISOString().substring(0, 10))
const summary = ref<QualitySummary | null>(null)
const detecting = ref(false)
const activeTab = ref('gaps')

const gaps = usePageList<GapItem>((params) => pageGaps(params as PageParams), {})
const failures = usePageList<FailureItem>((params) => pageFailures(params as PageParams), {})

async function loadSummary(): Promise<void> {
  summary.value = await qualitySummary({ date: date.value })
}

async function refreshAll(): Promise<void> {
  await Promise.all([loadSummary(), gaps.refresh(), failures.refresh()])
}

async function onDetect(): Promise<void> {
  detecting.value = true
  try {
    const count = await detectGaps(date.value)
    ElMessage.success(`检测完成：新增缺口 ${count} 个`)
    await refreshAll()
  } finally {
    detecting.value = false
  }
}

async function onBackfill(row: GapItem): Promise<void> {
  const count = await backfillGap(row.id)
  ElMessage.success(`补录完成：${count} 条`)
  await refreshAll()
}

onMounted(refreshAll)
</script>

<template>
  <div class="datalake-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" style="width: 160px" @change="refreshAll" />
          <el-button @click="refreshAll">刷新</el-button>
          <el-button v-if="userStore.hasPermission('datalake:gap:manage')" type="primary" :loading="detecting" @click="onDetect">
            检测缺失
          </el-button>
        </div>
        <span class="hint">统计日期：{{ date }}</span>
      </div>

      <div class="stat-grid" v-if="summary">
        <div class="stat"><span class="stat-label">原始数据</span><span class="stat-value">{{ summary.total }}</span></div>
        <div class="stat"><span class="stat-label">正常</span><span class="stat-value ok">{{ summary.normal }}</span></div>
        <div class="stat"><span class="stat-label">可疑</span><span class="stat-value warn">{{ summary.suspicious }}</span></div>
        <div class="stat"><span class="stat-label">无效</span><span class="stat-value bad">{{ summary.invalid }}</span></div>
        <div class="stat"><span class="stat-label">待清洗</span><span class="stat-value">{{ summary.pendingClean }}</span></div>
        <div class="stat"><span class="stat-label">待处理失败</span><span class="stat-value bad">{{ summary.failurePending }}</span></div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="数据缺口" name="gaps">
          <el-table v-loading="gaps.loading.value" :data="gaps.records.value" border stripe>
            <el-table-column prop="meterName" label="计量点" width="150" />
            <el-table-column prop="deviceName" label="设备" width="130" />
            <el-table-column prop="gapStart" label="缺口开始" width="170" />
            <el-table-column prop="gapEnd" label="缺口结束" width="170" />
            <el-table-column prop="expectedCount" label="应采" width="90" align="right" />
            <el-table-column prop="missingCount" label="缺失" width="90" align="right" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'danger'" size="small">
                  {{ row.status === 1 ? '已补录' : row.status === 2 ? '已忽略' : '待补录' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 0 && userStore.hasPermission('datalake:gap:manage')"
                  text
                  type="primary"
                  @click="onBackfill(row)"
                >
                  补录
                </el-button>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无缺口，数据完整" /></template>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="gaps.pageNum.value"
              v-model:page-size="gaps.pageSize.value"
              :total="gaps.total.value"
              layout="total, prev, pager, next"
              background
              @current-change="gaps.handlePageChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="失败记录" name="failures">
          <el-table v-loading="failures.loading.value" :data="failures.records.value" border stripe>
            <el-table-column prop="batchNo" label="批次号" min-width="200" />
            <el-table-column prop="meterName" label="计量点" width="140" />
            <el-table-column prop="collectTime" label="采集时间" width="170" />
            <el-table-column prop="errorCode" label="错误码" width="150" />
            <el-table-column prop="errorMsg" label="原因" min-width="240" show-overflow-tooltip />
            <el-table-column prop="createTime" label="记录时间" width="170" />
            <template #empty><el-empty description="暂无失败记录" /></template>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="failures.pageNum.value"
              v-model:page-size="failures.pageSize.value"
              :total="failures.total.value"
              layout="total, prev, pager, next"
              background
              @current-change="failures.handlePageChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.datalake-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hint {
  color: #909399;
  font-size: 13px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  background: #f7f9fc;
  border-radius: 10px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #0f2a43;
}

.stat-value.ok {
  color: #67c23a;
}

.stat-value.warn {
  color: #e6a23c;
}

.stat-value.bad {
  color: #f56c6c;
}
</style>
