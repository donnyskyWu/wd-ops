package cn.iocoder.yudao.module.oa.service.collect.unified;

import java.util.Map;
import java.util.Set;

/**
 * Channel-A 统一扫码登录平台白名单（unify-collector-api {@code QrCodeRequest.platform}）。
 */
public final class CollectorQrLoginSupport {

    public static final Set<String> QR_SUPPORTED_OA_PLATFORMS = Set.of(
            "WECHAT_OFFICIAL", "WECHAT_VIDEO", "DOUYIN", "KUAISHOU", "XIAOHONGSHU");

    private static final Map<String, String> OA_TO_COLLECTOR = Map.of(
            "WECHAT_OFFICIAL", "wechat_mp",
            "WECHAT_VIDEO", "wechat_channels",
            "DOUYIN", "douyin",
            "KUAISHOU", "kuaishou",
            "XIAOHONGSHU", "xiaohongshu");

    private CollectorQrLoginSupport() {
    }

    public static boolean supportsQrLogin(String oaPlatformType) {
        return QR_SUPPORTED_OA_PLATFORMS.contains(oaPlatformType);
    }

    public static String resolveCollectorPlatform(String oaPlatformType) {
        return OA_TO_COLLECTOR.get(oaPlatformType);
    }
}
