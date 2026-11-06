package com.smarten.module.rectification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smarten.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rectification_feedback")
public class RectificationFeedback extends BaseEntity {

    private Long taskId;

    /** 1 进度 2 完工 3 审核 4 驳回 5 效果验证 */
    private Integer feedbackType;

    private String content;

    private String beforeDesc;

    private String afterDesc;

    private String imagesJson;

    private BigDecimal actualSaving;

    private Long submitBy;

    private LocalDateTime submitTime;

    private Long verifyBy;

    private LocalDateTime verifyTime;

    private Integer verifyResult;

    private String verifyRemark;
}
