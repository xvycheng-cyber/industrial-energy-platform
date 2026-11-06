package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "三维坐标")
public class Vec3VO {

    private double x;

    private double y;

    private double z;

    public Vec3VO() {
    }

    public Vec3VO(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
