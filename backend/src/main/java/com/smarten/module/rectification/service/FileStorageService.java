package com.smarten.module.rectification.service;

import com.smarten.common.api.ResultCode;
import com.smarten.common.exception.BusinessException;
import com.smarten.common.util.SecurityUtils;
import com.smarten.module.rectification.entity.SysFile;
import com.smarten.module.rectification.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 统一文件上传：仅允许图片、单文件 ≤5MB，落盘 + sys_file 记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final SysFileMapper fileMapper;

    @Value("${smarten.upload.dir:uploads}")
    private String uploadDir;

    public SysFile saveImage(MultipartFile file, String bizType, Long enterpriseId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_TYPES.contains(contentType) || !ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg / png / webp 图片");
        }

        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(day).resolve(fileName);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException exception) {
            log.error("文件保存失败", exception);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片保存失败");
        }

        SysFile entity = new SysFile();
        entity.setEnterpriseId(enterpriseId);
        entity.setBizType(bizType == null ? "RECTIFY" : bizType);
        entity.setOriginalName(original);
        entity.setStoredPath(day + "/" + fileName);
        entity.setUrl("/uploads/" + day + "/" + fileName);
        entity.setContentType(contentType);
        entity.setFileSize(file.getSize());
        entity.setUploaderId(SecurityUtils.getUserIdOrNull());
        fileMapper.insert(entity);
        return entity;
    }
}
