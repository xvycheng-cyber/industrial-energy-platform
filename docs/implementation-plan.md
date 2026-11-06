# 智能耗 · 项目实施计划

| 项 | 内容 |
| --- | --- |
| 版本 | v1.0（P0 设计阶段） |
| 总周期 | 约 11 周（单人全职估算，可并行压缩） |
| 交付形态 | 模块化单体后端 + Web 管理端 + 原生小程序 |
| 当前阶段 | **P8 已完成**，全流程闭环 |

---

## 1. 阶段总览

```mermaid
flowchart LR
    P0["P0 设计<br/>规范/架构/库设计"] --> P1["P1 工程骨架<br/>+ 数据库"]
    P1 --> P2["P2 基础管理<br/>认证/权限/主数据"]
    P2 --> P3["P3 数据湖<br/>采集/清洗/聚合"]
    P3 --> P4["P4 画像与驾驶舱<br/>+ 三维工厂"]
    P4 --> P5["P5 动态基线"]
    P5 --> P6["P6 异常识别<br/>+ 诊断引擎"]
    P6 --> P7["P7 整改闭环<br/>+ 小程序"]
    P7 --> P8["P8 节能核算<br/>+ 指标验证"]
    P8 --> P9["P9 联调/性能<br/>+ 验收"]
```

| 阶段 | 名称 | 周期 | 核心产出 | 闭环状态 |
| --- | --- | --- | --- | --- |
| P0 | 设计 | 已完成 | 规范、架构、库设计、计划 | — |
| P1 | 工程骨架与数据库 | 已完成（代码部分） | 三端可启动 + 公共组件 + 登录占位 | 可登录占位 |
| P2 | 基础管理 | 已完成 | 登录/JWT/RBAC、企业车间产线设备能源仪表 | 主数据可维护 |
| P3 | 数据湖 | 已完成 | ODS/DWD/DWS/ADS、完整率、模拟采集 | 数据可入库可查 |
| P4 | 画像与驾驶舱 | 已完成 | 驾驶舱、企业/产线/设备画像 | 数据可视化 |
| P5 | 动态基线 | 1 周 | 回归模型、回退、偏差计算 | 基线可算可看 |
| P6 | 异常与诊断 | 1.5 周 | 8 类异常、规则引擎、诊断报告 | 异常可识别可诊断 |
| P7 | 整改闭环 + 小程序 | 1.5 周 | 任务流转、小程序回填、图片上传 | 闭环打通 |
| P8 | 节能核算 | 1 周 | 反事实基线核算、审核、追溯 | 效果可验证 |
| P9 | 联调与验收 | 1 周 | 端到端用例、性能优化、验收报告 | 可交付 |

---

## 2. 阶段详细计划

### P0 设计阶段（已完成）

**目标**：冻结技术栈、架构、数据模型与开发规范，避免返工。

**交付物**
- `AGENTS.md`：协作规范、分层约定、红线清单
- `README.md`：项目说明、启动方式（占位）
- `docs/architecture.md`：系统架构、模块划分、核心算法
- `docs/database-design.md`：24 张表设计
- `docs/implementation-plan.md`：本文件
- `docs/development-progress.md`：进度台账

**验收**：文档之间表名、模块名、接口路径一致；无未决技术选型。

---

### P1 工程骨架与数据库（已完成，数据库待建）

> 实际完成情况见 `docs/development-progress.md`。工程骨架、公共组件、安全链、三端启动均已验证；
> DDL 与预置数据因缺少 MySQL 凭据未执行，顺延到 P2 开始前完成。

**目标**：三端工程可启动/可构建/可导入，公共基础设施就位，数据库建表完成，跑通一个真实接口（`/api/auth/login` + `/api/system/health`）。

**后端交付物**

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/smarten/
    │   ├── SmartenApplication.java
    │   ├── common/
    │   │   ├── api/{Result, PageResult, ResultCode}.java
    │   │   ├── exception/{BusinessException, GlobalExceptionHandler}.java
    │   │   ├── entity/BaseEntity.java
    │   │   ├── page/PageQuery.java
    │   │   └── util/{JwtUtil, SecurityUtils, IpUtil}.java
    │   ├── config/{MybatisPlusConfig, CorsConfig, JacksonConfig, OpenApiConfig, WebMvcConfig}.java
    │   ├── security/{SecurityConfig, JwtAuthenticationFilter, JwtUserDetails, RestAuthEntryPoint, RestAccessDeniedHandler}.java
    │   └── module/system/{controller,service,mapper,entity,dto,vo}/...
    └── resources/
        ├── application.yml
        ├── application-dev.yml / application-test.yml / application-prod.yml
        ├── logback-spring.xml
        ├── mapper/            # MyBatis XML（复杂查询用）
        └── db/{schema.sql, data.sql}
```

**关键内容**
1. Maven 依赖：web、validation、security、aop、actuator、mybatis-plus-spring-boot3-starter、mysql-connector-j、jjwt(api/impl/jackson)、lombok、springdoc-openapi-starter-webmvc-ui、test。
2. 公共类：`Result`、`PageResult`、`ResultCode`、`BusinessException`、`GlobalExceptionHandler`、`BaseEntity`、`PageQuery`。
3. 安全：无状态 JWT 过滤链、BCrypt、放行白名单、401/403 返回 JSON。
4. 配置：多环境 yml、Hikari、MyBatis-Plus 分页拦截器（`maxLimit=500`）、逻辑删除、自动填充、Jackson 时间格式、CORS、日志滚动、Springdoc。
5. 数据库：执行 `schema.sql` 建 24 张表，`data.sql` 预置角色与管理员。
6. 最小可用接口：`POST /api/auth/login`、`GET /api/auth/me`、`GET /api/system/health`。

**前端交付物**

```
web/
├── package.json / vite.config.ts / tsconfig.json / .env.development / .env.production
└── src/
    ├── main.ts / App.vue
    ├── api/{auth.ts, types.ts}
    ├── router/{index.ts, guard.ts}
    ├── stores/{user.ts, app.ts}
    ├── layouts/DefaultLayout.vue
    ├── utils/{request.ts, auth.ts}
    ├── types/index.ts
    └── views/{login/LoginView.vue, dashboard/DashboardView.vue, error/NotFound.vue}
```

**小程序交付物**

```
miniprogram/
├── app.js / app.json / app.wxss / sitemap.json / project.config.json
├── utils/{config.js, request.js, auth.js, upload.js, format.js}
└── pages/{login, workbench, mine}/...
```

**验收标准（必须逐条验证）**
- [ ] `mvn -q clean package` 通过（Java 17）
- [ ] 后端启动成功，`GET /api/system/health` 返回 `Result` 结构
- [ ] Swagger UI 可访问并列出接口
- [ ] `npm run build` 通过；`npm run dev` 登录页可打开并调用真实登录接口
- [ ] 未登录访问受保护接口返回 401 JSON（非 HTML）
- [ ] 小程序可在微信开发者工具导入，登录页可渲染并请求后端
- [ ] `schema.sql` 在 MySQL 8 执行无错，24 张表齐全
- [ ] `docs/development-progress.md` 已更新

**风险**：本机 JDK 版本非 17 时需切换（`JAVA_HOME` 指向 JDK 17）；MySQL 未就绪时先提供 `docker-compose.yml` 或建库脚本。

---

### P2 基础管理（已完成）

> 实际交付见 `docs/development-progress.md`：数据库 22 表 + 幂等种子、登录/JWT/RBAC、企业隔离、
> 用户/角色/6 组主数据接口、6 个 Web 管理页；26 个测试通过。

**目标**：跑通「登录 → 权限 → 主数据维护」链路，为后续模块提供数据基础。

**范围**
- 认证：登录、登出、刷新 token、当前用户、修改密码。
- 系统：用户 CRUD、角色 CRUD、用户角色分配、权限码、菜单接口、操作日志（可选）。
- 主数据：企业、车间、产线、设备、能源类型、计量仪表的 CRUD + 分页查询 + 启停 + 编码唯一校验。
- 前端：登录页、主框架布局、动态菜单、企业/车间/产线/设备/能源 5 个管理页（含四态与分页）。

**表**：`sys_user`、`sys_role`、`sys_user_role`、`energy_enterprise`、`energy_workshop`、`energy_line`、`energy_device`、`energy_type`、`energy_meter`。

**接口（示例）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录，返回 token |
| POST | `/api/auth/refresh` | 刷新 |
| GET | `/api/auth/me` | 当前用户与权限 |
| GET | `/api/system/users` | 用户分页 |
| POST | `/api/master/enterprises` | 新增企业 |
| PUT | `/api/master/enterprises/{id}` | 修改 |
| DELETE | `/api/master/enterprises/{id}` | 逻辑删除 |
| GET | `/api/master/devices` | 设备分页（按企业/产线/类型过滤） |

**验收**
- 不同角色登录看到不同菜单，越权接口返回 403。
- 企业下存在车间时禁止删除企业（返回业务错误码 2xxx）。
- 设备编码重复返回明确提示。
- 每个 Controller 至少 1 条 MockMvc 测试。

---

### P3 数据湖（已完成）

**目标**：能耗数据可采集入库、可清洗成明细、可聚合成多维数据，完整率可统计。

**范围**
- 采集接入：批量上报接口（网关/导入）、CSV/Excel 导入。
- 原始数据：分页查询、按表计/时间筛选、批次查询。
- 清洗：质量校验、倍率换算、缺失标记、幂等重跑。
- 聚合：小时/日/月 × 企业/车间/产线/设备，定时任务 + 手工重算。
- 入库日志与完整率统计。

**表**：`energy_raw_data`、`energy_detail`、`energy_aggregate`、`data_ingestion_log`、`production_record`。

**验收**
- 同一批次重复提交不产生重复数据（幂等）。
- 同一日重复聚合结果一致（可重跑）。
- 完整率接口返回 `Σvalid/Σexpected`，与手工核对一致。
- 1 万条明细聚合耗时 < 3s（开发机）。

---

### P4 画像与驾驶舱（已完成）

**目标**：把数据变成可读的能耗画像与驾驶舱。

**范围**
- 驾驶舱：总能耗、同比环比、能源结构、Top 设备/产线、异常与整改概览。
- 能耗画像：企业/产线/设备多维画像，单位产品能耗、峰谷分布、日历热力图。
- 三维工厂：Three.js 场景展示产线与设备状态，点击下钻。
- 报表导出（Excel）。

**接口**：`/api/profile/overview`、`/api/profile/enterprise/{id}`、`/api/profile/device/{id}`、`/api/factory/scene`。
**表**：只读，不新增表（聚合读 `energy_aggregate`、`energy_baseline_detail`，概览读 `energy_anomaly`、`rectification_task`）。

**验收**
- 所有图表数据来自接口，切换筛选条件数据同步变化。
- 三维场景加载失败有降级占位，不白屏。
- 无数据时显示空态而非报错。

---

### P5 动态基线（已完成）

**目标**：按工况动态生成基线并计算偏差。

**范围**
- 工况数据维护（production_record）与导入。
- 基线训练：OLS/岭回归 + 4 级回退 + 模型指标。
- 基线预测：生成 `energy_baseline_detail`，计算偏差率。
- 基线管理页：模型列表、详情（参数、样本量、R²/RMSE/MAPE）、手工重算、版本切换。
- 单元测试：回归系数正确性、回退逻辑、偏差计算边界（基线为 0）。

**表**：`production_record`、`energy_baseline`、`energy_baseline_detail`。

**验收**
- 不同班次/负荷率下基线值不同（体现工况修正）。
- 样本不足自动回退且 `confidence` 标记为低。
- 同一维度仅一个生效版本。

---

### P6 异常识别与诊断

**目标**：8 类异常可识别，规则可配置，报告可生成。

**范围**
- 8 个检测器 + 阈值配置 + 去重合并 + 严重级别。
- 诊断规则 CRUD、启停、条件校验、试算。
- 诊断引擎：条件树求值、候选原因排序、结论与建议渲染、预计节能量。
- 诊断报告：列表、详情、转整改任务。
- 评估：独立标注测试集、Precision/Recall/F1/混淆矩阵脚本与页面。

**表**：`energy_anomaly`、`diagnosis_rule`、`diagnosis_report`。

**验收**
- 测试集 Recall ≥ 90%，输出可复现的评估报告。
- 规则条件非法时保存被拒绝（返回明确错误）。
- 历史报告不受规则修改影响（快照生效）。

---

### P7 整改闭环与小程序

**目标**：诊断结论转化为任务，工程师在小程序完成整改并回填。

**Web 范围**：任务列表、派发、详情、验收/驳回。
**小程序范围**：登录、工作台、异常列表/详情、任务列表/详情、整改反馈（图片上传）、我的。
**表**：`rectification_task`、`rectification_feedback`。

**验收**
- 状态机非法流转被拒绝（如待验收直接跳完成）。
- 小程序提交后 Web 端实时可见。
- 图片上传限制类型/大小，失败可重试。
- 401 自动回登录页。

---

### P8 节能核算

**目标**：节能效果可核算、可审核、可追溯。

**范围**
- 核算单生成（锁定基线版本与观察期）。
- 反事实基线计算、剔除规则、置信度。
- 审核流程（通过/驳回/重算）。
- 节能效果分析页：趋势、排名、明细。

**表**：`energy_saving_result`（只读关联 `rectification_task`、`energy_baseline`）。

**验收**
- 核算使用反事实基线，而非整改前后总量差。
- 观察期数据不足时返回 `INSUFFICIENT_DATA`。
- 从核算单可追溯至任务、报告、规则版本、基线版本、凭证。

---

### P9 联调与验收

**范围**
- 端到端用例：采集 → 清洗 → 聚合 → 基线 → 异常 → 诊断 → 任务 → 回填 → 核算。
- 性能：列表 P95 < 300ms，驾驶舱首屏 < 2s。
- 安全：越权、SQL 注入、文件上传、JWT 失效场景回归。
- 验收材料：指标统计页面、评估报告、部署文档、演示数据脚本。

---

## 3. 质量门禁（每阶段必须通过）

| 检查项 | 命令/方式 | 通过标准 |
| --- | --- | --- |
| 后端编译 | `mvn -q -DskipTests package` | 成功，无 ERROR |
| 后端测试 | `mvn -q test` | 全绿 |
| 前端类型检查 | `npm run type-check`（vue-tsc） | 无错误 |
| 前端构建 | `npm run build` | 成功 |
| 接口自测 | Swagger / curl | 关键接口 200 且结构正确 |
| 进度更新 | 编辑 `docs/development-progress.md` | 记录模块、接口、表变更、待办 |

---

## 4. 里程碑

| 里程碑 | 阶段 | 判定 |
| --- | --- | --- |
| M1 可运行骨架 | P1 | 三端启动，登录打通 |
| M2 主数据完备 | P2 | 企业→设备→仪表可维护 |
| M3 数据可入库 | P3 | 完整率可统计 |
| M4 数据可视化 | P4 | 驾驶舱与画像上线 |
| M5 基线可用 | P5 | 偏差可算 |
| M6 诊断可用 | P6 | Recall ≥ 90% |
| M7 闭环打通 | P7 | 小程序回填闭环 |
| M8 效果可验证 | P8 | 核算与审核完成 |
| M9 验收 | P9 | 全部指标达标 |

---

## 5. 资源与环境

| 项 | 要求 |
| --- | --- |
| JDK | 17（`JAVA_HOME` 指向 JDK 17，勿用更高版本编译） |
| Maven | 3.8+ |
| MySQL | 8.0+，库 `smarten_energy` |
| Node.js | 18+（推荐 20 LTS） |
| 微信开发者工具 | 最新稳定版，测试号 AppID |
| 端口 | 后端 8080，Web 5173，MySQL 3306 |

建议在 P1 提供 `docker-compose.yml`（MySQL 8 + 可选 Adminer）降低环境成本。

---

## 6. 风险与应对

| 风险 | 阶段 | 应对 |
| --- | --- | --- |
| 本机无 JDK 17 | P1 | 安装 Temurin 17 并设置项目级 `JAVA_HOME`；CI 固定 17 |
| 无真实能耗数据 | P3–P8 | 提供数据生成脚本（可配置异常注入比例），标注为模拟数据 |
| 算法样本不足 | P5 | 4 级回退 + 置信度标记，前端明确提示 |
| 异常标注成本高 | P6 | 规则注入合成异常 + 小样本人工标注，双轨评估 |
| 多租户越权 | P2 起 | 统一 `enterprise_id` 校验切面 + 越权测试用例 |
| 工期压缩 | 全程 | 优先保证闭环（P3/P5/P6/P7/P8），非核心页面（三维、导出）可后置 |

---

## 7. 下一阶段启动清单（P1）

1. 确认 JDK 17、MySQL 8、Node 18+ 可用。
2. 创建 `backend/`、`web/`、`miniprogram/` 三个工程。
3. 落地 24 张表 DDL 与预置数据。
4. 实现公共组件与安全链路。
5. 实现登录 + 健康检查两个真实接口，前端登录页联调。
6. 执行编译/构建/启动验证，更新进度文档，提交 P1 报告。
