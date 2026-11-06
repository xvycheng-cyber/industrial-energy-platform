<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createEnterprise,
  deleteEnterprise,
  pageEnterprises,
  updateEnterprise,
  updateEnterpriseStatus
} from '@/api/master'
import type { MasterQuery } from '@/api/master'
import type { Enterprise } from '@/types/models'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<Enterprise>(
    (params) => pageEnterprises(params as unknown as MasterQuery),
    { keyword: '', status: '' }
  )

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)
const form = reactive({
  code: '',
  name: '',
  industry: '',
  region: '',
  address: '',
  contactName: '',
  contactPhone: '',
  status: 1,
  remark: ''
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入企业编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }]
}

function resetForm(): void {
  Object.assign(form, {
    code: '',
    name: '',
    industry: '',
    region: '',
    address: '',
    contactName: '',
    contactPhone: '',
    status: 1,
    remark: ''
  })
  currentId.value = null
  formRef.value?.clearValidate()
}

function openCreate(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Enterprise): void {
  dialogMode.value = 'edit'
  currentId.value = row.id
  Object.assign(form, {
    code: row.code,
    name: row.name,
    industry: row.industry ?? '',
    region: row.region ?? '',
    address: row.address ?? '',
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
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
    if (dialogMode.value === 'create') {
      await createEnterprise({ ...form })
      ElMessage.success('企业创建成功')
    } else if (currentId.value) {
      await updateEnterprise(currentId.value, { ...form })
      ElMessage.success('企业更新成功')
    }
    dialogVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function remove(row: Enterprise): Promise<void> {
  await ElMessageBox.confirm(`确定删除企业「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteEnterprise(row.id)
  ElMessage.success('已删除')
  refresh()
}

async function toggleStatus(row: Enterprise): Promise<void> {
  const next = row.status === 1 ? 0 : 1
  await updateEnterpriseStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  refresh()
}

onMounted(refresh)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>企业管理</h2>
        <p>企业租户、行业与联系方式维护。</p>
      </div>
    </div>
    <el-card shadow="never">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" placeholder="企业编码 / 名称" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="userStore.hasPermission('master:enterprise:create')" type="primary" @click="openCreate">
          新增企业
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
      <template #default>
        <el-button size="small" @click="refresh">重新加载</el-button>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="code" label="企业编码" width="130" />
      <el-table-column prop="name" label="企业名称" min-width="190" show-overflow-tooltip />
      <el-table-column prop="industry" label="行业" width="120" />
      <el-table-column prop="region" label="地区" width="130" />
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="contactPhone" label="联系电话" width="135" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPermission('master:enterprise:update')" text type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-if="userStore.hasPermission('master:enterprise:update')" text type="warning" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button v-if="userStore.hasPermission('master:enterprise:delete')" text type="danger" @click="remove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无企业数据" />
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

  <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增企业' : '编辑企业'" width="560px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="企业编码" prop="code">
        <el-input v-model="form.code" maxlength="64" />
      </el-form-item>
      <el-form-item label="企业名称" prop="name">
        <el-input v-model="form.name" maxlength="128" />
      </el-form-item>
      <el-form-item label="行业">
        <el-input v-model="form.industry" maxlength="64" />
      </el-form-item>
      <el-form-item label="地区">
        <el-input v-model="form.region" maxlength="64" />
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="form.address" maxlength="255" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="form.contactName" maxlength="64" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="form.contactPhone" maxlength="20" />
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
