# AGENTS.md — 智能耗·工业企业能耗画像与节能诊断平台

> 本文件是本项目的**强制性协作规范**，对人和 AI 代理同样生效。
> 任何改动前先读本文件；与需求冲突时，先说明冲突点并给出建议，不要擅自变更技术栈或表结构。

---

## 1. 项目定位

| 项 | 内容 |
| --- | --- |
| 项目名称 | 智能耗 |
| 项目全称 | 工业企业能耗画像与节能诊断平台 |
| 业务目标 | 打通「采集 → 治理 → 画像 → 基线 → 异常 → 诊断 → 整改 → 核算」闭环 |
| 形态 | 模块化单体后端 + Web 管理端 + 原生微信小程序工程师端 |
| 架构风格 | 模块化单体（Modular Monolith），按业务域分模块，模块间只通过 Service 接口与 DTO 交互 |
| 当前阶段 | 最终验收完成（有条件通过：Docker/Nginx 与小程序真机未测） |

### 用户角色

- **平台管理员**：租户/企业、用户、角色、权限、字典维护。
- **能源管理员**：主数据、数据湖、基线、规则、诊断与整改派发。
- **工程师（小程序）**：接收整改任务、现场处置、上传凭证、回填效果。
- **企业管理者**：驾驶舱、画像、节能效果查看。

---

## 2. 技术栈（锁定，不得替换）

| 层 | 选型 | 版本基线 |
| --- | --- | --- |
| 后端语言 | Java | 17（LTS） |
| 后端框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.x（`mybatis-plus-spring-boot3-starter`） |
| 数据库 | MySQL | 8.0+ |
| 安全 | Spring Security 6 + JWT（jjwt 0.12.x） | 随 Boot 3.2 |
| 构建 | Maven | 3.8+ |
| 文档 | Springdoc OpenAPI | 2.x（`springdoc-openapi-starter-webmvc-ui`） |
| 工具 | Lombok、MapStruct（按需）、Hutool（按需） | — |
| Web | Vue 3 + TypeScript + Vite | Vue 3.4+ / Vite 5+ |
| UI | Element Plus | 2.x |
| 图表 | ECharts | 5.x |
| 三维 | Three.js | 0.16x |
| 状态/路由/请求 | Pinia、Vue Router 4、Axios | — |
| 小程序 | 原生微信小程序（WXML/WXSS/JS） | 基础库 3.x |

**禁止**：uni-app、Taro、React、Ant Design、Element UI（Vue2 版）、JPA/Hibernate 作为主 ORM、JDK 21+ 专属语法（如未开启预览的虚拟线程 API）。

---

## 3. 工程结构

```
industrial-energy-platform/
├── AGENTS.md                    # 本文件
├── README.md                    # 项目说明与启动方式
├── 项目计划.txt                  # 原始需求记录
├── docs/
│   ├── architecture.md          # 架构设计（权威）
│   ├── database-design.md       # 数据库设计（权威）
│   ├── implementation-plan.md   # 分阶段实施计划
│   └── development-progress.md  # 进度台账（每次改动必须更新）
├── backend/                     # Spring Boot 工程
│   ├── pom.xml
│   └── src/main/java/com/smarten/
│       ├── SmartenApplication.java
│       ├── common/              # api、exception、entity、page、util、web(TraceIdFilter)
│       ├── config/              # MyBatis-Plus、CORS、Jackson、OpenAPI、审计填充
│       ├── security/            # JWT、LoginUser、过滤器、401/403 处理器
│       └── module/
│           ├── system/          # 已落地：认证、用户、角色、菜单、权限码
│           ├── masterdata/      # 已落地：企业、车间、产线、设备、能源类型、计量点
│           ├── datalake/        # 已落地：ODS 接入、DWD 清洗、DWS 聚合、ADS 统计、模拟采集
│           ├── profile/         # 能耗画像、驾驶舱、三维工厂数据
│           ├── baseline/        # 动态基线、模型、偏差
│           ├── diagnosis/       # 异常识别、诊断规则、诊断报告
│           ├── rectification/   # 整改任务、反馈、状态流转
│           └── saving/          # 节能效果核算与追溯
├── web/                         # Vue3 管理端
│   ├── package.json
│   └── src/{api,components,composables,constants,layouts,router,stores,styles,types,utils,views}
│       └── 已落地：登录、布局、工作台、主数据、数据湖、驾驶舱、画像、三维工厂、动态基线、异常诊断、整改闭环、小程序 8 页、节能核算
└── miniprogram/                 # 原生小程序
    ├── app.js / app.json / app.wxss
    ├── pages/{index,mine}       # 已落地；后续补 login、anomaly、task
    └── utils/{config,request,auth,upload,format}
```

### 模块内部分层（后端统一）

```
module/<domain>/
├── controller/     # 只做参数绑定、鉴权注解、调用 Service、返回 Result
├── service/        # 业务编排、事务边界；接口 + impl
├── mapper/         # MyBatis-Plus Mapper，仅数据访问
├── entity/         # 表映射实体（继承 BaseEntity）
├── dto/            # 入参（Create/Update/Query 后缀）
├── vo/             # 出参（VO 后缀）
├── converter/      # Entity <-> DTO/VO 转换
└── enums/          # 模块内枚举
```

### 模块依赖规则

1. 只允许 `datalake → masterdata`、`profile/baseline/diagnosis/rectification/saving → datalake + masterdata` 这类**单向**依赖。
2. 禁止循环依赖；跨模块调用走 Service 接口，不直接操作对方 Mapper。
3. `common` 不依赖任何业务模块；业务模块可依赖 `common`。
4. 每个模块有独立的 `module/<domain>/package-info.java` 说明职责边界。

---

## 4. 后端规范

### 4.1 通用约束

- Controller 内**不允许**业务判断、循环聚合、SQL；单个方法不超过 30 行。
- 入参一律 DTO + `@Validated`/`@Valid` + Jakarta Validation 注解；出参一律 VO。
- 统一返回 `Result<T>`；分页统一 `PageResult<T>`，禁止直接返回 `IPage`。
- 自定义异常继承 `BusinessException`，由 `GlobalExceptionHandler` 统一转换；禁止在 Controller 里 `try/catch` 后返回 500 字符串。
- 写操作必须 `@Transactional(rollbackFor = Exception.class)`；跨表状态流转（如整改提交 + 反馈 + 任务状态）必须在同一事务。
- 逻辑删除字段 `deleted`（0/1），查询默认过滤；唯一索引需包含 `deleted` 或使用可空唯一列方案。
- 时间字段统一 `create_time` / `update_time` / `create_by` / `update_by`，由 MyBatis-Plus `MetaObjectHandler` 自动填充。
- 分页插件在 `MybatisPlusConfig` 中统一注册，`maxLimit` 不超过 500。
- 禁止字符串拼接 SQL；动态条件用 LambdaQueryWrapper / XML `<if>` + `#{}`。
- 接口路径：`/api/<模块>/<资源复数>`，动作型接口用动宾：`/api/rectification/tasks/{id}/submit`。
- 所有接口必须有 Springdoc 注解（`@Tag`、`@Operation`）与参数说明。

### 4.2 关键公共类（已落地）

| 类 | 说明 |
| --- | --- |
| `common/api/Result.java` | `code`、`message`、`data`、`timestamp`、`traceId`；静态工厂 `ok()/ok(data)/fail(code,msg)` |
| `common/api/PageResult.java` | `records`、`total`、`pageNum`、`pageSize`、`pages` |
| `common/api/ResultCode.java` | 错误码枚举：200 成功、400 参数、401 未登录、403 无权限、404 不存在、409 冲突、500 系统错误、1xxx 业务域错误 |
| `common/exception/BusinessException.java` | 继承 `RuntimeException`，携带 `ResultCode` 与参数 |
| `common/exception/GlobalExceptionHandler.java` | 处理 `BusinessException`、`MethodArgumentNotValidException`、`BindException`、`ConstraintViolationException`、`AccessDeniedException`、`HttpMessageNotReadableException`、兜底 `Exception` |
| `common/entity/BaseEntity.java` | id、createTime、updateTime、createBy、updateBy、deleted |
| `common/page/PageQuery.java` | pageNum、pageSize、orderBy、asc，含上限校验（≤500） |
| `common/web/TraceIdFilter.java` | 生成 traceId 写入 MDC 与响应头 `X-Trace-Id` |
| `security/LoginUser.java` | SecurityContext 中的当前用户（userId / username / roles / enterpriseId） |
| `security/DataScopeHelper.java` | 企业数据隔离：平台管理员不受限，其余强制 `enterprise_id = 本企业` |
| `common/security/PermissionCodes.java` | 权限码登记表；`*` 在登录时展开为全部权限码 |

**响应契约（已实现，后续模块必须沿用）**

| 场景 | HTTP 状态 | 响应体 |
| --- | --- | --- |
| 业务成功 | 200 | `code=200` |
| 业务异常 / 参数校验失败 | **200** | 业务码（1xxx–7xxx），前端统一提示 |
| 未登录 / token 失效 | 401 | `code=401` 或 `1004`（过期） |
| 无权限 | 403 | `code=403` |
| 未捕获异常 | 500 | `code=500` |
| 接口不存在 | 404 | `code=404` |

### 4.3 安全与权限

- 无状态 Session；`Authorization: Bearer <token>`。
- JWT：HS256（密钥 ≥32 字节时 jjwt 自动选更强算法），密钥从配置读取（禁止硬编码），有效期 access 2h、refresh 7d；载荷包含 userId、username、roles、权限码 `perms`、`enterpriseId`，不放敏感信息。
- 密码 BCrypt；登录失败计数与账号锁定尚未实现（P4 前补充）。
- 接口级：`@PreAuthorize("hasAuthority('master:device:list')")`；权限码格式 `<模块>:<资源>:<动作>`，新增权限必须同步 `PermissionCodes` 与前端 `constants/permissions.ts`。
- 数据权限：企业维度隔离（`enterprise_id`），统一用 `DataScopeHelper` 在 Service 层校验，不依赖前端。
- 放行清单（路径不含 context-path）：`/system/health`、`/auth/login`、`/auth/refresh`、`/actuator/health`、`/v3/api-docs/**`、`/swagger-ui/**`；生产环境关闭 Swagger。

### 4.4 数据湖分层规范（P3 已落地）

- **ODS（`energy_raw_data`）只追加**：不修改 `raw_value`、不物理删除；重复由唯一键拦截并计入重复，异常由 `quality` / `clean_status` 标记，原始行永远保留。
- **DWD（`energy_detail`）存周期能耗**：累计读数表用「相邻读数差值 × 倍率」，周期增量表直接使用读数；首个读数为基线不产生明细；表计清零按新起点处理并标为可疑。
- **DWS（`energy_aggregate`）按完整时间桶重算并 upsert**，禁止「增量累加」式更新，保证重复执行结果一致。
- **ADS 只读聚合表**，不直接查明细，避免热点。
- 补录必须 `source=BACKFILL`、`quality=2`，明细 `quality_flag=3`、`estimated=1`，不得覆盖已有原始数据。
- 新增数据湖权限码时，同步维护 `PermissionCodes` 与前端 `constants/permissions.ts`。

### 4.5 画像与驾驶舱规范（P4 已落地）

- 画像与驾驶舱只读 `energy_aggregate` / `production_record`，不直接扫描 DWD 明细。
- 依赖后续模块的指标（基线偏差、异常设备、整改任务、节能量）必须返回 `available = false` 与提示语，禁止用估算值或静态数据冒充。
- 综合能耗统一用 `energy_type.coal_factor` 换算（kgce → tce），不得在前端硬编码系数。
- 单耗 = 能耗 / 产量；无产量数据返回 null，前端显示「待生产数据」。
- 层级下钻统一走 `/api/profile/hierarchy`，前端不得自行拼装组织树。
- 图表统一使用 `components/BaseChart.vue`，页面只传 option；无数据必须空态。

### 4.6 三维可视化规范（P4 已落地）

- Three.js 场景必须拆分为独立模块：控制器、环境、建筑、设备、交互、配色/类型，页面只负责挂载与数据请求。
- 模型只允许基础几何体拼装，不得依赖外部模型文件；设备类型与几何的映射集中在 `device-factory.ts`。
- 场景坐标由后端 `/api/factory/scene` 下发，前端不得自行布局组织与设备的相对位置。
- 颜色只来自后端 `risk` 字段（NORMAL/LOW/MEDIUM/HIGH/OFFLINE），不得在前端按能耗自行判断风险。
- 设备详情中的基线、偏差、异常、诊断在 P5/P6 前必须显示「待接入」，不得填占位数值。
- 组件销毁必须调用 `controller.dispose()`：取消动画帧、解绑 ResizeObserver/窗口事件、释放 geometry/material/texture、销毁 OrbitControls 与 renderer。
- 三维页必须有加载态与错误重试；WebGL 不可用时不得白屏，至少保留 HUD 与错误提示。
### 4.7 动态基线算法规范（P5 已落地）

- 基线必须是「工况 → 能耗」的模型，禁止用固定阈值或全量历史均值冒充；算法入口 `BaselineTrainer`，与数据库解耦，便于单测。
- 训练样本必须经过：字段有效性 → DWD 质量门禁（`quality_flag != 1` 或 `estimated = 1` 剔除）→ 截尾最小二乘 → MAD 残差剔除。
- 停机/检修样本单独学习待机基线，不得混入运行回归；预测时按状态分流。
- 回退链固定为：回归 → 工况分组均值 → 历史正常中位数 → 待机基线；每次回退必须写入 `fallback_log` 与明细 `fallback_reason`。
- 训练样本不足（`n < max(20, 特征数+1)`）不得强行回归，必须走回退并标注置信度。
- 模型版本只增不改：重新训练生成新版本，旧版本保留为已归档，启用新版本时自动归档同对象旧版本。
- 预测明细必须保存 `condition_json`、`model_version`、`baseline_type`、实际值与偏差率；不得只存基线数值。
- 新增基线能力时同步 `PermissionCodes`、`constants/permissions.ts` 与 `data.sql` 的权限种子。
### 4.8 异常诊断规范（P6 已落地）

- 规则只能以结构化 JSON 配置，字段必须命中白名单；禁止在规则中执行 SQL、脚本或任意表达式。
- 规则参数变更必须版本 +1，并保留旧版本可追溯；启停状态由 `enabled` 控制。
- 异常评分固定为偏差 45% + 持续 25% + 历史 15% + 同类 15%；强制规则（R004）不得因综合分低而降级。
- 同一设备同一规则 6 小时内的新信号必须合并到已有未关闭异常，不得重复建单。
- 诊断报告的“系统推断原因”与“人工确认原因”必须分开存储、分开展示，禁止把推断写成已确认结论。
- 定时诊断与手动诊断必须共用同一把锁，禁止并发扫描。
- 异常识别指标必须在独立标注测试集上输出 TP/FP/TN/FN、Precision、Recall、F1 与混淆矩阵，Recall 目标 ≥ 90%；不得修改标签或使用训练集评测。
### 4.9 整改闭环规范（P7 已落地）

- 整改状态只能由后端 `RectificationStatus` 流转，前端不得直接改 `status`；非法流转返回 6001。
- 审核通过只能进入「效果验证中」，必须由效果验证接口才能到「已完成」，禁止直接完成。
- 驳回必须填写原因；撤销、转派必须留痕（反馈记录）。
- 小程序接口独立在 `/api/mobile/**`，登录校验工程师身份；任务查询、开始、反馈必须校验 `assignee_id` 归属。
- 图片统一走 `/api/mobile/upload` 或 `/api/rectification/upload`，仅 jpg/png/webp、≤5MB，落盘并记录 `sys_file`。
- 小程序必须使用原生 WXML/WXSS/JS，禁止 uni-app / Taro；页面必须有加载、空态、错误提示三态。
### 4.10 节能核算规范（P8 已落地）

- 节能量必须按观察期逐小时「修正后动态基线 − 实际」计算，禁止用整改前后总能耗相减。
- 基线版本、观察起点、单价、完整率、可比性、产量变化必须随结果落库，保证可追溯、可复算。
- 复算不得覆盖历史结果：新版本生效，旧版本标记归档，保留版本号与审核记录。
- 数据不足（完整率 <80% 或 <7 天）、停机（≥50% 小时）、产量变化超 ±30% 时不得判定“节能达成”。
- 节能量允许为负；金额按分时电价逐小时累加，不得用“总节能量 × 单一电价”。
- 核算审核通过才能完成任务；驳回必须退回整改，禁止直接改任务状态。
- 导出文件必须含 BOM 与表头，字段与列表一致。
---

## 5. Web 前端规范

- 一律 `<script setup lang="ts">` + Composition API，禁止 Options API 混用。
- API 集中在 `src/api/<模块>.ts`，函数返回 `Promise<T>`（已解包 data），类型来自 `src/types/`；页面不得直接 `axios.get`。
- Axios 封装 `src/utils/request.ts`：统一 baseURL、超时、token 注入、401 跳登录、错误码提示、下载流处理。
- 类型定义集中 `src/types/`，接口返回必须有 TS 类型，禁止 `any` 泛滥（确需时写 `unknown` + 收窄）。
- 业务数据一律来自接口；**禁止**在页面写死列表、图表数字、诊断结论。
- 每个列表页必须具备：加载态（`v-loading`）、空状态（`el-empty`）、错误态（重试按钮）、分页（`el-pagination`）。
- 图表统一封装 `BaseChart.vue`（ECharts 实例、resize、主题、销毁），页面只传 option。
- 三维工厂用 Three.js，封装 `ThreeScene` 组件，资源加载失败需降级为占位图，不得白屏。
- 路由守卫：未登录访问非公开路由跳 `/login?redirect=...`；无权限跳 `/403`；菜单由后端 `/auth/menus` 下发，前端不硬编码。
- Pinia：`user`（token、用户信息、权限码）、`app`（菜单、主题、面包屑）、`dict`（字典缓存）。
- 环境变量：`VITE_API_BASE_URL`，开发走 Vite proxy，生产走 Nginx 反代。

---

## 6. 小程序规范

- 原生小程序，禁用 uni-app/Taro；统一使用 `utils/request.js`，禁止页面内直接 `wx.request`。
- token 存 `storage`，`utils/auth.js` 统一读写与过期处理；401 清空并跳登录页。
- 任务状态只能由后端接口驱动；小程序不得本地改状态后直接展示为已完成。
- 图片上传走 `utils/upload.js`（`wx.uploadFile` → 后端统一上传接口），限制大小与类型，失败可重试。
- 页面必须具备加载中、空数据、加载失败三态；下拉刷新与上拉加载更多。
- 列表分页参数统一 `pageNum/pageSize`，与后端一致。

---

## 7. 数据规范

- 能耗量：`DECIMAL(18,4)`，单位 kWh；金额：`DECIMAL(18,2)` 元；比率/偏差：`DECIMAL(9,4)`（百分数存数值，如 12.3456 表示 12.3456%）。
- 时间：`DATETIME`，服务端统一 `Asia/Shanghai`，JSON 序列化 `yyyy-MM-dd HH:mm:ss`。
- 所有表：`id BIGINT` 主键、`create_time`、`update_time`、`deleted TINYINT`；业务表带 `enterprise_id` 做数据隔离。
- 原始采集数据只追加不修改（`energy_raw_data`），清洗与聚合结果可重算但必须幂等（唯一键 + upsert）。
- 诊断、整改、核算全过程留痕：规则版本、模型版本、操作人、时间、前后值。
- 禁止物理删除业务数据；确需清理走归档任务。

---

## 8. 测试与质量

| 层级 | 要求 |
| --- | --- |
| 单元测试 | 算法（基线回归、偏差、异常判定、节能核算）覆盖率 ≥ 80%，用 JUnit 5 + AssertJ |
| 集成测试 | 每个 Controller 至少 1 条 MockMvc 用例；关键流程（采集→诊断→整改→核算）端到端用例 |
| 前端 | `npm run build` 必须通过；关键组件可用 Vitest（按需） |
| 静态检查 | 后端编译零警告优先；前端 `vue-tsc --noEmit` 通过 |
| 每次改动 | 必须跑 `mvn -q test`（或至少 `mvn -q -DskipTests package`）与 `npm run build`，结果写入进度文档 |

**算法验收**：异常识别需在独立标注测试集上输出 Precision / Recall / F1 与混淆矩阵，Recall ≥ 90%，脚本可复现。

---

## 9. 协作流程（每次任务必须遵守）

1. **先读**：`AGENTS.md`、`docs/architecture.md`、`docs/database-design.md`、`docs/development-progress.md`，再读要改的代码。
2. **先分析**：说明现状、影响范围、**将修改/新增的文件清单**；涉及表结构或接口变更必须先说明。
3. **分阶段**：严格按 `docs/implementation-plan.md` 的阶段推进，一次只做一个阶段内的模块；不一次性生成整个系统。
4. **不造假**：不写无法运行的伪代码；不用静态数据冒充接口联通；未实现的功能不得在文档/汇报中标记完成。
5. **可运行**：每阶段结束保证后端可启动、前端可构建、小程序可导入；失败必须如实报告。
6. **收尾**：更新 `docs/development-progress.md`（完成模块、接口、表变更、待办、遗留问题），汇报「改了什么 / 怎么启动 / 测试结果 / 剩余问题」。

### 完成定义（DoD）

- [ ] 代码可编译、可启动，无 TODO 占位实现
- [ ] 接口有 OpenAPI 文档与参数校验
- [ ] 前端页面接真实接口，具备四态与分页
- [ ] 有对应测试且通过
- [ ] `docs/development-progress.md` 已更新
- [ ] 未破坏既有功能（回归通过）

---

## 10. 常用命令

> 本机默认 `JAVA_HOME` 是 JDK 27，**每次运行 Maven 前必须切到 JDK 17**（`D:\Dev\Java\jdk-17`）；
> PowerShell 若禁用 `npm.ps1`，一律使用 `npm.cmd`。

```powershell
# 后端（必须先设置 JDK 17）
$env:JAVA_HOME='D:\Dev\Java\jdk-17'; $env:Path="D:\Dev\Java\jdk-17\bin;$env:Path"
cd backend
mvn clean package                                   # 编译 + 测试
java -jar target\smarten-backend.jar --spring.profiles.active=dev
mvn test

# Web
cd web
npm.cmd install
npm.cmd run dev        # http://localhost:5173，/api 代理到 8080
npm.cmd run build      # vue-tsc --noEmit && vite build
npm.cmd run type-check

# 小程序
# 微信开发者工具 → 导入项目 → 选择 miniprogram/ → AppID 使用测试号
# 详情 → 本地设置 → 勾选「不校验合法域名」
```

**当前环境事实（P8 实测）**：JDK 17.0.21（`D:\Dev\Java\jdk-17`）、Maven 3.8.2、Node 24.18.0、MySQL 8.0.31（3306，`root/123456`，已建 `smarten_energy` 与 `smarten_energy_test`）。
dev/test 启动时自动执行 `db/schema.sql` + `db/data.sql`（幂等）。`/api/actuator/health` 当前为 `UP`。
集成测试依赖本机 MySQL：`mvn test` 会连接 `smarten_energy_test`。

---

## 11. 红线清单

1. 不更换技术栈、不引入第二套 ORM/UI 框架。
2. 不在 Controller 写业务逻辑，不拼接 SQL，不物理删除数据。
3. 不用静态数据冒充真实接口；不跳过权限校验。
4. 能耗/金额字段不使用 `FLOAT`/`DOUBLE`。
5. 不绕过后端直接改任务状态。
6. 不在未验证的情况下声称「已通过测试」。
