package com.smarten.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.smarten.common.util.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：create_time / update_time / create_by / update_by。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";
    private static final String CREATE_BY = "createBy";
    private static final String UPDATE_BY = "updateBy";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);

        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId != null) {
            this.strictInsertFill(metaObject, CREATE_BY, Long.class, userId);
            this.strictInsertFill(metaObject, UPDATE_BY, Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId != null) {
            this.strictUpdateFill(metaObject, UPDATE_BY, Long.class, userId);
        }
    }
}
