# 数据库验收报告

> 数据库：`smarten_energy`（MySQL 8.0.31），由 `backend/src/main/resources/db/schema.sql` + `data.sql` 初始化。

## 1. 总览

| 指标 | 数值 |
| --- | --- |
| 业务表数量 | 25 |
| 字段总数 | 492 |
| 索引总数 | 99 |
| 数据行合计（统计值） | 208925 |

## 2. 表清单

| 表名 | 说明 | 字段数 | 索引数 | 行数（统计） |
| --- | --- | --- | --- | --- |
| `data_ingest_failure` | 入库失败记录 | 15 | 4 | 0 |
| `data_ingestion_log` | 采集入库日志 | 20 | 4 | 3207 |
| `diagnosis_report` | 诊断报告 | 24 | 4 | 37 |
| `diagnosis_rule` | 诊断规则 | 20 | 4 | 8 |
| `energy_aggregate` | 能耗聚合 | 17 | 4 | 74932 |
| `energy_anomaly` | 异常事件 | 32 | 6 | 37 |
| `energy_baseline` | 动态基线模型 | 31 | 4 | 37 |
| `energy_baseline_detail` | 基线明细与偏差 | 24 | 4 | 22367 |
| `energy_data_gap` | 数据缺口 | 17 | 4 | 0 |
| `energy_detail` | 能耗明细 | 22 | 5 | 42850 |
| `energy_device` | 设备 | 18 | 5 | 35 |
| `energy_enterprise` | 企业 | 15 | 4 | 3 |
| `energy_line` | 产线 | 15 | 3 | 11 |
| `energy_meter` | 计量点（仪表） | 18 | 5 | 45 |
| `energy_raw_data` | 原始采集数据（只追加） | 20 | 5 | 43006 |
| `energy_saving_result` | 节能核算结果 | 37 | 4 | 3 |
| `energy_type` | 能源类型 | 13 | 2 | 5 |
| `energy_workshop` | 车间 | 13 | 3 | 8 |
| `production_record` | 生产工况 | 22 | 4 | 22298 |
| `rectification_feedback` | 整改反馈 | 19 | 3 | 17 |
| `rectification_task` | 整改任务 | 31 | 6 | 8 |
| `sys_file` | 统一上传文件 | 14 | 2 | 0 |
| `sys_role` | 角色 | 13 | 2 | 4 |
| `sys_user` | 用户 | 17 | 5 | 4 |
| `sys_user_role` | 用户角色关联 | 5 | 3 | 3 |

## 3. 约束与关系

- 主键：25 张表均为 `id BIGINT AUTO_INCREMENT`。
- 唯一约束：46 个唯一索引，覆盖用户名、角色码、设备/表计编码、批次号、规则码、任务号、核算版本等业务唯一键。
- 外键：**0 个物理外键**，采用应用层逻辑关联（`enterprise_id` / `workshop_id` / `line_id` / `device_id` / `meter_id` / `task_id` / `report_id`），便于分库与批量导入；关联完整性由 Service 校验与唯一索引保证。
- 逻辑删除：全部业务表含 `deleted TINYINT`，MyBatis-Plus 自动过滤。
- 审计字段：`create_time` / `update_time` / `create_by` / `update_by`。

## 4. 初始化脚本验证

在空库 `smarten_accept` 上连续执行两次 `schema.sql` 与 `data.sql`：

| 检查 | 结果 |
| --- | --- |
| 建表数量 | 25 / 25 |
| schema 幂等 | 通过（第二次无报错） |
| data 幂等 | 通过（第二次无报错、无重复） |
| 种子数据 | 能源类型 5、角色 4、用户 2、诊断规则 8 |
| 节能核算字段 | 37 列（含 V7 结论/完整率/可比性/追溯字段） |
| 文件表 | `sys_file` 14 列 |

## 5. 数据关系（核心链路）

```
energy_enterprise 1─n energy_workshop 1─n energy_line 1─n energy_device 1─n energy_meter
                                                │                      │
                                                │                      └─1─n energy_raw_data 1─1 energy_detail 1─n energy_aggregate
                                                └─ energy_baseline 1─n energy_baseline_detail
energy_anomaly 1─1 diagnosis_report 1─n rectification_task 1─n rectification_feedback
rectification_task 1─n energy_saving_result（version 递增，archived 标记历史版本）
sys_user 1─n sys_user_role n─1 sys_role；sys_file 记录整改图片
```

## 6. 验收结论

- 25 张业务表、492 字段、75 索引、46 唯一索引，结构与 P2–P8 业务一致。
- 初始化脚本可从零建库并重复执行；已修复 `sys_file` 缺失与 `energy_saving_result` 缺少 P8 字段的问题。
- 无物理外键属于设计选择，需在应用层保证关联；当前主要关联字段均已建索引。