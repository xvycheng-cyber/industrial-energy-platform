-- P8 节能核算：结论、完整性、可比性、电价与追溯字段（幂等）
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN conclusion VARCHAR(500) NULL COMMENT ''核算结论'' AFTER saving_amount',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'conclusion');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN completeness DECIMAL(5,2) NULL COMMENT ''数据完整率'' AFTER conclusion',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'completeness');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN comparable TINYINT NOT NULL DEFAULT 1 COMMENT ''工况是否可比'' AFTER completeness',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'comparable');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN output_before DECIMAL(18,4) NULL COMMENT ''整改前平均产量'' AFTER comparable',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'output_before');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN output_after DECIMAL(18,4) NULL COMMENT ''观察期平均产量'' AFTER output_before',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'output_after');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN output_change_rate DECIMAL(9,4) NULL COMMENT ''产量变化率%'' AFTER output_after',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'output_change_rate');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN unit_price DECIMAL(10,4) NULL COMMENT ''能源单价'' AFTER output_change_rate',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'unit_price');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN actual_days INT NOT NULL DEFAULT 0 COMMENT ''实际观察天数'' AFTER sample_days',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'actual_days');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN archived TINYINT NOT NULL DEFAULT 0 COMMENT ''是否历史版本'' AFTER status',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'archived');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE energy_saving_result ADD COLUMN detail_json JSON NULL COMMENT ''逐小时明细与电价分段'' AFTER excluded_json',
    'SELECT 1') FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'energy_saving_result' AND column_name = 'detail_json');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
