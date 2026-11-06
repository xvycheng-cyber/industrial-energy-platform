package com.smarten.common;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.api.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一响应结构单元测试。
 */
class ResultTest {

    @Test
    void okShouldCarryDataAndSuccessCode() {
        Result<String> result = Result.ok("payload");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("payload");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    void failShouldCarryBusinessCode() {
        Result<Void> result = Result.fail(ResultCode.DEVICE_NOT_FOUND);
        assertThat(result.getCode()).isEqualTo(2004);
        assertThat(result.getMessage()).isEqualTo("设备不存在");
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void pageResultShouldComputePages() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 45, 2, 20);
        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.getTotal()).isEqualTo(45);
        assertThat(page.getPageNum()).isEqualTo(2);
        assertThat(page.getPages()).isEqualTo(3);
    }

    @Test
    void emptyPageShouldHaveZeroPages() {
        PageResult<String> page = PageResult.empty(1, 20);
        assertThat(page.getRecords()).isEmpty();
        assertThat(page.getTotal()).isZero();
        assertThat(page.getPages()).isZero();
    }
}
