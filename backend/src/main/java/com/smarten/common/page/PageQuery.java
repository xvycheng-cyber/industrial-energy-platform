package com.smarten.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询基类。
 *
 * <p>排序字段需显式传入并限定为 snake_case 标识符，避免拼接不可信 SQL。</p>
 */
@Getter
@Setter
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 每页最大条数，与 MyBatis-Plus 分页插件上限保持一致 */
    public static final int MAX_PAGE_SIZE = 500;

    @Min(value = 1, message = "页码必须大于等于 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数不能超过 " + MAX_PAGE_SIZE)
    private long pageSize = 20;

    /** 排序字段，仅允许字母、数字、下划线 */
    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "排序字段不合法")
    private String orderBy;

    /** 是否升序，默认降序 */
    private boolean asc = false;

    public <T> Page<T> toPage() {
        return new Page<>(pageNum, pageSize);
    }
}
