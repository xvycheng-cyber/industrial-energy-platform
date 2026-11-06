package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "文件导入结果")
public class ImportResultVO {

    private String batchNo;

    private int totalRows;

    private int successRows;

    private int failedRows;

    private List<String> errors = new ArrayList<>();
}
