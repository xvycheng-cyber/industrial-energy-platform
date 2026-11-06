# 开发进度台账

> 每次改动必须更新本文件。格式：日期 / 阶段 / 变更内容 / 影响范围 / 验证结果 / 待办。

---

## 当前状态

| 项 | 值 |
| --- | --- |
| 当前阶段 | **P4 能耗画像与驾驶舱已完成**（驾驶舱 + 企业/产线/设备画像） |
| 数据库 | 24 张表；`energy_type` 新增折标煤系数 |
| 后端 | 画像 5 个接口 + 生产工况模拟，31 个测试通过 |
| Web | 能源驾驶舱（深蓝）、企业/产线/设备画像，全部接真实接口 |
| 后端 | 接入/导入/清洗/聚合/完整率/缺口补录/模拟采集完成，28 个测试通过 |
| Web | 登录、布局、6 个基础管理页 + 6 个数据湖页面，全部接真实接口 |
| 小程序 | 保持 P1 状态（自检页 + token 封装），P7 接入业务 |

---

## 已完成

### 2026-11-05 · 最终验收

- 后端 `mvn -B clean test` 与复跑 `mvn -B test` 均 68/68 通过；`mvn -DskipTests package` 成功。
- 前端 `npm ci` + `npm run build` 通过；小程序 14 JS / 11 JSON / 8 页面语法检查通过（真机未测）。
- 数据库：25 表、492 字段、75 索引、46 唯一索引；空库执行 schema+data 两遍成功；修复 `sys_file` 缺失与 P8 核算 10 字段缺失。
- 完整率 28,796 / 28,800 = 99.9861%；异常识别 TP68 FP0 TN890 FN2，P=1.0 / R=0.9714 / F1=0.9855。
- 基线实库 R² 0.8923–0.9165、MAE 0.17–1.20；核算实库 +204.50 kWh / 15.00% / 134.40 元。
- 权限实测 401/403/企业隔离；实库任务 12 完成「创建→小程序反馈→审核→效果验证」闭环。
- 新增部署文件：`docker-compose.yml`、`deploy/*`、`.env.example`；Docker/Nginx 实际启动未测试。
- 交付：`docs/acceptance/`（总览/功能/数据库/测试/指标/问题）、`docs/api/`、`docs/deployment.md`。

### 2026-11-05 · Web 端 UI 视觉统一优化

- 新增全局设计令牌与 Element Plus 覆盖：主色 #1d4ed8、浅灰背景、白色卡片、细边框、弱阴影；统一卡片/表格/筛选器/分页/弹窗/抽屉/表单/空态/加载。
- 布局：侧栏 232px 深蓝导航（Logo 副标题、当前项标记、数据服务状态）、58px 顶栏（首页图标 + 面包屑）、内容区统一间距；1100px 以下自动折叠。
- 图表：新增 `utils/chartTheme.ts`，BaseChart 统一字体、8 色配色、坐标轴、提示框、图例、深色模式与空态。
- 三维工厂：风险色校准、设备基座与地面风险环、产线标签、收紧取景、移除抖动动画，突出异常设备。
- 页面：登录页重设计；工作台改为常用功能入口；驾驶舱去渐变并统一深蓝面板；异常/基线/设备/企业/车间/产线/用户/角色增加页面标题；任务与异常表格列宽修复。
- 文档：`docs/ui-redesign.md`；截图 `docs/screenshots/ui/`。
- 验证：`npm run build` 通过；未改接口与业务逻辑，后端 68 个测试不受影响。

### 2026-11-05 · 全链路联调与模拟数据完善

- 新增 `module/demo`：幂等生成 1 企业 / 3 车间 / 6 产线 / 30 设备 / 40 计量点，30 天电水气汽四类能源数据，并联动基线、诊断、整改、核算。
- 接口：`POST /api/demo/full-chain?days=30`、`POST /api/demo/reset`、`GET /api/demo/status`（`demo:init`）。
- 所有采集与生产数据标记 `SIMULATOR`，企业备注标注 DEMO；演示工程师 `demo_engineer` 与 Web/小程序同源。
- 修复：累计读数写入错位 1 小时（导致基线/实际错位与大量误报）、定时模拟器与回填并发写入假尖峰、跨企业负责人导致建单失败、30 天 5 分钟聚合超时、任务号碰撞。
- 新增 `FullChainFlowTest`：组织→采集→DWD/DWS→基线→诊断→整改→核算，连续执行两次验证幂等。
- 报告：`docs/full-chain-test-report.md`；全量测试 **68 个用例通过**。

### 2026-11-05 · P8 节能效果核算与验证

**数据库**

- `db/migration/V7__saving.sql`：核算表增加结论、完整率、可比性、整改前后产量、产量变化率、单价、实际天数、历史版本标记、逐小时明细。

**算法（纯 Java，可单测）**

- `SavingCalculator`：逐小时「修正基线 − 实际」，汇总节能量、节能率、分时电价金额。
- 边界：数据不足（完整率 <80% 或 <7 天）、停机（≥50% 小时）、工况不可比（产量 ±30%）、能耗恶化（负节能）、无明显变化（<5%）。
- 分时电价：峰段 1.2、平段 1.0、谷段 0.5（基准 0.65 元/kWh，可传参）。

**后端**

- `SavingService`：核算（锁定观察起点与基线版本）、复算版本归档、列表/详情、审核、汇总排名/趋势、CSV 导出。
- 审核通过自动完成任务；驳回退回整改中。
- 接口 `/api/saving/**`，权限 `saving:view/calc/audit/export`，菜单「节能核算」。

**前端**

- `/saving/results`：累计节能/金额/记录/平均节能率、设备排名、节能趋势、筛选与导出。
- `/saving/results/:id`：逐小时「修正基线 vs 实际」对比曲线、核算口径、结论、审核、历史版本。

**验证结果**

| 检查 | 结果 |
| --- | --- |
| 单元测试 | 8 个场景（节能/无变化/恶化/工况变化/停机/不足/电价/可重复） |
| 集成测试 | 核算→复算→审核→任务完成；数据不足→驳回→退回整改；CSV 导出 |
| 全量测试 | 67 个用例通过 |
| 演示 | 注塑机 A2：204.50 kWh、15.00%、134.40 元、节能达成；装配机械手：2.27 kWh、0.85%、-0.41 元、无明显变化 |
| 页面 | `docs/screenshots/saving-results.png`、`saving-detail.png` |

**文档**

- 新增 `docs/saving-accounting.md`：公式、观察期、结论、接口、测试与边界。

### 2026-11-05 · P7 整改任务与微信小程序

**数据库**

- `db/migration/V6__rectification.sql`：任务表增加预期节能量、整改措施、驳回原因、审核人/时间、进度、转派次数；
  反馈表增加整改前/后情况；新增 `sys_file` 统一文件表。

**后端**

- 状态机 `RectificationStatus`：8 状态、白名单流转，非法流转返回业务码 6001；审核通过只能进入「效果验证中」。
- `RectificationService`：创建（报告/设备）、分配、转派、审核（驳回必填原因）、效果验证、撤销、超期判定、统计。
- Web 接口 `/api/rectification/**`；小程序接口 `/api/mobile/**`（独立 Controller），登录校验工程师身份，任务查询/操作校验归属。
- 图片上传：类型 jpg/png/webp、≤5MB，落盘 + `sys_file` + `/uploads/**` 静态访问。
- 权限：`rectify:view/create/assign/audit/verify/mobile`，菜单「整改管理」。

**前端**

- Web `/rectification/tasks`：统计卡片、状态/负责人/超期筛选、创建、分配/转派、审核（通过/驳回）、效果验证、撤销、详情抽屉（反馈时间线 + 现场图片）。
- 原生微信小程序 8 页：登录、工作台、异常列表、异常详情、任务列表、任务详情、整改反馈、个人中心；无 uni-app/Taro。

**验证结果**

| 检查 | 结果 |
| --- | --- |
| 后端测试 | 56 个用例通过（新增 RectificationFlowTest 6 个） |
| 闭环 | 创建→分配→小程序开始→完工反馈→审核→效果验证→完成，审核后状态为 7 而非 4 |
| 校验 | 驳回无原因返回 400；非法流转 6001；越权访问 403；只读角色登录小程序 403 |
| 上传 | png 成功、txt 拒绝；大小/类型限制生效 |
| 小程序 | 14 个 JS 语法通过、11 个 JSON 配置有效 |
| 页面 | `docs/screenshots/rectification-tasks.png`、`rectification-detail.png` |

**文档**

- 新增 `docs/rectification.md`：状态机、Web/小程序接口、上传规则、页面与测试。

### 2026-11-04 · P6 异常识别与节能诊断

**规则引擎**

- 8 类规则（R001–R008）以结构化 JSON 配置，支持阈值、窗口、持续时间、适用设备类型、优先级、启停与版本。
- `RuleJson` 白名单校验：未知字段直接拒绝，不拼接 SQL、不执行脚本；保存后版本号 +1。
- 新增 `db/migration/V5__diagnosis.sql`：异常增加评分/持续/合并/报告字段；报告增加描述/人工确认/风险等级。
- `diagnosis_rule` 种子内置 8 条规则（`data.sql` 幂等插入）。

**检测与评分**

- `DetectionEngine` 纯内存检测，覆盖突增、持续高耗、空载高耗、空转、单耗、夜间、波动、同类异常；R007 使用 MAD 稳健变异系数。
- `AnomalyScoring`：偏差 45% + 持续 25% + 历史 15% + 同类 15%，0–100 分；强制规则（R004）最低判高风险。
- 合并：同设备同规则 6 小时内合并，延长区间、累加次数、取最高分，避免重复事件。

**诊断报告**

- 根据规则、设备类型与工况生成：描述、系统推断原因、结论、整改建议、预计节能量、风险等级。
- 人工确认原因独立字段，与系统推断严格区分。

**接口**

`/api/diagnosis/scan`、`/anomalies`、`/anomalies/{id}`、`/anomalies/{id}/handle`、`/anomalies/{id}/report`、`/reports/{id}`、`/reports/{id}/confirm`、`/overview`、`/rules`、`/rules/{id}`、`/rules/{id}/toggle`。

**前端**

- `/diagnosis/overview`：异常总数/待确认/高风险 KPI、趋势、风险分布、规则命中、高发设备。
- `/diagnosis/anomalies`：规则/风险/状态/日期筛选、评分与偏差、确认/误报、跳详情与报告。
- `/diagnosis/anomalies/:id`：异常指标、规则证据、处理动作、报告摘要。
- `/diagnosis/reports/:id`：描述、结论、系统原因与人工确认双栏、建议、规则快照。
- `/diagnosis/rules`：规则列表、JSON 编辑、启停、版本展示。

**验证结果**

| 检查 | 结果 |
| --- | --- |
| 独立测试集 | TP=68、FP=0、TN=890、FN=2；Precision 1.0000、Recall 0.9714、F1 0.9855 |
| 规则单测 | 8 类覆盖、强制规则低分仍高风险、评分随偏差/持续时间上升 |
| 集成测试 | 扫描→异常→报告→人工确认→处理→重复扫描合并；规则拒绝 `sql` 字段 |
| 全量测试 | 50 个用例通过 |
| 演示扫描 | 27 条异常，覆盖全部 8 类规则；严重 6、高 4、中 3、低 14 |
| 页面 | 总览/列表/详情/报告/规则截图（`docs/screenshots/diagnosis-*.png`） |

**文档**

- 新增 `docs/diagnosis-engine.md`：规则表、评分公式、合并机制、报告结构、指标与接口。

### 2026-11-04 · P5 动态能耗基线

**算法（`module/baseline/algorithm`，纯 Java）**

- 特征：产量、运行时长、负荷率、环境温度、产品类型、班次、运行状态（含哑变量）。
- 模型：多变量岭回归 `β=(XᵀX+λI)⁻¹Xᵀy`，高斯消元求解，λ=1e-6。
- 清洗：字段有效性 → DWD 质量门禁（`quality_flag != 1` 或 `estimated = 1` 剔除）→ 截尾最小二乘（3 轮 × 15%）→ MAD 3σ 残差剔除。
- 有效样本：`n ≥ max(20, 特征数+1)`；不足则回退。
- 回退链：回归 → 工况分组均值（负荷档+产品，≥5 样本）→ 历史正常中位数 → 停机独立待机基线；每次回退写入模型与明细。
- 指标：MAE / RMSE / MAPE / R² / 置信度。

**数据库**

- `db/migration/V4__baseline.sql`：`energy_baseline` 增加 `mae`、`fallback_count`、`sample_rejected`、`outlier_count`；
  `energy_baseline_detail` 增加 `baseline_type`、`model_version`、`condition_json`、`fallback_reason`。
- 复用 `production_record.ambient_temp` 与既有基线表，无新增业务表。

**后端接口（`/api/baseline`）**

| 能力 | 接口 |
| --- | --- |
| 训练（可选立即启用） | `POST /baseline/models/train` |
| 生成预测 | `POST /baseline/predict` |
| 模型列表/详情 | `GET /baseline/models`、`GET /baseline/models/{id}` |
| 误差分析 | `GET /baseline/models/{id}/error` |
| 基线明细 | `GET /baseline/records` |
| 启用/停用/切换版本 | `POST /baseline/models/{id}/enable`、`/disable` |

- 训练后自动为训练区间生成预测明细；明细保存工况快照、模型版本、实际值与偏差。
- 未指定模型查询明细时自动使用当前生效版本，避免历史版本重复。
- 模拟采集补充环境温度、产品类型与夜间停机待机样本，使工况与能耗自洽。

**前端**

- `/baseline/models`：模型列表、训练对话框（企业/车间/产线/设备 × 能源类型 × 区间 × 是否启用）、启用/停用。
- `/baseline/models/:id`：模型 KPI、回归系数、工况分组均值、回退记录、误差分布、最差样本、版本历史切换。
- `/baseline/records`：实际 vs 基线趋势图、工况参数表、偏差率着色、分页。

**验证结果**

| 检查 | 结果 |
| --- | --- |
| 算法单测 | 7 个用例：R²>0.98、低/高负荷差异、产品切换、待机、异常剔除、样本不足回退、未分组回退 |
| 集成测试 | 训练→预测→明细→版本切换；低负荷基线 4.89、中负荷 9.15、高负荷 11.32/15.58，待机 3.82 |
| 全量测试 | 44 个用例通过 |
| 演示模型 | v5：有效样本 62、待机 56、质量校验剔除 50、MAE 0.2323、RMSE 0.2906、MAPE 3.37%、R² 0.9857 |
| 页面 | 模型列表、模型详情、基线列表截图（`docs/screenshots/baseline-*.png`） |

**文档**

- 新增 `docs/baseline-algorithm.md`：特征、公式、清洗、回退链、计算示例、接口与已知边界。

### 2026-11-04 · P4 附加：三维工厂可视化（Three.js）

**后端（`/api/factory`）**

| 能力 | 接口 |
| --- | --- |
| 工厂场景 | `GET /api/factory/scene?enterpriseId=` |
| 设备详情 | `GET /api/factory/devices/{id}` |
| 产线详情 | `GET /api/factory/lines/{id}` |

- 布局坐标由后端统一计算（车间按 X 轴居中排布，产线沿 Z 轴分层，设备沿产线等距），前端只渲染，保证两端一致。
- 风险等级规则：`status 0/2 → OFFLINE`；`3 → MEDIUM（检修）`；`1 且今日有能耗 → NORMAL`；`1 且今日无能耗 → LOW`；`HIGH` 预留给 P5 偏差 / P6 异常接入后触发。
- 设备详情返回名称、编号、状态、今日能耗、风险与原因；动态基线、偏差率、异常等级、诊断结论明确标记不可用。

**前端（`web/src/three` 模块化拆分）**

| 文件 | 职责 |
| --- | --- |
| `scene-controller.ts` | 渲染器/相机/控制器/动画循环/资源释放/相机复位 |
| `environment.ts` | 光照、地面、道路、能源管线、配电房、Canvas 标签 |
| `building-factory.ts` | 厂房（半透明墙体 + 屋面 + 立柱 + 边框）与产线 |
| `device-factory.ts` | 数控机床、空压机、冷却泵、工业风机、输送设备、机械手 |
| `interaction.ts` | Raycaster 悬浮/点击、高亮、悬停提示、事件清理 |
| `palette.ts` / `types.ts` / `index.ts` | 配色、类型、对外出口 |

- 交互：左键旋转、右键平移、滚轮缩放、设备/产线点击、悬浮提示、右侧详情栏、视角复位、`?deviceId=` 深链。
- 状态色：绿正常 / 黄低风险 / 橙中风险 / 红高风险 / 灰离线；高风险呼吸闪烁，中风险常亮。
- 资源释放：`dispose()` 取消 RAF、解绑 ResizeObserver 与 window 监听、释放 geometry/material/texture、销毁控制器与渲染器；重建场景时逐项释放旧资源。

**验证结果**

| 检查 | 结果 |
| --- | --- |
| `mvn test` | 35 个测试通过（新增 FactoryFlowTest 4 个） |
| 场景接口 | 2 车间 / 3 产线 / 4 设备 / 549.54 kWh，风险 NORMAL 与 OFFLINE 均有 |
| 悬浮 | 无头浏览器在 (480,292) 命中「注塑机 A1 / DEV001 / 正常」 |
| 点击 | 同一场景点击设备打开侧栏，显示 227.88 kWh、基线/偏差/诊断为「待接入」 |
| 数据隔离 | 工程师请求 enterpriseId=2 被强制回本企业 1 |
| 页面渲染 | `docs/screenshots/factory.png`、`factory-device.png` |

### 2026-11-04 · P4 能耗画像与驾驶舱

**数据库**：`db/migration/V3__profile.sql` 为 `energy_type` 增加 `coal_factor`（折标煤系数 kgce/单位），用于综合能耗（tce）换算。

**后端（`datalake` 模块 ADS 层新增）**

| 能力 | 接口 |
| --- | --- |
| 能源驾驶舱 | `GET /api/profile/cockpit?date=&scopeType=&scopeId=&energyTypeId=&granularity=` |
| 企业画像 | `GET /api/profile/enterprise/{id}?days=&energyTypeId=` |
| 产线画像 | `GET /api/profile/line/{id}?days=` |
| 设备画像 | `GET /api/profile/device/{id}?days=` |
| 层级树 | `GET /api/profile/hierarchy?enterpriseId=` |

- 驾驶舱 8 个 KPI：今日总能耗、综合能耗、能源成本、数据完整率（真实）；偏差率、异常设备、待整改、累计节能量（P5/P6/P7/P8 接入，返回 `available=false` 由前端显示「待接入」）。
- 图表数据：趋势（小时/日）、能源结构、产线排名、桑基流向（真实）；基线对比、异常分布、节能趋势（读对应表，空则前端空态）。
- 企业画像：总能耗/成本/综合能耗、结构、趋势、单位产品能耗、综合能源评价（完整率 60% + 日均稳定性 40%，A/B/C/D）。
- 产线/设备画像：能耗、产量、运行时长、负荷率、单耗；基线与异常字段留待 P5/P6。
- 模拟采集扩展：按小时聚合结果生成 `production_record`（产量/运行时长/负荷率），供单耗计算。
- 修复：产线聚合改为 `COALESCE(detail.line_id, device.line_id)`，设备表计入所属产线；模拟器改用「区间起点之前的最后读数」，并对定时/手工任务加同步，避免并发写坏累计读数序列。

**前端（4 页 + 图表组件）**

- `components/BaseChart.vue`：ECharts 封装（初始化、resize、销毁、loading、点击事件）。
- `views/cockpit/CockpitView.vue`：深蓝驾驶舱，8 KPI、7 张图、Cascader 层级筛选、日期/粒度/能源类型筛选。
- `views/profile/{Enterprise,Line,Device}ProfileView.vue`：画像页，支持 7/30/90 天切换、产线↔设备下钻、图表点击跳转。

**验证结果**

| 检查 | 结果 |
| --- | --- |
| `mvn test` | 31 个测试通过（新增 ProfileFlowTest 3 个） |
| 驾驶舱接口 | 8 个 KPI、24 个小时点、7 条桑基边、100% 完整率 |
| 企业画像 | 总能耗 549.54 kWh、综合能耗 0.0675 tce、单耗 0.4514 kWh/件、评级 A |
| 产线画像 | 506.6 kWh、产量 1002.6 件、单耗 0.5053、设备 2/2 |
| 设备画像 | 能耗、负荷率、产量、单耗均返回 |
| 页面渲染 | 驾驶舱 + 3 个画像页截图（`docs/screenshots/cockpit.png` 等） |


### 2026-11-04 · P3 能耗数据湖

**数据库**

- `db/migration/V2__datalake.sql`：为已有环境补充字段与表（新环境直接用最新 `schema.sql`）。
- `energy_meter` 新增 `collect_interval_seconds`（采集间隔，用于推算应采点数）、`reading_type`（1 累计读数 / 2 周期增量）。
- `energy_raw_data` 新增 `reading_type`、`received_time`。
- `energy_detail` 新增 `start_reading`、`end_reading`、`estimated`。
- 新增 `data_ingest_failure`（失败记录，保留原始内容）、`energy_data_gap`（缺口与补录状态）。
- 种子权限：ENERGY_ADMIN 全量数据湖权限，ENGINEER/VIEWER 只读权限；MTR003 设为周期增量表作为示例。

**后端（`module/datalake`）**

| 分层 | 能力 |
| --- | --- |
| ODS | 批量接入（批次号幂等）、单条接入、CSV/Excel 导入（POI）、模拟采集；原始数据只追加，重复提交被唯一键拦截并计入重复 |
| DWD | 格式/量程校验、仪表与设备校验、倍率换算、累计读数差值、表计清零识别、跳变检测、重复明细识别、失败记录 |
| DWS | 5 分钟 / 15 分钟 / 小时 / 日 / 月 × 企业 / 车间 / 产线 / 设备 × 能源类型，按完整时间桶 upsert |
| ADS | 驾驶舱（总能耗、成本、能耗构成、Top 设备、小时曲线、完整率）、能耗画像（日序列、构成） |
| 统计 | 应采点数（按采集间隔）、有效入库、缺失、完整率；按设备/日期/企业 |
| 运维 | 批次日志、失败记录、缺口检测、按历史均值补录（标记 BACKFILL + estimated）、按日期重算聚合 |

**接口（新增 14 个）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/datalake/ingest` | 批量接入（幂等） |
| POST | `/api/datalake/import` | Excel/CSV 导入 |
| POST | `/api/datalake/simulate` | 模拟采集 |
| GET | `/api/datalake/raw` | ODS 原始数据分页 |
| GET | `/api/datalake/details` | DWD 明细分页 |
| GET | `/api/datalake/aggregates` | DWS 聚合分页 |
| GET | `/api/datalake/logs` | 入库日志分页 |
| GET | `/api/datalake/failures` | 失败记录分页 |
| POST | `/api/datalake/clean` | 触发清洗 |
| POST | `/api/datalake/aggregate` | 按日期重算聚合 |
| GET | `/api/datalake/quality/summary` | 质量概览 |
| GET | `/api/datalake/completeness` | 完整率 |
| GET | `/api/datalake/gaps`、`POST /gaps/detect`、`POST /gaps/backfill` | 缺口检测与补录 |
| GET | `/api/datalake/ads/dashboard`、`/ads/profile` | 驾驶舱与画像 |

**前端（6 页）**：数据接入、数据导入、清洗记录、质量监控、入库日志、完整率。

**验证结果**

| 检查 | 结果 |
| --- | --- |
| `mvn test` | 28 个测试通过（新增 DatalakeFlowTest 2 个） |
| 全天 1440 点接入 → 清洗 → 聚合 → 完整率 | 通过，聚合能耗与明细求和一致（1439 × 0.5 = 719.5 kWh） |
| 幂等 | 同批次重复提交不重复入库；重复时刻读数计入重复、原始总量不变 |
| 模拟采集 | 5 个仪表 × 1440 分钟 = 7200 条，耗时 41s |
| 完整率 | 当天应采 7200、有效 7200、完整率 **100%**（≥ 99%） |
| 页面渲染 | 6 个数据湖页面 + 工作台截图，数据来自接口（`docs/screenshots/dl-*.png`） |


### 2026-11-04 · P2 基础管理

**数据库**

- 通过 `backend/src/main/resources/db/schema.sql` 建 24 张表（含注释、索引、唯一约束、逻辑删除字段），全部 `CREATE TABLE IF NOT EXISTS`，可重复执行。
- 通过 `db/data.sql` 初始化：4 个角色、2 个用户、5 种能源类型、2 家企业 / 3 个车间 / 4 条产线 / 5 台设备 / 5 个计量点，全部 `INSERT IGNORE`，重复执行不覆盖运行期修改。
- 演示账号：`admin / Admin@123`（平台管理员），`engineer / Engineer@123`（工程师，企业 1）。
- dev / test profile 开启 `spring.sql.init.mode=always`，启动时幂等初始化。
- 相对设计文档的调整：`sys_role` 增加 `permissions`（权限码，逗号分隔，`*` 表示全部）与 `builtin`（内置角色不可删除）两列。

**后端（新增约 45 个类）**

| 模块 | 内容 |
| --- | --- |
| system | `AuthService`（登录/刷新/当前用户/菜单）、`UserService`、`RoleService`、`MenuService`；3 个 Controller 共 17 个接口 |
| masterdata | 企业/车间/产线/设备/能源类型/计量点 6 组 Service + Controller，共 36 个接口 |
| 安全 | `LoginUser` 增加 `enterpriseId`；JWT 增加 `perms`/`enterpriseId` 声明；`DataScopeHelper` 企业隔离；`PermissionCodes` 权限登记表 |
| SPI | `EnterpriseNameProvider` 解决 system → masterdata 的反向依赖 |
| 公共 | `StatusDTO`、`IpUtil`、`MasterNameResolver`（批量名称解析，避免 N+1） |

**前端（新增 12 个文件）**

| 内容 | 说明 |
| --- | --- |
| 登录页 | 真实调用 `/auth/login`，支持 redirect 回跳 |
| 布局 | 左侧菜单由 `/auth/menus` 渲染、顶栏面包屑 + 用户下拉退出 |
| 路由 | 登录态与权限码守卫，未登录跳 `/login`，无权限跳 `/403` |
| 状态 | Pinia `user`：token、profile、菜单、权限判定 |
| 页面 | 企业、车间、产线、设备、用户、角色 6 个页面，含查询/分页/新增/编辑/删除/状态切换 |
| 复用 | `usePageList` 统一分页与加载/错误态；`permissionGroups` 权限配置 |

**接口清单（P2 新增）**

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/auth/login` | 公开 |
| POST | `/api/auth/refresh` | 公开 |
| GET | `/api/auth/me` | 登录 |
| GET | `/api/auth/menus` | 登录 |
| GET/POST/PUT/DELETE | `/api/system/users`、`/{id}`、`/{id}/status`、`/{id}/password` | `system:user:*` |
| GET/POST/PUT/DELETE | `/api/system/roles`、`/all`、`/{id}`、`/{id}/status` | `system:role:*` |
| GET/POST/PUT/DELETE | `/api/master/enterprises`、`/{id}`、`/{id}/status` | `master:enterprise:*` |
| GET/POST/PUT/DELETE | `/api/master/workshops`、`/{id}`、`/{id}/status` | `master:workshop:*` |
| GET/POST/PUT/DELETE | `/api/master/lines`、`/{id}`、`/{id}/status` | `master:line:*` |
| GET/POST/PUT/DELETE | `/api/master/devices`、`/{id}`、`/{id}/status` | `master:device:*` |
| GET/POST/PUT/DELETE | `/api/master/energy-types`、`/{id}`、`/{id}/status` | `master:energy:*` |
| GET/POST/PUT/DELETE | `/api/master/meters`、`/{id}`、`/{id}/status` | `master:meter:*` |

**验证结果**

| 检查 | 结果 |
| --- | --- |
| `mvn test` | 26 个测试全部通过（0 失败 / 0 错误） |
| 数据库脚本 | 连续执行两次无报错；24 张表、2 用户、2 企业、5 设备 |
| 登录接口 | `admin` 200 并返回 token（含 33 个权限码）；错误密码 1001 |
| 数据隔离 | 工程师企业列表 total=1；访问企业 2 返回 403；访问用户列表 403 |
| 未登录 | 受保护接口返回 401 JSON |
| 主数据 CRUD | 车间新增→详情（含企业名）→改名→列表→删除，全部 200 |
| Actuator | `/api/actuator/health` 返回 `UP`（数据库已连通） |
| 前端构建 | `npm run build` 通过（vue-tsc + vite，1709 模块） |
| 前端代理 | `:5173/api/auth/login` 返回 200，`:5173/api/master/enterprises` 返回 2 条 |

---

### 2026-11-04 · P2 复测（问题修复与重新验证）

**修复的问题**

| 问题 | 处理 |
| --- | --- |
| Vite 文件监听崩溃（EBUSY，监听到临时目录/浏览器 profile） | `vite.config.ts` 增加 `server.watch.ignored`（`.build-tmp`、`dist`、`node_modules`）；截图脚本的 Edge profile 移到项目目录之外 |
| 列表页「操作」列过窄，按钮换行、右侧列被固定列遮挡 | 企业/车间/产线/设备/用户 5 个页面重排列宽；企业、车间列表移除「创建时间」列 |
| 批量改列宽时 PowerShell 把字符串当数组索引，导致 `l` 被替换为 `a`，EnterpriseView/WorkshopView/LineView 三个文件损坏 | 已按原逻辑重建三个文件，构建恢复通过（教训：批量替换必须显式构造 `@(@(old,new),...)`，并先校验） |

**复测结果**

| 检查 | 命令/方式 | 结果 |
| --- | --- | --- |
| 后端干净构建 + 全量测试 | `mvn clean test package` | BUILD SUCCESS，26/26 通过 |
| 实时接口冒烟 | 19 项（健康、登录、错误密码、401、403、隔离、CRUD、分页、用户） | 19/19 通过 |
| 前端构建 | `npm run build` | 通过，1709 模块 |
| 页面渲染 | 无头 Edge 驱动真实登录后逐页截图 | 登录、工作台、企业、车间、产线、设备、用户、角色 8 个页面均正常，数据来自接口 |
| 截图归档 | `docs/screenshots/*.png` | 8 张 |

> 说明：自动化截图用于验证渲染与接口联通；交互（弹窗提交、表单校验提示）由 MockMvc 集成测试覆盖。

---

### 2026-11-04 · P1 工程骨架

环境：Temurin JDK 17.0.21（`D:\Dev\Java\jdk-17`）、Maven 3.8.2、Node 24.18.0、MySQL 8.0.31。

- 后端：`Result`/`PageResult`/`ResultCode`、`BusinessException`/`GlobalExceptionHandler`、JWT + Spring Security、多环境配置、日志滚动、CORS、MyBatis-Plus 分页与审计填充、健康检查。
- Web：Vite + Vue3 + TS、Axios 封装、路由守卫、Pinia、工程自检页。
- 小程序：原生工程、`request`/`auth`/`upload`/`format` 封装、自检页与我的页。
- 验证：jar 42.9 MB、7 个测试通过、前端构建通过、代理联通。

---

### 2026-11-04 · P0 设计阶段

交付 `AGENTS.md`、`README.md`、`docs/architecture.md`、`docs/database-design.md`、`docs/implementation-plan.md` 与本文件；冻结技术栈、模块边界、24 张表与核心算法设计。

---

## 待办

### P3 待办（下一步）

- [ ] 采集接入接口（`/api/datalake/ingest`，批次幂等）与 CSV 导入
- [ ] 原始数据查询、清洗任务、明细生成
- [ ] 小时/日/月 × 企业/车间/产线/设备 聚合与重算
- [ ] 采集入库日志与完整率统计接口
- [ ] 生产工况 `production_record` 维护
- [ ] 能源类型、计量点管理页（补前端，当前仅后端接口）
- [ ] 更新进度文档并跑测试/构建

### 后续

- [ ] P4 驾驶舱、能耗画像、三维工厂
- [ ] P5 动态基线与偏差
- [ ] P6 异常识别与诊断（含评估报告）
- [ ] P7 整改闭环与小程序
- [ ] P8 节能核算
- [ ] P9 联调、性能与验收

---

## 已知问题

| # | 问题 | 影响 | 计划 |
| --- | --- | --- | --- |
| 1 | 角色权限存于 JWT，修改角色后需重新登录才生效 | 权限变更不实时 | P4 前引入 token 版本或权限缓存 |
| 2 | 能源类型、计量点无前端页面（菜单未开放） | 只能通过接口管理 | P3 补页面 |
| 3 | `admin` JWT 携带 33 个权限码（约 1.2 KB） | 请求头体积偏大 | 可选改为服务端权限缓存 |
| 4 | Element Plus 全量引入，首包 ~1.25 MB（gzip 408 KB） | 首屏体积大 | 后续按需引入 |
| 5 | 构建时 esbuild 临时文件偶发 `Access is denied` | 构建中断 | 将 `TEMP/TMP` 指向 `web/.build-tmp` 规避 |
| 6 | 集成测试依赖本机 MySQL 与 `smarten_energy_test` 库 | 无 DB 时测试失败 | 后续可切 Testcontainers |
