package cn.iocoder.yudao.module.oa.service.perf;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.service.dict.DictService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PerfTemplatePositionSupport {

    private static final String DICT_POSITION = "dict_position";

    private PerfTemplatePositionSupport() {
    }

    public static List<String> parse(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        return JSONUtil.toList(JSONUtil.parseArray(json), String.class);
    }

    public static String toJson(List<String> positions) {
        if (positions == null || positions.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(normalize(positions));
    }

    public static List<String> normalize(List<String> positions) {
        if (positions == null || positions.isEmpty()) {
            return List.of();
        }
        return positions.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new));
    }

    public static boolean contains(String json, String position) {
        if (StrUtil.isBlank(position)) {
            return false;
        }
        return parse(json).contains(position);
    }

    public static void validatePositions(List<String> positions, DictService dictService) {
        List<String> normalized = normalize(positions);
        if (normalized.isEmpty()) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
        for (String position : normalized) {
            if (!dictService.isValidValue(DICT_POSITION, position)) {
                throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
            }
        }
    }

    public static String joinLabels(List<String> positions, Map<String, String> positionLabels) {
        return normalize(positions).stream()
                .map(position -> positionLabels.getOrDefault(position, position))
                .collect(Collectors.joining("、"));
    }
}
