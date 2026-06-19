package cn.iocoder.yudao.module.oa.util;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ImageKeyHelper {

    public static final String FILE_VIEW_PREFIX = "/admin-api/oa/file/view?key=";

    private ImageKeyHelper() {
    }

    public static String sanitizeImageKey(String key, Long tenantId) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        if (key.contains("..") || !key.startsWith(tenantId + "/")) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN.getCode(), "图片文件无效");
        }
        return key;
    }

    public static List<String> sanitizeImageKeys(List<String> keys, Long tenantId) {
        if (keys == null) {
            return null;
        }
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        return keys.stream()
                .map(key -> sanitizeImageKey(key, tenantId))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    public static String toFileViewUrl(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return FILE_VIEW_PREFIX + key;
    }

    public static List<String> toFileViewUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        return keys.stream()
                .map(ImageKeyHelper::toFileViewUrl)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
