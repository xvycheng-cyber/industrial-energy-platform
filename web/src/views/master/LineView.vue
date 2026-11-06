<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createLine,
  deleteLine,
  pageEnterprises,
  pageLines,
  pageWorkshops,
  updateLine,
  updateLineStatus
} from '@/api/master'
import type { MasterQuery } from '@/api/master'
import type { Enterprise, ProductionLine, Workshop } from '@/types/models'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('PLATFORM_ADMIN'))

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<ProductionLine>(
    (params) => pageLines(params as unknown as MasterQuery),
    { keyword: '', status: '', enterpriseId: '', workshopId: '' }
  )

const enterpriseOptions = ref<Enterprise[]>([])
const workshopOptions = ref<Workshop[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)
const form = reactive({
  enterpriseId: undefined as number | undefined,
  workshopId: undefined as number | undefined,
  code: '',
  name: '',
  productType: '',
  designCapacity: undefined as number | undefined,
  capacityUnit: '',
  status: 1,
  remark: ''
})

const rules: FormRules = {
  workshopId: [{ required: true, message: '请选择所属车间', trigger: 'change' }],
  code: [{ required: true, message: '请输入产线编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入产线名称', trigger: 'blur' }]
}

const formWorkshops = computed(() => {
  if (!form.enterpriseId) {
    return workshopOptions.value
  }
  return workshopOptions.value.filter((item) => item.enterpriseId === form.enterpriseId)
})

async function loadOptions(): Promise<void> {
  const [enterprises, workshops] = await Promise.all([
    pageEnterprises({ pageNum: 1, pageSize: 500 }),
    pageWorkshops({ pageNum: 1, pageSize: 500 })
  ])
  enterpriseOptions.value = enterprises.records
  workshopOptions.value = workshops.records
}

function openCreate(): void {
  dialogMode.value = 'create'
  currentId.value = null
  Object.assign(form, {
    enterpriseId: isAdmin.value ? enterpriseOptions.value[0]?.id : workshopOptions.value[0]?.enterpriseId,
    workshopId: undefined,
    code: '',
    name: '',
    productType: '',
    designCapacity: undefined,
    capacityUnit: '',
    status: 1,
    remark: ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEdit(row: ProductionLine): void {
  dialogMode.value = 'edit'
  currentId.value = row.id
  Object.assign(form, {
    enterpriseId: row.enterpriseId,
    workshopId: row.workshopId,
    code: row.code,
    name: row.name,
    productType: row.productType ?? '',
    designCapacity: row.designCapacity,
    capacityUnit: row.capacityUnit ?? '',
    status: row.status,
    remark: row.remark ?? ''
  })
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.workshopId) {
    return
  }
  saving.value = true
  try {
    const payload = {
      workshopId: form.workshopId,
      code: form.code,
      name: form.name,
      productType: form.productType,
      designCapacity: form.designCapacity,
      capacityUnit: form.capacityUnit,
      status: form.status,
      remark: form.remark,
      enterpriseId: isAdmin.value ? form.enterpriseId : undefined
    }
    if (dialogMode.value === 'create') {
      await createLine(payload)
      ElMessage.success('产线创建成功')
    } else if (currentId.value) {
      await updateLine(currentId.value, payload)
      ElMessage.success('产线更新成功')
    }
    dialogVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function remove(row: ProductionLine): Promise<void> {
  await ElMessageBox.confirm(`确定删除产线「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteLine(row.id)
  ElMessage.success('已删除')
  refresh()
}

async function toggleStatus(row: ProductionLine): Promise<void> {
  await updateLineStatus(row.id, row.status === 1 ? 0 : 1)
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
        <h2>产线管理</h2>
        <p>产线产能、产品类型与所属车间维护。</p>
      </div>
    </div>
    <el-card shadow="never">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" placeholder="产线编码 / 名称" clearable style="width: 190px" @keyup.enter="search" />
        <el-select v-if="isAdmin" v-model="query.enterpriseId" placeholder="所属企业" clearable style="width: 170px">
          <el-option v-for="item in enterpriseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.workshopId" placeholder="所属车间" clearable style="width: 150px">
          <el-option v-for="item in workshopOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="userStore.hasPermission('master:line:create')" type="primary" @click="openCreate">
          新增产线
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
      <template #default>
        <el-button size="small" @click="refresh">重新加载</el-button>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="code" label="产线编码" width="110" />
      <el-table-column prop="name" label="产线名称" min-width="140" />
      <el-table-column prop="workshopName" label="所属车间" width="130" />
      <el-table-column prop="enterpriseName" label="所属企业" min-width="150" show-overflow-tooltip />
      <el-table-column prop="productType" label="产品类型" width="110" />
      <el-table-column label="设计产能" width="120" align="right">
        <template #default="{ row }">
          {{ row.designCapacity ? `${row.designCapacity} ${row.capacityUnit || ''}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPermission('master:line:update')" text type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-if="userStore.hasPermission('master:line:update')" text type="warning" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button v-if="userStore.hasPermission('master:line:delete')" text type="danger" @click="remove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无产线数据" />
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

  <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增产线' : '编辑产线'" width="540px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item v-if="isAdmin" label="所属企业">
        <el-select v-model="form.enterpriseId" placeholder="请选择企业" style="width: 100%">
          <el-option v-for="item in enterpriseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属车间" prop="workshopId">
        <el-select v-model="form.workshopId" placeholder="请选择车间" style="width: 100%">
          <el-option v-for="item in formWorkshops" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="产线编码" prop="code">
        <el-input v-model="form.code" maxlength="64" />
      </el-form-item>
      <el-form-item label="产线名称" prop="name">
        <el-input v-model="form.name" maxlength="128" />
      </el-form-item>
      <el-form-item label="产品类型">
        <el-input v-model="form.productType" maxlength="64" />
      </el-form-item>
      <el-form-item label="设计产能">
        <el-input-number v-model="form.designCapacity" :min="0" :precision="4" style="width: 100%" />
      </el-form-item>
      <el-form-item label="产能单位">
        <el-input v-model="form.capacityUnit" maxlength="16" placeholder="如 件/日" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">正常</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
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
