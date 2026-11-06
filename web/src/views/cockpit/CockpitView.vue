<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/BaseChart.vue'
import { fetchCockpit, fetchHierarchy } from '@/api/profile'
import { pageEnergyTypes } from '@/api/master'
import type { CockpitData, HierarchyNode } from '@/types/profile'
import type { EnergyType } from '@/types/models'

const router = useRouter()

const loading = ref(false)
const error = ref('')
const data = ref<CockpitData | null>(null)
const hierarchy = ref<HierarchyNode[]>([])
const energyTypes = ref<EnergyType[]>([])

const date = ref(new Date().toISOString().substring(0, 10))
const granularity = ref<'hour' | 'day'>('hour')
const energyTypeId = ref<number | ''>('')
const scopePath = ref<number[]>([])

const palette = ['#3b82f6', '#22d3ee', '#f59e0b', '#ef4444', '#34d399', '#a78bfa']
const axisColor = '#7f9ab5'
const splitColor = 'rgba(125,151,179,0.15)'

const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: true
}

const scopeType = computed(() => (scopePath.value.length === 0 ? 1 : scopePath.value.length))
const scopeId = computed(() => scopePath.value[scopePath.value.length - 1])

const kpis = computed(() => data.value?.kpis ?? [])

const trendOption = computed<EChartsOption>(() => {
  const points = data.value?.trend ?? []
  return {
    backgroundColor: 'transparent',
    grid: { left: 46, right: 18, top: 24, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: (v: unknown) => `${v} kWh` },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.label),
      axisLine: { lineStyle: { color: splitColor } },
      axisLabel: { color: axisColor, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: 'kWh',
      nameTextStyle: { color: axisColor, fontSize: 11 },
      axisLabel: { color: axisColor, fontSize: 11 },
      splitLine: { lineStyle: { color: splitColor } }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: points.map((p) => p.energyValue),
        lineStyle: { width: 2, color: '#2ea3ff' },
        areaStyle: { color: 'rgba(46,163,255,0.12)' }
      }
    ]
  }
})

const structureOption = computed<EChartsOption>(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item', valueFormatter: (v: unknown) => `${v} kWh` },
  legend: { bottom: 0, textStyle: { color: axisColor, fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
  series: [
    {
      type: 'pie',
      radius: ['46%', '68%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: '#0f2740', borderWidth: 2 },
      label: { color: '#cfe3f5', fontSize: 11, formatter: '{b}\n{d}%' },
      data: (data.value?.structure ?? []).map((item, index) => ({
        name: item.energyTypeName,
        value: item.energyValue,
        itemStyle: { color: palette[index % palette.length] }
      }))
    }
  ]
}))

const rankingOption = computed<EChartsOption>(() => {
  const items = [...(data.value?.lineRanking ?? [])].reverse()
  return {
    backgroundColor: 'transparent',
    grid: { left: 96, right: 26, top: 16, bottom: 24 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v: unknown) => `${v} kWh` },
    xAxis: {
      type: 'value',
      axisLabel: { color: axisColor, fontSize: 11 },
      splitLine: { lineStyle: { color: splitColor } }
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => item.lineName),
      axisLabel: { color: axisColor, fontSize: 11 },
      axisLine: { lineStyle: { color: splitColor } }
    },
    series: [
      {
        type: 'bar',
        barWidth: 12,
        itemStyle: { color: '#2ea3ff', borderRadius: [0, 4, 4, 0] },
        data: items.map((item) => item.energyValue)
      }
    ]
  }
})

const baselineOption = computed<EChartsOption>(() => {
  const points = data.value?.baseline ?? []
  return {
    backgroundColor: 'transparent',
    grid: { left: 46, right: 18, top: 34, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { top: 4, textStyle: { color: axisColor, fontSize: 11 } },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.label),
      axisLabel: { color: axisColor, fontSize: 11 },
      axisLine: { lineStyle: { color: splitColor } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: axisColor, fontSize: 11 },
      splitLine: { lineStyle: { color: splitColor } }
    },
    series: [
      { name: '实际', type: 'line', data: points.map((p) => p.actual), itemStyle: { color: '#2ea3ff' } },
      { name: '基线', type: 'line', data: points.map((p) => p.baseline), lineStyle: { type: 'dashed' }, itemStyle: { color: '#f2b84b' } }
    ]
  }
})

const anomalyOption = computed<EChartsOption>(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item', formatter: '{b}: {c} 台 ({d}%)' },
  legend: { bottom: 0, textStyle: { color: axisColor, fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
  series: [
    {
      type: 'pie',
      radius: ['42%', '64%'],
      center: ['50%', '44%'],
      label: { color: '#cfe3f5', fontSize: 11, formatter: '{b}' },
      data: (data.value?.anomaly ?? []).map((item, index) => ({
        name: item.name,
        value: item.count,
        itemStyle: { color: palette[index % palette.length] }
      }))
    }
  ]
}))

const savingOption = computed<EChartsOption>(() => {
  const points = data.value?.saving ?? []
  return {
    backgroundColor: 'transparent',
    grid: { left: 46, right: 18, top: 24, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: (v: unknown) => `${v} kWh` },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.month),
      axisLabel: { color: axisColor, fontSize: 11 },
      axisLine: { lineStyle: { color: splitColor } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: axisColor, fontSize: 11 },
      splitLine: { lineStyle: { color: splitColor } }
    },
    series: [
      {
        type: 'bar',
        barWidth: 18,
        itemStyle: { color: '#35d0ba', borderRadius: [4, 4, 0, 0] },
        data: points.map((p) => p.savingEnergy)
      }
    ]
  }
})

const flowOption = computed<EChartsOption>(() => {
  const flow = data.value?.flow
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', triggerOn: 'mousemove' },
    series: [
      {
        type: 'sankey',
        left: 8,
        right: 110,
        top: 16,
        bottom: 16,
        nodeWidth: 10,
        nodeGap: 10,
        label: { color: '#cfe3f5', fontSize: 11 },
        lineStyle: { color: 'gradient', opacity: 0.28, curveness: 0.5 },
        itemStyle: { borderWidth: 0, color: '#2ea3ff' },
        data: flow?.nodes ?? [],
        links: flow?.links ?? []
      }
    ]
  }
})

const hasTrend = computed(() => (data.value?.trend ?? []).length > 0)
const hasStructure = computed(() => (data.value?.structure ?? []).length > 0)
const hasRanking = computed(() => (data.value?.lineRanking ?? []).length > 0)
const hasBaseline = computed(() => (data.value?.baseline ?? []).length > 0)
const hasAnomaly = computed(() => (data.value?.anomaly ?? []).length > 0)
const hasSaving = computed(() => (data.value?.saving ?? []).length > 0)
const hasFlow = computed(() => (data.value?.flow?.links ?? []).length > 0)

function scopeParams(): { scopeType: number; scopeId: number } {
  return { scopeType: scopeType.value, scopeId: scopeId.value }
}

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    data.value = await fetchCockpit({
      date: date.value,
      granularity: granularity.value,
      ...scopeParams(),
      energyTypeId: energyTypeId.value === '' ? undefined : energyTypeId.value
    })
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '驾驶舱加载失败'
    data.value = null
  } finally {
    loading.value = false
  }
}

async function loadBase(): Promise<void> {
  const [tree, types] = await Promise.all([fetchHierarchy(), pageEnergyTypes({ pageNum: 1, pageSize: 100 })])
  hierarchy.value = tree
  energyTypes.value = types.records
  if (tree.length > 0 && scopePath.value.length === 0) {
    scopePath.value = [tree[0].id]
  }
}

function onScopeChange(): void {
  void load()
}

function onRankingClick(params: unknown): void {
  const event = params as { dataIndex?: number }
  if (event?.dataIndex === undefined) {
    return
  }
  const items = [...(data.value?.lineRanking ?? [])].reverse()
  const target = items[event.dataIndex]
  if (target) {
    router.push({ path: '/profile/line', query: { id: target.lineId } })
  }
}

function onStructureClick(params: unknown): void {
  const event = params as { dataIndex?: number }
  if (event?.dataIndex === undefined) {
    return
  }
  const slice = data.value?.structure?.[event.dataIndex]
  if (slice?.energyTypeId) {
    energyTypeId.value = slice.energyTypeId
    void load()
  }
}

function openEnterprise(): void {
  const root = hierarchy.value[0]
  if (root) {
    router.push({ path: '/profile/enterprise', query: { id: root.id } })
  }
}

onMounted(async () => {
  try {
    await loadBase()
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '初始化失败'
  }
})

watch(granularity, () => void load())
</script>

<template>
  <div class="cockpit">
    <header class="cockpit-header">
      <div class="title-block">
        <h1>能源驾驶舱</h1>
        <p>{{ data?.scopeName || '工业企业能耗画像与节能诊断平台' }}</p>
      </div>
      <div class="filters">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" style="width: 140px" @change="load" />
        <el-cascader
          v-model="scopePath"
          :options="hierarchy"
          :props="cascaderProps"
          placeholder="选择层级"
          clearable
          style="width: 240px"
          @change="onScopeChange"
        />
        <el-radio-group v-model="granularity" size="small">
          <el-radio-button value="hour">按小时</el-radio-button>
          <el-radio-button value="day">按日</el-radio-button>
        </el-radio-group>
        <el-select v-model="energyTypeId" placeholder="全部能源" clearable style="width: 130px" @change="load">
          <el-option v-for="item in energyTypes" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button type="primary" plain :loading="loading" @click="load">刷新</el-button>
        <el-button text @click="openEnterprise">企业画像</el-button>
      </div>
    </header>

    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
      <template #default><el-button size="small" @click="load">重新加载</el-button></template>
    </el-alert>

    <section class="kpi-grid" v-loading="loading">
      <div v-for="item in kpis" :key="item.key" class="kpi-card" :class="{ muted: !item.available }">
        <div class="kpi-label">{{ item.label }}</div>
        <div class="kpi-value">
          <template v-if="item.available">{{ item.value ?? '—' }}<span class="kpi-unit">{{ item.unit }}</span></template>
          <template v-else><span class="pending">—</span></template>
        </div>
        <div class="kpi-hint">{{ item.available ? (item.hint || '') : '当前范围暂无数据' }}</div>
      </div>
      <div v-if="!kpis.length" class="kpi-empty"><el-empty description="暂无指标" :image-size="60" /></div>
    </section>

    <section class="chart-grid">
      <div class="chart-card span-8">
        <header><span>能耗趋势</span><small>{{ granularity === 'hour' ? '按小时' : '近 14 天' }}</small></header>
        <BaseChart v-if="hasTrend" :option="trendOption" height="300px" dark />
        <el-empty v-else description="所选范围暂无能耗数据" :image-size="70" />
      </div>

      <div class="chart-card span-4">
        <header><span>能源结构</span><small>点击切片筛选</small></header>
        <BaseChart v-if="hasStructure" :option="structureOption" height="300px" dark @chart-click="onStructureClick" />
        <el-empty v-else description="暂无能源结构数据" :image-size="70" />
      </div>

      <div class="chart-card span-4">
        <header><span>产线能耗排名</span><small>点击跳转画像</small></header>
        <BaseChart v-if="hasRanking" :option="rankingOption" height="280px" dark @chart-click="onRankingClick" />
        <el-empty v-else description="暂无产线数据" :image-size="70" />
      </div>

      <div class="chart-card span-4">
        <header><span>实际 vs 基线</span><small>逐小时动态基线</small></header>
        <BaseChart v-if="hasBaseline" :option="baselineOption" height="280px" dark />
        <el-empty v-else description="暂无基线对比数据" :image-size="70" />
      </div>

      <div class="chart-card span-4">
        <header><span>异常设备分布</span><small>近 7 天</small></header>
        <BaseChart v-if="hasAnomaly" :option="anomalyOption" height="280px" dark />
        <el-empty v-else description="近 7 天暂无异常" :image-size="70" />
      </div>

      <div class="chart-card span-4">
        <header><span>节能效果趋势</span><small>核算结果</small></header>
        <BaseChart v-if="hasSaving" :option="savingOption" height="280px" dark />
        <el-empty v-else description="暂无节能核算数据" :image-size="70" />
      </div>

      <div class="chart-card span-8">
        <header><span>能耗流向</span><small>企业 → 车间 → 产线 → 设备</small></header>
        <BaseChart v-if="hasFlow" :option="flowOption" height="320px" dark />
        <el-empty v-else description="暂无流向数据（需企业级能耗聚合）" :image-size="70" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.cockpit {
  min-height: 100%;
  margin: -18px;
  padding: 20px 24px 28px;
  background-color: #071a2b;
  background-image: linear-gradient(rgba(125, 176, 224, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(125, 176, 224, 0.045) 1px, transparent 1px);
  background-size: 48px 48px;
  color: #cfe3f5;
}

.cockpit-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.title-block h1 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 3px;
  color: #eaf4ff;
}

.title-block p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #7d97b3;
}

.filters {
  display: flex;
  padding: 10px 12px;
  background: #0b2237;
  border: 1px solid rgba(125, 176, 224, 0.14);
  border-radius: 8px;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.error-bar {
  margin-bottom: 14px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
  min-height: 110px;
}

.kpi-card {
  padding: 14px 16px;
  background: #0b2237;
  border: 1px solid rgba(125, 176, 224, 0.16);
  border-left: 3px solid #2563eb;
  border-radius: 8px;
}

.kpi-card.muted {
  opacity: 0.72;
}

.kpi-label {
  font-size: 12px;
  color: #8ba9c4;
}

.kpi-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
}

.kpi-unit {
  margin-left: 4px;
  font-size: 12px;
  color: #7d97b3;
  font-weight: 400;
}

.pending {
  font-size: 14px;
  color: #f2b84b;
}

.kpi-hint {
  margin-top: 6px;
  font-size: 11px;
  color: #6f8aa6;
  min-height: 14px;
}

.kpi-empty {
  grid-column: span 4;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
}

.chart-card {
  background: #0b2237;
  border: 1px solid rgba(125, 176, 224, 0.14);
  border-radius: 8px;
  padding: 12px 14px 8px;
  min-width: 0;
}

.chart-card header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 6px;
}

.chart-card header span {
  font-size: 14px;
  color: #dbeafe;
  font-weight: 500;
}

.chart-card header small {
  font-size: 11px;
  color: #6f8aa6;
}

.span-4 {
  grid-column: span 4;
}

.span-8 {
  grid-column: span 8;
}

:deep(.el-empty__description p) {
  color: #6f8aa6;
}

@media (max-width: 1280px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .span-8,
  .span-4 {
    grid-column: span 12;
  }
}

@media (max-width: 720px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .kpi-empty {
    grid-column: span 1;
  }
}
</style>
