package com.smarten.module.baseline.algorithm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 训练完成的基线模型：回归系数 + 工况分组均值 + 待机基线 + 历史正常均值。
 *
 * <p>模型自带特征编码规则，预测时按同一规则编码，保证训练与预测一致。</p>
 */
public class BaselineModel {

    public static final String RIDGE = "RIDGE";
    public static final String GROUP_MEAN = "GROUP_MEAN";
    public static final String HISTORICAL = "HISTORICAL";

    private String algo = HISTORICAL;

    private double[] beta;

    private String[] featureNames;

    private List<String> productTypes = new ArrayList<>();

    private List<Integer> shifts = new ArrayList<>();

    private Map<String, Double> groupMeans = new LinkedHashMap<>();

    private double standbyBaseline;

    private double historicalBaseline;

    private double mae;

    private double rmse;

    private double mape;

    private double r2;

    private int sampleCount;

    private int standbySamples;

    private int outlierCount;

    private int rejectedCount;

    private List<String> fallbackLog = new ArrayList<>();

    public boolean regression() {
        return RIDGE.equals(algo) && beta != null;
    }

    public double predict(Sample sample) {
        return predictDetailed(sample).value();
    }

    public Prediction predictDetailed(Sample sample) {
        if (sample.standby()) {
            if (standbyBaseline > 0) {
                return new Prediction(round(standbyBaseline), Prediction.STANDBY, "停机/检修工况使用待机基线");
            }
            return new Prediction(round(historicalBaseline), Prediction.HISTORICAL, "无待机样本，回退历史正常均值");
        }
        if (regression()) {
            double[] x = encode(sample);
            double value = 0;
            for (int i = 0; i < beta.length; i++) {
                value += beta[i] * x[i];
            }
            return new Prediction(round(Math.max(value, 0)), Prediction.REGRESSION, null);
        }
        if (GROUP_MEAN.equals(algo)) {
            Double mean = groupMeans.get(sample.conditionGroup());
            if (mean != null) {
                return new Prediction(round(mean), Prediction.GROUP_MEAN, "回归样本不足，使用工况分组均值");
            }
        }
        Double anyGroup = groupMeans.get(sample.conditionGroup());
        if (anyGroup != null && GROUP_MEAN.equals(algo)) {
            return new Prediction(round(anyGroup), Prediction.GROUP_MEAN, "使用工况分组均值");
        }
        return new Prediction(round(historicalBaseline), Prediction.HISTORICAL, "工况分组无样本，回退历史正常均值");
    }

    /** 特征编码：截距 + 4 个连续特征 + 产品哑变量 + 班次哑变量 + 运行标志 */
    public double[] encode(Sample sample) {
        List<Double> values = new ArrayList<>();
        values.add(1.0);
        values.add(sample.outputQty());
        values.add(sample.runMinutes());
        values.add(sample.loadRate());
        values.add(sample.ambientTemp());
        for (int i = 1; i < productTypes.size(); i++) {
            values.add(productTypes.get(i).equals(sample.productType()) ? 1.0 : 0.0);
        }
        for (int i = 1; i < shifts.size(); i++) {
            values.add(shifts.get(i) == sample.shift() ? 1.0 : 0.0);
        }
        values.add(sample.running() ? 1.0 : 0.0);

        double[] array = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    public int featureCount() {
        return beta == null ? featureNames.length : beta.length;
    }

    public Map<String, Object> toParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("algo", algo);
        params.put("beta", beta == null ? List.of() : toList(beta));
        params.put("featureNames", List.of(featureNames));
        params.put("productTypes", productTypes);
        params.put("shifts", shifts);
        params.put("groupMeans", groupMeans);
        params.put("standbyBaseline", standbyBaseline);
        params.put("historicalBaseline", historicalBaseline);
        params.put("mae", round(mae));
        params.put("rmse", round(rmse));
        params.put("mape", round(mape));
        params.put("r2", round(r2));
        params.put("sampleCount", sampleCount);
        params.put("standbySamples", standbySamples);
        params.put("outlierCount", outlierCount);
        params.put("rejectedCount", rejectedCount);
        params.put("fallbackLog", fallbackLog);
        return params;
    }

    private List<Double> toList(double[] values) {
        List<Double> list = new ArrayList<>(values.length);
        for (double value : values) {
            list.add(value);
        }
        return list;
    }

    private double round(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.round(value * 10000.0) / 10000.0;
    }

    public String getAlgo() {
        return algo;
    }

    public void setAlgo(String algo) {
        this.algo = algo;
    }

    public double[] getBeta() {
        return beta;
    }

    public void setBeta(double[] beta) {
        this.beta = beta;
    }

    public String[] getFeatureNames() {
        return featureNames;
    }

    public void setFeatureNames(String[] featureNames) {
        this.featureNames = featureNames;
    }

    public List<String> getProductTypes() {
        return productTypes;
    }

    public void setProductTypes(List<String> productTypes) {
        this.productTypes = productTypes;
    }

    public List<Integer> getShifts() {
        return shifts;
    }

    public void setShifts(List<Integer> shifts) {
        this.shifts = shifts;
    }

    public Map<String, Double> getGroupMeans() {
        return groupMeans;
    }

    public void setGroupMeans(Map<String, Double> groupMeans) {
        this.groupMeans = groupMeans;
    }

    public double getStandbyBaseline() {
        return standbyBaseline;
    }

    public void setStandbyBaseline(double standbyBaseline) {
        this.standbyBaseline = standbyBaseline;
    }

    public double getHistoricalBaseline() {
        return historicalBaseline;
    }

    public void setHistoricalBaseline(double historicalBaseline) {
        this.historicalBaseline = historicalBaseline;
    }

    public double getMae() {
        return mae;
    }

    public void setMae(double mae) {
        this.mae = mae;
    }

    public double getRmse() {
        return rmse;
    }

    public void setRmse(double rmse) {
        this.rmse = rmse;
    }

    public double getMape() {
        return mape;
    }

    public void setMape(double mape) {
        this.mape = mape;
    }

    public double getR2() {
        return r2;
    }

    public void setR2(double r2) {
        this.r2 = r2;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public int getStandbySamples() {
        return standbySamples;
    }

    public void setStandbySamples(int standbySamples) {
        this.standbySamples = standbySamples;
    }

    public int getOutlierCount() {
        return outlierCount;
    }

    public void setOutlierCount(int outlierCount) {
        this.outlierCount = outlierCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public List<String> getFallbackLog() {
        return fallbackLog;
    }

    public void setFallbackLog(List<String> fallbackLog) {
        this.fallbackLog = fallbackLog;
    }
}
