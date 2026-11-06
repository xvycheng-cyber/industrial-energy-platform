package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "三维车间/厂房节点")
public class FactoryWorkshopNodeVO {

    private Long id;

    private String code;

    private String name;

    private Vec3VO position;

    private double width;

    private double depth;

    private double height;

    private BigDecimal energyToday;

    private List<FactoryLineNodeVO> lines = new ArrayList<>();
}
