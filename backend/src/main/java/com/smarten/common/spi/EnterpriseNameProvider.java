package com.smarten.common.spi;

import java.util.Collection;
import java.util.Map;

/**
 * 企业名称查询（由 masterdata 模块实现，system 模块只依赖接口，避免模块反向依赖）。
 */
public interface EnterpriseNameProvider {

    /**
     * 批量查询企业名称。
     *
     * @param enterpriseIds 企业 ID 集合，可为空
     * @return id -> name
     */
    Map<Long, String> findNames(Collection<Long> enterpriseIds);
}
