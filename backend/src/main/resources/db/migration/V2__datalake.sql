-- =============================================================================
-- V2 数据湖迁移（P3）
-- 适用：已按 V1（schema.sql）建库的环境，执行一次即可。
-- 新环境直接执行最新 schema.sql，无需本文件。
--
-- 变更：
--   1. energy_meter 增加采集间隔与读数类型
--   2. energy_raw_data 增加读数类型与接收时间
--   3. energy_detail 增加起止读数与补录标记
--   4. 新增 data_ingest_failure、energy_data_gap
-- =============================================================================

SET NAMES utf8mb4;

ALTER TABLE energy_meter
    ADD COLUMN collect_interval_seconds INT NOT NULL DEFAULT 60 COMMENT '采集间隔（秒）' AFTER ratio,
    ADD COLUMN reading_type TINYINT NOT NULL DEFAULT 1 COMMENT '读数类型：1累计读数 2周期增量' AFTER collect_interval_seconds;

ALTER TABLE energy_raw_data
    ADD COLUMN reading_type TINYINT NOT NULL DEFAULT 1 COMMENT '读数类型：1累计读数 2周期增量' AFTER raw_unit,
    ADD COLUMN received_time DATETIME NULL COMMENT '接收时间' AFTER reading_type;

ALTER TABLE energy_detail
    ADD COLUMN start_reading DECIMAL(18,4) NULL COMMENT '区间起始读数' AFTER unit,
    ADD COLUMN end_reading DECIMAL(18,4) NULL COMMENT '区间结束读数' AFTER start_reading,
    ADD COLUMN estimated TINYINT NOT NULL DEFAULT 0 COMMENT '是否补录估算：1是' AFTER quality_flag;

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
