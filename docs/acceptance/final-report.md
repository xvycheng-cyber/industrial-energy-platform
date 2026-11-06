# 最终验收报告（总览）

> 项目：智能耗 · 工业企业能耗画像与节能诊断平台
> 验收日期：2026-11-05
> 验收方式：实际执行构建、测试、数据库初始化、接口调用、权限与闭环流程；未使用静态检查代替运行结果。

---

## 一、总体结论

**有条件通过。**

- 核心业务链路、算法指标、权限隔离、数据库初始化均通过实际运行验证。
- 两项未测试：Docker Compose/Nginx 容器化启动（环境无 Docker/Nginx）、小程序微信开发者工具真机。
- 无「功能失败」级遗留缺陷。

| 验收域 | 结论 |
| --- | --- |
| 工程构建（后端/前端） | ✅ 通过 |
| 数据库结构与初始化 | ✅ 通过（修复 2 个建库缺陷后） |
| 数据完整率 | ✅ 99.9861%（目标 ≥99%） |
| 异常识别 | ✅ Recall 97.14%、Precision 100%、F1 0.9855 |
| 动态基线 | ✅ 工况敏感，R² 0.89–0.92、MAE 0.17–1.20 |
| 节能核算 | ✅ 节能量/率/金额/负值/追溯均验证 |
| 权限隔离 | ✅ 401/403、企业隔离生效 |
| 业务闭环 | ✅ 实库任务 12 全流程完成 |
| 部署 | 🟡 配置完成，容器运行未测试 |
| 小程序 | 🟡 语法/页面/接口通过，真机未测试 |

---

## 二、关键实测数据

| 指标 | 结果 | 证据 |
| --- | --- | --- |
| 后端测试 | 68 / 68 通过 | `mvn -B clean test`、`mvn -B test` 两次 BUILD SUCCESS |
| 前端构建 | 成功 | `npm ci` 0、`npm run build` 0 |
| 数据库 | 25 表 / 492 字段 / 75 索引 / 46 唯一索引 | information_schema |
| 空库初始化 | 25 表，schema/data 各执行两遍均成功 | `smarten_accept` 库 |
| 数据完整率 | 28,796 / 28,800 = 99.9861% | 联调企业 30 天、40 表计 |
| 异常识别 | TP68 FP0 TN890 FN2；P=1.0 R=0.9714 F1=0.9855 | `DiagnosisEngineMetricsTest` |
| 基线模型 | R² 0.8923–0.9165；MAE 0.1695–1.2016 | 实库 30 个模型 |
| 核算结果 | +204.5027 kWh / 15.0001% / 134.40 元 | 实库 `energy_saving_result` |
| 权限 | 未登录 401、越权 403、工程师仅 5 条/联调 3 条 | 实库 API |
| 闭环 | 任务 12：待整改→…→已完成，4 条反馈 | 实库 API |
| API | 129 操作 / 104 路径 | `/api/v3/api-docs` |

---

## 三、交付文档

| 文档 | 路径 |
| --- | --- |
| 功能清单 | [feature-list.md](feature-list.md) |
| 数据库验收 | [database-acceptance.md](database-acceptance.md) |
| API 接口清单 | [../api/api-inventory.md](../api/api-inventory.md) |
| OpenAPI JSON | [../api/openapi.json](../api/openapi.json) |
| 部署文档 | [../deployment.md](../deployment.md) |
| 测试报告 | [test-report.md](test-report.md) |
| 验收指标 | [acceptance-metrics.md](acceptance-metrics.md) |
| 已知问题 | [known-issues.md](known-issues.md) |
| 数据库设计（原） | [../database-design.md](../database-design.md) |
| UI 视觉说明 | [../ui-redesign.md](../ui-redesign.md) |

---

## 四、未测试 / 未通过

### 未测试

1. **Docker Compose / Nginx 实际启动**：本机无 Docker 与 Nginx。已提供 `docker-compose.yml`、两个 Dockerfile、`nginx.conf`、`.env.example`，并完成配置审查；未拉起容器、未验证健康检查与初始化挂载。
2. **小程序真机**：仅完成 14 个 JS 语法检查、11 个 JSON 解析、8 页面清单和 7 类 `/mobile/**` 接口路径检查；未在微信开发者工具运行。

### 未通过

无（本次验收无失败断言）。

### 部分通过

1. 驾驶舱 4 个 KPI 后端仍标记不可用。
2. 注入的持续高耗/空载/空转/夜间异常未被 R002/R003/R004/R006 检出，实库异常仅 R001/R005/R007/R008。

### 本轮修复

1. `schema.sql` 缺 `sys_file` 表。
2. `schema.sql` 缺 P8 核算 10 个字段。
修复后空库可建 25 表、37 列核算表，schema/data 幂等。

---

## 五、复现命令

```powershell
# 后端（JDK 17）
$env:JAVA_HOME='D:\Dev\Java\jdk-17'; $env:Path="D:\Dev\Java\jdk-17\bin;$env:Path"
cd backend
mvn -B clean test
mvn -DskipTests package
java -jar target\smarten-backend.jar --spring.profiles.active=dev

# 前端
cd web
npm ci
npm run build
npm run dev

# 空库初始化
mysql -uroot -p -e "CREATE DATABASE smarten_accept DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p smarten_accept < backend/src/main/resources/db/schema.sql
mysql -uroot -p smarten_accept < backend/src/main/resources/db/data.sql

# 接口文档
curl http://127.0.0.1:8080/api/v3/api-docs -o docs/api/openapi.json
```

---

## 六、验收签署

| 角色 | 结论 |
| --- | --- |
| 工程/构建 | 通过 |
| 数据库 | 通过（修复后） |
| 算法（异常/基线/核算） | 通过 |
| 权限/闭环 | 通过 |
| 部署 | 待容器环境复验 |
| 小程序 | 待真机复验 |

**综合：有条件通过；在具备 Docker 与微信开发者工具的环境完成两项未测试项后，可转为完全通过。**
