package cn.iocoder.yudao.module.oa.service.file;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskAttachmentVO;
import cn.iocoder.yudao.module.oa.config.OaFileProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final String DOWNLOAD_PREFIX = "/admin-api/oa/file/download?key=";

    private final OaFileProperties fileProperties;

    public TaskAttachmentVO storeTaskAttachment(MultipartFile file, Long tenantId, Long taskId) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > fileProperties.getMaxFileSize()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文件大小超过限制");
        }
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "file");
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String relativeKey = tenantId + "/task/" + taskId + "/" + IdUtil.fastSimpleUUID() + "_" + safeName;
        Path target = resolvePath(relativeKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文件保存失败");
        }
        TaskAttachmentVO vo = new TaskAttachmentVO();
        vo.setName(originalName);
        vo.setUrl(DOWNLOAD_PREFIX + relativeKey);
        return vo;
    }

    public Path resolveReadablePath(String key, Long tenantId) {
        if (StrUtil.isBlank(key) || key.contains("..")) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文件路径非法");
        }
        if (!key.startsWith(tenantId + "/")) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        Path path = resolvePath(key);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "文件不存在");
        }
        return path;
    }

    private Path resolvePath(String relativeKey) {
        Path root = Paths.get(fileProperties.getUploadDir()).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativeKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文件路径非法");
        }
        return resolved;
    }
}
