package com.smarten.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.common.util.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * 企业级数据权限。
 *
 * <p>规则：平台管理员（PLATFORM_ADMIN）不受限；其余用户只能访问
 * {@code enterprise_id = 自己所属企业} 的数据。所有校验在服务端完成，不依赖前端。</p>
 */
@Component
public class DataScopeHelper {

    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    /** 是否平台管理员 */
    public boolean isPlatformAdmin() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser != null && loginUser.roles().contains(PLATFORM_ADMIN);
    }

    /** 当前用户所属企业；平台管理员可能为空 */
    public Long currentEnterpriseId() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser == null ? null : loginUser.enterpriseId();
    }

    /** 当前用户所属企业，未绑定则拒绝 */
    public Long requireEnterpriseId() {
        Long enterpriseId = currentEnterpriseId();
        if (enterpriseId == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号未绑定企业，无法访问企业数据");
        }
        return enterpriseId;
    }

    /** 解析请求中的企业 ID：非平台管理员强制为自己的企业 */
    public Long resolveEnterpriseId(Long requested) {
        if (isPlatformAdmin()) {
            return requested;
        }
        return requireEnterpriseId();
    }

    /** 校验资源归属，越权直接拒绝 */
    public void checkAccessible(Long enterpriseId) {
        if (isPlatformAdmin()) {
            return;
        }
        Long own = requireEnterpriseId();
        if (enterpriseId == null || !own.equals(enterpriseId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问其他企业的数据");
        }
    }

    /** 给查询条件追加企业范围 */
    public <T> void applyScope(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> enterpriseColumn) {
        if (!isPlatformAdmin()) {
            wrapper.eq(enterpriseColumn, requireEnterpriseId());
        }
    }
}
