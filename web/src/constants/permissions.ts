export interface PermissionOption {
  code: string
  label: string
}

export interface PermissionGroup {
  label: string
  options: PermissionOption[]
}

/**
 * 权限码分组（与后端 PermissionCodes 保持一致，新增权限时同步维护）。
 */
export const permissionGroups: PermissionGroup[] = [
  {
    label: '系统管理',
    options: [
      { code: 'system:user:list', label: '用户查看' },
      { code: 'system:user:create', label: '用户新增' },
      { code: 'system:user:update', label: '用户修改' },
      { code: 'system:user:delete', label: '用户删除' },
      { code: 'system:user:reset', label: '重置密码' },
      { code: 'system:role:list', label: '角色查看' },
      { code: 'system:role:create', label: '角色新增' },
      { code: 'system:role:update', label: '角色修改' },
      { code: 'system:role:delete', label: '角色删除' }
    ]
  },
  {
    label: '企业管理',
    options: [
      { code: 'master:enterprise:list', label: '查看' },
      { code: 'master:enterprise:create', label: '新增' },
      { code: 'master:enterprise:update', label: '修改' },
      { code: 'master:enterprise:delete', label: '删除' }
    ]
  },
  {
    label: '车间管理',
    options: [
      { code: 'master:workshop:list', label: '查看' },
      { code: 'master:workshop:create', label: '新增' },
      { code: 'master:workshop:update', label: '修改' },
      { code: 'master:workshop:delete', label: '删除' }
    ]
  },
  {
    label: '产线管理',
    options: [
      { code: 'master:line:list', label: '查看' },
      { code: 'master:line:create', label: '新增' },
      { code: 'master:line:update', label: '修改' },
      { code: 'master:line:delete', label: '删除' }
    ]
  },
  {
    label: '设备管理',
    options: [
      { code: 'master:device:list', label: '查看' },
      { code: 'master:device:create', label: '新增' },
      { code: 'master:device:update', label: '修改' },
      { code: 'master:device:delete', label: '删除' }
    ]
  },
  {
    label: '能源类型',
    options: [
      { code: 'master:energy:list', label: '查看' },
      { code: 'master:energy:create', label: '新增' },
      { code: 'master:energy:update', label: '修改' },
      { code: 'master:energy:delete', label: '删除' }
    ]
  },
  {
    label: '计量点',
    options: [
      { code: 'master:meter:list', label: '查看' },
      { code: 'master:meter:create', label: '新增' },
      { code: 'master:meter:update', label: '修改' },
      { code: 'master:meter:delete', label: '删除' }
    ]
  },
  {
    label: '数据湖',
    options: [
      { code: 'datalake:raw:list', label: '原始数据查看' },
      { code: 'datalake:detail:list', label: '清洗明细查看' },
      { code: 'datalake:log:list', label: '入库日志查看' },
      { code: 'datalake:quality:list', label: '质量监控' },
      { code: 'datalake:completeness:list', label: '完整率查看' },
      { code: 'datalake:ingest', label: '数据接入' },
      { code: 'datalake:import', label: '文件导入' },
      { code: 'datalake:clean', label: '触发清洗' },
      { code: 'datalake:simulate', label: '模拟采集' },
      { code: 'datalake:gap:manage', label: '缺失检测与补录' },
      { code: 'datalake:ads:view', label: '驾驶舱与画像' }
    ]
  },
  {
    label: '画像与驾驶舱',
    options: [
      { code: 'profile:cockpit:view', label: '能源驾驶舱' },
      { code: 'profile:enterprise:view', label: '企业画像' },
      { code: 'profile:line:view', label: '产线画像' },
      { code: 'profile:device:view', label: '设备画像' },
      { code: 'profile:hierarchy:view', label: '层级下钻' },
      { code: 'factory:view', label: '三维工厂' },
      { code: 'baseline:view', label: '基线查看' },
      { code: 'baseline:train', label: '基线训练/预测' },
      { code: 'baseline:manage', label: '基线版本管理' },
      { code: 'diagnosis:view', label: '异常诊断查看' },
      { code: 'diagnosis:scan', label: '执行诊断扫描' },
      { code: 'diagnosis:handle', label: '异常处理/报告确认' },
      { code: 'diagnosis:rule', label: '规则配置' },
      { code: 'rectify:view', label: '整改查看' },
      { code: 'rectify:create', label: '创建/上传' },
      { code: 'rectify:assign', label: '分配/转派/撤销' },
      { code: 'rectify:audit', label: '整改审核' },
      { code: 'rectify:verify', label: '效果验证' },
      { code: 'rectify:mobile', label: '小程序端' },
      { code: 'saving:view', label: '核算查看' },
      { code: 'saving:calc', label: '执行核算' },
      { code: 'saving:audit', label: '核算审核' },
      { code: 'saving:export', label: '核算导出' }
    ]
  }
]

export const allPermissionCodes: string[] = permissionGroups.flatMap((group) =>
  group.options.map((option) => option.code)
)
