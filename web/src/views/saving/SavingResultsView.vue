<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { ElMessage } from 'element-plus'
import BaseChart from '@/components/BaseChart.vue'
import {
  confirmSavingResult,
  exportSavingCsv,
  fetchSavingSummary,
  pageSavingResults
} from '@/api/saving'
import type { SavingResult, SavingSummary } from '@/types/saving'

const router = useRouter()
const loading = ref(false)
const exporting = ref(false)
const records = ref<SavingResult[]>([])
const total = ref(0)
const summary = ref<SavingSummary | null>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined,
  conclusionCode: undefined as number | undefined,
  includeArchived: false
})

const conclusionMap: Record<number, { label: string; type: 'success' | 'info' | 'danger' | 'warning' }> = {
  1: { label: '节能达成', type: 'success' },
  2: { label: '无明显变化', type: 'info' },
  3: { label: '能耗恶化', type: 'danger' },
  4: { label: '数据不足待验证', type: 'warning' },
  5: { label: '工况不可比', type: 'warning' },
  6: { label: '停机不判定节能', type: 'info' }
}

const rankingOption = computed<EChartsOption>(() => {
  const list = [...(summary.value?.ranking ?? [])].reverse()
  return {
    grid: { left: 118, right: 30, top: 16, bottom: 24 },
    tooltip: { trigger: 'axis', valueFormatter: (value: unknown) => `${value} kWh` },
    xAxis: { type: 'value', axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    yAxis: { type: 'category', data: list.map((item) => item.deviceName), axisLabel: { color: '#606266', fontSize: 11 } },
    series: [{
      type: 'bar',
      barWidth: 14,
      data: list.map((item) => ({
        value: item.saving,
        itemStyle: { color: Number(item.saving) < 0 ? '#dc2626' : '#16a34a', borderRadius: [0, 4, 4, 0] }
      }))
    }]
  }
})

const trendOption = computed<EChartsOption>(() => {
  const list = summary.value?.trend ?? []
  return {
    grid: { left: 50, right: 20, top: 34, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#606266', fontSize: 12 } },
    xAxis: { type: 'category', data: list.map((item) => item.month), axisLabel: { color: '#909399', fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { color: '#909399', fontSize: 11 }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [
      { name: '节能量 kWh', type: 'line', smooth: true, data: list.map((item) => item.saving), itemStyle: { color: '#2f9e44' }, areaStyle: { color: 'rgba(47,158,68,0.10)' } },
      { name: '节能金额 元', type: 'line', smooth: true, data: list.map((item) => item.amount), itemStyle: { color: '#e6a23c' } }
    ]
  }
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const [page, stat] = await Promise.all([
      pageSavingResults({ ...query }),
      fetchSavingSummary()
    ])
    records.value = page.records
    total.value = page.total
    summary.value = stat
  } finally {
    loading.value = false
  }
}

async function exportCsv(): Promise<void> {
  exporting.value = true
  try {
    await exportSavingCsv({ status: query.status, conclusionCode: query.conclusionCode })
    ElMessage.success('导出已开始')
  } finally {
    exporting.value = false
  }
}

async function audit(item: SavingResult, pass: boolean): Promise<void> {
  await confirmSavingResult(item.id, pass, pass ? '核算结果审核通过' : '核算结果驳回，退回整改')
  ElMessage.success(pass ? '审核通过，整改任务已完成' : '已驳回，任务退回整改')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>节能效果核算</h2>
          <p class="muted">按观察期逐小时「修正基线 − 实际」核算，可复算并保留版本。</p>
        </div>
        <div class="actions">
          <el-button type="primary" :loading="exporting" @click="exportCsv">导出 CSV</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>
      <div class="kpi-row">
        <div class="kpi"><span>累计节能量</span><strong class="green">{{ summary?.totalSaving ?? 0 }}<em>kWh</em></strong></div>
        <div class="kpi"><span>累计节能金额</span><strong>{{ summary?.totalAmount ?? 0 }}<em>元</em></strong></div>
        <div class="kpi"><span>核算记录</span><strong>{{ summary?.resultCount ?? 0 }}</strong></div>
        <div class="kpi">
          <span>平均节能率</span>
          <strong>{{ records.length ? (records.reduce((sum, item) => sum + Number(item.savingRate || 0), 0) / records.length).toFixed(2) : 0 }}<em>%</em></strong>
        </div>
      </div>
    </el-card>

    <div class="grid">
      <el-card shadow="never" class="span-7">
        <template #header><span class="title">设备节能排名</span></template>
        <BaseChart v-if="(summary?.ranking.length ?? 0) > 0" :option="rankingOption" height="260px" />
        <el-empty v-else description="暂无核算数据" />
      </el-card>
      <el-card shadow="never" class="span-5">
        <template #header><span class="title">节能趋势</span></template>
        <BaseChart v-if="(summary?.trend.length ?? 0) > 0" :option="trendOption" height="260px" />
        <el-empty v-else description="暂无趋势数据" />
      </el-card>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load">
          <el-option label="待审核" :value="0" />
          <el-option label="已生效" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-select v-model="query.conclusionCode" placeholder="全部结论" clearable style="width: 160px" @change="load">
          <el-option v-for="(item, key) in conclusionMap" :key="key" :label="item.label" :value="Number(key)" />
        </el-select>
        <el-checkbox v-model="query.includeArchived" @change="load">包含历史版本</el-checkbox>
        <el-button :loading="loading" @click="load">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="taskNo" label="任务号" width="170" />
        <el-table-column prop="deviceName" label="设备" width="120" />
        <el-table-column label="观察期" width="175">
          <template #default="{ row }">{{ row.verifyStart?.slice(0, 10) }} ~ {{ row.verifyEnd?.slice(0, 10) }}</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="baselineTotal" label="基线 kWh" width="105" />
        <el-table-column prop="actualTotal" label="实际 kWh" width="105" />
        <el-table-column label="节能量" width="100">
          <template #default="{ row }">
            <span :class="Number(row.savingValue) < 0 ? 'red' : 'green'">{{ row.savingValue }}</span>
          </template>
        </el-table-column>
        <el-table-column label="节能率" width="100">
          <template #default="{ row }">{{ Number(row.savingRate || 0).toFixed(2) }}%</template>
        </el-table-column>
        <el-table-column label="金额(元)" width="105"><template #default="{ row }">{{ Number(row.savingAmount || 0).toFixed(2) }}</template></el-table-column>
        <el-table-column label="结论" width="140">
          <template #default="{ row }">
            <el-tag :type="conclusionMap[row.conclusionCode]?.type || 'info'" size="small">
              {{ conclusionMap[row.conclusionCode]?.label || row.conclusion }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="router.push(`/saving/results/${row.id}`)">详情</el-button>
            <template v-if="row.status === 0">
              <el-button text type="success" @click="audit(row, true)">通过</el-button>
              <el-button text type="danger" @click="audit(row, false)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无核算结果" /></template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.head h2 { margin: 0; font-size: 20px; color: #0f2a43; }
.muted { margin: 6px 0 0; color: #909399; font-size: 12px; }
.actions { display: flex; gap: 10px; }
.kpi-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin-top: 16px; }
.kpi { display: flex; flex-direction: column; gap: 6px; padding: 12px 14px; background: #f7f9fc; border-radius: 10px; }
.kpi span { font-size: 12px; color: #909399; }
.kpi strong { font-size: 22px; color: #0f2a43; }
.kpi em { margin-left: 4px; font-size: 12px; color: #909399; font-style: normal; }
.green { color: #2f9e44; }
.red { color: #e5484d; }
.grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 16px; }
.span-7 { grid-column: span 7; }
.span-5 { grid-column: span 5; }
.title { font-weight: 600; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
@media (max-width: 1200px) { .span-7, .span-5 { grid-column: span 12; } }
</style>
