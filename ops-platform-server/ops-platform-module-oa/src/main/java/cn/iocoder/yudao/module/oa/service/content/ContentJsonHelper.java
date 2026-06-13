package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ContentJsonHelper {

    private ContentJsonHelper() {
    }

    static String toPlatformTypesJson(List<String> platformTypes) {
        if (platformTypes == null || platformTypes.isEmpty()) {
            return null;
        }
        return JSONUtil.toJsonStr(platformTypes);
    }

    static String toAccountIdsJson(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return null;
        }
        return JSONUtil.toJsonStr(accountIds);
    }

    static List<String> fromPlatformTypesJson(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, String.class);
    }

    static List<Long> fromAccountIdsJson(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : JSONUtil.parseArray(json)) {
            if (item == null) {
                continue;
            }
            ids.add(Long.valueOf(String.valueOf(item)));
        }
        return ids;
    }
}
