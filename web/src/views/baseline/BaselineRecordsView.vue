<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/BaseChart.vue'
import { pageBaselineModels, pageBaselineRecords } from '@/api/baseline'
import { pageDevices } from '@/api/master'
import type { BaselineRecord } from '@/types/baseline'
import type { Device } from '@/types/models'

const loading = ref(false)
const chartLoading = ref(false)
const records = ref<BaselineRecord[]>([])
const chartRecords = ref<BaselineRecord[]>([])
const total = ref(0)
const devices = ref<Device[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  dimType: 4,
  dimId: 1,
  startDate: '',
  endDate: '',
  onlyDeviation: false
})

function initDates(): void {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  query.endDate = end.toISOString().slice(0, 10)
  query.startDate = start.toISOString().slice(0, 10)
}

const chartOption = computed<EChartsOption>(() => {
  const list = [...chartRecords.value].sort((a, b) => a.periodStart.localeCompare(b.periodStart))
  return {
    grid: { left: 50, right: 20, top: 34, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { top: 4, textStyle: { color: '#606266', fontSize: 12 } },
    xAxis: {
      type: 'category',
      data: list.map((item) => item.periodStart.slice(5, 16)),
      axisLabel: { color: '#909399', fontSize: 10, interval: Math.max(0, Math.floor(list.length / 10)) },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      name: 'kWh',
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      {
        name: '实际能耗',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: list.map((item) => item.actualValue ?? null),
        lineStyle: { width: 2, color: '#2f6fed' },
        areaStyle: { color: 'rgba(47,111,237,0.08)' }
      },
      {
        name: '动态基线',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: list.map((item) => item.baselineValue),
        lineStyle: { width: 2, color: '#e6a23c', type: 'dashed' }
      }
    ]
  }
})

function deviationType(rate?: number): 'success' | 'warning' | 'danger' | 'info' {
  if (rate === undefined || rate === null) return 'info'
  const value = Math.abs(rate)
  if (value > 20) return 'danger'
  if (value > 10) return 'warning'
  return 'success'
}

async function loadTable(): Promise<void> {
  loading.value = true
  try {
    const page = await pageBaselineRecords({ ...query })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function loadChart(): Promise<void> {
  chartLoading.value = true
  try {
    const page = await pageBaselineRecords({ ...query, pageNum: 1, pageSize: 300 })
    chartRecords.value = [...page.records].reverse()
  } finally {
    chartLoading.value = false
  }
}

async function refresh(): Promise<void> {
  await Promise.all([loadTable(), loadChart()])
}

onMounted(async () => {
  initDates()
  const [page, models] = await Promise.all([
    pageDevices({ pageNum: 1, pageSize: 200 }),
    pageBaselineModels({ pageNum: 1, pageSize: 1, dimType: 4, status: 1 })
  ])
  devices.value = page.records
  query.dimId = models.records[0]?.dimId ?? page.records[0]?.id ?? 1
  await refresh()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.dimId" filterable style="width: 220px" @change="refresh">
          <el-option v-for="item in devices" :key="item.id" :label="`${item.name}（${item.code}）`" :value="item.id" />
        </el-select>
        <el-date-picker
          v-model="query.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始"
          style="width: 145px"
          @change="refresh"
        />
        <span class="sep">至</span>
        <el-date-picker
          v-model="query.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束"
          style="width: 145px"
          @change="refresh"
        />
        <el-checkbox v-model="query.onlyDeviation" @change="refresh">仅看有偏差</el-checkbox>
        <el-button :loading="loading" @click="refresh">刷新</el-button>
      </div>

      <div class="chart" v-loading="chartLoading">
        <BaseChart v-if="chartRecords.length" :option="chartOption" height="280px" />
        <el-empty v-else description="所选区间暂无基线数据，请先在基线模型中训练" />
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>基线明细（实际 vs 基线）</span>
          <span class="muted">偏差率 =（实际 − 基线）/ 基线 × 100%</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="periodStart" label="统计周期" min-width="155" />
        <el-table-column label="负荷率" width="90">
          <template #default="{ row }">{{ row.condition?.loadRate ?? '—' }}%</template>
        </el-table-column>
        <el-table-column label="产量" width="90">
          <template #default="{ row }">{{ row.condition?.outputQty ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="温度" width="80">
          <template #default="{ row }">{{ row.condition?.ambientTemp ?? '—' }}℃</template>
        </el-table-column>
        <el-table-column label="产品/班次" min-width="140">
          <template #default="{ row }">{{ row.condition?.productType ?? '—' }} / {{ row.condition?.shift ?? '—' }}班</template>
        </el-table-column>
        <el-table-column prop="actualValue" label="实际 kWh" width="110" />
        <el-table-column prop="baselineValue" label="基线 kWh" width="110" />
        <el-table-column prop="deviationValue" label="偏差 kWh" width="110" />
        <el-table-column label="偏差率" width="110">
          <template #default="{ row }">
            <el-tag :type="deviationType(row.deviationRate)" size="small">
              {{ row.deviationRate != null ? Number(row.deviationRate).toFixed(2) : '—' }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baselineTypeName" label="预测类型" width="110" />
        <el-table-column prop="modelVersion" label="模型版本" min-width="150" />
        <template #empty><el-empty description="暂无基线明细" /></template>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @size-change="loadTable"
          @current-change="loadTable"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 8px;
}

.sep {
  color: #909399;
}

.chart {
  min-height: 280px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.muted {
  font-size: 12px;
  color: #909399;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
