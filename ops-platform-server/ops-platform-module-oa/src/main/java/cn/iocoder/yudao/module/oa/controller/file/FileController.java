package cn.iocoder.yudao.module.oa.controller.file;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.file.FileUploadVO;
import cn.iocoder.yudao.module.oa.service.file.LocalFileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@RestController
@RequestMapping("/admin-api/oa/file")
@RequiredArgsConstructor
public class FileController {

    private final LocalFileStorageService localFileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return CommonResult.success(localFileStorageService.storeContentImage(file, tenantId));
    }

    @GetMapping("/view")
    public void view(@RequestParam("key") String key, HttpServletResponse response) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        Path path = localFileStorageService.resolveReadablePath(key, tenantId);
        try (InputStream in = Files.newInputStream(path)) {
            response.setContentType(resolveImageMediaType(path.getFileName().toString()));
            response.setHeader("Content-Disposition", "inline");
            FileCopyUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文件读取失败");
        }
    }

    @GetMapping("/download")
    public void download(@RequestParam("key") String key, HttpServletResponse response) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        Path path = localFileStorageService.resolveReadablePath(key, tenantId);
        String fileName = path.getFileName().toString();
        int idx = fileName.indexOf('_');
        if (idx >= 0 && idx < fileName.length() - 1) {
            fileName = fileName.substring(idx + 1);
        }
        try (InputStream in = Files.newInputStream(path)) {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            FileCopyUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文件下载失败");
        }
    }

    private static String resolveImageMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }
}
