package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "综合能源评价")
public class RatingVO {

    private Integer score;

    /** A / B / C / D */
    private String grade;

    private String basis;

    private List<String> dimensions = new ArrayList<>();
}
