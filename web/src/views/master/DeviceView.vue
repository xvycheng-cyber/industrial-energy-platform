<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createDevice,
  deleteDevice,
  pageDevices,
  pageEnergyTypes,
  pageLines,
  updateDevice,
  updateDeviceStatus
} from '@/api/master'
import type { MasterQuery } from '@/api/master'
import type { Device, EnergyType, ProductionLine } from '@/types/models'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<Device>(
    (params) => pageDevices(params as unknown as MasterQuery),
    { keyword: '', status: '', lineId: '', deviceType: '' }
  )

const lineOptions = ref<ProductionLine[]>([])
const energyTypeOptions = ref<EnergyType[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)
const form = reactive({
  lineId: undefined as number | undefined,
  code: '',
  name: '',
  deviceType: '',
  model: '',
  ratedPower: undefined as number | undefined,
  energyTypeId: undefined as number | undefined,
  commissionDate: '',
  status: 1,
  remark: ''
})

const statusOptions = [
  { label: '运行', value: 1, type: 'success' },
  { label: '停机', value: 2, type: 'info' },
  { label: '检修', value: 3, type: 'warning' },
  { label: '停用', value: 0, type: 'danger' }
]

function statusInfo(status: number): { label: string; type: string } {
  const item = statusOptions.find((option) => option.value === status)
  return item ? { label: item.label, type: item.type } : { label: '未知', type: 'info' }
}

const rules: FormRules = {
  lineId: [{ required: true, message: '请选择所属产线', trigger: 'change' }],
  code: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请输入设备类型', trigger: 'blur' }]
}

const selectedLine = computed(() => lineOptions.value.find((item) => item.id === form.lineId))

async function loadOptions(): Promise<void> {
  const [lines, energyTypes] = await Promise.all([
    pageLines({ pageNum: 1, pageSize: 500 }),
    pageEnergyTypes({ pageNum: 1, pageSize: 500 })
  ])
  lineOptions.value = lines.records
  energyTypeOptions.value = energyTypes.records
}

function openCreate(): void {
  dialogMode.value = 'create'
  currentId.value = null
  Object.assign(form, {
    lineId: lineOptions.value[0]?.id,
    code: '',
    name: '',
    deviceType: '',
    model: '',
    ratedPower: undefined,
    energyTypeId: energyTypeOptions.value[0]?.id,
    commissionDate: '',
    status: 1,
    remark: ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEdit(row: Device): void {
  dialogMode.value = 'edit'
  currentId.value = row.id
  Object.assign(form, {
    lineId: row.lineId,
    code: row.code,
    name: row.name,
    deviceType: row.deviceType,
    model: row.model ?? '',
    ratedPower: row.ratedPower,
    energyTypeId: row.energyTypeId,
    commissionDate: row.commissionDate ?? '',
    status: row.status,
    remark: row.remark ?? ''
  })
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.lineId) {
    return
  }
  saving.value = true
  try {
    const payload = {
      lineId: form.lineId,
      code: form.code,
      name: form.name,
      deviceType: form.deviceType,
      model: form.model,
      ratedPower: form.ratedPower,
      energyTypeId: form.energyTypeId,
      commissionDate: form.commissionDate || undefined,
      status: form.status,
      remark: form.remark
    }
    if (dialogMode.value === 'create') {
      await createDevice(payload)
      ElMessage.success('设备创建成功')
    } else if (currentId.value) {
      await updateDevice(currentId.value, payload)
      ElMessage.success('设备更新成功')
    }
    dialogVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function remove(row: Device): Promise<void> {
  await ElMessageBox.confirm(`确定删除设备「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteDevice(row.id)
  ElMessage.success('已删除')
  refresh()
}

async function changeStatus(row: Device, status: number): Promise<void> {
  await updateDeviceStatus(row.id, status)
  ElMessage.success('状态已更新')
  refresh()
}

onMounted(async () => {
  await loadOptions()
  await refresh()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>设备管理</h2>
        <p>设备主数据、所属产线与运行状态维护。</p>
      </div>
    </div>
    <el-card shadow="never">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" placeholder="设备编码 / 名称 / 型号" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.lineId" placeholder="所属产线" clearable filterable style="width: 170px">
          <el-option v-for="item in lineOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="userStore.hasPermission('master:device:create')" type="primary" @click="openCreate">
          新增设备
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
      <template #default>
        <el-button size="small" @click="refresh">重新加载</el-button>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="code" label="设备编码" width="120" />
      <el-table-column prop="name" label="设备名称" min-width="120" />
      <el-table-column prop="deviceType" label="类型" width="90" />
      <el-table-column prop="model" label="型号" width="90" />
      <el-table-column prop="lineName" label="所属产线" width="100" />
      
      <el-table-column label="额定功率" width="95" align="right">
        <template #default="{ row }">{{ row.ratedPower ?? '-' }}<span v-if="row.ratedPower" class="text-muted"> kW</span></template>
      </el-table-column>
      <el-table-column prop="energyTypeName" label="能源类型" width="80" />
      <el-table-column label="状态" width="76" align="center">
        <template #default="{ row }">
          <el-tag :type="statusInfo(row.status).type as any" size="small">{{ statusInfo(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPermission('master:device:update')" text type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-dropdown v-if="userStore.hasPermission('master:device:update')" trigger="click">
            <el-button text type="warning">状态</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in statusOptions" :key="item.value" @click="changeStatus(row, item.value)">
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-if="userStore.hasPermission('master:device:delete')" text type="danger" @click="remove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无设备数据" />
      </template>
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

  <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增设备' : '编辑设备'" width="560px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="所属产线" prop="lineId">
        <el-select v-model="form.lineId" placeholder="请选择产线" filterable style="width: 100%">
          <el-option v-for="item in lineOptions" :key="item.id" :label="`${item.name}（${item.workshopName || '-'}）`" :value="item.id" />
        </el-select>
        <div v-if="selectedLine" class="text-muted">所属车间：{{ selectedLine.workshopName }}</div>
      </el-form-item>
      <el-form-item label="设备编码" prop="code">
        <el-input v-model="form.code" maxlength="64" />
      </el-form-item>
      <el-form-item label="设备名称" prop="name">
        <el-input v-model="form.name" maxlength="128" />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-input v-model="form.deviceType" maxlength="64" placeholder="如 注塑机 / 空压机" />
      </el-form-item>
      <el-form-item label="型号">
        <el-input v-model="form.model" maxlength="64" />
      </el-form-item>
      <el-form-item label="额定功率(kW)">
        <el-input-number v-model="form.ratedPower" :min="0" :precision="4" style="width: 100%" />
      </el-form-item>
      <el-form-item label="能源类型">
        <el-select v-model="form.energyTypeId" placeholder="请选择" clearable style="width: 100%">
          <el-option v-for="item in energyTypeOptions" :key="item.id" :label="`${item.name}（${item.unit}）`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="投运日期">
        <el-date-picker v-model="form.commissionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<style scoped>
.error-bar {
  margin-bottom: 12px;
}
</style>
