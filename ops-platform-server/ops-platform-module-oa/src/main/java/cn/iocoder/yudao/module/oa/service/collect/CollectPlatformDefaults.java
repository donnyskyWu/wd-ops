package cn.iocoder.yudao.module.oa.service.collect;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 采集任务平台默认映射：platform → source/method，以及全量采集 dataType 顺序。
 */
public final class CollectPlatformDefaults {

    public static final String DATA_TYPE_ALL = "ALL";
    /** 渠道粉丝统计（对应 task.dataType 为空时的 follower-stats 路由）。 */
    public static final String SENTINEL_FOLLOWER_STATS = "FOLLOWER_STATS";

    private static final String METHOD_INTERNAL = "INTERNAL";

    @Getter
    public static final class PlatformConfig {
        private final String source;
        private final String method;
        private final List<String> dataTypes;

        PlatformConfig(String source, String method, List<String> dataTypes) {
            this.source = source;
            this.method = method;
            this.dataTypes = dataTypes;
        }
    }

    private static final Map<String, PlatformConfig> BY_PLATFORM = new LinkedHashMap<>();

    static {
        BY_PLATFORM.put("WECHAT_OFFICIAL", new PlatformConfig(
                "WECHAT_MP_API", METHOD_INTERNAL,
                List.of("MP_FOLLOWER_STATS", "MP_FOLLOWER_LIST", "MP_ARTICLE_LIST",
                        "MP_ARTICLE_STATS", "MP_ARTICLE_CONTENT")));
        BY_PLATFORM.put("WECHAT_VIDEO", new PlatformConfig(
                "WECHAT_CHANNELS_API", METHOD_INTERNAL,
                List.of(SENTINEL_FOLLOWER_STATS, "WECHAT_VIDEO_LIST", "WECHAT_VIDEO_STATS")));
        BY_PLATFORM.put("DOUYIN", new PlatformConfig(
                "DOUYIN_OPEN_API", METHOD_INTERNAL,
                List.of(SENTINEL_FOLLOWER_STATS, "DOUYIN_FOLLOWER_LIST",
                        "DOUYIN_VIDEO_LIST", "DOUYIN_VIDEO_STATS")));
        BY_PLATFORM.put("KUAISHOU", new PlatformConfig(
                "KUAISHOU_OPEN_API", METHOD_INTERNAL,
                List.of(SENTINEL_FOLLOWER_STATS, "KUAISHOU_VIDEO_LIST", "KUAISHOU_VIDEO_STATS")));
        BY_PLATFORM.put("XIAOHONGSHU", new PlatformConfig(
                "XIAOHONGSHU_OPEN_API", METHOD_INTERNAL,
                List.of(SENTINEL_FOLLOWER_STATS, "XIAOHONGSHU_NOTE_LIST", "XIAOHONGSHU_NOTE_STATS")));
        BY_PLATFORM.put("BILIBILI", new PlatformConfig(
                "BILIBILI_OPEN_API", METHOD_INTERNAL,
                List.of(SENTINEL_FOLLOWER_STATS)));
        BY_PLATFORM.put("WEWORK", new PlatformConfig(
                "WECOM_API", METHOD_INTERNAL,
                List.of("WECOM_DAILY_STATS")));
        BY_PLATFORM.put("WECHAT_PERSONAL", new PlatformConfig(
                "AOCHUANG_API", METHOD_INTERNAL,
                List.of()));
    }

    private CollectPlatformDefaults() {
    }

    public static Optional<PlatformConfig> find(String platformType) {
        if (StrUtil.isBlank(platformType)) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_PLATFORM.get(platformType));
    }

    public static boolean isCollectAll(String dataType) {
        return StrUtil.isBlank(dataType) || DATA_TYPE_ALL.equalsIgnoreCase(dataType);
    }

    public static String resolveMethod(String platformType, String method) {
        if (StrUtil.isNotBlank(method)) {
            return method;
        }
        return find(platformType).map(PlatformConfig::getMethod).orElse(method);
    }

    public static String resolveSource(String platformType, String source) {
        if (StrUtil.isNotBlank(source)) {
            return source;
        }
        return find(platformType).map(PlatformConfig::getSource).orElse(source);
    }

    public static String normalizeStoredDataType(String dataType) {
        if (StrUtil.isBlank(dataType) || DATA_TYPE_ALL.equalsIgnoreCase(dataType)) {
            return null;
        }
        return dataType;
    }

    /**
     * 执行层 dataType：空/ALL → 按平台顺序全量；否则仅执行指定类型。
     */
    public static List<String> resolveExecutionDataTypes(CollectTaskDO task) {
        if (!isCollectAll(task.getDataType())) {
            return List.of(task.getDataType());
        }
        return find(task.getPlatformType())
                .filter(cfg -> StrUtil.equals(cfg.getSource(), task.getSource()))
                .map(PlatformConfig::getDataTypes)
                .orElse(List.of());
    }

    public static String normalizeSentinel(String dataType) {
        if (SENTINEL_FOLLOWER_STATS.equals(dataType)) {
            return null;
        }
        return dataType;
    }

    public static CollectTaskDO sliceWithDataType(CollectTaskDO task, String dataType) {
        CollectTaskDO slice = new CollectTaskDO();
        slice.setTenantId(task.getTenantId());
        slice.setPlatformType(task.getPlatformType());
        slice.setAccountId(task.getAccountId());
        slice.setMethod(task.getMethod());
        slice.setSource(task.getSource());
        slice.setDataType(normalizeSentinel(dataType));
        return slice;
    }
}
