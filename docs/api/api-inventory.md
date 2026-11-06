# API 接口清单

> 由运行中的 Springdoc OpenAPI 自动生成：`GET /api/v3/api-docs`。
> 接口总数：**129**，路径数：**104**。

## /auth

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 登录 | 公开 |
| GET | `/auth/me` | 当前用户 | 公开 |
| GET | `/auth/menus` | 当前用户菜单 | 公开 |
| POST | `/auth/refresh` | 刷新令牌 | 公开 |

## /baseline

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/baseline/models` | 模型列表 | 公开 |
| GET | `/baseline/models/{id}` | 模型详情 | 公开 |
| POST | `/baseline/models/{id}/disable` | 停用模型 | 公开 |
| POST | `/baseline/models/{id}/enable` | 启用/切换版本 | 公开 |
| GET | `/baseline/models/{id}/error` | 模型误差分析 | 公开 |
| POST | `/baseline/models/train` | 训练基线模型 | 公开 |
| POST | `/baseline/predict` | 按模型生成基线预测 | 公开 |
| GET | `/baseline/records` | 基线明细（基线 vs 实际） | 公开 |

## /datalake

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/datalake/ads/dashboard` | 驾驶舱统计 | 公开 |
| GET | `/datalake/ads/profile` | 能耗画像 | 公开 |
| POST | `/datalake/aggregate` | 重算聚合 | 公开 |
| GET | `/datalake/aggregates` | 聚合数据分页（DWS） | 公开 |
| POST | `/datalake/clean` | 触发清洗 | 公开 |
| GET | `/datalake/completeness` | 数据完整率 | 公开 |
| GET | `/datalake/details` | 清洗明细分页（DWD） | 公开 |
| GET | `/datalake/failures` | 失败记录分页 | 公开 |
| GET | `/datalake/gaps` | 缺口分页 | 公开 |
| POST | `/datalake/gaps/backfill` | 缺口补录 | 公开 |
| POST | `/datalake/gaps/detect` | 缺失检测 | 公开 |
| POST | `/datalake/import` | Excel/CSV 导入 | 公开 |
| POST | `/datalake/ingest` | 批量接入 | 公开 |
| GET | `/datalake/logs` | 入库日志分页 | 公开 |
| GET | `/datalake/quality/summary` | 数据质量概览 | 公开 |
| GET | `/datalake/raw` | 原始数据分页（ODS） | 公开 |
| POST | `/datalake/simulate` | 模拟采集 | 公开 |

## /demo

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/demo/full-chain` | 生成全链路演示数据 | 公开 |
| POST | `/demo/reset` | 重置联调演示数据 | 公开 |
| GET | `/demo/status` | 联调数据状态 | 公开 |

## /diagnosis

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/diagnosis/anomalies` | 异常列表 | 公开 |
| GET | `/diagnosis/anomalies/{id}` | 异常详情 | 公开 |
| POST | `/diagnosis/anomalies/{id}/handle` | 处理异常（确认/误报/转诊断/关闭） | 公开 |
| GET | `/diagnosis/anomalies/{id}/report` | 按异常查询诊断报告 | 公开 |
| GET | `/diagnosis/overview` | 诊断总览（趋势、分类、状态） | 公开 |
| GET | `/diagnosis/reports/{id}` | 诊断报告详情 | 公开 |
| POST | `/diagnosis/reports/{id}/confirm` | 人工确认故障原因 | 公开 |
| GET | `/diagnosis/rules` | 规则列表 | 公开 |
| PUT | `/diagnosis/rules/{id}` | 更新规则 | 公开 |
| POST | `/diagnosis/rules/{id}/toggle` | 启用/停用规则 | 公开 |
| POST | `/diagnosis/scan` | 执行诊断扫描 | 公开 |

## /factory

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/factory/devices/{id}` | 设备详情 | 公开 |
| GET | `/factory/lines/{id}` | 产线详情 | 公开 |
| GET | `/factory/scene` | 工厂场景 | 公开 |

## /master

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/master/devices` | 设备分页 | 公开 |
| POST | `/master/devices` | 新增设备 | 公开 |
| DELETE | `/master/devices/{id}` | 删除设备 | 公开 |
| GET | `/master/devices/{id}` | 设备详情 | 公开 |
| PUT | `/master/devices/{id}` | 修改设备 | 公开 |
| PUT | `/master/devices/{id}/status` | 设备状态变更（运行/停机/检修/停用） | 公开 |
| GET | `/master/energy-types` | 能源类型分页 | 公开 |
| POST | `/master/energy-types` | 新增能源类型 | 公开 |
| DELETE | `/master/energy-types/{id}` | 删除能源类型 | 公开 |
| GET | `/master/energy-types/{id}` | 能源类型详情 | 公开 |
| PUT | `/master/energy-types/{id}` | 修改能源类型 | 公开 |
| PUT | `/master/energy-types/{id}/status` | 启用/停用能源类型 | 公开 |
| GET | `/master/enterprises` | 企业分页 | 公开 |
| POST | `/master/enterprises` | 新增企业 | 公开 |
| DELETE | `/master/enterprises/{id}` | 删除企业 | 公开 |
| GET | `/master/enterprises/{id}` | 企业详情 | 公开 |
| PUT | `/master/enterprises/{id}` | 修改企业 | 公开 |
| PUT | `/master/enterprises/{id}/status` | 启用/停用企业 | 公开 |
| GET | `/master/lines` | 产线分页 | 公开 |
| POST | `/master/lines` | 新增产线 | 公开 |
| DELETE | `/master/lines/{id}` | 删除产线 | 公开 |
| GET | `/master/lines/{id}` | 产线详情 | 公开 |
| PUT | `/master/lines/{id}` | 修改产线 | 公开 |
| PUT | `/master/lines/{id}/status` | 启用/停用产线 | 公开 |
| GET | `/master/meters` | 计量点分页 | 公开 |
| POST | `/master/meters` | 新增计量点 | 公开 |
| DELETE | `/master/meters/{id}` | 删除计量点 | 公开 |
| GET | `/master/meters/{id}` | 计量点详情 | 公开 |
| PUT | `/master/meters/{id}` | 修改计量点 | 公开 |
| PUT | `/master/meters/{id}/status` | 启用/停用计量点 | 公开 |
| GET | `/master/workshops` | 车间分页 | 公开 |
| POST | `/master/workshops` | 新增车间 | 公开 |
| DELETE | `/master/workshops/{id}` | 删除车间 | 公开 |
| GET | `/master/workshops/{id}` | 车间详情 | 公开 |
| PUT | `/master/workshops/{id}` | 修改车间 | 公开 |
| PUT | `/master/workshops/{id}/status` | 启用/停用车间 | 公开 |

## /mobile

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/mobile/anomalies` | 异常列表（企业授权范围） | 公开 |
| GET | `/mobile/anomalies/{id}` | 异常详情 | 公开 |
| POST | `/mobile/auth/login` | 小程序登录 | 公开 |
| GET | `/mobile/profile` | 个人中心 | 公开 |
| GET | `/mobile/tasks` | 我的任务 | 公开 |
| GET | `/mobile/tasks/{id}` | 任务详情 | 公开 |
| POST | `/mobile/tasks/{id}/feedback` | 提交整改反馈 | 公开 |
| POST | `/mobile/tasks/{id}/start` | 开始整改 | 公开 |
| POST | `/mobile/upload` | 上传现场图片 | 公开 |

## /profile

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/profile/cockpit` | 能源驾驶舱 | 公开 |
| GET | `/profile/device/{id}` | 设备能耗画像 | 公开 |
| GET | `/profile/enterprise/{id}` | 企业能耗画像 | 公开 |
| GET | `/profile/hierarchy` | 层级树 | 公开 |
| GET | `/profile/line/{id}` | 产线能耗画像 | 公开 |

## /rectification

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/rectification/assignees` | 可分配工程师 | 公开 |
| GET | `/rectification/stats` | 整改统计 | 公开 |
| GET | `/rectification/tasks` | 任务列表 | 公开 |
| POST | `/rectification/tasks` | 创建整改任务 | 公开 |
| GET | `/rectification/tasks/{id}` | 任务详情（含反馈与诊断结论） | 公开 |
| POST | `/rectification/tasks/{id}/assign` | 分配任务 | 公开 |
| POST | `/rectification/tasks/{id}/audit` | 审核 | 公开 |
| POST | `/rectification/tasks/{id}/cancel` | 撤销任务 | 公开 |
| POST | `/rectification/tasks/{id}/feedback` | Web 端提交反馈（可选） | 公开 |
| POST | `/rectification/tasks/{id}/transfer` | 转派任务 | 公开 |
| POST | `/rectification/tasks/{id}/verify` | 效果验证 | 公开 |
| POST | `/rectification/upload` | 上传现场图片 | 公开 |

## /saving

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/saving/export` | 导出核算结果 CSV | 公开 |
| GET | `/saving/results` | 核算结果列表 | 公开 |
| GET | `/saving/results/{id}` | 核算详情（含逐小时对比与历史版本） | 公开 |
| POST | `/saving/results/{id}/confirm` | 审核核算结果 | 公开 |
| GET | `/saving/summary` | 节能排名与趋势 | 公开 |
| POST | `/saving/tasks/{taskId}/calculate` | 执行核算 | 公开 |

## /system

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/system/health` | 健康检查 | 公开 |
| GET | `/system/roles` | 角色分页 | 公开 |
| POST | `/system/roles` | 新增角色 | 公开 |
| DELETE | `/system/roles/{id}` | 删除角色 | 公开 |
| GET | `/system/roles/{id}` | 角色详情 | 公开 |
| PUT | `/system/roles/{id}` | 修改角色 | 公开 |
| PUT | `/system/roles/{id}/status` | 启用/禁用角色 | 公开 |
| GET | `/system/roles/all` | 全部启用角色（下拉用） | 公开 |
| GET | `/system/users` | 用户分页 | 公开 |
| POST | `/system/users` | 新增用户 | 公开 |
| DELETE | `/system/users/{id}` | 删除用户 | 公开 |
| GET | `/system/users/{id}` | 用户详情 | 公开 |
| PUT | `/system/users/{id}` | 修改用户 | 公开 |
| PUT | `/system/users/{id}/password` | 重置密码 | 公开 |
| PUT | `/system/users/{id}/status` | 启用/禁用用户 | 公开 |

