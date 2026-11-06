package com.smarten.module.masterdata.controller;

import com.smarten.common.api.PageResult;
import com.smarten.common.api.Result;
import com.smarten.common.dto.StatusDTO;
import com.smarten.module.masterdata.dto.DeviceDTO;
import com.smarten.module.masterdata.dto.MasterQueryDTO;
import com.smarten.module.masterdata.service.DeviceService;
import com.smarten.module.masterdata.vo.DeviceVO;
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

@Tag(name = "设备")
@RestController
@RequestMapping("/master/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "设备分页")
    @GetMapping
    @PreAuthorize("hasAuthority('master:device:list')")
    public Result<PageResult<DeviceVO>> page(@Valid MasterQueryDTO query) {
        return Result.ok(deviceService.page(query));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('master:device:list')")
    public Result<DeviceVO> detail(@PathVariable Long id) {
        return Result.ok(deviceService.detail(id));
    }

    @Operation(summary = "新增设备")
    @PostMapping
    @PreAuthorize("hasAuthority('master:device:create')")
    public Result<Long> create(@Valid @RequestBody DeviceDTO dto) {
        return Result.ok(deviceService.create(dto));
    }

    @Operation(summary = "修改设备")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:device:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeviceDTO dto) {
        deviceService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('master:device:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "设备状态变更（运行/停机/检修/停用）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('master:device:update')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        deviceService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
