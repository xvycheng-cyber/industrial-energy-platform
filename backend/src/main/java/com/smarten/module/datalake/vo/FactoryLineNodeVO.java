package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "三维产线节点")
public class FactoryLineNodeVO {

    private Long id;

    private String code;

    private String name;

    private BigDecimal energyToday;

    private Vec3VO position;

    private List<FactoryDeviceNodeVO> devices = new ArrayList<>();
}
