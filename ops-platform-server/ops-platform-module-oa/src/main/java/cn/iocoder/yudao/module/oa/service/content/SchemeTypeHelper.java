package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class SchemeTypeHelper {

    private static final String SEPARATOR = ",";

    private SchemeTypeHelper() {
    }

    public static String toStored(List<String> schemeTypes) {
        if (schemeTypes == null || schemeTypes.isEmpty()) {
            return null;
        }
        return schemeTypes.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.joining(SEPARATOR));
    }

    public static List<String> fromStored(String stored) {
        if (StrUtil.isBlank(stored)) {
            return Collections.emptyList();
        }
        return Arrays.stream(stored.split(SEPARATOR))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
