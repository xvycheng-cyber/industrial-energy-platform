# 最终验收测试报告

> 日期：2026-11-05
> 被测版本：`smarten-backend` 1.0.0 / Web 构建产物 `web/dist`
> 环境：Windows 11、JDK 17.0.21、Maven 3.8.2、Node 24.18.0、MySQL 8.0.31、Edge Headless

---

## 1. 测试范围与命令

| 项 | 命令 / 方法 | 结果 |
| --- | --- | --- |
| 后端清理构建 | `mvn -B clean test` | BUILD SUCCESS |
| 后端打包 | `mvn -q -DskipTests package` | 成功，生成 `target/smarten-backend.jar` |
| 前端依赖 | `npm ci --no-audit --no-fund` | 退出码 0 |
| 前端生产构建 | `npm run build`（vue-tsc + vite） | 退出码 0，dist 生成 |
| 小程序语法 | `node --check` × 14 JS；JSON.parse × 11 | 0 错误 |
| 数据库空库初始化 | 空库执行 `schema.sql`、`data.sql` 各两遍 | 25 表、幂等通过 |
| 数据库结构统计 | `information_schema` 查询 | 492 字段 / 75 索引 / 46 唯一索引 |
| 完整率 | 原始表按 `meter_id + 小时` 去重 | 99.9861% |
| 异常指标 | `DiagnosisEngineMetricsTest` | P=1.0 R=0.9714 F1=0.9855 |
| 基线 | `BaselineTrainerTest` + 实库模型指标 | 7 场景通过；R² 0.89–0.92 |
| 节能核算 | `SavingCalculatorTest` + `SavingFlowTest` + 实库 | 通过 |
| 权限 | `DataIsolationTest`、`AuthControllerTest` + 实库 API | 401/403 符合预期 |
| 闭环 | `FullChainFlowTest` + 实库任务 12 | 通过 |
| API 清单 | 运行中 `/api/v3/api-docs` | 129 操作 / 104 路径 |
| UI | 构建 + 逐页截图走查 | 通过（人工） |

---

## 2. 后端测试明细（68 个）

| 测试类 | 用例数 | 结果 |
| --- | --- | --- |
| ResultTest | 4 | 通过 |
| BaselineFlowTest | 2 | 通过 |
| BaselineTrainerTest | 7 | 通过 |
| DatalakeFlowTest | 2 | 通过 |
| FactoryFlowTest | 4 | 通过 |
| ProfileFlowTest | 3 | 通过 |
| FullChainFlowTest | 1（76.7s） | 通过 |
| DiagnosisEngineMetricsTest | 3 | 通过 |
| DiagnosisFlowTest | 3 | 通过 |
| DataIsolationTest | 5 | 通过 |
| MasterDataCrudTest | 5 | 通过 |
| RectificationFlowTest | 6 | 通过 |
| SavingCalculatorTest | 8 | 通过 |
| SavingFlowTest | 3 | 通过 |
| AuthControllerTest | 6 | 通过 |
| HealthControllerTest | 2 | 通过 |
| UserCrudTest | 3 | 通过 |
| SmartenApplicationTests | 1 | 通过 |
| **合计** | **68** | **0 失败 / 0 错误** |

---

## 3. 数据库验收

- 25 张业务表，492 字段，75 索引，46 唯一索引，0 物理外键（应用层关联）。
- 核心表：企业 3、车间 8、产线 11、设备 35、计量点 45、原始数据 43,006、明细 42,850、聚合 74,932、基线 37、异常 37、诊断报告 37、整改任务 8、核算结果 3。
- 种子数据：能源类型 5、角色 4、用户 2（运行时另有 viewer / demo_engineer）、诊断规则 8。
- 空库初始化发现并修复两个缺陷：
  1. `schema.sql` 缺少 `sys_file` 表（V6 迁移才有）；
  2. `schema.sql` 的 `energy_saving_result` 缺少 P8 的 10 个字段（V7 迁移才有）。
- 修复后空库建表 25 张、核算表 37 列、`sys_file` 14 列，schema/data 重复执行均无报错。
- 详细报告：`docs/acceptance/database-acceptance.md`。

---

## 4. 数据完整率

| 范围 | 预期 | 有效 | 完整率 |
| --- | --- | --- | --- |
| 30 天 / 40 表计 / 720 小时 | 28,800 | 28,796 | **99.9861%** |
| 2026-11-04 单日 | 960 | 956 | 99.58% |

缺失 4 条来自注入的「连续 4 小时缺失」事件，属预期异常样本；整体 ≥99% 达标。
质量分布：原始数据质量 `quality=1`（正常）30,476 条；无物理重复（重复提交被唯一键拦截）。

---

## 5. 异常识别（独立标注集）

```
TP=68  FP=0  TN=890  FN=2
Precision=1.0000  Recall=0.9714  F1=0.9855
```

样本：8 类异常 + 正常，960 个标注点；Recall 97.14% ≥ 90%。

实库另有 10 条异常（R001/R005/R007/R008），用于演示与闭环；注入的 R002/R003/R004/R006 在当前 30 天样本中未被规则检出（见已知问题）。

---

## 6. 动态基线

- 7 个单测覆盖：工况回归、低/高负荷、产品切换、停机待机、异常剔除、样本不足回退、无分组回退。
- 实库 30 个模型：R² 0.8923–0.9165，MAE 0.1695–1.2016。
- 停机预测走 `STANDBY` 分支；低/中/高负荷基线随负荷上升。

---

## 7. 节能核算

- 8 个单元测试：节能达成、无明显变化、负节能、工况不可比、停机、数据不足、分时电价、重复计算一致。
- 3 个集成测试：复算版本归档、审核完成任务、导出 CSV。
- 实库：任务 11 结果 `baseline=1363.3450 actual=1158.8423 saving=204.5027 rate=15.0001 amount=134.40 conclusion=1:节能达成`，逐小时 168 点、版本 1。

---

## 8. 权限

| 场景 | 实测 |
| --- | --- |
| 未登录访问企业列表 | 401 |
| 工程师访问用户管理 | 403 |
| 工程师访问其他企业任务 | 403 |
| 工程师任务范围 | 5 条（企业 1） |
| 联调工程师任务范围 | 3 条（企业 3） |
| 管理员 | 全量 |

---

## 9. 业务闭环

实库任务 12：

```
管理员创建（待整改）
→ demo_engineer 小程序开始
→ 小程序提交反馈（待审核）
→ 管理员审核通过（效果验证中）
→ 管理员效果验证通过（已完成，4 条反馈留痕）
```

同时 `FullChainFlowTest` 在测试库执行完整链路并连续跑两次验证幂等，76.7s 通过。

---

## 10. 前端与小程序

- 前端：`npm ci` + `npm run build` 通过；登录、工作台、驾驶舱、三维、异常、整改、核算、设备、企业、响应式均已截图走查。
- 小程序：8 页面（登录、工作台、异常、异常详情、任务、任务详情、反馈、我的），14 JS / 11 JSON 语法通过，7 类 `/mobile/**` 接口调用；**未在微信开发者工具真机运行**。

---

## 11. 本轮修复的问题

| # | 问题 | 影响 | 处理 |
| --- | --- | --- | --- |
| 1 | `schema.sql` 缺 `sys_file` | 全新部署上传/图片查询失败 | 已补建表 |
| 2 | `schema.sql` 缺 P8 核算字段 | 全新部署核算 SQL 失败 | 已补 10 个字段 |
| 3 | 前端 `npm ci` 失败（esbuild 被运行中的 Vite 占用） | 依赖安装/构建失败 | 停止 dev server 后重装通过（环境问题，非代码缺陷） |

---

## 12. 未测试项

- Docker Compose / Nginx 实际启动（本机无 Docker、Nginx）。
- 微信开发者工具真机与真机网络。
- 浏览器兼容矩阵（仅 Edge Headless）。
- 生产 HTTPS / 域名 / 证书。

详见 `docs/acceptance/known-issues.md`。
