package com.smarten.module.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "企业保存参数")
public class EnterpriseDTO {

    @NotBlank(message = "企业编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$", message = "企业编码只能包含字母、数字、下划线、中划线")
    private String code;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128)
    private String name;

    @Size(max = 64)
    private String industry;

    @Size(max = 64)
    private String region;

    @Size(max = 255)
    private String address;

    @Size(max = 64)
    private String contactName;

    @Pattern(regexp = "^$|^[0-9-]{6,20}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Min(0)
    @Max(1)
    private Integer status;

    @Size(max = 255)
    private String remark;
}
