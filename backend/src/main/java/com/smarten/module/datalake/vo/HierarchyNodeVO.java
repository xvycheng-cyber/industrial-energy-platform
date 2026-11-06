package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "层级树节点（企业→车间→产线→设备）")
public class HierarchyNodeVO {

    private Long id;

    /** enterprise / workshop / line / device */
    private String type;

    private String name;

    private String code;

    private List<HierarchyNodeVO> children = new ArrayList<>();
}
