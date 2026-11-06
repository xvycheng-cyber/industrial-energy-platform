package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "三维工厂场景")
public class FactorySceneVO {

    private Long enterpriseId;

    private String enterpriseName;

    private double width;

    private double depth;

    private int workshopCount;

    private int lineCount;

    private int deviceCount;

    private BigDecimal energyToday;

    private String dataTime;

    private List<FactoryWorkshopNodeVO> workshops = new ArrayList<>();
}
