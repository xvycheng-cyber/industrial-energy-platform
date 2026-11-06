package com.smarten.module.baseline;

import com.smarten.module.baseline.algorithm.BaselineModel;
import com.smarten.module.baseline.algorithm.BaselineTrainer;
import com.smarten.module.baseline.algorithm.Prediction;
import com.smarten.module.baseline.algorithm.Sample;
import com.smarten.module.baseline.algorithm.TrainOptions;
import com.smarten.module.baseline.algorithm.TrainResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 动态基线算法：工况敏感性、产品切换、停机待机、异常样本剔除、样本不足回退。
 */
class BaselineTrainerTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 1, 0, 0);

    private Sample sample(int hour, double output, double runMinutes, double load, double temp,
                          String product, int shift, int status, double energy) {
        return new Sample(1L, BASE.plusHours(hour), output, runMinutes, load, temp, product, shift, status, energy);
    }

    /** 真实规律：能耗 = 2 + 0.35*产量 + 0.9*负荷 + 0.05*温度 + 产品/班次差异 */
    private double truth(double output, double load, double temp, String product, int shift) {
        double value = 2.0 + 0.35 * output + 0.9 * load + 0.05 * temp;
        if ("B型支架".equals(product)) {
            value += 4.0;
        }
        if (shift == 3) {
            value += 1.5;
        }
        return value;
    }

    private List<Sample> dataset(int count, String product, int shift, double load, double output) {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double temp = 20 + (i % 5);
            samples.add(sample(i, output, 60, load, temp, product, shift, 1,
                    truth(output, load, temp, product, shift)));
        }
        return samples;
    }

    @Test
    @DisplayName("回归可还原工况敏感关系，评分达标")
    void regressionFitsConditionSensitivity() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            double load = 40 + (i % 5) * 10;
            double output = 20 + (i % 7) * 3;
            double temp = 15 + (i % 6) * 2;
            samples.add(sample(i, output, 60, load, temp, "A型壳体", i % 3 + 1, 1,
                    truth(output, load, temp, "A型壳体", i % 3 + 1)));
        }

        TrainResult result = new BaselineTrainer().train(samples, TrainOptions.defaults());

        assertThat(result.model().getAlgo()).isEqualTo(BaselineModel.RIDGE);
        assertThat(result.model().getR2()).isGreaterThan(0.98);
        assertThat(result.model().getMae()).isLessThan(1.0);
        assertThat(result.normalSamples()).isEqualTo(80);
    }

    @Test
    @DisplayName("同一设备低负荷与高负荷得到不同基线")
    void differentLoadDifferentBaseline() {
        List<Sample> samples = new ArrayList<>();
        samples.addAll(dataset(30, "A型壳体", 1, 35, 30));
        samples.addAll(dataset(30, "A型壳体", 1, 95, 30));

        BaselineModel model = new BaselineTrainer().train(samples, TrainOptions.defaults()).model();

        double low = model.predict(sample(100, 30, 60, 35, 20, "A型壳体", 1, 1, 0));
        double high = model.predict(sample(101, 30, 60, 95, 20, "A型壳体", 1, 1, 0));

        assertThat(high).isGreaterThan(low + 30);
        assertThat(high - low).isCloseTo(0.9 * 60, org.assertj.core.data.Offset.offset(25.0));
    }

    @Test
    @DisplayName("产品切换会得到不同基线")
    void productSwitchDifferentBaseline() {
        List<Sample> samples = new ArrayList<>();
        samples.addAll(dataset(40, "A型壳体", 1, 70, 40));
        samples.addAll(dataset(40, "B型支架", 1, 70, 40));

        BaselineModel model = new BaselineTrainer().train(samples, TrainOptions.defaults()).model();

        double a = model.predict(sample(200, 40, 60, 70, 20, "A型壳体", 1, 1, 0));
        double b = model.predict(sample(201, 40, 60, 70, 20, "B型支架", 1, 1, 0));

        assertThat(b - a).isCloseTo(4.0, org.assertj.core.data.Offset.offset(1.5));
    }

    @Test
    @DisplayName("停机工况使用待机基线，不落入运行回归")
    void standbyUsesStandbyBaseline() {
        List<Sample> samples = new ArrayList<>(dataset(40, "A型壳体", 1, 76, 45));
        for (int i = 0; i < 10; i++) {
            samples.add(sample(300 + i, 0, 0, 0, 22, "A型壳体", 3, 2, 1.2));
        }

        TrainResult result = new BaselineTrainer().train(samples, TrainOptions.defaults());
        BaselineModel model = result.model();

        Prediction prediction = model.predictDetailed(sample(400, 0, 0, 0, 22, "A型壳体", 3, 2, 0));

        assertThat(prediction.baselineType()).isEqualTo(Prediction.STANDBY);
        assertThat(prediction.value()).isCloseTo(1.2, org.assertj.core.data.Offset.offset(0.2));
        assertThat(model.getStandbySamples()).isEqualTo(10);
        assertThat(prediction.value()).isLessThan(model.predict(sample(401, 45, 60, 76, 22, "A型壳体", 1, 1, 0)));
    }

    @Test
    @DisplayName("异常高耗样本被剔除，不污染正常基线")
    void anomaliesAreRejected() {
        List<Sample> clean = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            double load = 50 + (i % 4) * 10;
            double output = 30 + (i % 5) * 2;
            clean.add(sample(i, output, 60, load, 20, "A型壳体", 1, 1,
                    truth(output, load, 20, "A型壳体", 1)));
        }
        List<Sample> polluted = new ArrayList<>(clean);
        for (int i = 0; i < 6; i++) {
            polluted.add(sample(500 + i, 30, 60, 90, 20, "A型壳体", 1, 1,
                    truth(30, 90, 20, "A型壳体", 1) + 120));
        }

        TrainResult cleanResult = new BaselineTrainer().train(clean, TrainOptions.defaults());
        TrainResult pollutedResult = new BaselineTrainer().train(polluted, TrainOptions.defaults());

        assertThat(pollutedResult.outliers()).isGreaterThanOrEqualTo(5);
        assertThat(pollutedResult.model().getAlgo()).isEqualTo(BaselineModel.RIDGE);
        // 被污染模型对正常工况的预测与干净模型差异很小
        double reference = cleanResult.model().predict(sample(600, 30, 60, 70, 20, "A型壳体", 1, 1, 0));
        double actual = pollutedResult.model().predict(sample(600, 30, 60, 70, 20, "A型壳体", 1, 1, 0));
        assertThat(Math.abs(actual - reference)).isLessThan(2.0);
    }

    @Test
    @DisplayName("样本不足时回退工况分组均值并记录原因")
    void insufficientSamplesFallbackToGroupMean() {
        List<Sample> samples = new ArrayList<>();
        samples.addAll(dataset(8, "A型壳体", 1, 55, 25));
        samples.addAll(dataset(8, "B型支架", 1, 90, 25));

        TrainResult result = new BaselineTrainer().train(samples, TrainOptions.defaults());
        BaselineModel model = result.model();

        assertThat(model.getAlgo()).isIn(BaselineModel.GROUP_MEAN, BaselineModel.HISTORICAL);
        assertThat(model.getFallbackLog()).isNotEmpty();
        assertThat(result.normalSamples()).isLessThan(result.requiredSamples());
        if (BaselineModel.GROUP_MEAN.equals(model.getAlgo())) {
            Prediction prediction = model.predictDetailed(sample(700, 25, 60, 55, 20, "A型壳体", 1, 1, 0));
            assertThat(prediction.baselineType()).isEqualTo(Prediction.GROUP_MEAN);
            assertThat(prediction.fallbackReason()).isNotBlank();
        }
    }

    @Test
    @DisplayName("无分组样本时回退历史正常均值")
    void unseenGroupFallsBackToHistorical() {
        List<Sample> samples = new ArrayList<>(dataset(6, "A型壳体", 1, 50, 20));
        BaselineModel model = new BaselineTrainer().train(samples, TrainOptions.defaults()).model();

        Prediction prediction = model.predictDetailed(sample(800, 20, 60, 95, 20, "C型新品", 2, 1, 0));

        assertThat(prediction.baselineType()).isIn(Prediction.GROUP_MEAN, Prediction.HISTORICAL);
        if (prediction.baselineType() == Prediction.HISTORICAL) {
            assertThat(prediction.fallbackReason()).contains("历史");
        }
    }
}
