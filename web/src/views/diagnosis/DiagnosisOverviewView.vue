<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { ElMessage } from 'element-plus'
import BaseChart from '@/components/BaseChart.vue'
import { fetchOverview, runScan } from '@/api/diagnosis'
import type { OverviewData } from '@/types/diagnosis'

const router = useRouter()
const loading = ref(false)
const scanning = ref(false)
const data = ref<OverviewData | null>(null)

const trendOption = computed<EChartsOption>(() => {
  const trend = data.value?.trend ?? []
  return {
    grid: { left: 44, right: 18, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#606266', fontSize: 12 } },
    xAxis: {
      type: 'category',
      data: trend.map((item) => item.date),
      axisLabel: { color: '#909399', fontSize: 11 },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      { name: '异常数', type: 'bar', barWidth: 18, data: trend.map((item) => item.count), itemStyle: { color: '#e6a23c', borderRadius: [3, 3, 0, 0] } },
      { name: '高风险', type: 'line', smooth: true, data: trend.map((item) => item.high), itemStyle: { color: '#e5484d' } }
    ]
  }
})

const ruleOption = computed<EChartsOption>(() => {
  const entries = Object.entries(data.value?.byRule ?? {}).sort((a, b) => b[1] - a[1])
  return {
    grid: { left: 80, right: 24, top: 16, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    yAxis: { type: 'category', data: entries.map((item) => item[0]), axisLabel: { color: '#606266', fontSize: 11 } },
    series: [{ type: 'bar', barWidth: 14, data: entries.map((item) => item[1]), itemStyle: { color: '#2f6fed', borderRadius: [0, 4, 4, 0] } }]
  }
})

const severityOption = computed<EChartsOption>(() => {
  const palette = ['#909399', '#e6a23c', '#f08c2e', '#e5484d']
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#606266', fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['45%', '68%'],
      center: ['50%', '44%'],
      label: { formatter: '{b}\n{c}', fontSize: 11 },
      data: Object.entries(data.value?.bySeverity ?? {}).map(([name, value], index) => ({
        name,
        value,
        itemStyle: { color: palette[index % palette.length] }
      }))
    }]
  }
})

async function load(): Promise<void> {
  loading.value = true
  try {
    data.value = await fetchOverview()
  } finally {
    loading.value = false
  }
}

async function scan(): Promise<void> {
  scanning.value = true
  try {
    const result = await runScan()
    if (result.skipped) {
      ElMessage.warning('诊断任务正在执行，请稍后')
    } else {
      ElMessage.success(`扫描完成：新增 ${result.newAnomalies} 条，合并 ${result.mergedAnomalies} 条，报告 ${result.reportCount} 份`)
      await load()
    }
  } finally {
    scanning.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="head">
        <div>
          <h2>异常诊断总览</h2>
          <p class="muted">基于动态基线与八类规则，识别能耗异常并生成诊断报告</p>
        </div>
        <div class="actions">
          <el-button :loading="scanning" type="primary" @click="scan">立即诊断</el-button>
          <el-button @click="router.push('/diagnosis/anomalies')">异常列表</el-button>
        </div>
      </div>
      <div class="kpi-row" v-loading="loading">
        <div class="kpi"><span>异常总数</span><strong>{{ data?.total ?? 0 }}</strong></div>
        <div class="kpi"><span>待确认</span><strong class="warn">{{ data?.openCount ?? 0 }}</strong></div>
        <div class="kpi"><span>高风险</span><strong class="danger">{{ data?.highRiskCount ?? 0 }}</strong></div>
        <div class="kpi"><span>已处理</span><strong>{{ data?.handledCount ?? 0 }}</strong></div>
        <div class="kpi"><span>误报</span><strong>{{ data?.falsePositiveCount ?? 0 }}</strong></div>
      </div>
    </el-card>

    <div class="grid">
      <el-card shadow="never" class="span-8">
        <template #header><span class="title">异常趋势</span></template>
        <BaseChart v-if="(data?.trend.length ?? 0) > 0" :option="trendOption" height="280px" />
        <el-empty v-else description="暂无异常数据，可点击「立即诊断」" />
      </el-card>
      <el-card shadow="never" class="span-4">
        <template #header><span class="title">风险分布</span></template>
        <BaseChart v-if="Object.keys(data?.bySeverity ?? {}).length" :option="severityOption" height="280px" />
        <el-empty v-else description="暂无数据" />
      </el-card>
      <el-card shadow="never" class="span-7">
        <template #header><span class="title">规则命中分布</span></template>
        <BaseChart v-if="Object.keys(data?.byRule ?? {}).length" :option="ruleOption" height="260px" />
        <el-empty v-else description="暂无数据" />
      </el-card>
      <el-card shadow="never" class="span-5">
        <template #header><span class="title">高发设备 TOP5</span></template>
        <el-table :data="data?.topDevices ?? []" size="small" border>
          <el-table-column prop="deviceName" label="设备" />
          <el-table-column prop="count" label="异常数" width="90" />
          <template #empty><el-empty description="暂无数据" :image-size="60" /></template>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.head { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.head h2 { margin: 0; font-size: 20px; color: #0f2a43; }
.muted { margin: 6px 0 0; color: #909399; font-size: 12px; }
.actions { display: flex; gap: 10px; }
.kpi-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-top: 16px; }
.kpi { display: flex; flex-direction: column; gap: 6px; padding: 12px 14px; background: #f7f9fc; border-radius: 10px; }
.kpi span { font-size: 12px; color: #909399; }
.kpi strong { font-size: 22px; color: #0f2a43; }
.kpi .warn { color: #e6a23c; }
.kpi .danger { color: #e5484d; }
.grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 16px; }
.span-4 { grid-column: span 4; }
.span-5 { grid-column: span 5; }
.span-7 { grid-column: span 7; }
.span-8 { grid-column: span 8; }
.title { font-weight: 600; }
@media (max-width: 1200px) { .span-4, .span-5, .span-7, .span-8 { grid-column: span 12; } }
</style>
