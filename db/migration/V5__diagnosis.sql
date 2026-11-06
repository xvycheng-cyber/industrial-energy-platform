-- P6 异常诊断：评分、合并、报告确认字段（幂等）
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_anomaly ADD COLUMN risk_score DECIMAL(5,2) NULL COMMENT ''综合评分 0-100'' AFTER severity',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_anomaly' AND column_name = 'risk_score');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_anomaly ADD COLUMN duration_minutes INT NOT NULL DEFAULT 0 COMMENT ''持续时长（分钟）'' AFTER period_end',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_anomaly' AND column_name = 'duration_minutes');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_anomaly ADD COLUMN merge_count INT NOT NULL DEFAULT 1 COMMENT ''合并次数'' AFTER duration_minutes',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_anomaly' AND column_name = 'merge_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_anomaly ADD COLUMN first_detect_time DATETIME NULL COMMENT ''首次发现时间'' AFTER detect_time',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_anomaly' AND column_name = 'first_detect_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_anomaly ADD COLUMN last_detect_time DATETIME NULL COMMENT ''最近命中时间'' AFTER first_detect_time',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_anomaly' AND column_name = 'last_detect_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_anomaly ADD COLUMN report_id BIGINT NULL COMMENT ''诊断报告'' AFTER last_detect_time',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_anomaly' AND column_name = 'report_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE diagnosis_report ADD COLUMN description VARCHAR(1000) NULL COMMENT ''异常描述'' AFTER model_version',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'diagnosis_report' AND column_name = 'description');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE diagnosis_report ADD COLUMN confirmed_cause VARCHAR(500) NULL COMMENT ''人工确认原因'' AFTER cause_candidates_json',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'diagnosis_report' AND column_name = 'confirmed_cause');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE diagnosis_report ADD COLUMN confirmed_by BIGINT NULL COMMENT ''确认人'' AFTER confirmed_cause',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'diagnosis_report' AND column_name = 'confirmed_by');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE diagnosis_report ADD COLUMN confirmed_time DATETIME NULL COMMENT ''确认时间'' AFTER confirmed_by',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'diagnosis_report' AND column_name = 'confirmed_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE diagnosis_report ADD COLUMN risk_level TINYINT NULL COMMENT ''风险等级'' AFTER status',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'diagnosis_report' AND column_name = 'risk_level');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
