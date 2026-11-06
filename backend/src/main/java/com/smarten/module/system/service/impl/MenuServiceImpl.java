package com.smarten.module.system.service.impl;

import com.smarten.module.system.service.MenuService;
import com.smarten.module.system.vo.MenuVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树构建：菜单由「权限码」推导，前端不硬编码菜单。
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final String ALL = "*";

    @Override
    public List<MenuVO> buildMenus(List<String> permissions) {
        List<MenuVO> menus = new ArrayList<>();
        menus.add(new MenuVO("dashboard", "工作台", "Odometer", "/dashboard"));

        MenuVO cockpit = new MenuVO("cockpit", "能源驾驶舱", "DataBoard", "/cockpit");
        if (hasPermission(permissions, "profile:cockpit:view")) {
            menus.add(cockpit);
        }

        MenuVO profile = new MenuVO("profile", "能耗画像", "PieChart", null);
        addIfAllowed(profile, permissions, "enterpriseProfile", "企业画像", "OfficeBuilding", "/profile/enterprise", "profile:enterprise:view");
        addIfAllowed(profile, permissions, "lineProfile", "产线画像", "Connection", "/profile/line", "profile:line:view");
        addIfAllowed(profile, permissions, "deviceProfile", "设备画像", "Cpu", "/profile/device", "profile:device:view");
        if (!profile.getChildren().isEmpty()) {
            menus.add(profile);
        }

        if (hasPermission(permissions, "factory:view")) {
            menus.add(new MenuVO("factory", "三维工厂", "Coordinate", "/factory"));
        }

        MenuVO baseline = new MenuVO("baseline", "动态基线", "TrendCharts", null);
        addIfAllowed(baseline, permissions, "baselineRecords", "基线列表", "DataLine", "/baseline/records", "baseline:view");
        addIfAllowed(baseline, permissions, "baselineModels", "基线模型", "Cpu", "/baseline/models", "baseline:view");
        if (!baseline.getChildren().isEmpty()) {
            menus.add(baseline);
        }

        MenuVO diagnosis = new MenuVO("diagnosis", "异常诊断", "Warning", null);
        addIfAllowed(diagnosis, permissions, "diagnosisAnomalies", "异常监控", "Bell", "/diagnosis/anomalies", "diagnosis:view");
        addIfAllowed(diagnosis, permissions, "diagnosisOverview", "诊断总览", "DataAnalysis", "/diagnosis/overview", "diagnosis:view");
        addIfAllowed(diagnosis, permissions, "diagnosisRules", "规则配置", "SetUp", "/diagnosis/rules", "diagnosis:rule");
        if (!diagnosis.getChildren().isEmpty()) {
            menus.add(diagnosis);
        }

        MenuVO rectify = new MenuVO("rectification", "整改管理", "Tools", null);
        addIfAllowed(rectify, permissions, "rectifyTasks", "整改任务", "List", "/rectification/tasks", "rectify:view");
        if (!rectify.getChildren().isEmpty()) {
            menus.add(rectify);
        }

        MenuVO saving = new MenuVO("saving", "节能核算", "Money", null);
        addIfAllowed(saving, permissions, "savingResults", "核算列表", "Tickets", "/saving/results", "saving:view");
        if (!saving.getChildren().isEmpty()) {
            menus.add(saving);
        }

        MenuVO org = new MenuVO("org", "组织管理", "OfficeBuilding", null);
        addIfAllowed(org, permissions, "enterprise", "企业", "School", "/master/enterprises", "master:enterprise:list");
        addIfAllowed(org, permissions, "workshop", "车间", "House", "/master/workshops", "master:workshop:list");
        addIfAllowed(org, permissions, "line", "产线", "Connection", "/master/lines", "master:line:list");
        addIfAllowed(org, permissions, "device", "设备", "Cpu", "/master/devices", "master:device:list");
        // 能源类型、计量点菜单在 P3（数据湖）阶段随页面一并开放
        if (!org.getChildren().isEmpty()) {
            menus.add(org);
        }

        MenuVO lake = new MenuVO("datalake", "数据湖", "Coin", null);
        addIfAllowed(lake, permissions, "ingest", "数据接入", "Upload", "/datalake/ingest", "datalake:ingest");
        addIfAllowed(lake, permissions, "import", "数据导入", "Document", "/datalake/import", "datalake:import");
        addIfAllowed(lake, permissions, "cleaning", "清洗记录", "Filter", "/datalake/cleaning", "datalake:detail:list");
        addIfAllowed(lake, permissions, "quality", "质量监控", "Warning", "/datalake/quality", "datalake:quality:list");
        addIfAllowed(lake, permissions, "logs", "入库日志", "Tickets", "/datalake/logs", "datalake:log:list");
        addIfAllowed(lake, permissions, "completeness", "完整率", "DataLine", "/datalake/completeness", "datalake:completeness:list");
        if (!lake.getChildren().isEmpty()) {
            menus.add(lake);
        }

        MenuVO system = new MenuVO("system", "系统管理", "Setting", null);
        addIfAllowed(system, permissions, "user", "用户管理", "User", "/system/users", "system:user:list");
        addIfAllowed(system, permissions, "role", "角色管理", "UserFilled", "/system/roles", "system:role:list");
        if (!system.getChildren().isEmpty()) {
            menus.add(system);
        }
        return menus;
    }

    private void addIfAllowed(MenuVO parent, List<String> permissions, String key, String title,
                              String icon, String path, String permission) {
        if (hasPermission(permissions, permission)) {
            parent.getChildren().add(new MenuVO(key, title, icon, path));
        }
    }

    private boolean hasPermission(List<String> permissions, String code) {
        return permissions != null && (permissions.contains(ALL) || permissions.contains(code));
    }
}
