<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createRole, deleteRole, pageRoles, updateRole, updateRoleStatus } from '@/api/role'
import type { RolePayload, RoleQuery } from '@/api/role'
import type { RoleItem } from '@/types/models'
import { permissionGroups } from '@/constants/permissions'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<RoleItem>(
    (params) => pageRoles(params as unknown as RoleQuery),
    { keyword: '', status: '' }
  )

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)
const form = reactive({
  roleCode: '',
  roleName: '',
  description: '',
  sortNo: 100,
  status: 1,
  permissions: [] as string[],
  allPermissions: false,
  builtin: 0
})

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

function openCreate(): void {
  dialogMode.value = 'create'
  currentId.value = null
  Object.assign(form, {
    roleCode: '',
    roleName: '',
    description: '',
    sortNo: 100,
    status: 1,
    permissions: [],
    allPermissions: false,
    builtin: 0
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEdit(row: RoleItem): void {
  dialogMode.value = 'edit'
  currentId.value = row.id
  const isAll = row.permissions.includes('*')
  Object.assign(form, {
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description ?? '',
    sortNo: row.sortNo ?? 100,
    status: row.status,
    permissions: isAll ? [] : [...row.permissions],
    allPermissions: isAll,
    builtin: row.builtin ?? 0
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
    const payload: RolePayload = {
      roleCode: form.roleCode,
      roleName: form.roleName,
      description: form.description,
      sortNo: form.sortNo,
      status: form.status,
      permissions: form.allPermissions ? ['*'] : form.permissions
    }
    if (dialogMode.value === 'create') {
      await createRole(payload)
      ElMessage.success('角色创建成功')
    } else if (currentId.value) {
      await updateRole(currentId.value, payload)
      ElMessage.success('角色更新成功')
    }
    dialogVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: RoleItem): Promise<void> {
  await updateRoleStatus(row.id, row.status === 1 ? 0 : 1)
  refresh()
}

async function remove(row: RoleItem): Promise<void> {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '删除确认', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  refresh()
}

onMounted(refresh)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>角色管理</h2>
        <p>角色权限码与数据范围配置。</p>
      </div>
    </div>
    <el-card shadow="never">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" placeholder="角色编码 / 名称" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="userStore.hasPermission('system:role:create')" type="primary" @click="openCreate">
          新增角色
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
      <template #default>
        <el-button size="small" @click="refresh">重新加载</el-button>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="roleCode" label="角色编码" width="170" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="权限" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.permissions.includes('*')" type="danger" size="small">全部</el-tag>
          <span v-else>{{ row.permissions.length }} 项</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.builtin === 1 ? 'info' : 'success'" size="small">
            {{ row.builtin === 1 ? '内置' : '自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :disabled="!userStore.hasPermission('system:role:update') || row.builtin === 1"
            @change="toggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPermission('system:role:update')" text type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-if="userStore.hasPermission('system:role:delete')"
            text
            type="danger"
            :disabled="row.builtin === 1"
            @click="remove(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无角色数据" />
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

  <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增角色' : '编辑角色'" width="680px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="角色编码" prop="roleCode">
        <el-input v-model="form.roleCode" :disabled="form.builtin === 1" maxlength="64" />
      </el-form-item>
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" maxlength="64" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" maxlength="255" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortNo" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="全部权限">
        <el-switch v-model="form.allPermissions" />
        <span class="text-muted" style="margin-left: 8px">开启后拥有系统全部权限（仅平台管理员应使用）</span>
      </el-form-item>
      <el-form-item v-if="!form.allPermissions" label="权限配置">
        <div class="perm-groups">
          <div v-for="group in permissionGroups" :key="group.label" class="perm-group">
            <div class="perm-group-title">{{ group.label }}</div>
            <el-checkbox-group v-model="form.permissions">
              <el-checkbox v-for="option in group.options" :key="option.code" :value="option.code">
                {{ option.label }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
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

.perm-groups {
  width: 100%;
}

.perm-group {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}

.perm-group:last-child {
  border-bottom: none;
}

.perm-group-title {
  margin-bottom: 6px;
  font-weight: 600;
  color: #606266;
  font-size: 13px;
}
</style>
