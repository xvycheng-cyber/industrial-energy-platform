-- =============================================================================
-- V3 能耗画像与驾驶舱（P4）
-- 适用：已执行 V1/V2 的环境，执行一次。
-- 变更：energy_type 增加折标煤系数，用于综合能耗（吨标煤）换算。
-- =============================================================================

SET NAMES utf8mb4;

ALTER TABLE energy_type
    ADD COLUMN coal_factor DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '折标煤系数（kgce/单位）' AFTER co2_factor;

UPDATE energy_type SET coal_factor = 0.1229 WHERE code = 'ELEC';
UPDATE energy_type SET coal_factor = 0.2429 WHERE code = 'WATER';
UPDATE energy_type SET coal_factor = 1.3300 WHERE code = 'GAS';
UPDATE energy_type SET coal_factor = 90.0000 WHERE code = 'STEAM';
UPDATE energy_type SET coal_factor = 0.0400 WHERE code = 'AIR';
