package cn.iocoder.yudao.module.oa.service.sop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.Collections;
import java.util.List;

public final class SopJsonHelper {

    private SopJsonHelper() {
    }

    public static String toJson(List<Long> predecessors) {
        if (predecessors == null || predecessors.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(predecessors);
    }

    public static List<Long> fromJson(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, Long.class);
    }
}
