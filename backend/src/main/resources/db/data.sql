-- =============================================================================
-- 智能耗 · 数据库初始化数据（幂等）
-- 说明：
--   1. 全部使用 INSERT IGNORE + 固定主键，可重复执行，不会覆盖运行期修改。
--   2. 管理员初始密码：Admin@123；演示工程师：Engineer@123（BCrypt）。
--   3. 演示数据仅用于开发联调，生产环境请勿执行本文件的演示部分。
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 角色
-- * 表示拥有全部权限
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_role (id, role_code, role_name, description, permissions, sort_no, status, builtin) VALUES
(1, 'PLATFORM_ADMIN', '平台管理员', '平台最高权限，可管理所有企业数据', '*', 1, 1, 1),
(2, 'ENERGY_ADMIN', '能源管理员', '管理本企业主数据、基线与整改', 'system:user:list,system:role:list,master:enterprise:list,master:enterprise:update,master:workshop:list,master:workshop:create,master:workshop:update,master:workshop:delete,master:line:list,master:line:create,master:line:update,master:line:delete,master:device:list,master:device:create,master:device:update,master:device:delete,master:energy:list,master:energy:create,master:energy:update,master:energy:delete,master:meter:list,master:meter:create,master:meter:update,master:meter:delete', 2, 1, 1),
(3, 'ENGINEER', '工程师', '查看本企业设备与能耗，执行整改任务', 'master:enterprise:list,master:workshop:list,master:line:list,master:device:list,master:energy:list,master:meter:list', 3, 1, 1),
(4, 'VIEWER', '只读用户', '仅可查看，不可修改', 'master:enterprise:list,master:workshop:list,master:line:list,master:device:list,master:energy:list,master:meter:list', 4, 1, 1);

-- -----------------------------------------------------------------------------
-- 用户（id=1 平台管理员，无企业；id=2 演示工程师，属企业 1）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_user (id, username, password, real_name, phone, email, enterprise_id, status, remark) VALUES
(1, 'admin', '$2a$10$WojpaGs4vNSbK5UG6Gng7.vQBICdYAo806Y9l9yclLpYBGt60MB3W', '平台管理员', '13800000000', 'admin@smarten.local', NULL, 1, '内置超级管理员'),
(2, 'engineer', '$2a$10$68q234cLLJ8OSMnIim.zmObDQ19cB7XkI3WgogI1wgamGiZ8qnuse', '张工', '13900000001', 'engineer@smarten.local', 1, 1, '演示工程师');

INSERT IGNORE INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1),
(2, 2, 3);

-- -----------------------------------------------------------------------------
-- 能源类型
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO energy_type (id, code, name, unit, price, co2_factor, status) VALUES
(1, 'ELEC', '电', 'kWh', 0.6800, 0.5700, 1),
(2, 'WATER', '水', 'm³', 4.2000, 0.3400, 1),
(3, 'GAS', '天然气', 'm³', 3.5000, 2.1600, 1),
(4, 'STEAM', '蒸汽', 't', 220.0000, 0.1100, 1),
(5, 'AIR', '压缩空气', 'm³', 0.1200, 0.0000, 1);

-- -----------------------------------------------------------------------------
-- 演示企业（用于数据隔离验证：企业1 为工程师所属企业，企业2 仅平台管理员可见）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO energy_enterprise (id, code, name, industry, region, address, contact_name, contact_phone, status, remark) VALUES
(1, 'ENT001', '华东精密制造有限公司', '汽车零部件', '江苏省苏州市', '苏州工业园区星湖街 88 号', '李经理', '0512-88880001', 1, '演示企业'),
(2, 'ENT002', '华南新材料科技有限公司', '新材料', '广东省佛山市', '佛山市南海区科技路 12 号', '王经理', '0757-88880002', 1, '演示企业（隔离测试）');

INSERT IGNORE INTO energy_workshop (id, enterprise_id, code, name, manager, area, status) VALUES
(1, 1, 'WS01', '一车间', '赵主任', 3200.00, 1),
(2, 1, 'WS02', '二车间', '钱主任', 2800.00, 1),
(3, 2, 'WS01', '佛山一车间', '孙主任', 4100.00, 1);

INSERT IGNORE INTO energy_line (id, enterprise_id, workshop_id, code, name, product_type, design_capacity, capacity_unit, status) VALUES
(1, 1, 1, 'LN01', '注塑一线', '精密塑料件', 12000.0000, '件/日', 1),
(2, 1, 1, 'LN02', '注塑二线', '精密塑料件', 10000.0000, '件/日', 1),
(3, 1, 2, 'LN03', '装配线', '总成组件', 8000.0000, '件/日', 1),
(4, 2, 3, 'LN01', '涂覆一线', '功能薄膜', 6000.0000, '卷/日', 1);

INSERT IGNORE INTO energy_device (id, enterprise_id, workshop_id, line_id, code, name, device_type, model, rated_power, energy_type_id, commission_date, status) VALUES
(1, 1, 1, 1, 'DEV001', '注塑机 A1', '注塑机', 'HT-180', 45.0000, 1, '2021-03-15', 1),
(2, 1, 1, 1, 'DEV002', '注塑机 A2', '注塑机', 'HT-180', 45.0000, 1, '2021-03-15', 1),
(3, 1, 1, 2, 'DEV003', '注塑机 B1', '注塑机', 'HT-250', 60.0000, 1, '2022-06-01', 2),
(4, 1, 2, 3, 'DEV004', '装配机械手', '机械手', 'RB-20', 8.5000, 1, '2023-01-10', 1),
(5, 2, 3, 4, 'DEV001', '涂覆机 C1', '涂覆机', 'CT-300', 75.0000, 1, '2020-09-20', 1);

INSERT IGNORE INTO energy_meter (id, enterprise_id, device_id, line_id, meter_code, meter_name, energy_type_id, ratio, install_time, status) VALUES
(1, 1, 1, NULL, 'MTR001', '注塑机 A1 电表', 1, 1.0000, '2021-03-20 00:00:00', 1),
(2, 1, 2, NULL, 'MTR002', '注塑机 A2 电表', 1, 1.0000, '2021-03-20 00:00:00', 1),
(3, 1, 4, NULL, 'MTR003', '装配机械手电表', 1, 1.0000, '2023-01-15 00:00:00', 1),
(4, 1, NULL, 1, 'MTR004', '注塑一线总表', 1, 40.0000, '2021-03-20 00:00:00', 1),
(5, 2, 5, NULL, 'MTR101', '涂覆机 C1 电表', 1, 1.0000, '2020-09-25 00:00:00', 1);

-- -----------------------------------------------------------------------------
-- 数据湖权限（幂等追加，不影响已存在的角色配置）
-- -----------------------------------------------------------------------------
UPDATE sys_role
SET permissions = CONCAT(permissions,
    ',datalake:raw:list,datalake:detail:list,datalake:log:list,datalake:quality:list,datalake:completeness:list,datalake:ads:view,datalake:ingest,datalake:import,datalake:clean,datalake:simulate,datalake:gap:manage')
WHERE role_code = 'ENERGY_ADMIN'
  AND permissions NOT LIKE '%datalake:raw:list%';

UPDATE sys_role
SET permissions = CONCAT(permissions,
    ',datalake:raw:list,datalake:detail:list,datalake:log:list,datalake:quality:list,datalake:completeness:list,datalake:ads:view')
WHERE role_code IN ('ENGINEER', 'VIEWER')
  AND permissions NOT LIKE '%datalake:raw:list%';

-- 演示：MTR003 使用「周期增量」读数，其余为「累计读数」
UPDATE energy_meter SET reading_type = 2 WHERE meter_code = 'MTR003';
-- 折标煤系数（kgce/单位），用于综合能耗换算
UPDATE energy_type SET coal_factor = 0.1229 WHERE code = 'ELEC';
UPDATE energy_type SET coal_factor = 0.2429 WHERE code = 'WATER';
UPDATE energy_type SET coal_factor = 1.3300 WHERE code = 'GAS';
UPDATE energy_type SET coal_factor = 90.0000 WHERE code = 'STEAM';
UPDATE energy_type SET coal_factor = 0.0400 WHERE code = 'AIR';
-- 画像与驾驶舱权限（幂等追加）
UPDATE sys_role
SET permissions = CONCAT(permissions,
    ',profile:cockpit:view,profile:enterprise:view,profile:line:view,profile:device:view,profile:hierarchy:view')
WHERE role_code = 'ENERGY_ADMIN'
  AND permissions NOT LIKE '%profile:cockpit:view%';

UPDATE sys_role
SET permissions = CONCAT(permissions,
    ',profile:cockpit:view,profile:enterprise:view,profile:line:view,profile:device:view,profile:hierarchy:view')
WHERE role_code IN ('ENGINEER', 'VIEWER')
  AND permissions NOT LIKE '%profile:cockpit:view%';
-- 三维工厂权限（幂等追加）
UPDATE sys_role
SET permissions = CONCAT(permissions, ',factory:view')
WHERE role_code IN ('ENERGY_ADMIN', 'ENGINEER', 'VIEWER')
  AND permissions NOT LIKE '%factory:view%';
-- 动态基线权限（幂等追加）
UPDATE sys_role
SET permissions = CONCAT(permissions, ',baseline:view,baseline:train,baseline:manage')
WHERE role_code IN ('ENERGY_ADMIN', 'ENGINEER')
  AND permissions NOT LIKE '%baseline:view%';

UPDATE sys_role
SET permissions = CONCAT(permissions, ',baseline:view')
WHERE role_code = 'VIEWER'
  AND permissions NOT LIKE '%baseline:view%';
-- 异常诊断规则（幂等）
INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R001', '能耗突增', 'R001',
       '{"threshold":40,"windowHours":1,"durationHours":1,"forced":false,"deviceTypes":[]}',
       '{"threshold":40}', 1.0,
       '设备启动冲击;工艺参数设置偏高;计量异常;保温/预热损耗',
       '检查启动时序与预热策略;核对工艺参数;校验计量表;评估分时启停',
       'max(0, actual-baseline)', 10, 1, 1, '单小时能耗较上一小时与基线同时显著上升'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R001');

INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R002', '持续高能耗', 'R002',
       '{"threshold":25,"windowHours":3,"durationHours":3,"forced":false,"deviceTypes":[]}',
       '{"threshold":25}', 1.0,
       '负荷长期偏高;设备老化效率下降;冷却/润滑不良;工艺排产不合理',
       '复核排产与负荷分配;安排能效检修;检查冷却与润滑;评估设备更新',
       'sum(max(0, actual-baseline))', 20, 1, 1, '连续 3 小时以上高于动态基线'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R002');

INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R003', '空载高能耗', 'R003',
       '{"threshold":30,"minLoadRate":20,"forced":false,"deviceTypes":[]}',
       '{"threshold":30}', 1.0,
       '空载运行未停机;待机加热/保温未关闭;设备内部泄漏;控制逻辑缺失',
       '设置空载自动停机;关闭非必要加热;排查气/水泄漏;优化控制逻辑',
       'max(0, actual-baseline)', 30, 1, 1, '低负荷或无产量时能耗仍显著高于基线'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R003');

INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R004', '设备空转', 'R004',
       '{"threshold":0.8,"forced":true,"deviceTypes":[]}',
       '{"threshold":0.8}', 1.0,
       '停机未断电;辅助设备持续运行;待机功耗未管理',
       '落实停机断电;关闭辅助设备;加装待机功率管理',
       'actual * duration_hours', 40, 1, 1, '停机或未运行时仍持续耗能（强制规则）'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R004');

INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R005', '单位产品能耗异常', 'R005',
       '{"threshold":30,"forced":false,"deviceTypes":[]}',
       '{"threshold":30}', 1.0,
       '产品切换未调整参数;良率下降;原料批次差异;设备磨损',
       '核对产品配方与参数;分析良率;抽检原料;检查关键部件磨损',
       'max(0, (actual-baseline))', 50, 1, 1, '单位产品能耗偏离基线'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R005');

INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R006', '夜间异常耗能', 'R006',
       '{"threshold":30,"nightStart":22,"nightEnd":6,"forced":false,"deviceTypes":[]}',
       '{"threshold":30}', 1.0,
       '夜间未按计划停机;值班照明/空调未关;夜间保温运行;盗电/漏电',
       '执行夜间停机计划;分区控制照明空调;评估保温需求;排查线路损耗',
       'max(0, actual-baseline)', 60, 1, 1, '夜间时段能耗异常'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R006');

INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R007', '设备能耗波动', 'R007',
       '{"threshold":35,"windowHours":6,"cvThreshold":0.35,"forced":false,"deviceTypes":[]}',
       '{"threshold":35}', 1.0,
       '设备启停频繁;控制参数震荡;传感器漂移;负荷忽高忽低',
       '优化启停策略;整定控制参数;校准传感器;平衡负荷',
       'max(0, mean-actual_min)', 70, 1, 1, '滑动窗口内能耗波动异常'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R007');

INSERT INTO diagnosis_rule (rule_code, rule_name, anomaly_type, condition_json, threshold_json, weight,
                            cause_template, suggestion_template, saving_formula, priority, version, enabled, remark)
SELECT 'R008', '同类设备能耗异常', 'R008',
       '{"threshold":30,"peerRate":30,"forced":false,"deviceTypes":[]}',
       '{"threshold":30,"peerRate":30}', 1.0,
       '设备效率低于同类;维护不到位;选型不匹配;运行参数差异',
       '对标同类设备参数;安排专项维护;评估选型;统一运行策略',
       'max(0, actual-peer_median)', 80, 1, 1, '同类设备能耗显著高于中位数'
WHERE NOT EXISTS (SELECT 1 FROM diagnosis_rule WHERE rule_code = 'R008');
-- 异常诊断权限（幂等追加）
UPDATE sys_role
SET permissions = CONCAT(permissions, ',diagnosis:view,diagnosis:scan,diagnosis:handle,diagnosis:rule')
WHERE role_code IN ('ENERGY_ADMIN', 'ENGINEER')
  AND permissions NOT LIKE '%diagnosis:view%';

UPDATE sys_role
SET permissions = CONCAT(permissions, ',diagnosis:view')
WHERE role_code = 'VIEWER'
  AND permissions NOT LIKE '%diagnosis:view%';
-- 整改闭环权限（幂等追加）
UPDATE sys_role
SET permissions = CONCAT(permissions, ',rectify:view,rectify:create,rectify:assign,rectify:audit,rectify:verify')
WHERE role_code = 'ENERGY_ADMIN'
  AND permissions NOT LIKE '%rectify:view%';

UPDATE sys_role
SET permissions = CONCAT(permissions, ',rectify:view,rectify:mobile')
WHERE role_code = 'ENGINEER'
  AND permissions NOT LIKE '%rectify:mobile%';

UPDATE sys_role
SET permissions = CONCAT(permissions, ',rectify:view')
WHERE role_code = 'VIEWER'
  AND permissions NOT LIKE '%rectify:view%';
-- 节能核算权限（幂等追加）
UPDATE sys_role
SET permissions = CONCAT(permissions, ',saving:view,saving:calc,saving:audit,saving:export')
WHERE role_code = 'ENERGY_ADMIN'
  AND permissions NOT LIKE '%saving:view%';

UPDATE sys_role
SET permissions = CONCAT(permissions, ',saving:view,saving:export')
WHERE role_code IN ('ENGINEER', 'VIEWER')
  AND permissions NOT LIKE '%saving:view%';