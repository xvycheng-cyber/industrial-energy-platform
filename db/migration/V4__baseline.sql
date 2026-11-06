-- P5 动态基线：模型指标、回退记录与明细工况（幂等：先查 information_schema 再 ALTER）

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline ADD COLUMN mae DECIMAL(18,4) NULL COMMENT ''平均绝对误差'' AFTER r2',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline' AND column_name = 'mae');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline ADD COLUMN fallback_count INT NOT NULL DEFAULT 0 COMMENT ''回退预测次数'' AFTER mape',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline' AND column_name = 'fallback_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline ADD COLUMN sample_rejected INT NOT NULL DEFAULT 0 COMMENT ''清洗拒绝样本数'' AFTER fallback_count',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline' AND column_name = 'sample_rejected');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline ADD COLUMN outlier_count INT NOT NULL DEFAULT 0 COMMENT ''异常值剔除数'' AFTER sample_rejected',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline' AND column_name = 'outlier_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline_detail ADD COLUMN baseline_type TINYINT NULL COMMENT ''本次预测类型'' AFTER deviation_rate',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline_detail' AND column_name = 'baseline_type');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline_detail ADD COLUMN model_version VARCHAR(64) NULL COMMENT ''模型版本'' AFTER baseline_type',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline_detail' AND column_name = 'model_version');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline_detail ADD COLUMN condition_json JSON NULL COMMENT ''工况参数快照'' AFTER model_version',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline_detail' AND column_name = 'condition_json');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_baseline_detail ADD COLUMN fallback_reason VARCHAR(255) NULL COMMENT ''回退原因'' AFTER condition_json',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_baseline_detail' AND column_name = 'fallback_reason');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
