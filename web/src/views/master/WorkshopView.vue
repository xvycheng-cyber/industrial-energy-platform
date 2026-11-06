<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createWorkshop,
  deleteWorkshop,
  pageEnterprises,
  pageWorkshops,
  updateWorkshop,
  updateWorkshopStatus
} from '@/api/master'
import type { MasterQuery } from '@/api/master'
import type { Enterprise, Workshop } from '@/types/models'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('PLATFORM_ADMIN'))

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<Workshop>(
    (params) => pageWorkshops(params as unknown as MasterQuery),
    { keyword: '', status: '', enterpriseId: '' }
  )

const enterpriseOptions = ref<Enterprise[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)
const form = reactive({
  enterpriseId: undefined as number | undefined,
  code: '',
  name: '',
  manager: '',
  area: undefined as number | undefined,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  enterpriseId: [{ required: true, message: '请选择所属企业', trigger: 'change' }],
  code: [{ required: true, message: '请输入车间编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入车间名称', trigger: 'blur' }]
}

async function loadEnterpriseOptions(): Promise<void> {
  const result = await pageEnterprises({ pageNum: 1, pageSize: 500 })
  enterpriseOptions.value = result.records
}

function openCreate(): void {
  dialogMode.value = 'create'
  currentId.value = null
  Object.assign(form, {
    enterpriseId: isAdmin.value ? enterpriseOptions.value[0]?.id : undefined,
    code: '',
    name: '',
    manager: '',
    area: undefined,
    status: 1,
    remark: ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEdit(row: Workshop): void {
  dialogMode.value = 'edit'
  currentId.value = row.id
  Object.assign(form, {
    enterpriseId: row.enterpriseId,
    code: row.code,
    name: row.name,
    manager: row.manager ?? '',
    area: row.area,
    status: row.status,
    remark: row.remark ?? ''
  })
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const payload = {
      code: form.code,
      name: form.name,
      manager: form.manager,
      area: form.area,
      status: form.status,
      remark: form.remark,
      enterpriseId: isAdmin.value ? form.enterpriseId : undefined
    }
    if (dialogMode.value === 'create') {
      await createWorkshop(payload)
      ElMessage.success('车间创建成功')
    } else if (currentId.value) {
      await updateWorkshop(currentId.value, payload)
      ElMessage.success('车间更新成功')
    }
    dialogVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function remove(row: Workshop): Promise<void> {
  await ElMessageBox.confirm(`确定删除车间「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteWorkshop(row.id)
  ElMessage.success('已删除')
  refresh()
}

async function toggleStatus(row: Workshop): Promise<void> {
  await updateWorkshopStatus(row.id, row.status === 1 ? 0 : 1)
  refresh()
}

onMounted(async () => {
  await loadEnterpriseOptions()
  await refresh()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>车间管理</h2>
        <p>车间归属、负责人与面积信息维护。</p>
      </div>
    </div>
    <el-card shadow="never">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" placeholder="车间编码 / 名称" clearable style="width: 210px" @keyup.enter="search" />
        <el-select v-if="isAdmin" v-model="query.enterpriseId" placeholder="所属企业" clearable style="width: 190px">
          <el-option v-for="item in enterpriseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="userStore.hasPermission('master:workshop:create')" type="primary" @click="openCreate">
          新增车间
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
      <template #default>
        <el-button size="small" @click="refresh">重新加载</el-button>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="enterpriseName" label="所属企业" min-width="160" show-overflow-tooltip />
      <el-table-column prop="code" label="车间编码" width="120" />
      <el-table-column prop="name" label="车间名称" min-width="150" />
      <el-table-column prop="manager" label="负责人" width="100" />
      <el-table-column prop="area" label="面积(㎡)" width="105" align="right" />
      <el-table-column label="状态" width="85" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPermission('master:workshop:update')" text type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-if="userStore.hasPermission('master:workshop:update')" text type="warning" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button v-if="userStore.hasPermission('master:workshop:delete')" text type="danger" @click="remove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无车间数据" />
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

  <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增车间' : '编辑车间'" width="520px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item v-if="isAdmin" label="所属企业" prop="enterpriseId">
        <el-select v-model="form.enterpriseId" placeholder="请选择企业" style="width: 100%">
          <el-option v-for="item in enterpriseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="车间编码" prop="code">
        <el-input v-model="form.code" maxlength="64" />
      </el-form-item>
      <el-form-item label="车间名称" prop="name">
        <el-input v-model="form.name" maxlength="128" />
      </el-form-item>
      <el-form-item label="负责人">
        <el-input v-model="form.manager" maxlength="64" />
      </el-form-item>
      <el-form-item label="面积(㎡)">
        <el-input-number v-model="form.area" :min="0" :precision="2" style="width: 100%" />
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
