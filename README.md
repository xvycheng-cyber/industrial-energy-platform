# 智能耗 · 工业企业能耗画像与节能诊断平台

面向工业企业的能耗采集、治理、画像、诊断与节能闭环平台。后端为模块化单体（Spring Boot 3.2 + Java 17），Web 端为 Vue 3 + Element Plus，工程师端为原生微信小程序。

> **当前状态：P8 + 全链路联调 + UI 统一 + 最终验收已完成（有条件通过：容器/真机未测）。**
> 已交付：24 张表与幂等初始化、登录/JWT/RBAC、企业级数据隔离、用户与角色、主数据接口、6 个 Web 管理页，以及 ODS→DWD→DWS→ADS 数据湖全链路（接入、导入、清洗、聚合、质量监控、完整率、模拟采集）。
> 未交付：能耗画像页、动态基线、异常诊断、整改、核算（P4 起）。

---

## 1. 已实现能力

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| 数据库 | ✅ | 24 张表，索引/唯一约束/注释完整，脚本幂等 |
| 登录鉴权 | ✅ | 账号密码登录、JWT、刷新令牌、401/403 JSON |
| 用户管理 | ✅ | 增删改查、分页、启停、重置密码、分配角色 |
| 角色管理 | ✅ | 角色 CRUD、权限码配置、内置角色保护 |
| 菜单权限 | ✅ | 菜单由后端按权限码下发，前端不硬编码 |
| 接口权限 | ✅ | `@PreAuthorize` + 权限码，越权返回 403 |
| 数据隔离 | ✅ | 非平台管理员仅能访问本企业数据 |
| 企业管理 | ✅ | CRUD、分页、启停、删除前置校验 |
| 车间/产线/设备 | ✅ | CRUD、层级关联、状态管理、关联名称回填 |
| 能源类型/计量点 | ✅（后端） | 接口完整；前端页面待 P4 |
| 数据湖 ODS | ✅ | 批量接入、CSV/Excel 导入、模拟采集、批次幂等 |
| 数据湖 DWD | ✅ | 校验、倍率换算、累计读数差值、去重、异常标记、失败记录 |
| 数据湖 DWS | ✅ | 5 分钟/15 分钟/小时/日/月 × 企业/车间/产线/设备 × 能源类型 |
| 数据湖 ADS | ✅ | 驾驶舱、能耗画像统计接口 |
| 完整率与缺口 | ✅ | 应采/有效/缺失/完整率，缺口检测与补录 |
| 能源驾驶舱 | ✅ | 8 项 KPI、7 张图，深蓝工业风 |
| 企业/产线/设备画像 | ✅ | 总量/结构/单耗/评价/趋势与下钻 |
| 三维工厂 | ✅ | 厂房/产线/设备/道路/管线，点击与悬浮交互，风险配色 |
| 动态基线 | ✅ | 多变量回归、工况分组、待机基线、版本管理 |
| 异常诊断 | ✅ | 8 类规则、评分合并、诊断报告、人工确认 |
| 整改闭环 | ✅ | 8 状态机、分配/转派/审核/验证、小程序 8 页 |
| 节能核算 | ✅ | 逐小时修正基线、分时电价、审核与导出 |
| 小程序 | ⬜ 骨架 | 登录与任务页 P7 接入 |

## 2. 技术栈（实际版本）

| 层 | 组件 | 版本 |
| --- | --- | --- |
| 后端 | Java（Temurin） | 17.0.21 |
| | Spring Boot | 3.2.12 |
| | MyBatis-Plus | 3.5.7 |
| | MySQL Connector/J | 8.x |
| | Spring Security / jjwt | 6.2.x / 0.12.6 |
| | Springdoc OpenAPI | 2.5.0 |
| Web | Vue / TypeScript / Vite | 3.5.44 / 5.6.3 / 5.4.21 |
| | Element Plus / ECharts | 2.14.7 / 5.6.0 |
| | Three.js / Axios | 0.169.0 / 1.21.0 |
| | Pinia / Vue Router | 2.3.1 / 4.6.4 |
| 小程序 | 原生 WXML / WXSS / JS | 基础库 3.5.5 |

## 3. 目录结构

```
industrial-energy-platform/
├── AGENTS.md                 # 协作规范（必读）
├── docs/                     # 架构、数据库、实施计划、进度
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/smarten/
│       │   ├── common/       # Result / 异常 / 分页 / 工具 / SPI
│       │   ├── config/       # MyBatis-Plus、CORS、Jackson、OpenAPI
│       │   ├── security/     # JWT、DataScope、403/401
│       │   └── module/
│       │       ├── system/       # 认证、用户、角色、菜单
│       │       └── masterdata/   # 企业、车间、产线、设备、能源、仪表
│       └── resources/
│           ├── application*.yml
│           └── db/{schema.sql, data.sql}
├── web/src/                  # api / router / stores / views / layouts / composables
└── miniprogram/              # pages / utils
```

## 4. 环境要求

| 组件 | 要求 | 本机实测 |
| --- | --- | --- |
| JDK | **17**（勿用 21/27 编译） | `D:\Dev\Java\jdk-17` |
| Maven | 3.8+ | 3.8.2 |
| MySQL | 8.0+ | 8.0.31，3306 |
| Node.js | 18+ | 24.18.0 |
| 微信开发者工具 | 最新版 | 需自行安装 |

## 5. 快速开始

### 5.1 数据库

```sql
CREATE DATABASE IF NOT EXISTS smarten_energy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

建表与初始化数据由应用启动时自动执行（幂等）：

- `backend/src/main/resources/db/schema.sql`：24 张表
- `backend/src/main/resources/db/data.sql`：角色、账号、能源类型、演示企业/车间/产线/设备/计量点

也可手工执行：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p smarten_energy < backend/src/main/resources/db/schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p smarten_energy < backend/src/main/resources/db/data.sql
```

默认连接 `root/123456`（仅 dev/test），可用环境变量覆盖：`SMARTEN_DB_URL`、`SMARTEN_DB_USERNAME`、`SMARTEN_DB_PASSWORD`。

### 5.2 后端

```powershell
cd backend
$env:JAVA_HOME='D:\Dev\Java\jdk-17'
$env:Path="D:\Dev\Java\jdk-17\bin;$env:Path"
mvn clean package
java -jar target\smarten-backend.jar --spring.profiles.active=dev
```

| 地址 | 说明 |
| --- | --- |
| http://localhost:8080/api/system/health | 健康检查 |
| http://localhost:8080/api/swagger-ui/index.html | 接口文档 |
| http://localhost:8080/api/actuator/health | Actuator（当前为 UP） |

### 5.3 Web

```powershell
cd web
npm.cmd install
npm.cmd run dev      # http://localhost:5173
npm.cmd run build
```

### 5.4 演示账号

| 账号 | 密码 | 角色 | 可见数据 |
| --- | --- | --- | --- |
| `admin` | `Admin@123` | 平台管理员 | 全部企业 |
| `engineer` | `Engineer@123` | 工程师 | 仅企业 1（华东精密制造） |

### 5.5 小程序

微信开发者工具导入 `miniprogram/` → 测试号 → 勾选「不校验合法域名」。

## 6. 接口概览

| 分组 | 路径前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/auth/**` | 登录、刷新、当前用户、菜单 |
| 用户 | `/api/system/users/**` | CRUD、状态、重置密码 |
| 角色 | `/api/system/roles/**` | CRUD、状态、全量列表 |
| 企业 | `/api/master/enterprises/**` | CRUD、状态 |
| 车间 | `/api/master/workshops/**` | CRUD、状态 |
| 产线 | `/api/master/lines/**` | CRUD、状态 |
| 设备 | `/api/master/devices/**` | CRUD、状态 |
| 能源类型 | `/api/master/energy-types/**` | CRUD、状态 |
| 计量点 | `/api/master/meters/**` | CRUD、状态 |
| 数据湖 | `/api/datalake/**` | 接入、导入、清洗、聚合、日志、质量、完整率、缺口、驾驶舱/画像 |

响应约定：业务异常返回 HTTP 200 + 业务码；401/403 返回对应 HTTP 状态；分页统一 `PageResult`。

## 7. 验证结果（P4，2026-11-04）

| 检查 | 结果 |
| --- | --- |
| 后端测试 | 68 个用例通过（0 失败 / 0 错误） |
| 前端视觉 | 设计令牌 + 统一图表主题 + 登录/工作台/驾驶舱/三维/列表页改造，
pm run build 通过 |
| 建表脚本 | 连续执行两次成功，24 张表 |
| 数据湖全链路 | 1440 点接入 → 1439 条明细 → 日聚合 719.5 kWh → 完整率 100% |
| 幂等 | 同批次重复提交不重复入库；重复时刻读数被唯一键拦截 |
| 模拟采集 | 5 表 × 1440 分钟 = 7200 条，41 秒完成 |
| 实时接口冒烟 | 19 项全部通过（健康、登录、错误密码、401、403、数据隔离、CRUD、分页、用户） |
| 登录 | `admin` 200；错误密码 1001；禁用账号 1002 |
| 接口权限 | 工程师访问用户列表 403；未登录 401 |
| 数据隔离 | 工程师企业列表 total=1；跨企业访问 403 |
| 主数据 CRUD | 新增→详情→修改→列表→删除 全链路 200 |
| Actuator | `UP` |
| 前端构建 | `npm run build` 通过（含 6 个数据湖页面） |
| 页面渲染 | 管理页、数据湖页、驾驶舱与 3 个画像页全部真实渲染，截图见 docs/screenshots/ |
| 驾驶舱 | 8 个 KPI、24 个小时点、7 条桑基边、完整率 100% |
| 画像 | 企业评级 A、单耗 0.4514 kWh/件；产线 506.6 kWh |
| 三维工厂 | 2 车间/3 产线/4 设备；悬浮与点击命中真实设备；风险色与离线设备正确 |
| 动态基线 | 同设备低/中/高负荷基线 4.89/9.49/15.58，待机 3.82；MAE 0.35、R² 0.988；版本可切换 |
| 异常诊断 | 独立测试集 TP=68/FP=0/TN=890/FN=2，Recall 97.14%、F1 0.9855；演示 27 条异常覆盖 8 类规则 |
| 整改闭环 | 创建→分配→小程序反馈→审核→效果验证全链路通过；驳回/越权/上传校验通过 |
| 节能核算 | 注塑机 A2 7 天核算 204.50 kWh、15.00%、134.40 元；复算版本归档、审核完成任务、CSV 导出 |
| 全链路联调 | 1 企业/3 车间/6 产线/30 设备/40 表计、30 天四类能源 28,796 条读数；修复 5 个缺陷，联调报告见 docs/full-chain-test-report.md |
| 前端联调 | dev server 200；代理登录 200；企业列表 2 条 |

## 8. 已知问题

| # | 问题 | 计划 |
| --- | --- | --- |
| 1 | 权限写入 JWT，改角色后需重新登录 | P4 前加 token 版本 |
| 2 | 能源类型/计量点无前端页面 | P5 补 |
| 3 | 小程序仍为骨架 | P7 |
| 4 | 前端首包 ~1.25 MB（ECharts 单独 chunk） | 后续按需引入 |
| 5 | 偏差率/异常/整改/节能量 KPI 与 3 张图依赖 P5/P6/P7/P8 | 对应模块接入后自动生效 |
| 6 | 单耗依赖模拟生成的 production_record | 接入真实 MES 后替换 |
| 7 | 三维高风险红色仍需 P6 异常数据 | P6 接入后自动生效 |
| 8 | 基线训练粒度为小时，日/班次由小时聚合 | 后续扩展 granularity |
| 9 | 同一时段可能命中多条规则，按规则分行展示 | 后续可增加跨规则二次合并 |
| 10 | 小程序未在微信开发者工具实机运行，仅完成语法/JSON 校验与接口测试 | 需在开发者工具导入验证 |

## 9. 文档索引

| 文档 | 内容 |
| --- | --- |
| [AGENTS.md](AGENTS.md) | 技术栈、分层、编码红线、协作流程 |
| [docs/architecture.md](docs/architecture.md) | 架构、模块、算法、接口与安全 |
| [docs/database-design.md](docs/database-design.md) | 24 张表设计 |
| [docs/implementation-plan.md](docs/implementation-plan.md) | P0–P9 计划 |
| [docs/development-progress.md](docs/development-progress.md) | 进度、接口、验证、待办 |

## 10. 验收指标（总目标）

| 指标 | 目标 | 当前 |
| --- | --- | --- |
| 核心数据表 | ≥ 9 张 | 24 张 ✅ |
| 入库完整率 | ≥ 99% | 100%（模拟全天 7200 点）✅ |
| 异常识别率 | ≥ 90% | P6 |
| 动态基线 | 按工况修正 | P5 |
| 节能核算 | 可追溯 | P8 |
