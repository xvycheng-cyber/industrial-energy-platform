# 整改任务与微信小程序

> 模块：`backend/src/main/java/com/smarten/module/rectification` + `miniprogram/`
> 闭环：诊断报告 → 整改任务 → 分配/转派 → 小程序反馈 → Web 审核 → 效果验证 → 完成。

---

## 1. 任务状态机

| 状态码 | 状态 | 允许流转到 |
| --- | --- | --- |
| 0 | 待分配 | 待整改、已撤销 |
| 1 | 待整改 | 整改中、待整改（转派）、已撤销 |
| 2 | 整改中 | 待审核、待整改（转派）、已撤销 |
| 3 | 待审核 | 效果验证中（审核通过）、已驳回、已撤销 |
| 7 | 效果验证中 | 已完成、整改中（验证不通过）、已撤销 |
| 4 | 已完成 | 终态 |
| 5 | 已驳回 | 待整改（重新分配/转派）、已撤销 |
| 6 | 已撤销 | 终态 |

所有流转在 `RectificationStatus.check()` 中统一校验，非法流转返回业务码 **6001**，前端无法直接改状态。

**关键约束**：审核通过只能进入「效果验证中」（7），必须再由效果验证接口才能到「已完成」（4），不会直接标记节能完成。

---

## 2. Web 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/rectification/tasks` | `rectify:view` | 状态/负责人/关键字/超期筛选 |
| GET | `/api/rectification/tasks/{id}` | `rectify:view` | 详情 + 反馈 + 诊断结论 |
| POST | `/api/rectification/tasks` | `rectify:create` | 从报告或设备创建设备任务 |
| POST | `/api/rectification/tasks/{id}/assign` | `rectify:assign` | 分配 |
| POST | `/api/rectification/tasks/{id}/transfer` | `rectify:assign` | 转派 |
| POST | `/api/rectification/tasks/{id}/audit` | `rectify:audit` | 通过/驳回（驳回必填原因） |
| POST | `/api/rectification/tasks/{id}/verify` | `rectify:verify` | 效果验证 |
| POST | `/api/rectification/tasks/{id}/cancel` | `rectify:assign` | 撤销 |
| GET | `/api/rectification/assignees` | `rectify:assign` | 可分配工程师 |
| GET | `/api/rectification/stats` | `rectify:view` | 状态统计 + 超期数 |
| POST | `/api/rectification/upload` | `rectify:create` | 上传现场图片 |

---

## 3. 小程序接口（独立 Controller）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/mobile/auth/login` | 校验工程师/管理员身份后签发 JWT |
| GET | `/api/mobile/tasks` | 仅返回 `assignee_id = 当前用户` 的任务 |
| GET | `/api/mobile/tasks/{id}` | 归属校验，非本人任务返回 403 |
| POST | `/api/mobile/tasks/{id}/start` | 开始整改 |
| POST | `/api/mobile/tasks/{id}/feedback` | 进度/完工反馈（含前后情况、图片、实际节能） |
| POST | `/api/mobile/upload` | 现场图片上传 |
| GET | `/api/mobile/anomalies` | 异常列表（企业授权范围） |
| GET | `/api/mobile/anomalies/{id}` | 异常详情 |
| GET | `/api/mobile/profile` | 个人中心与任务统计 |

登录复用后端 `AuthService`，移动端只做身份白名单校验；任务查询、开始、反馈均二次校验 `assignee_id`，防止越权。

---

## 4. 图片上传

- 接口：`POST /api/mobile/upload`（multipart，字段 `file`）。
- 类型：仅 `image/jpeg`、`image/png`、`image/webp`，扩展名同步校验。
- 大小：单文件 ≤ **5MB**，单次反馈最多 9 张。
- 存储：`uploads/yyyyMMdd/<uuid>.<ext>`，记录 `sys_file`（原文件名、大小、MIME、上传人、URL）。
- 访问：`/uploads/**` 静态映射（用于小程序 `image` 预览）；开发环境 Vite 已代理 `/uploads`。

---

## 5. 微信小程序（原生）

页面（`miniprogram/pages`）：

| 页面 | 功能 |
| --- | --- |
| `login` | 账号密码登录，保存 token 与用户 |
| `index` | 工作台：待整改/进行中/已完成统计、快捷入口、待办任务 |
| `anomalies` | 异常列表（风险筛选、分页） |
| `anomaly-detail` | 异常详情、能耗对比、规则证据 |
| `tasks` | 我的任务（全部/待整改/整改中/待审核/已完成） |
| `task-detail` | 任务详情、整改措施、诊断结论、反馈时间线、开始整改 |
| `feedback` | 整改说明、整改前后情况、实际节能量、现场图片、完工开关 |
| `mine` | 个人信息、统计、退出登录 |

- 原生 WXML/WXSS/JS，无 uni-app / Taro。
- 统一 `utils/request.js`（token 注入、401 跳登录），`utils/config.js` 配置后端地址与上传路径。
- 页面具备加载、空态、错误提示；图片使用 `wx.chooseMedia` + `wx.uploadFile`。

---

## 6. 测试

`RectificationFlowTest`：

1. 创建→分配→小程序登录→开始→完工反馈→审核→效果验证→完成，断言审核后状态为「效果验证中」而非完成。
2. 驳回必须填写原因；驳回后转派回到「待整改」，转派次数 +1。
3. 工程师访问他人任务返回 403。
4. 小程序登录：工程师 200，只读角色 403。
5. 图片上传：png 成功、txt 拒绝。
6. 个人中心与异常列表可用。

全量测试：**56 个用例通过**。

---

## 7. 演示与边界

- 演示库中 3 条任务覆盖「待分配 / 待整改 / 待审核」三种状态。
- 小程序未在本机微信开发者工具中运行（环境未安装），已完成 JS 语法、JSON 配置校验；页面接口与后端集成测试覆盖。
- 图片访问为公开静态路径，生产建议改为带签名的对象存储 URL。
- 效果验证目前为人工确认；P8 节能量核算接入后可自动带出实际节能量并校验观察期。
