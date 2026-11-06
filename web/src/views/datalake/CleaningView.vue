<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aggregateDay, cleanData, pageDetails } from '@/api/datalake'
import type { PageParams } from '@/api/datalake'
import { pageMeters } from '@/api/master'
import type { MeterItem } from '@/types/models'
import type { DetailItem } from '@/types/datalake'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const meters = ref<MeterItem[]>([])
const cleaning = ref(false)
const aggregating = ref(false)

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<DetailItem>(
    (params) => pageDetails(params as PageParams),
    { meterId: '', qualityFlag: '', start: '', end: '' }
  )

const qualityOptions = [
  { value: 1, label: '正常' },
  { value: 2, label: '越界' },
  { value: 3, label: '缺失补全' },
  { value: 4, label: '重复' },
  { value: 5, label: '人工修正' }
]

function qualityTag(flag: number): { type: string; label: string } {
  const map: Record<number, { type: string; label: string }> = {
    1: { type: 'success', label: '正常' },
    2: { type: 'warning', label: '越界' },
    3: { type: 'info', label: '缺失补全' },
    4: { type: 'danger', label: '重复' },
    5: { type: 'info', label: '人工修正' }
  }
  return map[flag] ?? { type: 'info', label: '未知' }
}


async function runClean(): Promise<void> {
  cleaning.value = true
  try {
    const payload: { batchNo?: string; date?: string; limit?: number } = { limit: 5000 }
    if (query.start) payload.date = (query.start as string).substring(0, 10)
    const count = await cleanData(payload)
    ElMessage.success(`清洗完成：处理 ${count} 条`)
    refresh()
  } finally {
    cleaning.value = false
  }
}

async function runAggregate(): Promise<void> {
  const day = query.start ? (query.start as string).substring(0, 10) : new Date().toISOString().substring(0, 10)
  aggregating.value = true
  try {
    const count = await aggregateDay(day)
    ElMessage.success(`聚合完成：${count} 个周期桶`)
  } finally {
    aggregating.value = false
  }
}

onMounted(async () => {
  const result = await pageMeters({ pageNum: 1, pageSize: 500 })
  meters.value = result.records
  await refresh()
})
</script>

<template>
  <div class="datalake-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="query.meterId" placeholder="计量点" clearable filterable style="width: 220px">
            <el-option v-for="m in meters" :key="m.id" :label="`${m.meterCode} ${m.meterName}`" :value="m.id" />
          </el-select>
          <el-date-picker
            v-model="query.start"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="开始时间"
            style="width: 200px"
          />
          <el-date-picker
            v-model="query.end"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="结束时间"
            style="width: 200px"
          />
          <el-select v-model="query.qualityFlag" placeholder="质量标记" clearable style="width: 140px">
            <el-option v-for="item in qualityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button v-if="userStore.hasPermission('datalake:clean')" type="warning" :loading="cleaning" @click="runClean">
            触发清洗
          </el-button>
          <el-button v-if="userStore.hasPermission('datalake:clean')" type="primary" plain :loading="aggregating" @click="runAggregate">
            重算聚合
          </el-button>
        </div>
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
        <template #default><el-button size="small" @click="refresh">重新加载</el-button></template>
      </el-alert>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="meterName" label="计量点" width="150" />
        <el-table-column prop="deviceName" label="设备" width="130" />
        <el-table-column prop="periodStart" label="区间开始" width="165" />
        <el-table-column prop="periodEnd" label="区间结束" width="165" />
        <el-table-column prop="startReading" label="起始读数" width="120" align="right" />
        <el-table-column prop="endReading" label="结束读数" width="120" align="right" />
        <el-table-column label="周期能耗" width="140" align="right">
          <template #default="{ row }">{{ row.energyValue }} {{ row.unit || '' }}</template>
        </el-table-column>

        <el-table-column label="质量" width="110">
          <template #default="{ row }">
            <el-tag :type="qualityTag(row.qualityFlag).type as any" size="small">{{ qualityTag(row.qualityFlag).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="补录" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.estimated === 1" type="info" size="small">估算</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <template #empty><el-empty description="暂无清洗明细" /></template>
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
