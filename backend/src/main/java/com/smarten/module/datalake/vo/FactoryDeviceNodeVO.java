package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "三维设备节点")
public class FactoryDeviceNodeVO {

    private Long id;

    private String code;

    private String name;

    private String deviceType;

    /** 1 运行 / 2 停机 / 3 检修 / 0 停用 */
    private Integer status;

    /** NORMAL / LOW / MEDIUM / HIGH / OFFLINE */
    private String risk;

    private String riskLabel;

    private String riskReason;

    private BigDecimal energyToday;

    private Vec3VO position;

}
