package cn.iocoder.yudao.module.oa.controller.file;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.service.file.LocalFileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/admin-api/oa/file")
@RequiredArgsConstructor
public class FileController {

    private final LocalFileStorageService localFileStorageService;

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
}
