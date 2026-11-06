<script setup lang="ts">
import { onMounted } from 'vue'
import { pageLogs } from '@/api/datalake'
import type { PageParams } from '@/api/datalake'
import type { IngestLogItem } from '@/types/datalake'
import { usePageList } from '@/composables/usePageList'

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<IngestLogItem>(
    (params) => pageLogs(params as PageParams),
    { batchNo: '', status: '', startDate: '', endDate: '' }
  )

function statusInfo(status: number): { type: string; label: string } {
  const map: Record<number, { type: string; label: string }> = {
    0: { type: 'info', label: '进行中' },
    1: { type: 'success', label: '成功' },
    2: { type: 'warning', label: '部分成功' },
    3: { type: 'danger', label: '失败' }
  }
  return map[status] ?? { type: 'info', label: '未知' }
}

onMounted(refresh)
</script>

<template>
  <div class="datalake-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.batchNo" placeholder="批次号" clearable style="width: 220px" @keyup.enter="search" />
          <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 150px" />
          <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width: 150px" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
            <el-option label="成功" :value="1" />
            <el-option label="部分成功" :value="2" />
            <el-option label="失败" :value="3" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
        <template #default><el-button size="small" @click="refresh">重新加载</el-button></template>
      </el-alert>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="batchNo" label="批次号" min-width="220" />
        <el-table-column prop="source" label="来源" width="110" />
        <el-table-column prop="enterpriseName" label="企业" min-width="170" show-overflow-tooltip />
        <el-table-column prop="dataDate" label="数据日期" width="120" />
        <el-table-column prop="receivedCount" label="接收" width="80" align="right" />
        <el-table-column prop="validCount" label="有效" width="80" align="right" />
        <el-table-column prop="missingCount" label="缺失" width="80" align="right" />
        <el-table-column prop="successRate" label="完整率(%)" width="110" align="right" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).type as any" size="small">{{ statusInfo(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <template #empty><el-empty description="暂无入库日志" /></template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.error-bar {
  margin-bottom: 12px;
}
</style>
