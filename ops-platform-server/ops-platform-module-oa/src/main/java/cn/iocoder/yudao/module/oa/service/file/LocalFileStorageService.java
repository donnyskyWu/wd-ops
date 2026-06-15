package cn.iocoder.yudao.module.oa.service.file;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.file.FileUploadVO;
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
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final String DOWNLOAD_PREFIX = "/admin-api/oa/file/download?key=";
    private static final String VIEW_PREFIX = "/admin-api/oa/file/view?key=";
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

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

    public FileUploadVO storeContentImage(MultipartFile file, Long tenantId) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "图片大小不能超过 5MB");
        }
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "image.png");
        String ext = extensionOf(originalName);
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "仅支持 jpg/png/gif/webp 图片");
        }
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String relativeKey = tenantId + "/content/" + IdUtil.fastSimpleUUID() + "_" + safeName;
        Path target = resolvePath(relativeKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文件保存失败");
        }
        FileUploadVO vo = new FileUploadVO();
        vo.setName(originalName);
        vo.setKey(relativeKey);
        vo.setUrl(VIEW_PREFIX + relativeKey);
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

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
