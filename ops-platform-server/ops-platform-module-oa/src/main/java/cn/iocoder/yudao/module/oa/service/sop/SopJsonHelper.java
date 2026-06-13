package cn.iocoder.yudao.module.oa.service.sop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskAttachmentVO;

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

    public static String toAttachmentJson(List<TaskAttachmentVO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        return JSONUtil.toJsonStr(attachments);
    }

    public static List<TaskAttachmentVO> fromAttachmentJson(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, TaskAttachmentVO.class);
    }
}
