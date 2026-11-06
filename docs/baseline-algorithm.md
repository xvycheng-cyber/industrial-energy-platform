# 动态能耗基线算法说明

> 适用模块：`backend/src/main/java/com/smarten/module/baseline`
> 目标：按生产工况预测「应当消耗多少能源」，而不是固定阈值或历史平均值。

---

## 1. 为什么不是固定阈值 / 历史均值

同一台设备在低负荷、高负荷、停机、切换产品时的合理能耗差异可达数倍。固定阈值会把正常的高负荷时段误判为异常；简单历史均值会把夜班待机与白班满负荷混在一起，偏差率长期失真。

本模块用**多变量回归 + 工况分组 + 待机基线**三件套，把「能耗」解释为「工况的函数」，并允许在样本不足时逐级回退。

---

## 2. 特征

| 特征 | 来源 | 编码 |
| --- | --- | --- |
| 产量 `outputQty` | `production_record.output_qty` | 连续 |
| 运行时长 `runMinutes` | `production_record.run_minutes` | 连续 |
| 负荷率 `loadRate` | `production_record.load_rate` | 连续（0-100） |
| 环境温度 `ambientTemp` | `production_record.ambient_temp` | 连续 |
| 产品类型 `productType` | `production_record.product_type` | 哑变量（首类为基准） |
| 班次 `shift` | `production_record.shift` | 哑变量（1 为基准） |
| 运行状态 `status` | `production_record.status` + `run_minutes` | 运行标志 + 停机单独建模 |

特征向量（含截距）：

```
x = [1, outputQty, runMinutes, loadRate, ambientTemp,
     1(product=p2), …, 1(shift=2), 1(shift=3), 1(running)]
```

---

## 3. 模型

### 3.1 多变量岭回归

```
ŷ = β0 + β1·output + β2·run + β3·load + β4·temp
   + Σ γk·1(product=k) + Σ δs·1(shift=s) + θ·1(running)
```

参数估计：

```
β = (XᵀX + λI)⁻¹ Xᵀy        λ = 1e-6，截距不惩罚
```

- 用高斯消元（部分主元）求解，矩阵奇异时判定为不可用并回退。
- 岭项用于处理共线（如运行时时长恒为 60 分钟、环境温度短期近似恒定），避免矩阵不可逆。

### 3.2 训练样本清洗

1. **有效性**：字段缺失、能量为负 → 直接剔除并计入 `sampleRejected`。
2. **质量门禁**：该小时在 DWD（`energy_detail`）中存在 `quality_flag != 1` 或 `estimated = 1` 的记录 → 不参与训练（防止补录、重复、越界数据污染）。
3. **异常值剔除**：
   - 第一轮：截尾最小二乘，重复 3 次，每轮剔除残差绝对值最大的 15%；
   - 第二轮：用全量残差的中位数与 MAD，剔除 `|r − median| > 3 × 1.4826 × MAD` 的样本；
   - 两轮之后重新拟合，记录 `outlierCount`。
4. **有效样本判断**：`n ≥ max(20, 特征数 + 1)` 才允许回归，否则进入回退链。

### 3.3 回退链（逐级降级，全部记录）

| 级别 | 条件 | 预测值 | `baseline_type` |
| --- | --- | --- | --- |
| 1 | 回归可用 | βᵀx | 1 |
| 2 | 回归样本不足，但存在同工况分组（负荷档 + 产品）且样本 ≥ 5 | 分组均值 | 2 |
| 3 | 无匹配分组 | 历史正常样本中位数 | 4 |
| 4 | 停机/检修（`status != 1` 或运行时长 0） | 独立待机基线（停机样本中位数） | 5 |

- 回退原因写入模型 `fallback_log`，并逐条写入明细 `fallback_reason`。
- 待机工况**永远不进入**运行回归，避免夜班低耗把日间基线拉低。

### 3.4 模型评价

| 指标 | 含义 |
| --- | --- |
| MAE | 平均绝对误差（kWh） |
| RMSE | 均方根误差（kWh） |
| MAPE | 平均绝对百分比误差（%） |
| R² | 决定系数，越接近 1 越好 |
| 置信度 | 回归且样本 ≥ 100 → 高；回归 → 中；回退模型 → 低 |

---

## 4. 计算示例（演示数据，设备「注塑机 A1」v4 模型）

模型参数（截距 + 特征）：

```
β0 = -0.6319   outputQty = 0.2987   runMinutes = 0.0000
loadRate = 0.0575   ambientTemp = 0.0326
product=B型支架 = -0.1670   shift=2 = -0.2005   shift=3 = -0.3256   running = 0.0000
待机基线 = 3.8226 kWh    历史正常均值 = 9.8717 kWh
```

某小时工况：产量 33.21 件、负荷率 96%、温度 13.03℃、产品 A 型壳体、1 班、运行。

```
ŷ = -0.6319 + 0.2987×33.21 + 0.0575×96 + 0.0326×13.03
  = -0.6319 + 9.9193 + 5.5200 + 0.4248
  = 15.2322 kWh
```

实际能耗 14.3272 kWh：

```
偏差量 = 14.3272 − 15.2322 = -0.9050 kWh
偏差率 = -0.9050 / 15.2322 × 100% = -5.94%
```

同一设备的三种工况（来自模型分组均值）：

| 工况 | 预期基线 | 说明 |
| --- | --- | --- |
| LOW + A 型壳体 | 4.8947 kWh | 低负荷 |
| MID + A 型壳体 | 9.4900 kWh | 中负荷 |
| HIGH + A 型壳体 | 15.5750 kWh | 高负荷 |
| 停机（待机基线） | 3.8226 kWh | 独立待机模型 |

> 同一设备、不同工况得到明显不同的基线，这是动态基线的核心验收点。

---

## 5. 模型更新与版本

- 每次训练生成新版本：`v{序号}-{yyyyMMddHHmmss}`，旧版本保留为「已归档」，可随时切回。
- 训练数据只取通过质量门禁、工况匹配（有产量/负荷/温度）且状态正常的样本；整改后仍带质量标记的数据不会进入训练。
- 启用新版本时自动归档同对象、同能源类型的生效版本，`effective_from` 记录生效时间。
- 停用后该对象无生效模型，预测接口会提示先训练。

---

## 6. 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/baseline/models/train` | `baseline:train` | 训练并可选启用，同时生成训练区间预测 |
| POST | `/api/baseline/predict` | `baseline:train` | 按指定/生效模型生成预测 |
| GET | `/api/baseline/models` | `baseline:view` | 模型分页（维度、状态、能源类型） |
| GET | `/api/baseline/models/{id}` | `baseline:view` | 模型详情（参数、权重、分组均值、回退记录） |
| GET | `/api/baseline/models/{id}/error` | `baseline:view` | 误差分析（MAE/RMSE/MAPE/R²、残差分布、最差样本） |
| GET | `/api/baseline/records` | `baseline:view` | 基线明细（实际 vs 基线、工况、偏差率） |
| POST | `/api/baseline/models/{id}/enable` | `baseline:manage` | 启用/切换版本 |
| POST | `/api/baseline/models/{id}/disable` | `baseline:manage` | 停用 |

### 请求示例

```http
POST /api/baseline/models/train
{
  "dimType": 4,
  "dimId": 1,
  "energyTypeId": 1,
  "startDate": "2026-10-28",
  "endDate": "2026-11-03",
  "activate": true
}
```

### 响应示例

```json
{
  "code": 200,
  "data": {
    "modelId": 4,
    "modelVersion": "v4-20261104192623",
    "algo": "RIDGE",
    "sampleCount": 112,
    "normalSamples": 112,
    "standbySamples": 56,
    "rejectedSamples": 0,
    "outlierSamples": 3,
    "requiredSamples": 10,
    "mae": 0.3458,
    "rmse": 0.4425,
    "mape": 3.6477,
    "r2": 0.9884,
    "predictedCount": 168,
    "activated": true,
    "fallbacks": ["剔除异常样本 3 条（MAD>3.0σ）"]
  }
}
```

### 明细示例

```json
{
  "periodStart": "2026-11-03 20:00:00",
  "actualValue": 9.2669,
  "baselineValue": 9.8680,
  "deviationValue": -0.6011,
  "deviationRate": -6.09,
  "baselineType": 1,
  "baselineTypeName": "回归",
  "condition": {
    "outputQty": 22.22,
    "runMinutes": 60,
    "loadRate": 64.36,
    "ambientTemp": 11.21,
    "productType": "A型壳体",
    "shift": 2,
    "status": 1
  }
}
```

---

## 7. 测试覆盖

| 用例 | 断言 |
| --- | --- |
| 回归还原工况关系 | R² > 0.98，MAE < 1 |
| 低负荷 vs 高负荷 | 同设备基线相差 ≥ 30 kWh（测试数据） |
| 产品切换 | B 型支架基线高于 A 型约 4 kWh |
| 停机 | 预测类型 = 待机基线，值接近停机样本中位数 |
| 异常样本污染 | 6 条极端样本被剔除，正常工况预测偏移 < 2 kWh |
| 样本不足 | 回退分组均值/历史均值，`fallbackLog` 非空 |
| 未见工况分组 | 回退历史正常均值 |
| 端到端 | 训练→预测→明细→版本切换全链路通过 |

---

## 8. 已知边界

1. 训练粒度为**小时**；日/班次维度由小时预测聚合，后续可扩展 `granularity`。
2. 产量、负荷等工况由模拟采集生成（`DatalakeSimulator`），接入真实 MES 后无需改算法。
3. 当前回归为线性；非线性（温度二次项、交互项）可在 `Encoder` 扩展，模型版本会记录特征清单。
4. 异常样本剔除阈值（15% 截尾、3σ）为默认值，可按行业配置为 `TrainOptions` 参数。
