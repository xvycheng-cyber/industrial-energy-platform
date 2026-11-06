package com.smarten.module.masterdata.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.masterdata.dto.EnterpriseDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.service.EnterpriseService;
import com.smarten.module.masterdata.vo.EnterpriseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "企业")
@RestController
@RequestMapping("/master/enterprises")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @Operation(summary = "企业分页")
    @GetMapping
    @PreAuthorize("hasAuthority('master:enterprise:list')")
    public Result<PageResult<EnterpriseVO>> page(@Valid MasterQueryDTO query) {
        return Result.ok(enterpriseService.page(query));
    }

    @Operation(summary = "企业详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:enterprise:list')")
    public Result<EnterpriseVO> detail(@PathVariable Long id) {
        return Result.ok(enterpriseService.detail(id));
    }

    @Operation(summary = "新增企业")
    @PostMapping
    @PreAuthorize("hasAuthority('master:enterprise:create')")
    public Result<Long> create(@Valid @RequestBody EnterpriseDTO dto) {
        return Result.ok(enterpriseService.create(dto));
    }

    @Operation(summary = "修改企业")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:enterprise:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EnterpriseDTO dto) {
        enterpriseService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除企业")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('master:enterprise:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        enterpriseService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "启用/停用企业")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:enterprise:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        enterpriseService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
