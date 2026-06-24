package cn.iocoder.yudao.module.oa.service.config;

import java.util.Set;

/**
 * M8 INTERNAL 平台类 Tab 退役（ADR-047 · M8-COL hard cut）。
 * 企微 / 个微奥创（Channel-B/C）不受影响。
 */
public final class InternalCollectPlatformSupport {

    /** M8 已退役的平台 Tab（凭证 SSOT → M4 oa_account） */
    public static final Set<String> DEPRECATED_PLATFORM_TAB_TYPES = Set.of(
            "WECHAT_OFFICIAL", "DOUYIN", "KUAISHOU", "WECHAT_VIDEO", "SERVICE_ACCOUNT");

  /** Legacy seed sub_type values grouped under the platform tab. */
    public static final Set<String> LEGACY_PLATFORM_SUB_TYPES = Set.of(
            "ACCOUNT_METRICS", "CONTENT_METRICS", "LIVE_METRICS");

    private InternalCollectPlatformSupport() {
    }

    public static boolean isDeprecatedPlatformTab(String platformType, String subType) {
        if ("WEWORK".equals(platformType) || "PERSONAL_WECHAT".equals(platformType)) {
            return false;
        }
        if (platformType != null && DEPRECATED_PLATFORM_TAB_TYPES.contains(platformType)) {
            return true;
        }
        return "platform".equals(subType) || LEGACY_PLATFORM_SUB_TYPES.contains(subType);
    }

    public static boolean isDeprecatedPlatformCollectRow(String scope, String platformType, String subType) {
        return CollectConfigScope.INTERNAL.equals(scope) && isDeprecatedPlatformTab(platformType, subType);
    }
}
