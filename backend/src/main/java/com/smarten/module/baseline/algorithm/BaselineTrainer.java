package com.smarten.module.baseline.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态基线训练器：多变量岭回归 + 异常值剔除 + 分组均值/历史均值/待机基线回退。
 *
 * <p>训练流程：</p>
 * <ol>
 *   <li>样本清洗：字段缺失、能量为负、状态非运行 → 剔除或归入待机池；</li>
 *   <li>特征编码：产量、运行时长、负荷率、环境温度、产品类型、班次、运行状态；</li>
 *   <li>岭回归拟合，按残差 MAD 剔除异常样本后复拟合（最多两轮）；</li>
 *   <li>有效样本不足或矩阵奇异 → 工况分组均值 → 历史正常均值；</li>
 *   <li>停机/检修样本单独学习待机基线，不混入正常运行模型。</li>
 * </ol>
 */
public class BaselineTrainer {

    public TrainResult train(List<Sample> samples, TrainOptions options) {
        List<Sample> valid = new ArrayList<>();
        int rejected = 0;
        for (Sample sample : samples) {
            if (sample.valid()) {
                valid.add(sample);
            } else {
                rejected++;
            }
        }

        List<Sample> standby = valid.stream().filter(Sample::standby).toList();
        List<Sample> normal = valid.stream().filter(Sample::running).toList();

        BaselineModel model = new BaselineModel();
        model.setRejectedCount(rejected);
        model.setStandbySamples(standby.size());
        model.setStandbyBaseline(round(median(standby.stream().map(Sample::energy).toList())));
        model.setProductTypes(productTypes(normal));
        model.setShifts(shifts(normal));
        model.setFeatureNames(featureNames(model.getProductTypes(), model.getShifts()));

        List<String> logs = new ArrayList<>();
        List<Sample> kept = new ArrayList<>(normal);
        int outliers = 0;
        double[] beta = null;

        if (normal.size() >= 4) {
            // 第一轮：OLS 拟合 + 截尾最小二乘（每轮剔除残差最大的 15%），避免少量极端样本拖偏系数
            beta = fit(normal, model, options.ridgeLambda());
            List<Sample> trimmed = new ArrayList<>(normal);
            if (beta != null && normal.size() >= 20) {
                for (int round = 0; round < 3; round++) {
                    double[] current = residuals(trimmed, beta, model);
                    List<Integer> order = new ArrayList<>();
                    for (int i = 0; i < current.length; i++) {
                        order.add(i);
                    }
                    order.sort((a, b) -> Double.compare(Math.abs(current[b]), Math.abs(current[a])));
                    int drop = Math.max(1, (int) Math.floor(trimmed.size() * 0.15));
                    List<Sample> next = new ArrayList<>();
                    for (int i = 0; i < trimmed.size(); i++) {
                        if (!order.subList(0, Math.min(drop, order.size())).contains(i)) {
                            next.add(trimmed.get(i));
                        }
                    }
                    if (next.size() < 8) {
                        break;
                    }
                    trimmed = next;
                    beta = fit(trimmed, model, options.ridgeLambda());
                    if (beta == null) {
                        break;
                    }
                }
            }
            // 第二轮：用全量残差的 MAD 做最终去噪（把被误删的正常样本收回，只留真正离群点）
            if (beta != null) {
                double[] current = residuals(normal, beta, model);
                double median = medianOf(current);
                double mad = medianOf(java.util.Arrays.stream(current)
                        .map(r -> Math.abs(r - median)).toArray());
                if (mad > 1.0e-9) {
                    double limit = options.outlierSigma() * 1.4826 * mad;
                    List<Sample> filtered = new ArrayList<>();
                    for (int i = 0; i < normal.size(); i++) {
                        if (Math.abs(current[i] - median) <= limit) {
                            filtered.add(normal.get(i));
                        }
                    }
                    if (filtered.size() >= Math.max(8, model.featureCount() + 1)) {
                        kept = filtered;
                        beta = fit(kept, model, options.ridgeLambda());
                    }
                } else {
                    kept = normal;
                }
            }
        }
        outliers = Math.max(0, normal.size() - kept.size());

        model.setOutlierCount(outliers);
        model.setSampleCount(kept.size());
        model.setGroupMeans(groupMeans(kept));
        model.setHistoricalBaseline(round(median(kept.stream().map(Sample::energy).toList())));

        int required = Math.max(options.minSamples(), model.featureCount() + 1);
        boolean enough = beta != null && kept.size() >= required;
        Map<String, Integer> groupCounts = groupCounts(kept);
        boolean groupEnough = groupCounts.values().stream()
                .anyMatch(count -> count >= options.minGroupSamples());

        if (enough) {
            model.setAlgo(BaselineModel.RIDGE);
            model.setBeta(beta);
            evaluate(model, kept);
        } else {
            model.setAlgo(groupEnough ? BaselineModel.GROUP_MEAN : BaselineModel.HISTORICAL);
            model.setBeta(null);
            if (beta == null) {
                logs.add("回归矩阵奇异或样本不足，模型回退为" + (groupEnough ? "工况分组均值" : "历史正常均值"));
            } else {
                logs.add("有效样本 " + kept.size() + " < 要求 " + required + "，回退为"
                        + (groupEnough ? "工况分组均值" : "历史正常均值"));
            }
            evaluate(model, normal);
        }

        if (standby.isEmpty()) {
            logs.add("无停机样本，待机工况暂用历史正常均值");
        }
        if (outliers > 0) {
            logs.add("剔除异常样本 " + outliers + " 条（MAD>" + options.outlierSigma() + "σ）");
        }
        if (rejected > 0) {
            logs.add("清洗剔除无效样本 " + rejected + " 条（字段缺失或能量为负）");
        }
        model.setFallbackLog(logs);

        return new TrainResult(model, normal.size(), standby.size(), rejected, outliers, required);
    }

    private double[] fit(List<Sample> data, BaselineModel model, double lambda) {
        if (data.isEmpty()) {
            return null;
        }
        int k = model.encode(data.get(0)).length;
        double[][] matrix = new double[k][k];
        double[] vector = new double[k];
        for (Sample sample : data) {
            double[] x = model.encode(sample);
            for (int i = 0; i < k; i++) {
                vector[i] += x[i] * sample.energy();
                for (int j = 0; j < k; j++) {
                    matrix[i][j] += x[i] * x[j];
                }
            }
        }
        for (int i = 1; i < k; i++) {
            matrix[i][i] += lambda;
        }
        return solve(matrix, vector);
    }

    /** 高斯消元（部分主元），奇异返回 null */
    private double[] solve(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(m[pivot][col]) < 1.0e-12) {
                return null;
            }
            double[] temp = m[col];
            m[col] = m[pivot];
            m[pivot] = temp;

            for (int row = 0; row < n; row++) {
                if (row == col) {
                    continue;
                }
                double factor = m[row][col] / m[col][col];
                for (int j = col; j <= n; j++) {
                    m[row][j] -= factor * m[col][j];
                }
            }
        }
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = m[i][n] / m[i][i];
        }
        return result;
    }

    private double[] residuals(List<Sample> data, double[] beta, BaselineModel model) {
        double[] result = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            double[] x = model.encode(data.get(i));
            double predicted = 0;
            for (int j = 0; j < beta.length; j++) {
                predicted += beta[j] * x[j];
            }
            result[i] = data.get(i).energy() - predicted;
        }
        return result;
    }

    private void evaluate(BaselineModel model, List<Sample> data) {
        if (data.isEmpty()) {
            return;
        }
        double sumAbs = 0;
        double sumSq = 0;
        double sumMape = 0;
        int mapeCount = 0;
        List<Double> actuals = new ArrayList<>();
        for (Sample sample : data) {
            double predicted = model.predict(sample);
            double error = sample.energy() - predicted;
            sumAbs += Math.abs(error);
            sumSq += error * error;
            if (sample.energy() > 0) {
                sumMape += Math.abs(error) / sample.energy();
                mapeCount++;
            }
            actuals.add(sample.energy());
        }
        double mean = actuals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sst = actuals.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum();
        model.setMae(round(sumAbs / data.size()));
        model.setRmse(round(Math.sqrt(sumSq / data.size())));
        model.setMape(round(mapeCount == 0 ? 0 : (sumMape / mapeCount) * 100));
        model.setR2(round(sst <= 0 ? 0 : 1 - sumSq / sst));
    }

    private Map<String, Integer> groupCounts(List<Sample> data) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Sample sample : data) {
            counts.merge(sample.conditionGroup(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<String, Double> groupMeans(List<Sample> data) {        Map<String, List<Double>> groups = new LinkedHashMap<>();
        for (Sample sample : data) {
            groups.computeIfAbsent(sample.conditionGroup(), key -> new ArrayList<>()).add(sample.energy());
        }
        Map<String, Double> means = new LinkedHashMap<>();
        groups.forEach((key, values) -> means.put(key, round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0))));
        return means;
    }

    private List<String> productTypes(List<Sample> data) {
        return data.stream()
                .map(Sample::productType)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<Integer> shifts(List<Sample> data) {
        return new ArrayList<>(data.stream().map(Sample::shift).collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream().sorted(Comparator.naturalOrder()).toList();
    }

    private String[] featureNames(List<String> productTypes, List<Integer> shifts) {
        List<String> names = new ArrayList<>();
        names.add("intercept");
        names.add("outputQty");
        names.add("runMinutes");
        names.add("loadRate");
        names.add("ambientTemp");
        for (int i = 1; i < productTypes.size(); i++) {
            names.add("product=" + productTypes.get(i));
        }
        for (int i = 1; i < shifts.size(); i++) {
            names.add("shift=" + shifts.get(i));
        }
        names.add("running");
        return names.toArray(new String[0]);
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return medianOf(values.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private double medianOf(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2.0 : sorted[middle];
    }

    private double round(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.round(value * 10000.0) / 10000.0;
    }
}
