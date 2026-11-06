<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  disableBaseline,
  enableBaseline,
  pageBaselineModels,
  trainBaseline,
  type BaselineTrainPayload
} from '@/api/baseline'
import { pageDevices, pageEnergyTypes, pageEnterprises, pageLines, pageWorkshops } from '@/api/master'
import type { BaselineModel, BaselineTrainResult } from '@/types/baseline'
import type { Device, EnergyType, Enterprise, ProductionLine, Workshop } from '@/types/models'

const router = useRouter()
const loading = ref(false)
const records = ref<BaselineModel[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, dimType: undefined as number | undefined, status: undefined as number | undefined })

const trainVisible = ref(false)
const training = ref(false)
const trainResult = ref<BaselineTrainResult | null>(null)
const energyTypes = ref<EnergyType[]>([])
const enterprises = ref<Enterprise[]>([])
const workshops = ref<Workshop[]>([])
const lines = ref<ProductionLine[]>([])
const devices = ref<Device[]>([])
const trainForm = reactive<BaselineTrainPayload>({
  dimType: 4,
  dimId: 1,
  energyTypeId: 1,
  startDate: '',
  endDate: '',
  activate: true
})

const statusTag: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning' }

const scopeOptions = computed(() => {
  if (trainForm.dimType === 1) return enterprises.value.map((item) => ({ id: item.id, name: item.name }))
  if (trainForm.dimType === 2) return workshops.value.map((item) => ({ id: item.id, name: item.name }))
  if (trainForm.dimType === 3) return lines.value.map((item) => ({ id: item.id, name: item.name }))
  return devices.value.map((item) => ({ id: item.id, name: item.name }))
})

function defaultDates(): void {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  const format = (value: Date) => value.toISOString().slice(0, 10)
  trainForm.endDate = format(end)
  trainForm.startDate = format(start)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await pageBaselineModels({ ...query })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function openTrain(): Promise<void> {
  defaultDates()
  trainResult.value = null
  if (energyTypes.value.length === 0) {
    const [types, ents, wss, lns, devs] = await Promise.all([
      pageEnergyTypes({ pageNum: 1, pageSize: 100 }),
      pageEnterprises({ pageNum: 1, pageSize: 200 }),
      pageWorkshops({ pageNum: 1, pageSize: 200 }),
      pageLines({ pageNum: 1, pageSize: 200 }),
      pageDevices({ pageNum: 1, pageSize: 200 })
    ])
    energyTypes.value = types.records
    enterprises.value = ents.records
    workshops.value = wss.records
    lines.value = lns.records
    devices.value = devs.records
    trainForm.energyTypeId = types.records[0]?.id ?? 1
    trainForm.dimId = devs.records[0]?.id ?? 1
  }
  trainVisible.value = true
}

function onScopeTypeChange(): void {
  trainForm.dimId = scopeOptions.value[0]?.id ?? 0
}

async function submitTrain(): Promise<void> {
  if (!trainForm.dimId || !trainForm.startDate || !trainForm.endDate) {
    ElMessage.warning('请选择训练对象与日期区间')
    return
  }
  training.value = true
  try {
    trainResult.value = await trainBaseline({ ...trainForm })
    ElMessage.success(`训练完成：${trainResult.value.modelVersion}`)
    await load()
  } finally {
    training.value = false
  }
}

async function enable(item: BaselineModel): Promise<void> {
  await enableBaseline(item.id)
  ElMessage.success('已启用该版本')
  await load()
}

async function disable(item: BaselineModel): Promise<void> {
  await ElMessageBox.confirm(`确认停用模型 ${item.modelVersion}？停用后该对象将没有生效基线。`, '停用模型', {
    type: 'warning'
  })
  await disableBaseline(item.id)
  ElMessage.success('已停用')
  await load()
}

function openDetail(item: BaselineModel): void {
  router.push({ path: `/baseline/models/${item.id}` })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>动态基线</h2>
        <p>按工况训练的能耗模型，版本化保留，支持偏差与回退追溯。</p>
      </div>
    </div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.dimType" placeholder="全部维度" clearable style="width: 130px" @change="load">
          <el-option label="企业" :value="1" />
          <el-option label="车间" :value="2" />
          <el-option label="产线" :value="3" />
          <el-option label="设备" :value="4" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load">
          <el-option label="草稿" :value="0" />
          <el-option label="生效中" :value="1" />
          <el-option label="已归档" :value="2" />
        </el-select>
        <el-button type="primary" @click="openTrain">训练新基线</el-button>
        <el-button class="refresh" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="dimName" label="对象" min-width="160" />
        <el-table-column prop="modelVersion" label="版本" min-width="160" />
        <el-table-column prop="algo" label="算法" width="90" />
        <el-table-column prop="sampleCount" label="样本" width="70" />
        <el-table-column prop="mae" label="MAE" width="80" />
        <el-table-column prop="rmse" label="RMSE" width="80" />
        <el-table-column prop="r2" label="R²" width="70" />
        <el-table-column prop="outlierCount" label="剔除" width="60" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag[row.status] as 'info' | 'success' | 'warning'" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="effectiveFrom" label="生效时间" min-width="140" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status !== 1" text type="success" @click="enable(row)">启用</el-button>
            <el-button v-else text type="warning" @click="disable(row)">停用</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无基线模型，可先训练" /></template>
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

    <el-dialog v-model="trainVisible" title="训练动态基线" width="640px">
      <el-form label-width="100px">
        <el-form-item label="维度">
          <el-select v-model="trainForm.dimType" style="width: 160px" @change="onScopeTypeChange">
            <el-option label="企业" :value="1" />
            <el-option label="车间" :value="2" />
            <el-option label="产线" :value="3" />
            <el-option label="设备" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="训练对象">
          <el-select v-model="trainForm.dimId" filterable style="width: 100%">
            <el-option v-for="item in scopeOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="能源类型">
          <el-select v-model="trainForm.energyTypeId" style="width: 100%">
            <el-option v-for="item in energyTypes" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="训练区间">
          <el-date-picker
            v-model="trainForm.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始"
            style="width: 160px"
          />
          <span class="sep">至</span>
          <el-date-picker
            v-model="trainForm.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="立即启用">
          <el-switch v-model="trainForm.activate" />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="trainResult"
        type="success"
        :closable="false"
        :title="`${trainResult.modelVersion} · ${trainResult.algo} · 样本 ${trainResult.sampleCount}`"
      >
        <div class="train-summary">
          <span>MAE {{ trainResult.mae }}</span>
          <span>RMSE {{ trainResult.rmse }}</span>
          <span>R² {{ trainResult.r2 }}</span>
          <span>剔除异常 {{ trainResult.outlierSamples }}</span>
          <span>预测 {{ trainResult.predictedCount }} 条</span>
        </div>
        <ul v-if="trainResult.fallbacks.length" class="fallback-list">
          <li v-for="item in trainResult.fallbacks" :key="item">{{ item }}</li>
        </ul>
      </el-alert>

      <template #footer>
        <el-button @click="trainVisible = false">关闭</el-button>
        <el-button type="primary" :loading="training" @click="submitTrain">开始训练</el-button>
      </template>
    </el-dialog>
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
  justify-content: flex-start;
  margin-bottom: 14px;
}

.toolbar .refresh {
  margin-left: auto;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.sep {
  margin: 0 8px;
  color: #909399;
}

.train-summary {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 6px;
  font-size: 13px;
}

.fallback-list {
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: #a05a00;
  line-height: 1.8;
}
</style>
