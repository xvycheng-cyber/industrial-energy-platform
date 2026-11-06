-- P7 整改闭环：任务字段、反馈前后情况、统一文件表（幂等）
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_task ADD COLUMN expected_saving DECIMAL(18,4) NULL COMMENT ''预期节能量 kWh'' AFTER priority',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_task' AND column_name = 'expected_saving');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_task ADD COLUMN measure TEXT NULL COMMENT ''整改措施'' AFTER content',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_task' AND column_name = 'measure');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_task ADD COLUMN reject_reason VARCHAR(500) NULL COMMENT ''驳回原因'' AFTER status',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_task' AND column_name = 'reject_reason');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_task ADD COLUMN audit_by BIGINT NULL COMMENT ''审核人'' AFTER source',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_task' AND column_name = 'audit_by');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_task ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_by',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_task' AND column_name = 'audit_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_task ADD COLUMN progress INT NOT NULL DEFAULT 0 COMMENT ''整改进度 0-100'' AFTER status',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_task' AND column_name = 'progress');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_task ADD COLUMN transfer_count INT NOT NULL DEFAULT 0 COMMENT ''转派次数'' AFTER assignee_name',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_task' AND column_name = 'transfer_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_feedback ADD COLUMN before_desc VARCHAR(1000) NULL COMMENT ''整改前情况'' AFTER content',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_feedback' AND column_name = 'before_desc');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rectification_feedback ADD COLUMN after_desc VARCHAR(1000) NULL COMMENT ''整改后情况'' AFTER before_desc',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'rectification_feedback' AND column_name = 'after_desc');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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
