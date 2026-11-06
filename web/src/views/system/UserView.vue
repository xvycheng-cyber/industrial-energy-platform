<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createUser,
  deleteUser,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '@/api/user'
import type { UserPayload, UserQuery } from '@/api/user'
import { allRoles } from '@/api/role'
import { pageEnterprises } from '@/api/master'
import type { Enterprise, RoleItem, UserItem } from '@/types/models'
import { usePageList } from '@/composables/usePageList'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('PLATFORM_ADMIN'))

const { loading, error, records, total, pageNum, pageSize, query, search, reset, refresh, handlePageChange, handleSizeChange } =
  usePageList<UserItem>(
    (params) => pageUsers(params as unknown as UserQuery),
    { keyword: '', status: '', enterpriseId: '' }
  )

const enterpriseOptions = ref<Enterprise[]>([])
const roleOptions = ref<RoleItem[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)
const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  enterpriseId: undefined as number | undefined,
  status: 1,
  roleIds: [] as number[],
  remark: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const passwordDialogVisible = ref(false)
const passwordSaving = ref(false)
const passwordForm = reactive({ password: '' })
const passwordTargetId = ref<number | null>(null)

async function loadOptions(): Promise<void> {
  const [enterprises, roles] = await Promise.all([
    pageEnterprises({ pageNum: 1, pageSize: 500 }),
    allRoles()
  ])
  enterpriseOptions.value = enterprises.records
  roleOptions.value = roles
}

function openCreate(): void {
  dialogMode.value = 'create'
  currentId.value = null
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    enterpriseId: isAdmin.value ? enterpriseOptions.value[0]?.id : undefined,
    status: 1,
    roleIds: [],
    remark: ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEdit(row: UserItem): void {
  dialogMode.value = 'edit'
  currentId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName ?? '',
    phone: row.phone ?? '',
    email: row.email ?? '',
    enterpriseId: row.enterpriseId,
    status: row.status,
    roleIds: row.roles.map((role) => role.id),
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
    const payload: UserPayload = {
      username: form.username,
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      enterpriseId: isAdmin.value ? form.enterpriseId : undefined,
      status: form.status,
      roleIds: form.roleIds,
      remark: form.remark
    }
    if (dialogMode.value === 'create') {
      payload.password = form.password
      await createUser(payload)
      ElMessage.success('用户创建成功')
    } else if (currentId.value) {
      if (form.password) {
        payload.password = form.password
      }
      await updateUser(currentId.value, payload)
      ElMessage.success('用户更新成功')
    }
    dialogVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: UserItem): Promise<void> {
  await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
  refresh()
}

function openResetPassword(row: UserItem): void {
  passwordTargetId.value = row.id
  passwordForm.password = ''
  passwordDialogVisible.value = true
}

async function submitResetPassword(): Promise<void> {
  if (!passwordTargetId.value || passwordForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  passwordSaving.value = true
  try {
    await resetUserPassword(passwordTargetId.value, passwordForm.password)
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } finally {
    passwordSaving.value = false
  }
}

async function remove(row: UserItem): Promise<void> {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '删除确认', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
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
        <h2>用户管理</h2>
        <p>平台与企业账号、角色与状态维护。</p>
      </div>
    </div>
    <el-card shadow="never">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" placeholder="用户名 / 姓名 / 手机号" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-if="isAdmin" v-model="query.enterpriseId" placeholder="所属企业" clearable style="width: 190px">
          <el-option v-for="item in enterpriseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="userStore.hasPermission('system:user:create')" type="primary" @click="openCreate">
          新增用户
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="error-bar">
      <template #default>
        <el-button size="small" @click="refresh">重新加载</el-button>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="username" label="用户名" width="105" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="enterpriseName" label="所属企业" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.enterpriseName || '平台级' }}</template>
      </el-table-column>
      <el-table-column label="角色" min-width="130">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles" :key="role.id" size="small" class="role-tag">{{ role.roleName }}</el-tag>
          <span v-if="!row.roles?.length" class="text-muted">未分配</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="125" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :disabled="!userStore.hasPermission('system:user:update')"
            @change="toggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="最后登录" width="170">
        <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.hasPermission('system:user:update')" text type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-if="userStore.hasPermission('system:user:reset')" text type="warning" @click="openResetPassword(row)">
            重置密码
          </el-button>
          <el-button v-if="userStore.hasPermission('system:user:delete')" text type="danger" @click="remove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户数据" />
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

  <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增用户' : '编辑用户'" width="540px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="dialogMode === 'edit'" maxlength="64" />
      </el-form-item>
      <el-form-item :label="dialogMode === 'create' ? '初始密码' : '新密码'" :prop="dialogMode === 'create' ? 'password' : ''">
        <el-input v-model="form.password" type="password" show-password maxlength="64" :placeholder="dialogMode === 'edit' ? '留空则不修改' : ''" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.realName" maxlength="64" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" maxlength="20" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" maxlength="128" />
      </el-form-item>
      <el-form-item v-if="isAdmin" label="所属企业">
        <el-select v-model="form.enterpriseId" placeholder="平台账号可留空" clearable style="width: 100%">
          <el-option v-for="item in enterpriseOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色" prop="roleIds">
        <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
          <el-option v-for="item in roleOptions" :key="item.id" :label="item.roleName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
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

  <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px">
    <el-form label-width="80px">
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.password" type="password" show-password placeholder="至少 6 位" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordSaving" @click="submitResetPassword">确定</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<style scoped>
.error-bar {
  margin-bottom: 12px;
}

.role-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}
</style>
