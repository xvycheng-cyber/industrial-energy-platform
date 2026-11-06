package com.smarten.module.diagnosis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.module.diagnosis.dto.RuleUpdateRequest;
import com.smarten.module.diagnosis.entity.DiagnosisRule;
import com.smarten.module.diagnosis.engine.RuleConfig;
import com.smarten.module.diagnosis.engine.RuleJson;
import com.smarten.module.diagnosis.mapper.DiagnosisRuleMapper;
import com.smarten.module.diagnosis.vo.DiagnosisRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则管理：JSON 白名单校验、版本递增、启停；不执行任何 SQL 或脚本。
 */
@Service
@RequiredArgsConstructor
public class DiagnosisRuleService {

    private final DiagnosisRuleMapper ruleMapper;
    private final ObjectMapper objectMapper;

    public List<DiagnosisRuleVO> list() {
        return ruleMapper.selectList(new LambdaQueryWrapper<DiagnosisRule>()
                        .orderByAsc(DiagnosisRule::getPriority))
                .stream().map(this::toVO).toList();
    }

    public List<RuleConfig> enabledRules() {
        List<RuleConfig> configs = new ArrayList<>();
        for (DiagnosisRule rule : ruleMapper.selectList(new LambdaQueryWrapper<DiagnosisRule>()
                .eq(DiagnosisRule::getEnabled, 1)
                .orderByAsc(DiagnosisRule::getPriority))) {
            try {
                configs.add(parse(rule));
            } catch (IllegalArgumentException ignored) {
                // 单条规则配置非法时跳过，不阻断整体诊断
            }
        }
        return configs;
    }

    public RuleConfig parse(DiagnosisRule rule) {
        Map<String, Object> condition = read(rule.getConditionJson());
        Map<String, Object> threshold = read(rule.getThresholdJson());
        return RuleJson.parse(rule.getRuleCode(), rule.getRuleName(), rule.getAnomalyType(),
                rule.getPriority() == null ? 100 : rule.getPriority(),
                rule.getWeight() == null ? 1 : rule.getWeight().doubleValue(),
                rule.getConditionJson(), rule.getThresholdJson(), condition, threshold);
    }

    @Transactional(rollbackFor = Exception.class)
    public DiagnosisRuleVO update(Long id, RuleUpdateRequest request) {
        DiagnosisRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "规则不存在");
        }
        Map<String, Object> condition = request.getCondition() == null
                ? read(rule.getConditionJson()) : request.getCondition();
        Map<String, Object> threshold = request.getThreshold() == null
                ? read(rule.getThresholdJson()) : request.getThreshold();
        try {
            RuleJson.validate(condition);
            RuleJson.validate(threshold);
            RuleJson.parse(rule.getRuleCode(), rule.getRuleName(), rule.getAnomalyType(),
                    request.getPriority() == null ? rule.getPriority() : request.getPriority(),
                    request.getWeight() == null ? 1 : request.getWeight(),
                    null, null, condition, threshold);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, exception.getMessage());
        }
        rule.setConditionJson(write(condition));
        rule.setThresholdJson(write(threshold));
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }
        if (request.getWeight() != null) {
            rule.setWeight(java.math.BigDecimal.valueOf(request.getWeight()));
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled() ? 1 : 0);
        }
        if (request.getRemark() != null) {
            rule.setRemark(request.getRemark());
        }
        rule.setVersion(rule.getVersion() == null ? 1 : rule.getVersion() + 1);
        ruleMapper.updateById(rule);
        return toVO(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggle(Long id, boolean enabled) {
        DiagnosisRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "规则不存在");
        }
        rule.setEnabled(enabled ? 1 : 0);
        ruleMapper.updateById(rule);
    }

    private DiagnosisRuleVO toVO(DiagnosisRule rule) {
        DiagnosisRuleVO vo = new DiagnosisRuleVO();
        vo.setId(rule.getId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setAnomalyType(rule.getAnomalyType());
        vo.setCondition(read(rule.getConditionJson()));
        vo.setThreshold(read(rule.getThresholdJson()));
        vo.setWeight(rule.getWeight() == null ? 1 : rule.getWeight().doubleValue());
        vo.setPriority(rule.getPriority());
        vo.setVersion(rule.getVersion());
        vo.setEnabled(rule.getEnabled());
        vo.setCauseTemplate(rule.getCauseTemplate());
        vo.setSuggestionTemplate(rule.getSuggestionTemplate());
        vo.setSavingFormula(rule.getSavingFormula());
        vo.setRemark(rule.getRemark());
        return vo;
    }

    private Map<String, Object> read(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    private String write(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
