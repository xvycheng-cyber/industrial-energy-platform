-- =============================================================================
-- 智能耗 · 工业企业能耗画像与节能诊断平台
-- 数据库初始化脚本（结构）
-- 库：smarten_energy    MySQL 8.0+
--
-- 约定：
--   1. 脚本可重复执行：全部使用 CREATE TABLE IF NOT EXISTS。
--   2. 不使用物理外键，采用「逻辑外键 + 索引 + 应用层校验」，便于分库与批量导入。
--   3. 所有表含 id / create_time / update_time / create_by / update_by / deleted。
--   4. 能耗 DECIMAL(18,4)，金额 DECIMAL(18,2)，比率 DECIMAL(9,4)。
--   5. 时间统一 DATETIME，服务端时区 Asia/Shanghai。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 系统：角色
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code       VARCHAR(64)  NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(64)  NOT NULL COMMENT '角色名称',
    description     VARCHAR(255) NULL COMMENT '描述',
    permissions     VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '权限码，逗号分隔，* 表示全部',
    sort_no         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    builtin         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否内置：1是 0否（内置不可删除）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL COMMENT '创建人',
    update_by       BIGINT       NULL COMMENT '更新人',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色';

-- -----------------------------------------------------------------------------
-- 2. 系统：用户
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(64)  NOT NULL COMMENT '登录名',
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    real_name       VARCHAR(64)  NULL COMMENT '姓名',
    phone           VARCHAR(20)  NULL COMMENT '手机号',
    email           VARCHAR(128) NULL COMMENT '邮箱',
    avatar          VARCHAR(255) NULL COMMENT '头像',
    enterprise_id   BIGINT       NULL COMMENT '所属企业，平台管理员为空',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    last_login_time DATETIME     NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(64)  NULL COMMENT '最后登录 IP',
    remark          VARCHAR(255) NULL COMMENT '备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL COMMENT '创建人',
    update_by       BIGINT       NULL COMMENT '更新人',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_enterprise (enterprise_id),
    KEY idx_user_phone (phone),
    KEY idx_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户';

-- -----------------------------------------------------------------------------
-- 3. 系统：用户角色关联
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    role_id     BIGINT   NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_ur_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联';

-- -----------------------------------------------------------------------------
-- 4. 主数据：企业
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_enterprise (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code          VARCHAR(64)  NOT NULL COMMENT '企业编码',
    name          VARCHAR(128) NOT NULL COMMENT '企业名称',
    industry      VARCHAR(64)  NULL COMMENT '所属行业',
    region        VARCHAR(64)  NULL COMMENT '所在地区',
    address       VARCHAR(255) NULL COMMENT '详细地址',
    contact_name  VARCHAR(64)  NULL COMMENT '联系人',
    contact_phone VARCHAR(20)  NULL COMMENT '联系电话',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    remark        VARCHAR(255) NULL COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ent_code (code),
    KEY idx_ent_name (name),
    KEY idx_ent_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企业';

-- -----------------------------------------------------------------------------
-- 5. 主数据：车间
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_workshop (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id BIGINT       NOT NULL COMMENT '所属企业',
    code          VARCHAR(64)  NOT NULL COMMENT '车间编码（企业内唯一）',
    name          VARCHAR(128) NOT NULL COMMENT '车间名称',
    manager       VARCHAR(64)  NULL COMMENT '车间负责人',
    area          DECIMAL(12,2) NULL COMMENT '面积（㎡）',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    remark        VARCHAR(255) NULL COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ws_ent_code (enterprise_id, code),
    KEY idx_ws_enterprise (enterprise_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车间';

-- -----------------------------------------------------------------------------
-- 6. 主数据：产线
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_line (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id   BIGINT       NOT NULL COMMENT '所属企业',
    workshop_id     BIGINT       NOT NULL COMMENT '所属车间',
    code            VARCHAR(64)  NOT NULL COMMENT '产线编码（企业内唯一）',
    name            VARCHAR(128) NOT NULL COMMENT '产线名称',
    product_type    VARCHAR(64)  NULL COMMENT '主要产品类型',
    design_capacity DECIMAL(18,4) NULL COMMENT '设计产能',
    capacity_unit   VARCHAR(16)  NULL COMMENT '产能单位',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    remark          VARCHAR(255) NULL COMMENT '备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL COMMENT '创建人',
    update_by       BIGINT       NULL COMMENT '更新人',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_line_ent_code (enterprise_id, code),
    KEY idx_line_workshop (workshop_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产线';

-- -----------------------------------------------------------------------------
-- 7. 主数据：能源类型
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(32)  NOT NULL COMMENT '编码：ELEC/WATER/GAS/STEAM/AIR',
    name        VARCHAR(64)  NOT NULL COMMENT '名称',
    unit        VARCHAR(16)  NOT NULL COMMENT '计量单位',
    price       DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '参考单价（元/单位）',
    co2_factor  DECIMAL(12,4) NULL COMMENT '碳排放因子',
    coal_factor DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '折标煤系数 kgce/单位',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL COMMENT '创建人',
    update_by   BIGINT       NULL COMMENT '更新人',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_et_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '能源类型';

-- -----------------------------------------------------------------------------
-- 8. 主数据：设备
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_device (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id   BIGINT       NOT NULL COMMENT '所属企业',
    workshop_id     BIGINT       NOT NULL COMMENT '所属车间',
    line_id         BIGINT       NOT NULL COMMENT '所属产线',
    code            VARCHAR(64)  NOT NULL COMMENT '设备编码（企业内唯一）',
    name            VARCHAR(128) NOT NULL COMMENT '设备名称',
    device_type     VARCHAR(64)  NOT NULL COMMENT '设备类型',
    model           VARCHAR(64)  NULL COMMENT '型号，用于同类对比',
    rated_power     DECIMAL(12,4) NULL COMMENT '额定功率 kW',
    energy_type_id  BIGINT       NULL COMMENT '主要能源类型',
    commission_date DATE         NULL COMMENT '投运日期',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1运行 2停机 3检修 0停用',
    remark          VARCHAR(255) NULL COMMENT '备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL COMMENT '创建人',
    update_by       BIGINT       NULL COMMENT '更新人',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dev_ent_code (enterprise_id, code),
    KEY idx_dev_line (line_id),
    KEY idx_dev_type (enterprise_id, device_type, model),
    KEY idx_dev_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备';

-- -----------------------------------------------------------------------------
-- 9. 主数据：计量点（仪表）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_meter (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id BIGINT       NOT NULL COMMENT '所属企业',
    device_id     BIGINT       NULL COMMENT '绑定设备，为空表示产线/车间总表',
    line_id       BIGINT       NULL COMMENT '绑定产线',
    meter_code    VARCHAR(64)  NOT NULL COMMENT '仪表编号',
    meter_name    VARCHAR(128) NOT NULL COMMENT '仪表名称',
    energy_type_id BIGINT      NOT NULL COMMENT '能源类型',
    ratio         DECIMAL(12,4) NOT NULL DEFAULT 1 COMMENT '倍率',
    collect_interval_seconds INT NOT NULL DEFAULT 60 COMMENT '采集间隔（秒），用于计算应采点数',
    reading_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '读数类型：1累计读数 2周期增量',
    install_time  DATETIME     NULL COMMENT '安装时间',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    remark        VARCHAR(255) NULL COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meter_code (meter_code),
    KEY idx_meter_device (device_id),
    KEY idx_meter_line (line_id),
    KEY idx_meter_enterprise (enterprise_id, energy_type_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '计量点（仪表）';

-- -----------------------------------------------------------------------------
-- 10. 数据湖：原始采集数据
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_raw_data (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id BIGINT      NOT NULL COMMENT '所属企业',
    meter_id     BIGINT       NOT NULL COMMENT '仪表ID',
    device_id    BIGINT       NULL COMMENT '设备ID（冗余）',
    collect_time DATETIME     NOT NULL COMMENT '采集时间点',
    point_code   VARCHAR(64)  NOT NULL DEFAULT 'TOTAL' COMMENT '点位编码',
    raw_value    DECIMAL(18,4) NOT NULL COMMENT '原始读数（未换算）',
    raw_unit     VARCHAR(16)  NULL COMMENT '原始单位',
    reading_type TINYINT      NOT NULL DEFAULT 1 COMMENT '读数类型：1累计读数 2周期增量',
    received_time DATETIME    NULL COMMENT '接收时间',
    quality      TINYINT      NOT NULL DEFAULT 1 COMMENT '质量：1正常 2可疑 3无效',
    source       VARCHAR(32)  NOT NULL DEFAULT 'API' COMMENT '来源：GATEWAY/IMPORT/MANUAL/API',
    batch_no     VARCHAR(64)  NOT NULL COMMENT '采集批次号',
    clean_status TINYINT      NOT NULL DEFAULT 0 COMMENT '清洗状态：0待清洗 1已清洗 2已忽略',
    ext_json     JSON         NULL COMMENT '原始报文扩展',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    BIGINT       NULL COMMENT '创建人',
    update_by    BIGINT       NULL COMMENT '更新人',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_raw_meter_point_time (meter_id, collect_time, point_code),
    KEY idx_raw_time (collect_time),
    KEY idx_raw_batch (batch_no),
    KEY idx_raw_clean (clean_status, collect_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '原始采集数据（只追加）';

-- -----------------------------------------------------------------------------
-- 11. 数据湖：能耗明细
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_detail (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id  BIGINT       NOT NULL COMMENT '所属企业',
    meter_id       BIGINT       NOT NULL COMMENT '仪表ID',
    device_id      BIGINT       NULL COMMENT '设备ID',
    line_id        BIGINT       NULL COMMENT '产线ID',
    energy_type_id BIGINT       NOT NULL COMMENT '能源类型',
    period_start   DATETIME     NOT NULL COMMENT '区间开始（含）',
    period_end     DATETIME     NOT NULL COMMENT '区间结束（不含）',
    energy_value   DECIMAL(18,4) NOT NULL COMMENT '折算后能耗',
    unit           VARCHAR(16)  NOT NULL COMMENT '单位',
    start_reading  DECIMAL(18,4) NULL COMMENT '区间起始读数',
    end_reading    DECIMAL(18,4) NULL COMMENT '区间结束读数',
    estimated      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否补录估算：1是',
    quality_flag   TINYINT      NOT NULL DEFAULT 1 COMMENT '质量：1正常 2越界 3缺失补全 4重复 5人工修正',
    source_count   INT          NOT NULL DEFAULT 0 COMMENT '参与计算的原始点数',
    calc_version   VARCHAR(32)  NULL COMMENT '清洗算法版本',
    remark         VARCHAR(255) NULL COMMENT '备注',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL COMMENT '创建人',
    update_by      BIGINT       NULL COMMENT '更新人',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_detail_meter_period (meter_id, period_start, period_end),
    KEY idx_detail_device_period (device_id, period_start),
    KEY idx_detail_ent_period (enterprise_id, period_start),
    KEY idx_detail_quality (quality_flag)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '能耗明细';

-- -----------------------------------------------------------------------------
-- 12. 数据湖：能耗聚合
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_aggregate (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id  BIGINT       NOT NULL COMMENT '所属企业',
    dim_type       TINYINT      NOT NULL COMMENT '维度：1企业 2车间 3产线 4设备',
    dim_id         BIGINT       NOT NULL COMMENT '维度对象ID',
    energy_type_id BIGINT       NOT NULL COMMENT '能源类型',
    period_type    TINYINT      NOT NULL COMMENT '粒度：1小时 2日 3月',
    period_start   DATETIME     NOT NULL COMMENT '周期开始',
    energy_value   DECIMAL(18,4) NOT NULL COMMENT '聚合能耗',
    cost_amount    DECIMAL(18,2) NULL COMMENT '折算金额',
    data_count     INT          NOT NULL DEFAULT 0 COMMENT '有效明细数',
    missing_count  INT          NOT NULL DEFAULT 0 COMMENT '缺失时段数',
    calc_version   VARCHAR(32)  NULL COMMENT '聚合版本',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL COMMENT '创建人',
    update_by      BIGINT       NULL COMMENT '更新人',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_agg_dim_period (dim_type, dim_id, energy_type_id, period_type, period_start),
    KEY idx_agg_ent_period (enterprise_id, period_type, period_start),
    KEY idx_agg_period (period_start)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '能耗聚合';

-- -----------------------------------------------------------------------------
-- 13. 数据湖：采集入库日志
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_ingestion_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no       VARCHAR(64)  NOT NULL COMMENT '批次号',
    enterprise_id  BIGINT       NULL COMMENT '企业',
    meter_id       BIGINT       NULL COMMENT '仪表',
    data_date      DATE         NOT NULL COMMENT '数据归属日期',
    source         VARCHAR(32)  NOT NULL DEFAULT 'API' COMMENT '来源',
    expected_count INT          NOT NULL DEFAULT 0 COMMENT '应采点数',
    received_count INT          NOT NULL DEFAULT 0 COMMENT '实收点数',
    valid_count    INT          NOT NULL DEFAULT 0 COMMENT '有效点数',
    missing_count  INT          NOT NULL DEFAULT 0 COMMENT '缺失点数',
    success_rate   DECIMAL(9,4) NOT NULL DEFAULT 0 COMMENT '完整率（%）',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0进行中 1成功 2部分成功 3失败',
    error_msg      VARCHAR(1000) NULL COMMENT '错误摘要',
    start_time     DATETIME     NULL COMMENT '开始时间',
    end_time       DATETIME     NULL COMMENT '结束时间',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL COMMENT '创建人',
    update_by      BIGINT       NULL COMMENT '更新人',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_batch (batch_no),
    KEY idx_log_ent_date (enterprise_id, data_date),
    KEY idx_log_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采集入库日志';

-- -----------------------------------------------------------------------------
-- 14. 工况：生产记录
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS production_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id BIGINT       NOT NULL COMMENT '所属企业',
    workshop_id   BIGINT       NULL COMMENT '车间',
    line_id       BIGINT       NULL COMMENT '产线',
    device_id     BIGINT       NULL COMMENT '设备',
    record_date   DATE         NOT NULL COMMENT '生产日期',
    shift         TINYINT      NOT NULL COMMENT '班次：1白 2中 3夜',
    start_time    DATETIME     NOT NULL COMMENT '班次开始',
    end_time      DATETIME     NOT NULL COMMENT '班次结束',
    product_type  VARCHAR(64)  NULL COMMENT '产品类型',
    output_qty    DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '产量',
    output_unit   VARCHAR(16)  NULL COMMENT '产量单位',
    run_minutes   INT          NOT NULL DEFAULT 0 COMMENT '运行时长（分钟）',
    load_rate     DECIMAL(5,2) NULL COMMENT '负荷率 0-100',
    ambient_temp  DECIMAL(6,2) NULL COMMENT '环境温度℃',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 2停机 3检修',
    data_source   VARCHAR(32)  NULL COMMENT '数据来源：MES/MANUAL/IMPORT',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_prod_device_shift (device_id, start_time, shift),
    KEY idx_prod_line_date (line_id, record_date),
    KEY idx_prod_ent_date (enterprise_id, record_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '生产工况';

-- -----------------------------------------------------------------------------
-- 15. 基线：动态基线模型
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_baseline (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id  BIGINT       NOT NULL COMMENT '所属企业',
    dim_type       TINYINT      NOT NULL COMMENT '维度：3产线 4设备',
    dim_id         BIGINT       NOT NULL COMMENT '维度对象ID',
    energy_type_id BIGINT       NOT NULL COMMENT '能源类型',
    granularity    TINYINT      NOT NULL COMMENT '粒度：1日 2班次 3小时',
    baseline_type  TINYINT      NOT NULL COMMENT '类型：1回归 2分组均值 3产线缩放 4全局均值',
    model_version  VARCHAR(64)  NOT NULL COMMENT '模型版本',
    algo           VARCHAR(32)  NULL COMMENT '算法：OLS/RIDGE/GROUP_MEAN',
    sample_count   INT          NOT NULL DEFAULT 0 COMMENT '训练样本数',
    train_start    DATE         NULL COMMENT '训练窗口开始',
    train_end      DATE         NULL COMMENT '训练窗口结束',
    r2             DECIMAL(9,6) NULL COMMENT '决定系数',
    mae            DECIMAL(18,4) NULL COMMENT '平均绝对误差',
    rmse           DECIMAL(18,4) NULL COMMENT '均方根误差',
    mape           DECIMAL(9,4) NULL COMMENT '平均绝对百分比误差',
    fallback_count INT          NOT NULL DEFAULT 0 COMMENT '回退预测次数',
    sample_rejected INT         NOT NULL DEFAULT 0 COMMENT '清洗拒绝样本数',
    outlier_count  INT          NOT NULL DEFAULT 0 COMMENT '异常值剔除数',
    params_json    JSON         NULL COMMENT '模型参数',
    feature_json   JSON         NULL COMMENT '特征定义',
    confidence     TINYINT      NOT NULL DEFAULT 2 COMMENT '置信度：1高 2中 3低',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1生效 2归档',
    effective_from DATETIME     NULL COMMENT '生效时间',
    effective_to   DATETIME     NULL COMMENT '失效时间',
    calc_time      DATETIME     NULL COMMENT '计算完成时间',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL COMMENT '创建人',
    update_by      BIGINT       NULL COMMENT '更新人',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bl_dim_version (dim_type, dim_id, energy_type_id, granularity, model_version),
    KEY idx_bl_dim_status (dim_type, dim_id, status),
    KEY idx_bl_ent (enterprise_id, granularity)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动态基线模型';

-- -----------------------------------------------------------------------------
-- 16. 基线：基线明细与偏差
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_baseline_detail (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    baseline_id    BIGINT       NOT NULL COMMENT '基线模型ID',
    enterprise_id  BIGINT       NOT NULL COMMENT '所属企业',
    dim_type       TINYINT      NOT NULL COMMENT '维度类型',
    dim_id         BIGINT       NOT NULL COMMENT '维度对象ID',
    energy_type_id BIGINT       NOT NULL COMMENT '能源类型',
    period_start   DATETIME     NOT NULL COMMENT '周期开始',
    period_end     DATETIME     NOT NULL COMMENT '周期结束',
    baseline_value DECIMAL(18,4) NOT NULL COMMENT '基线预测值',
    actual_value   DECIMAL(18,4) NULL COMMENT '实际能耗',
    deviation_value DECIMAL(18,4) NULL COMMENT '绝对偏差',
    deviation_rate DECIMAL(9,4) NULL COMMENT '偏差率（%）',
    baseline_type  TINYINT      NULL COMMENT '本次预测类型：1回归 2分组均值 4历史均值 5待机',
    model_version  VARCHAR(64)  NULL COMMENT '模型版本',
    condition_json JSON         NULL COMMENT '工况参数快照',
    fallback_reason VARCHAR(255) NULL COMMENT '回退原因',
    estimated      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否工况缺失推算：1是',
    comparable     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可比较：1是 0否',
    computed_at    DATETIME     NULL COMMENT '计算时间',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL COMMENT '创建人',
    update_by      BIGINT       NULL COMMENT '更新人',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bld_baseline_period (baseline_id, period_start),
    KEY idx_bld_dim_period (dim_type, dim_id, period_start),
    KEY idx_bld_ent_period (enterprise_id, period_start)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '基线明细与偏差';

-- -----------------------------------------------------------------------------
-- 17. 诊断：异常事件
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_anomaly (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id BIGINT       NOT NULL COMMENT '所属企业',
    workshop_id   BIGINT       NULL COMMENT '车间',
    line_id       BIGINT       NULL COMMENT '产线',
    device_id     BIGINT       NOT NULL COMMENT '设备',
    anomaly_type  VARCHAR(32)  NOT NULL COMMENT '异常类型',
    severity      TINYINT      NOT NULL DEFAULT 1 COMMENT '严重级别：1低 2中 3高 4严重',
    period_start  DATETIME     NOT NULL COMMENT '异常区间开始',
    period_end    DATETIME     NOT NULL COMMENT '异常区间结束',
    actual_value  DECIMAL(18,4) NULL COMMENT '实际值',
    baseline_value DECIMAL(18,4) NULL COMMENT '基线值',
    deviation_rate DECIMAL(9,4) NULL COMMENT '偏差率',
    threshold_value DECIMAL(18,4) NULL COMMENT '触发阈值',
    rule_code     VARCHAR(64)  NULL COMMENT '命中检测规则',
    evidence_json JSON         NULL COMMENT '证据',
    detect_time   DATETIME     NOT NULL COMMENT '检出时间',
    detect_batch  VARCHAR(64)  NULL COMMENT '检测批次',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待确认 1已确认 2误报 3已转诊断 4已关闭',
    confirm_by    BIGINT       NULL COMMENT '确认人',
    confirm_time  DATETIME     NULL COMMENT '确认时间',
    remark        VARCHAR(500) NULL COMMENT '说明',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ano_device_type_period (device_id, anomaly_type, period_start),
    KEY idx_ano_ent_detect (enterprise_id, detect_time),
    KEY idx_ano_status (status),
    KEY idx_ano_type (anomaly_type),
    KEY idx_ano_device_period (device_id, period_start)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '异常事件';

-- -----------------------------------------------------------------------------
-- 18. 诊断：诊断规则
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS diagnosis_rule (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_code           VARCHAR(64)  NOT NULL COMMENT '规则编码',
    rule_name           VARCHAR(128) NOT NULL COMMENT '规则名称',
    anomaly_type        VARCHAR(32)  NOT NULL COMMENT '适用异常类型',
    condition_json      JSON         NOT NULL COMMENT '条件树',
    threshold_json      JSON         NULL COMMENT '阈值参数',
    weight              DECIMAL(5,2) NOT NULL DEFAULT 1 COMMENT '权重',
    cause_template      VARCHAR(500) NULL COMMENT '原因模板',
    suggestion_template VARCHAR(1000) NULL COMMENT '建议模板',
    saving_formula      VARCHAR(255) NULL COMMENT '节能量公式',
    priority            INT          NOT NULL DEFAULT 100 COMMENT '优先级',
    version             INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    enabled             TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    enterprise_id       BIGINT       NULL COMMENT '企业，为空表示全局规则',
    remark              VARCHAR(255) NULL COMMENT '备注',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT       NULL COMMENT '创建人',
    update_by           BIGINT       NULL COMMENT '更新人',
    deleted             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code),
    KEY idx_rule_type_enabled (anomaly_type, enabled),
    KEY idx_rule_ent (enterprise_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '诊断规则';

-- -----------------------------------------------------------------------------
-- 19. 诊断：诊断报告
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS diagnosis_report (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    report_no             VARCHAR(64)  NOT NULL COMMENT '报告编号',
    enterprise_id         BIGINT       NOT NULL COMMENT '所属企业',
    anomaly_id            BIGINT       NOT NULL COMMENT '关联异常',
    device_id             BIGINT       NOT NULL COMMENT '设备',
    baseline_id           BIGINT       NULL COMMENT '使用基线模型',
    model_version         VARCHAR(64)  NULL COMMENT '模型版本快照',
    conclusion            VARCHAR(1000) NOT NULL COMMENT '诊断结论',
    cause_candidates_json JSON         NULL COMMENT '原因候选',
    suggestions_json      JSON         NULL COMMENT '整改建议',
    estimated_saving      DECIMAL(18,4) NULL COMMENT '预计节能量',
    rule_snapshot_json    JSON         NULL COMMENT '规则快照',
    status                TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待处理 1已转任务 2已关闭',
    generated_at          DATETIME     NOT NULL COMMENT '生成时间',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by             BIGINT       NULL COMMENT '创建人',
    update_by             BIGINT       NULL COMMENT '更新人',
    deleted               TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_no (report_no),
    KEY idx_report_anomaly (anomaly_id),
    KEY idx_report_ent_time (enterprise_id, generated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '诊断报告';

-- -----------------------------------------------------------------------------
-- 20. 整改：整改任务
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rectification_task (
    id                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_no                VARCHAR(64)  NOT NULL COMMENT '任务编号',
    report_id              BIGINT       NULL COMMENT '来源诊断报告',
    anomaly_id             BIGINT       NULL COMMENT '来源异常',
    enterprise_id          BIGINT       NOT NULL COMMENT '所属企业',
    line_id                BIGINT       NULL COMMENT '产线',
    device_id              BIGINT       NULL COMMENT '设备',
    title                  VARCHAR(200) NOT NULL COMMENT '任务标题',
    content                TEXT         NULL COMMENT '任务描述',
    priority               TINYINT      NOT NULL DEFAULT 2 COMMENT '优先级：1低 2中 3高 4紧急',
    assignee_id            BIGINT       NULL COMMENT '负责人',
    assignee_name          VARCHAR(64)  NULL COMMENT '负责人姓名快照',
    assign_time            DATETIME     NULL COMMENT '派发时间',
    deadline               DATETIME     NULL COMMENT '截止时间',
    status                 TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待派发 1已派发 2整改中 3待验收 4已完成 5已驳回 6已取消',
    source                 TINYINT      NOT NULL DEFAULT 1 COMMENT '来源：1自动 2手工',
    baseline_snapshot_json JSON         NULL COMMENT '整改前基线快照',
    finish_time            DATETIME     NULL COMMENT '完成时间',
    remark                 VARCHAR(500) NULL COMMENT '备注',
    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by              BIGINT       NULL COMMENT '创建人',
    update_by              BIGINT       NULL COMMENT '更新人',
    deleted                TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_task_assignee (assignee_id, status),
    KEY idx_task_ent_status (enterprise_id, status),
    KEY idx_task_device (device_id),
    KEY idx_task_report (report_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '整改任务';

-- -----------------------------------------------------------------------------
-- 21. 整改：反馈与验收
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rectification_feedback (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id       BIGINT       NOT NULL COMMENT '整改任务',
    feedback_type TINYINT      NOT NULL COMMENT '类型：1进度 2完工 3验收 4驳回',
    content       VARCHAR(1000) NULL COMMENT '反馈内容',
    images_json   JSON         NULL COMMENT '凭证文件',
    actual_saving DECIMAL(18,4) NULL COMMENT '自估节能量',
    submit_by     BIGINT       NOT NULL COMMENT '提交人',
    submit_time   DATETIME     NOT NULL COMMENT '提交时间',
    verify_by     BIGINT       NULL COMMENT '验收人',
    verify_time   DATETIME     NULL COMMENT '验收时间',
    verify_result TINYINT      NULL COMMENT '验收结果：1通过 2驳回',
    verify_remark VARCHAR(500) NULL COMMENT '验收意见',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_fb_task (task_id, submit_time),
    KEY idx_fb_type (feedback_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '整改反馈';

-- -----------------------------------------------------------------------------
-- 22. 核算：节能结果
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_saving_result (
    id                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id                BIGINT       NOT NULL COMMENT '整改任务',
    report_id              BIGINT       NULL COMMENT '诊断报告',
    enterprise_id          BIGINT       NOT NULL COMMENT '所属企业',
    device_id              BIGINT       NULL COMMENT '设备',
    version                INT          NOT NULL DEFAULT 1 COMMENT '核算版本',
    verify_start           DATETIME     NOT NULL COMMENT '观察期开始',
    verify_end             DATETIME     NOT NULL COMMENT '观察期结束',
    sample_days            INT          NOT NULL DEFAULT 0 COMMENT '有效样本天数',
    method                 TINYINT      NOT NULL DEFAULT 1 COMMENT '方法：1反事实基线 2对比法',
    baseline_model_version VARCHAR(64)  NOT NULL COMMENT '锁定的基线版本',
    baseline_total         DECIMAL(18,4) NOT NULL COMMENT '反事实基线总量',
    actual_total           DECIMAL(18,4) NOT NULL COMMENT '整改后实际总量',
    saving_value           DECIMAL(18,4) NOT NULL COMMENT '节能量',
    saving_rate            DECIMAL(9,4) NULL COMMENT '节能率%',
    saving_amount          DECIMAL(18,2) NULL COMMENT '折合金额',
    confidence             TINYINT      NOT NULL DEFAULT 2 COMMENT '置信度',
    excluded_json          JSON         NULL COMMENT '剔除时段',
    status                 TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已生效 2已驳回',
    audited_by             BIGINT       NULL COMMENT '审核人',
    audited_at             DATETIME     NULL COMMENT '审核时间',
    audit_remark           VARCHAR(500) NULL COMMENT '审核意见',
    conclusion             VARCHAR(500) NULL COMMENT '核算结论',
    completeness           DECIMAL(5,2) NULL COMMENT '数据完整率%',
    comparable             TINYINT      NOT NULL DEFAULT 1 COMMENT '工况是否可比',
    output_before          DECIMAL(18,4) NULL COMMENT '整改前平均产量',
    output_after           DECIMAL(18,4) NULL COMMENT '观察期平均产量',
    output_change_rate     DECIMAL(9,4) NULL COMMENT '产量变化率%',
    unit_price             DECIMAL(10,4) NULL COMMENT '能源单价',
    actual_days            INT          NOT NULL DEFAULT 0 COMMENT '实际观察天数',
    archived               TINYINT      NOT NULL DEFAULT 0 COMMENT '是否历史版本',
    detail_json            JSON         NULL COMMENT '逐小时明细与电价分段',
    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by              BIGINT       NULL COMMENT '创建人',
    update_by              BIGINT       NULL COMMENT '更新人',
    deleted                TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_save_task_version (task_id, version),
    KEY idx_save_ent_time (enterprise_id, verify_start),
    KEY idx_save_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '节能核算结果';

-- -----------------------------------------------------------------------------
-- 23. 统一上传文件（P7）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id BIGINT       NULL COMMENT '所属企业',
    biz_type      VARCHAR(32)  NOT NULL COMMENT '业务类型：RECTIFY/AVATAR',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_path   VARCHAR(500) NOT NULL COMMENT '存储相对路径',
    url           VARCHAR(500) NOT NULL COMMENT '访问地址',
    content_type  VARCHAR(100) NULL COMMENT 'MIME',
    file_size     BIGINT       NOT NULL DEFAULT 0 COMMENT '字节数',
    uploader_id   BIGINT       NULL COMMENT '上传人',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_file_biz (biz_type, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '统一上传文件';

-- -----------------------------------------------------------------------------
-- 23. 数据湖：入库失败记录
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_ingest_failure (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no      VARCHAR(64)  NOT NULL COMMENT '批次号',
    enterprise_id BIGINT       NULL COMMENT '所属企业',
    meter_id      BIGINT       NULL COMMENT '仪表ID',
    collect_time  DATETIME     NULL COMMENT '采集时间',
    payload       TEXT         NULL COMMENT '原始提交内容',
    error_code    VARCHAR(32)  NOT NULL COMMENT '错误码',
    error_msg     VARCHAR(500) NULL COMMENT '错误原因',
    retry_count   INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待处理 1已补录 2已忽略',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL COMMENT '创建人',
    update_by     BIGINT       NULL COMMENT '更新人',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_fail_batch (batch_no),
    KEY idx_fail_meter_time (meter_id, collect_time),
    KEY idx_fail_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入库失败记录';

-- -----------------------------------------------------------------------------
-- 24. 数据湖：数据缺口
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS energy_data_gap (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    enterprise_id  BIGINT       NOT NULL COMMENT '所属企业',
    meter_id       BIGINT       NOT NULL COMMENT '仪表ID',
    energy_type_id BIGINT       NULL COMMENT '能源类型',
    gap_start      DATETIME     NOT NULL COMMENT '缺口开始',
    gap_end        DATETIME     NOT NULL COMMENT '缺口结束（不含）',
    expected_count INT          NOT NULL DEFAULT 0 COMMENT '应采点数',
    actual_count   INT          NOT NULL DEFAULT 0 COMMENT '实采点数',
    missing_count  INT          NOT NULL DEFAULT 0 COMMENT '缺失点数',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待补录 1已补录 2已忽略',
    fill_time      DATETIME     NULL COMMENT '补录时间',
    remark         VARCHAR(255) NULL COMMENT '备注',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL COMMENT '创建人',
    update_by      BIGINT       NULL COMMENT '更新人',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_gap_meter_time (meter_id, gap_start),
    KEY idx_gap_status (status),
    KEY idx_gap_ent (enterprise_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据缺口';

SET FOREIGN_KEY_CHECKS = 1;
