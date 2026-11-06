package com.smarten.module.rectification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarten.common.api.PageResult;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.common.util.SecurityUtils;
import com.smarten.module.diagnosis.entity.DiagnosisReport;
import com.smarten.module.diagnosis.entity.DiagnosisRule;
import com.smarten.module.diagnosis.entity.EnergyAnomaly;
import com.smarten.module.diagnosis.mapper.DiagnosisReportMapper;
import com.smarten.module.diagnosis.mapper.DiagnosisRuleMapper;
import com.smarten.module.diagnosis.mapper.EnergyAnomalyMapper;
import com.smarten.module.masterdata.entity.EnergyDevice;
import com.smarten.module.masterdata.entity.EnergyLine;
import com.smarten.module.masterdata.mapper.EnergyDeviceMapper;
import com.smarten.module.masterdata.mapper.EnergyLineMapper;
import com.smarten.module.rectification.dto.FeedbackRequest;
import com.smarten.module.rectification.dto.TaskAssignRequest;
import com.smarten.module.rectification.dto.TaskAuditRequest;
import com.smarten.module.rectification.dto.TaskCreateRequest;
import com.smarten.module.rectification.dto.TaskQuery;
import com.smarten.module.rectification.entity.RectificationFeedback;
import com.smarten.module.rectification.entity.RectificationTask;
import com.smarten.module.rectification.enums.RectificationStatus;
import com.smarten.module.rectification.mapper.RectificationFeedbackMapper;
import com.smarten.module.rectification.mapper.RectificationTaskMapper;
import com.smarten.module.rectification.vo.AssigneeVO;
import com.smarten.module.rectification.vo.MobileProfileVO;
import com.smarten.module.rectification.vo.RectificationFeedbackVO;
import com.smarten.module.rectification.vo.RectificationStatsVO;
import com.smarten.module.rectification.vo.RectificationTaskDetailVO;
import com.smarten.module.rectification.vo.RectificationTaskVO;
import com.smarten.module.system.entity.SysRole;
import com.smarten.module.system.entity.SysUser;
import com.smarten.module.system.entity.SysUserRole;
import com.smarten.module.system.mapper.SysRoleMapper;
import com.smarten.module.system.mapper.SysUserMapper;
import com.smarten.module.system.mapper.SysUserRoleMapper;
import com.smarten.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 整改任务：状态机校验、分配/转派、反馈、审核、效果验证、移动端归属校验。
 */
@Service
@RequiredArgsConstructor
public class RectificationService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RectificationTaskMapper taskMapper;
    private final RectificationFeedbackMapper feedbackMapper;
    private final EnergyDeviceMapper deviceMapper;
    private final EnergyLineMapper lineMapper;
    private final DiagnosisReportMapper reportMapper;
    private final EnergyAnomalyMapper anomalyMapper;
    private final DiagnosisRuleMapper ruleMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final DataScopeHelper dataScope;
    private final ObjectMapper objectMapper;

    /* ------------------------------- Web ------------------------------- */

    @Transactional(rollbackFor = Exception.class)
    public RectificationTaskDetailVO create(TaskCreateRequest request) {
        EnergyDevice device = resolveDevice(request);
        DiagnosisReport report = request.getReportId() == null ? null : reportMapper.selectById(request.getReportId());
        EnergyAnomaly anomaly = request.getAnomalyId() == null
                ? (report == null ? null : anomalyMapper.selectById(report.getAnomalyId()))
                : anomalyMapper.selectById(request.getAnomalyId());

        RectificationTask task = new RectificationTask();
        task.setTaskNo("RT" + LocalDateTime.now().format(STAMP) + "-" + java.util.UUID.randomUUID().toString().substring(0, 6));
        task.setReportId(report == null ? null : report.getId());
        task.setAnomalyId(anomaly == null ? null : anomaly.getId());
        task.setEnterpriseId(device.getEnterpriseId());
        task.setLineId(device.getLineId());
        task.setDeviceId(device.getId());
        task.setTitle(request.getTitle() == null || request.getTitle().isBlank()
                ? (anomaly == null ? "能效整改任务" : anomaly.getRuleCode() + " 整改任务")
                : request.getTitle());
        task.setContent(request.getContent() == null && report != null ? report.getConclusion() : request.getContent());
        task.setMeasure(request.getMeasure());
        task.setPriority(request.getPriority() == null ? 2 : request.getPriority());
        task.setExpectedSaving(request.getExpectedSaving());
        task.setDeadline(request.getDeadline());
        task.setStatus(RectificationStatus.PENDING.code());
        task.setProgress(0);
        task.setSource(request.getReportId() == null ? 2 : 1);
        task.setTransferCount(0);

        if (request.getAssigneeId() != null) {
            SysUser assignee = requireAssignee(request.getAssigneeId(), device.getEnterpriseId());
            task.setAssigneeId(assignee.getId());
            task.setAssigneeName(displayName(assignee));
            task.setAssignTime(LocalDateTime.now());
            task.setStatus(RectificationStatus.ASSIGNED.code());
        }
        taskMapper.insert(task);
        return detail(task.getId());
    }

    public PageResult<RectificationTaskVO> page(TaskQuery query) {
        LambdaQueryWrapper<RectificationTask> wrapper = new LambdaQueryWrapper<RectificationTask>()
                .orderByDesc(RectificationTask::getCreateTime);
        if (query.getStatus() != null) {
            wrapper.eq(RectificationTask::getStatus, query.getStatus());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(RectificationTask::getDeviceId, query.getDeviceId());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq(RectificationTask::getAssigneeId, query.getAssigneeId());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(item -> item.like(RectificationTask::getTitle, query.getKeyword())
                    .or().like(RectificationTask::getTaskNo, query.getKeyword()));
        }
        if (Boolean.TRUE.equals(query.getOverdue())) {
            wrapper.lt(RectificationTask::getDeadline, LocalDateTime.now())
                    .in(RectificationTask::getStatus, List.of(1, 2, 3, 7));
        }
        if (query.getStartDate() != null) {
            wrapper.ge(RectificationTask::getCreateTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.lt(RectificationTask::getCreateTime, query.getEndDate().plusDays(1).atStartOfDay());
        }
        dataScope.applyScope(wrapper, RectificationTask::getEnterpriseId);
        Page<RectificationTask> page = taskMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(toVOs(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public RectificationTaskDetailVO detail(Long id) {
        RectificationTask task = requireTask(id);
        dataScope.checkAccessible(task.getEnterpriseId());
        return buildDetail(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, TaskAssignRequest request) {
        RectificationTask task = requireTask(id);
        dataScope.checkAccessible(task.getEnterpriseId());
        RectificationStatus from = RectificationStatus.of(task.getStatus());
        RectificationStatus.check(from, RectificationStatus.ASSIGNED);
        SysUser assignee = requireAssignee(request.getAssigneeId(), task.getEnterpriseId());
        task.setAssigneeId(assignee.getId());
        task.setAssigneeName(displayName(assignee));
        task.setAssignTime(LocalDateTime.now());
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        task.setStatus(RectificationStatus.ASSIGNED.code());
        task.setRejectReason(null);
        taskMapper.updateById(task);
        addFeedback(task.getId(), 1, "任务已分配给 " + displayName(assignee), null, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long id, TaskAssignRequest request) {
        RectificationTask task = requireTask(id);
        dataScope.checkAccessible(task.getEnterpriseId());
        RectificationStatus.check(RectificationStatus.of(task.getStatus()), RectificationStatus.ASSIGNED);
        SysUser assignee = requireAssignee(request.getAssigneeId(), task.getEnterpriseId());
        String previous = task.getAssigneeName();
        task.setAssigneeId(assignee.getId());
        task.setAssigneeName(displayName(assignee));
        task.setAssignTime(LocalDateTime.now());
        task.setStatus(RectificationStatus.ASSIGNED.code());
        task.setTransferCount((task.getTransferCount() == null ? 0 : task.getTransferCount()) + 1);
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        taskMapper.updateById(task);
        addFeedback(task.getId(), 1, "任务由 " + (previous == null ? "未分配" : previous) + " 转派给 "
                + displayName(assignee) + (request.getReason() == null ? "" : "，原因：" + request.getReason()),
                null, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, TaskAuditRequest request) {
        RectificationTask task = requireTask(id);
        dataScope.checkAccessible(task.getEnterpriseId());
        boolean pass = Boolean.TRUE.equals(request.getPass());
        RectificationStatus.check(RectificationStatus.of(task.getStatus()),
                pass ? RectificationStatus.VERIFYING : RectificationStatus.REJECTED);
        if (!pass && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "驳回必须填写原因");
        }
        task.setAuditBy(SecurityUtils.getUserIdOrNull());
        task.setAuditTime(LocalDateTime.now());
        if (pass) {
            task.setStatus(RectificationStatus.VERIFYING.code());
            task.setRejectReason(null);
            addFeedback(task.getId(), 3, "审核通过，进入效果验证", null, null, null, request.getRemark());
        } else {
            task.setStatus(RectificationStatus.REJECTED.code());
            task.setRejectReason(request.getReason());
            addFeedback(task.getId(), 4, request.getReason(), null, null, null, request.getRemark());
        }
        taskMapper.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void verify(Long id, TaskAuditRequest request) {
        RectificationTask task = requireTask(id);
        dataScope.checkAccessible(task.getEnterpriseId());
        boolean pass = Boolean.TRUE.equals(request.getPass());
        RectificationStatus.check(RectificationStatus.of(task.getStatus()),
                pass ? RectificationStatus.COMPLETED : RectificationStatus.IN_PROGRESS);
        if (pass) {
            task.setStatus(RectificationStatus.COMPLETED.code());
            task.setProgress(100);
            task.setFinishTime(LocalDateTime.now());
        } else {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "效果验证不通过必须填写原因");
            }
            task.setStatus(RectificationStatus.IN_PROGRESS.code());
        }
        taskMapper.updateById(task);
        addFeedback(task.getId(), 5,
                pass ? "效果验证通过" : "效果验证未通过：" + request.getReason(),
                null, null, request.getActualSaving(), request.getRemark());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        RectificationTask task = requireTask(id);
        dataScope.checkAccessible(task.getEnterpriseId());
        RectificationStatus.check(RectificationStatus.of(task.getStatus()), RectificationStatus.CANCELLED);
        task.setStatus(RectificationStatus.CANCELLED.code());
        task.setRemark(reason);
        taskMapper.updateById(task);
        addFeedback(task.getId(), 4, "任务已撤销" + (reason == null ? "" : "：" + reason),
                null, null, null, null);
    }

    public RectificationStatsVO stats() {
        LambdaQueryWrapper<RectificationTask> wrapper = new LambdaQueryWrapper<>();
        dataScope.applyScope(wrapper, RectificationTask::getEnterpriseId);
        List<RectificationTask> tasks = taskMapper.selectList(wrapper);
        RectificationStatsVO vo = new RectificationStatsVO();
        vo.setTotal(tasks.size());
        for (RectificationStatus status : RectificationStatus.values()) {
            vo.getByStatus().put(status.label(), 0L);
        }
        for (RectificationTask task : tasks) {
            vo.getByStatus().merge(RectificationStatus.labelOf(task.getStatus()), 1L, Long::sum);
            if (isOverdue(task)) {
                vo.setOverdue(vo.getOverdue() + 1);
            }
        }
        return vo;
    }

    public List<AssigneeVO> assignees() {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "ENGINEER").last("LIMIT 1"));
        if (role == null) {
            return List.of();
        }
        List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, role.getId()))
                .stream().map(SysUserRole::getUserId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, userIds).eq(SysUser::getStatus, 1);
        Long enterpriseId = dataScope.currentEnterpriseId();
        if (enterpriseId != null) {
            userWrapper.eq(SysUser::getEnterpriseId, enterpriseId);
        }
        return userMapper.selectList(userWrapper).stream().map(user -> {
            AssigneeVO vo = new AssigneeVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            return vo;
        }).toList();
    }

    /* ------------------------------ Mobile ------------------------------ */

    public PageResult<RectificationTaskVO> myTasks(Long userId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<RectificationTask> wrapper = new LambdaQueryWrapper<RectificationTask>()
                .eq(RectificationTask::getAssigneeId, userId)
                .orderByDesc(RectificationTask::getCreateTime);
        if (status != null) {
            wrapper.eq(RectificationTask::getStatus, status);
        }
        Page<RectificationTask> page = taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(toVOs(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public RectificationTaskDetailVO myTask(Long id, Long userId) {
        RectificationTask task = requireTask(id);
        if (!Objects.equals(task.getAssigneeId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看分配给自己的任务");
        }
        return buildDetail(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(Long id, Long userId) {
        RectificationTask task = requireOwned(id, userId);
        RectificationStatus.check(RectificationStatus.of(task.getStatus()), RectificationStatus.IN_PROGRESS);
        task.setStatus(RectificationStatus.IN_PROGRESS.code());
        taskMapper.updateById(task);
        addFeedback(task.getId(), 1, "工程师开始整改", null, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void feedback(Long id, Long userId, FeedbackRequest request) {
        RectificationTask task = requireOwned(id, userId);
        RectificationStatus from = RectificationStatus.of(task.getStatus());
        if (from != RectificationStatus.ASSIGNED && from != RectificationStatus.IN_PROGRESS) {
            throw new BusinessException(ResultCode.TASK_STATUS_ILLEGAL, "当前状态不允许提交整改反馈");
        }
        if (request.getImages() != null && request.getImages().size() > 9) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "最多上传 9 张现场图片");
        }
        boolean complete = Boolean.TRUE.equals(request.getComplete());
        if (complete && (request.getContent() == null || request.getContent().isBlank())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "完工反馈必须填写整改说明");
        }
        if (complete) {
            RectificationStatus.check(from, RectificationStatus.PENDING_AUDIT);
            task.setStatus(RectificationStatus.PENDING_AUDIT.code());
            task.setProgress(100);
            task.setFinishTime(LocalDateTime.now());
        } else {
            if (from == RectificationStatus.ASSIGNED) {
                task.setStatus(RectificationStatus.IN_PROGRESS.code());
            }
            int progress = request.getProgress() == null ? Math.min(100, (task.getProgress() == null ? 0 : task.getProgress()) + 20)
                    : Math.max(0, Math.min(100, request.getProgress()));
            task.setProgress(progress);
        }
        taskMapper.updateById(task);
        addFeedback(task.getId(), complete ? 2 : 1, request.getContent(),
                request.getBeforeDesc(), request.getAfterDesc(), request.getActualSaving(), null,
                request.getImages());
    }

    public MobileProfileVO profile(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        MobileProfileVO vo = new MobileProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        vo.setRoles(roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds)
                .stream().map(SysRole::getRoleCode).toList());
        List<RectificationTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<RectificationTask>()
                .eq(RectificationTask::getAssigneeId, userId));
        vo.setPendingCount(tasks.stream().filter(item -> RectificationStatus.of(item.getStatus()) == RectificationStatus.ASSIGNED).count());
        vo.setInProgressCount(tasks.stream().filter(item -> {
            RectificationStatus status = RectificationStatus.of(item.getStatus());
            return status == RectificationStatus.IN_PROGRESS || status == RectificationStatus.PENDING_AUDIT
                    || status == RectificationStatus.VERIFYING;
        }).count());
        vo.setCompletedCount(tasks.stream().filter(item -> RectificationStatus.of(item.getStatus()) == RectificationStatus.COMPLETED).count());
        return vo;
    }

    /* ------------------------------ 辅助 ------------------------------ */

    private RectificationTask requireTask(Long id) {
        RectificationTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private RectificationTask requireOwned(Long id, Long userId) {
        RectificationTask task = requireTask(id);
        if (!Objects.equals(task.getAssigneeId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作分配给自己的任务");
        }
        return task;
    }

    private EnergyDevice resolveDevice(TaskCreateRequest request) {
        Long deviceId = request.getDeviceId();
        if (deviceId == null && request.getReportId() != null) {
            DiagnosisReport report = reportMapper.selectById(request.getReportId());
            if (report == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "诊断报告不存在");
            }
            deviceId = report.getDeviceId();
        }
        if (deviceId == null && request.getAnomalyId() != null) {
            EnergyAnomaly anomaly = anomalyMapper.selectById(request.getAnomalyId());
            if (anomaly == null) {
                throw new BusinessException(ResultCode.ANOMALY_NOT_FOUND);
            }
            deviceId = anomaly.getDeviceId();
        }
        if (deviceId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择设备或诊断报告");
        }
        EnergyDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        dataScope.checkAccessible(device.getEnterpriseId());
        return device;
    }

    private SysUser requireAssignee(Long assigneeId, Long enterpriseId) {
        SysUser user = userMapper.selectById(assigneeId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "负责人不存在或已禁用");
        }
        if (enterpriseId != null && user.getEnterpriseId() != null
                && !enterpriseId.equals(user.getEnterpriseId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "负责人必须属于同一企业");
        }
        return user;
    }

    private void addFeedback(Long taskId, int type, String content, String before, String after,
                             BigDecimal actualSaving, String verifyRemark) {
        addFeedback(taskId, type, content, before, after, actualSaving, verifyRemark, null);
    }

    private void addFeedback(Long taskId, int type, String content, String before, String after,
                             BigDecimal actualSaving, String verifyRemark, List<String> images) {
        RectificationFeedback feedback = new RectificationFeedback();
        feedback.setTaskId(taskId);
        feedback.setFeedbackType(type);
        feedback.setContent(content);
        if (images != null) {
            feedback.setImagesJson(write(images));
        }
        feedback.setBeforeDesc(before);
        feedback.setAfterDesc(after);
        feedback.setActualSaving(actualSaving);
        feedback.setSubmitBy(SecurityUtils.getUserIdOrNull());
        feedback.setSubmitTime(LocalDateTime.now());
        if (type == 3 || type == 4 || type == 5) {
            feedback.setVerifyBy(SecurityUtils.getUserIdOrNull());
            feedback.setVerifyTime(LocalDateTime.now());
            feedback.setVerifyResult(type == 3 || type == 5 ? 1 : 2);
        }
        feedback.setVerifyRemark(verifyRemark);
        feedbackMapper.insert(feedback);
    }

    private RectificationTaskDetailVO buildDetail(RectificationTask task) {
        RectificationTaskDetailVO vo = new RectificationTaskDetailVO();
        fill(vo, task);
        EnergyDevice device = deviceMapper.selectById(task.getDeviceId());
        if (device != null) {
            vo.setDeviceName(device.getName());
            vo.setDeviceCode(device.getCode());
            EnergyLine line = device.getLineId() == null ? null : lineMapper.selectById(device.getLineId());
            vo.setLineName(line == null ? null : line.getName());
        }
        List<RectificationFeedback> feedbacks = feedbackMapper.selectList(
                new LambdaQueryWrapper<RectificationFeedback>()
                        .eq(RectificationFeedback::getTaskId, task.getId())
                        .orderByDesc(RectificationFeedback::getSubmitTime));
        Map<Long, String> names = userNames(feedbacks.stream().map(RectificationFeedback::getSubmitBy).toList());
        vo.setFeedbacks(feedbacks.stream().map(item -> toFeedbackVO(item, names)).toList());
        if (task.getReportId() != null) {
            DiagnosisReport report = reportMapper.selectById(task.getReportId());
            if (report != null) {
                vo.setReportNo(report.getReportNo());
                vo.setReportDescription(report.getDescription());
                vo.setReportConclusion(report.getConclusion());
            }
        }
        if (task.getAnomalyId() != null) {
            EnergyAnomaly anomaly = anomalyMapper.selectById(task.getAnomalyId());
            if (anomaly != null) {
                DiagnosisRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<DiagnosisRule>()
                        .eq(DiagnosisRule::getRuleCode, anomaly.getRuleCode()).last("LIMIT 1"));
                vo.setRuleName(rule == null ? anomaly.getRuleCode() : rule.getRuleName());
                List<String> anomalies = new ArrayList<>();
                anomalies.add(String.format("%s：实际 %.2f kWh，基线 %.2f kWh，偏差 %.2f%%",
                        rule == null ? anomaly.getRuleCode() : rule.getRuleName(),
                        anomaly.getActualValue() == null ? 0 : anomaly.getActualValue().doubleValue(),
                        anomaly.getBaselineValue() == null ? 0 : anomaly.getBaselineValue().doubleValue(),
                        anomaly.getDeviationRate() == null ? 0 : anomaly.getDeviationRate().doubleValue()));
                vo.setAnomalies(anomalies);
            }
        }
        return vo;
    }

    private List<RectificationTaskVO> toVOs(List<RectificationTask> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, EnergyDevice> devices = deviceMapper.selectBatchIds(
                        tasks.stream().map(RectificationTask::getDeviceId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(EnergyDevice::getId, item -> item, (a, b) -> a));
        Map<Long, String> lines = lineNames(devices.values().stream().map(EnergyDevice::getLineId).collect(Collectors.toSet()));
        List<RectificationTaskVO> result = new ArrayList<>();
        for (RectificationTask task : tasks) {
            RectificationTaskVO vo = new RectificationTaskVO();
            fill(vo, task);
            EnergyDevice device = devices.get(task.getDeviceId());
            if (device != null) {
                vo.setDeviceName(device.getName());
                vo.setDeviceCode(device.getCode());
                vo.setLineName(lines.get(device.getLineId()));
            }
            result.add(vo);
        }
        return result;
    }

    private void fill(RectificationTaskVO vo, RectificationTask task) {
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTitle(task.getTitle());
        vo.setDeviceId(task.getDeviceId());
        vo.setReportId(task.getReportId());
        vo.setAnomalyId(task.getAnomalyId());
        vo.setStatus(task.getStatus());
        vo.setStatusLabel(RectificationStatus.labelOf(task.getStatus()));
        vo.setPriority(task.getPriority());
        vo.setPriorityLabel(priorityLabel(task.getPriority()));
        vo.setAssigneeId(task.getAssigneeId());
        vo.setAssigneeName(task.getAssigneeName());
        vo.setTransferCount(task.getTransferCount());
        vo.setExpectedSaving(task.getExpectedSaving());
        vo.setProgress(task.getProgress());
        vo.setDeadline(task.getDeadline());
        vo.setOverdue(isOverdue(task));
        vo.setRejectReason(task.getRejectReason());
        vo.setAssignTime(task.getAssignTime());
        vo.setFinishTime(task.getFinishTime());
        vo.setAuditTime(task.getAuditTime());
        vo.setCreateTime(task.getCreateTime());
        vo.setContent(task.getContent());
        vo.setMeasure(task.getMeasure());
    }

    private RectificationFeedbackVO toFeedbackVO(RectificationFeedback feedback, Map<Long, String> names) {
        RectificationFeedbackVO vo = new RectificationFeedbackVO();
        vo.setId(feedback.getId());
        vo.setFeedbackType(feedback.getFeedbackType());
        vo.setFeedbackTypeLabel(feedbackTypeLabel(feedback.getFeedbackType()));
        vo.setContent(feedback.getContent());
        vo.setBeforeDesc(feedback.getBeforeDesc());
        vo.setAfterDesc(feedback.getAfterDesc());
        vo.setImages(readImages(feedback.getImagesJson()));
        vo.setActualSaving(feedback.getActualSaving());
        vo.setSubmitBy(feedback.getSubmitBy());
        vo.setSubmitByName(names.get(feedback.getSubmitBy()));
        vo.setSubmitTime(feedback.getSubmitTime());
        vo.setVerifyResult(feedback.getVerifyResult());
        vo.setVerifyRemark(feedback.getVerifyRemark());
        vo.setVerifyTime(feedback.getVerifyTime());
        return vo;
    }

    private Map<Long, String> userNames(List<Long> ids) {
        Set<Long> filtered = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (filtered.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(filtered).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        user -> displayName(user), (a, b) -> a));
    }

    private Map<Long, String> lineNames(Set<Long> ids) {
        Set<Long> filtered = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (filtered.isEmpty()) {
            return Map.of();
        }
        return lineMapper.selectBatchIds(filtered).stream()
                .collect(Collectors.toMap(EnergyLine::getId, EnergyLine::getName, (a, b) -> a));
    }

    private boolean isOverdue(RectificationTask task) {
        if (task.getDeadline() == null) {
            return false;
        }
        int status = task.getStatus() == null ? 0 : task.getStatus();
        boolean active = status == 1 || status == 2 || status == 3 || status == 7;
        return active && task.getDeadline().isBefore(LocalDateTime.now());
    }

    private String priorityLabel(Integer priority) {
        if (priority == null) {
            return "中";
        }
        return switch (priority) {
            case 1 -> "低";
            case 3 -> "高";
            case 4 -> "紧急";
            default -> "中";
        };
    }

    private String feedbackTypeLabel(Integer type) {
        if (type == null) {
            return "反馈";
        }
        return switch (type) {
            case 1 -> "进度";
            case 2 -> "完工";
            case 3 -> "审核";
            case 4 -> "驳回";
            case 5 -> "效果验证";
            default -> "反馈";
        };
    }

    private String displayName(SysUser user) {
        return user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private List<String> readImages(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    public Map<String, Long> statusCounts(Long enterpriseId) {
        QueryWrapper<RectificationTask> wrapper = new QueryWrapper<>();
        wrapper.select("status", "COUNT(*) AS cnt").eq("deleted", 0);
        if (enterpriseId != null) {
            wrapper.eq("enterprise_id", enterpriseId);
        }
        wrapper.groupBy("status");
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : taskMapper.selectMaps(wrapper)) {
            result.put(RectificationStatus.labelOf(((Number) row.get("status")).intValue()),
                    ((Number) row.get("cnt")).longValue());
        }
        return result;
    }
}
