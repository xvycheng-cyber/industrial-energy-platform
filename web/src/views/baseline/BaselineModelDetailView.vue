<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/BaseChart.vue'
import {
  disableBaseline,
  enableBaseline,
  fetchBaselineError,
  fetchBaselineModel,
  pageBaselineModels
} from '@/api/baseline'
import type { BaselineErrorAnalysis, BaselineModel } from '@/types/baseline'

interface ModelParams {
  beta?: number[]
  featureNames?: string[]
  groupMeans?: Record<string, number>
  standbyBaseline?: number
  historicalBaseline?: number
  fallbackLog?: string[]
  sampleCount?: number
  standbySamples?: number
}

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const model = ref<BaselineModel | null>(null)
const analysis = ref<BaselineErrorAnalysis | null>(null)
const versions = ref<BaselineModel[]>([])

const dimTypeLabels: Record<number, string> = { 1: '企业', 2: '车间', 3: '产线', 4: '设备' }

const params = computed<ModelParams>(() => (model.value?.params ?? {}) as ModelParams)

const coefficients = computed(() => {
  const names = model.value?.featureNames ?? params.value.featureNames ?? []
  const beta = params.value.beta ?? []
  return names.map((name, index) => ({ name, value: Number(beta[index] ?? 0).toFixed(4) }))
})

const groupMeans = computed(() =>
  Object.entries(params.value.groupMeans ?? {}).map(([key, value]) => {
    const [load, product] = key.split('|')
    return { key, load, product, value }
  })
)

const fallbackLog = computed(() => model.value?.fallbackLog ?? params.value.fallbackLog ?? [])

const residualOption = computed<EChartsOption>(() => {
  const buckets = analysis.value?.buckets ?? []
  return {
    grid: { left: 46, right: 16, top: 20, bottom: 34 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: buckets.map((item) => item.range),
      axisLabel: { color: '#909399', fontSize: 11 },
      axisLine: { lineStyle: { color: '#e4e7ed' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    series: [
      {
        type: 'bar',
        barWidth: 22,
        data: buckets.map((item) => item.count),
        itemStyle: { color: '#2f6fed', borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

async function load(): Promise<void> {
  const id = Number(route.params.id)
  if (!id) {
    error.value = '缺少模型参数'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const detail = await fetchBaselineModel(id)
    model.value = detail
    const [err, list] = await Promise.all([
      fetchBaselineError(id),
      pageBaselineModels({
        pageNum: 1,
        pageSize: 50,
        dimType: detail.dimType,
        dimId: detail.dimId,
        energyTypeId: detail.energyTypeId
      })
    ])
    analysis.value = err
    versions.value = list.records
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '模型详情加载失败'
  } finally {
    loading.value = false
  }
}

async function enable(item: BaselineModel): Promise<void> {
  await enableBaseline(item.id)
  ElMessage.success('已启用该版本')
  await load()
}

async function disable(): Promise<void> {
  if (!model.value) return
  await disableBaseline(model.value.id)
  ElMessage.success('已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never" v-loading="loading">
      <div class="head">
        <div>
          <h2>{{ model?.dimName || '基线模型' }}</h2>
          <p class="muted">
            {{ dimTypeLabels[model?.dimType ?? 4] }} · {{ model?.energyTypeName }} · {{ model?.modelVersion }} ·
            {{ model?.algo }}
          </p>
        </div>
        <div class="actions">
          <el-tag v-if="model" :type="model.status === 1 ? 'success' : model.status === 2 ? 'warning' : 'info'">
            {{ model?.statusLabel }}
          </el-tag>
          <el-button size="small" @click="router.push('/baseline/models')">返回列表</el-button>
          <el-button v-if="model && model.status !== 1" size="small" type="success" @click="model && enable(model)">
            启用
          </el-button>
          <el-button v-else size="small" type="warning" @click="disable">停用</el-button>
        </div>
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />

      <div class="kpi-row">
        <div class="kpi"><span>训练样本</span><strong>{{ model?.sampleCount ?? '—' }}</strong></div>
        <div class="kpi"><span>待机样本</span><strong>{{ params.standbySamples ?? model?.standbySamples ?? '—' }}</strong></div>
        <div class="kpi"><span>MAE</span><strong>{{ model?.mae ?? '—' }}</strong></div>
        <div class="kpi"><span>RMSE</span><strong>{{ model?.rmse ?? '—' }}</strong></div>
        <div class="kpi"><span>MAPE</span><strong>{{ model?.mape ?? '—' }}%</strong></div>
        <div class="kpi"><span>R²</span><strong>{{ model?.r2 ?? '—' }}</strong></div>
        <div class="kpi"><span>剔除异常</span><strong>{{ model?.outlierCount ?? 0 }}</strong></div>
        <div class="kpi"><span>回退次数</span><strong>{{ model?.fallbackCount ?? 0 }}</strong></div>
      </div>
    </el-card>

    <div class="grid">
      <el-card shadow="never" class="span-7">
        <template #header><span class="card-title">回归参数（工况特征权重）</span></template>
        <el-table :data="coefficients" border stripe size="small">
          <el-table-column prop="name" label="特征" min-width="160" />
          <el-table-column prop="value" label="系数" width="140" />
          <template #empty><el-empty description="该模型为回退模型，无回归系数" :image-size="60" /></template>
        </el-table>
        <div class="baseline-extra">
          <span>待机基线：<b>{{ params.standbyBaseline ?? '—' }}</b> kWh</span>
          <span>历史正常均值：<b>{{ params.historicalBaseline ?? '—' }}</b> kWh</span>
        </div>
      </el-card>

      <el-card shadow="never" class="span-5">
        <template #header><span class="card-title">工况分组均值</span></template>
        <el-table :data="groupMeans" border stripe size="small" max-height="240">
          <el-table-column prop="load" label="负荷档" width="90" />
          <el-table-column prop="product" label="产品" min-width="120" />
          <el-table-column prop="value" label="均值 kWh" width="110" />
          <template #empty><el-empty description="暂无分组样本" :image-size="60" /></template>
        </el-table>
      </el-card>

      <el-card shadow="never" class="span-12">
        <template #header><span class="card-title">回退记录</span></template>
        <ul v-if="fallbackLog.length" class="logs">
          <li v-for="item in fallbackLog" :key="item">{{ item }}</li>
        </ul>
        <el-empty v-else description="本次训练未触发回退" :image-size="60" />
      </el-card>

      <el-card shadow="never" class="span-6">
        <template #header><span class="card-title">误差分布（偏差率区间）</span></template>
        <BaseChart v-if="analysis && analysis.buckets.length" :option="residualOption" height="260px" />
        <el-empty v-else description="暂无误差数据" />
      </el-card>

      <el-card shadow="never" class="span-6">
        <template #header><span class="card-title">偏差最大的样本</span></template>
        <el-table :data="analysis?.worst ?? []" border stripe size="small" max-height="260">
          <el-table-column prop="periodStart" label="周期" min-width="140" />
          <el-table-column prop="actualValue" label="实际" width="90" />
          <el-table-column prop="baselineValue" label="基线" width="90" />
          <el-table-column prop="deviationRate" label="偏差率%" width="100" />
          <template #empty><el-empty description="暂无偏差样本" :image-size="60" /></template>
        </el-table>
      </el-card>

      <el-card shadow="never" class="span-12">
        <template #header><span class="card-title">版本历史</span></template>
        <el-table :data="versions" border stripe size="small">
          <el-table-column prop="modelVersion" label="版本" min-width="150" />
          <el-table-column prop="algo" label="算法" width="100" />
          <el-table-column prop="sampleCount" label="样本" width="80" />
          <el-table-column prop="mae" label="MAE" width="90" />
          <el-table-column prop="r2" label="R²" width="80" />
          <el-table-column prop="statusLabel" label="状态" width="100" />
          <el-table-column prop="effectiveFrom" label="生效时间" min-width="150" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button v-if="row.status !== 1" text type="primary" @click="enable(row)">切换</el-button>
              <span v-else class="muted">当前</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.head h2 {
  margin: 0;
  font-size: 20px;
  color: #0f2a43;
}

.muted {
  margin: 6px 0 0;
  color: #909399;
  font-size: 12px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.kpi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  background: #f7f9fc;
  border-radius: 10px;
}

.kpi span {
  font-size: 12px;
  color: #909399;
}

.kpi strong {
  font-size: 20px;
  color: #0f2a43;
}

.grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 16px;
}

.span-5 {
  grid-column: span 5;
}

.span-6 {
  grid-column: span 6;
}

.span-7 {
  grid-column: span 7;
}

.span-12 {
  grid-column: span 12;
}

.card-title {
  font-weight: 600;
}

.baseline-extra {
  display: flex;
  gap: 24px;
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
}

.logs {
  margin: 0;
  padding-left: 18px;
  color: #a05a00;
  font-size: 13px;
  line-height: 1.9;
}

@media (max-width: 1200px) {
  .span-5,
  .span-6,
  .span-7 {
    grid-column: span 12;
  }
}
</style>
