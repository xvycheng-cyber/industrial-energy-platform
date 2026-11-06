import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupRouterGuard } from './guard'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: DefaultLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'saving/results',
        name: 'saving-results',
        component: () => import('@/views/saving/SavingResultsView.vue'),
        meta: { title: '节能核算', permissions: ['saving:view'] }
      },
      {
        path: 'saving/results/:id',
        name: 'saving-detail',
        component: () => import('@/views/saving/SavingDetailView.vue'),
        meta: { title: '核算详情', permissions: ['saving:view'] }
      },
      {
        path: 'rectification/tasks',
        name: 'rectification-tasks',
        component: () => import('@/views/rectification/RectificationTasksView.vue'),
        meta: { title: '整改任务', permissions: ['rectify:view'] }
      },
      {
        path: 'diagnosis/overview',
        name: 'diagnosis-overview',
        component: () => import('@/views/diagnosis/DiagnosisOverviewView.vue'),
        meta: { title: '诊断总览', permissions: ['diagnosis:view'] }
      },
      {
        path: 'diagnosis/anomalies',
        name: 'diagnosis-anomalies',
        component: () => import('@/views/diagnosis/AnomalyListView.vue'),
        meta: { title: '异常监控', permissions: ['diagnosis:view'] }
      },
      {
        path: 'diagnosis/anomalies/:id',
        name: 'diagnosis-anomaly-detail',
        component: () => import('@/views/diagnosis/AnomalyDetailView.vue'),
        meta: { title: '异常详情', permissions: ['diagnosis:view'] }
      },
      {
        path: 'diagnosis/reports/:id',
        name: 'diagnosis-report',
        component: () => import('@/views/diagnosis/ReportView.vue'),
        meta: { title: '诊断报告', permissions: ['diagnosis:view'] }
      },
      {
        path: 'diagnosis/rules',
        name: 'diagnosis-rules',
        component: () => import('@/views/diagnosis/RulesView.vue'),
        meta: { title: '规则配置', permissions: ['diagnosis:rule'] }
      },
      {
        path: 'baseline/records',
        name: 'baseline-records',
        component: () => import('@/views/baseline/BaselineRecordsView.vue'),
        meta: { title: '基线列表', permissions: ['baseline:view'] }
      },
      {
        path: 'baseline/models',
        name: 'baseline-models',
        component: () => import('@/views/baseline/BaselineModelsView.vue'),
        meta: { title: '基线模型', permissions: ['baseline:view'] }
      },
      {
        path: 'baseline/models/:id',
        name: 'baseline-model-detail',
        component: () => import('@/views/baseline/BaselineModelDetailView.vue'),
        meta: { title: '基线模型详情', permissions: ['baseline:view'] }
      },
      {
        path: 'factory',
        name: 'factory',
        component: () => import('@/views/factory/Factory3DView.vue'),
        meta: { title: '三维工厂', permissions: ['factory:view'] }
      },
      {
        path: 'cockpit',
        name: 'cockpit',
        component: () => import('@/views/cockpit/CockpitView.vue'),
        meta: { title: '能源驾驶舱', permissions: ['profile:cockpit:view'] }
      },
      {
        path: 'profile/enterprise',
        name: 'profile-enterprise',
        component: () => import('@/views/profile/EnterpriseProfileView.vue'),
        meta: { title: '企业画像', permissions: ['profile:enterprise:view'] }
      },
      {
        path: 'profile/line',
        name: 'profile-line',
        component: () => import('@/views/profile/LineProfileView.vue'),
        meta: { title: '产线画像', permissions: ['profile:line:view'] }
      },
      {
        path: 'profile/device',
        name: 'profile-device',
        component: () => import('@/views/profile/DeviceProfileView.vue'),
        meta: { title: '设备画像', permissions: ['profile:device:view'] }
      },
      {
        path: 'master/enterprises',
        name: 'enterprises',
        component: () => import('@/views/master/EnterpriseView.vue'),
        meta: { title: '企业管理', permissions: ['master:enterprise:list'] }
      },
      {
        path: 'master/workshops',
        name: 'workshops',
        component: () => import('@/views/master/WorkshopView.vue'),
        meta: { title: '车间管理', permissions: ['master:workshop:list'] }
      },
      {
        path: 'master/lines',
        name: 'lines',
        component: () => import('@/views/master/LineView.vue'),
        meta: { title: '产线管理', permissions: ['master:line:list'] }
      },
      {
        path: 'master/devices',
        name: 'devices',
        component: () => import('@/views/master/DeviceView.vue'),
        meta: { title: '设备管理', permissions: ['master:device:list'] }
      },
      {
        path: 'datalake/ingest',
        name: 'datalake-ingest',
        component: () => import('@/views/datalake/IngestView.vue'),
        meta: { title: '数据接入', permissions: ['datalake:ingest'] }
      },
      {
        path: 'datalake/import',
        name: 'datalake-import',
        component: () => import('@/views/datalake/ImportView.vue'),
        meta: { title: '数据导入', permissions: ['datalake:import'] }
      },
      {
        path: 'datalake/cleaning',
        name: 'datalake-cleaning',
        component: () => import('@/views/datalake/CleaningView.vue'),
        meta: { title: '清洗记录', permissions: ['datalake:detail:list'] }
      },
      {
        path: 'datalake/quality',
        name: 'datalake-quality',
        component: () => import('@/views/datalake/QualityView.vue'),
        meta: { title: '质量监控', permissions: ['datalake:quality:list'] }
      },
      {
        path: 'datalake/logs',
        name: 'datalake-logs',
        component: () => import('@/views/datalake/LogsView.vue'),
        meta: { title: '入库日志', permissions: ['datalake:log:list'] }
      },
      {
        path: 'datalake/completeness',
        name: 'datalake-completeness',
        component: () => import('@/views/datalake/CompletenessView.vue'),
        meta: { title: '完整率', permissions: ['datalake:completeness:list'] }
      },
      {
        path: 'system/users',
        name: 'users',
        component: () => import('@/views/system/UserView.vue'),
        meta: { title: '用户管理', permissions: ['system:user:list'] }
      },
      {
        path: 'system/roles',
        name: 'roles',
        component: () => import('@/views/system/RoleView.vue'),
        meta: { title: '角色管理', permissions: ['system:role:list'] }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { title: '无权访问', public: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

setupRouterGuard(router)

export default router
