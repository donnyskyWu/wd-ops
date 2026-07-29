package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 采集 / Collector 错误信息可读化（错误码 → 中文说明）。
 */
public final class CollectorErrorMessages {

    private static final String REASON_MARKER = "[原因:";
    private static final Pattern UPSTREAM_ERROR_CODE = Pattern.compile("Error\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_IN_MESSAGE = Pattern.compile("code[=:]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /** unify-collector-api 业务错误码（README §5.2） */
    private static final Map<Integer, String> COLLECTOR_API_CODES = Map.ofEntries(
            Map.entry(40001, "参数错误，请检查请求字段"),
            Map.entry(40002, "Collector API 鉴权失败，请检查 API_TOKEN 配置"),
            Map.entry(40003, "权限不足，账号可能被禁用或存在跨租户访问"),
            Map.entry(40004, "资源不存在，请确认 account_id 或数据 ID 有效"),
            Map.entry(40005, "请求频率超限，请稍后重试"),
            Map.entry(40006, "Collector 服务内部错误，请联系运维"),
            Map.entry(40007, "上游平台采集失败（Cookie 失效、风控拦截、熔断或接口未实现）"),
            Map.entry(40008, "上游平台响应超时，请稍后重试")
    );

    /** 各平台上游 errCode（嵌在 Collector message 中） */
    private static final Map<Integer, String> UPSTREAM_PLATFORM_CODES = Map.of(
            300333, "视频号登录态/Cookie 已失效，请重新登录视频号助手并同步凭证"
    );

    private CollectorErrorMessages() {
    }

    public static String enrich(String rawMessage) {
        return enrich(rawMessage, 0);
    }

    /**
     * 在原始错误后追加 {@code [原因: …]}；已含原因标记则原样返回。
     */
    public static String enrich(String rawMessage, int businessCode) {
        if (StrUtil.isBlank(rawMessage) || rawMessage.contains(REASON_MARKER)) {
            return rawMessage;
        }
        String hint = resolveHint(rawMessage, businessCode);
        if (StrUtil.isBlank(hint)) {
            Integer extractedCode = extractErrorCode(rawMessage);
            if (extractedCode != null) {
                hint = "未知错误码 " + extractedCode + "，请联系运维";
            } else {
                return rawMessage;
            }
        }
        return rawMessage + " [原因: " + hint + "]";
    }

    static String resolveHint(String message, int businessCode) {
        Integer upstreamCode = extractErrorCode(message);
        if (upstreamCode != null) {
            String mapped = UPSTREAM_PLATFORM_CODES.get(upstreamCode);
            if (mapped != null) {
                return mapped;
            }
        }
        if (businessCode != 0) {
            String mapped = COLLECTOR_API_CODES.get(businessCode);
            if (mapped != null) {
                return mapped;
            }
        }
        return matchPattern(message);
    }

    private static String matchPattern(String message) {
        String lower = message.toLowerCase();
        if (message.contains("Collector 服务不可达")
                || lower.contains("connection refused")
                || lower.contains("connectexception")
                || lower.contains("collector api 空响应")) {
            return "unify-collector-api 未启动或网络不通，请确认服务已启动（默认 :8000）";
        }
        if (message.contains("认证过期")
                || lower.contains("authentication expired")
                || lower.contains("cookie_expired")
                || lower.contains("relogin_needed")
                || lower.contains("token_fail")
                || message.contains("凭证已失效")
                || message.contains("需重新登录")) {
            return "平台登录态已失效，请重新扫码登录并同步凭证";
        }
        if (message.contains("熔断")
                || lower.contains("circuit_broken")
                || lower.contains("circuit breaker")) {
            return "账号连续采集失败已熔断，请稍后重试或重新授权";
        }
        if (lower.contains("频率超限")
                || lower.contains("rate_limit")
                || lower.contains("rate limit")
                || lower.contains("too many requests")) {
            return "请求过于频繁，请稍后重试";
        }
        if (message.contains("模拟采集") || lower.contains("stub mode")) {
            return "OPS 采集 stub 模式已开启，未调用真实 Collector";
        }
        if (message.contains("功能未实现") || message.contains("未接入") || message.contains("后台通道")) {
            return "该采集能力尚未接入或依赖后台通道，请联系运维确认";
        }
        return null;
    }

    static Integer extractErrorCode(String message) {
        Matcher upstream = UPSTREAM_ERROR_CODE.matcher(message);
        if (upstream.find()) {
            return parseCode(upstream.group(1));
        }
        Matcher inline = CODE_IN_MESSAGE.matcher(message);
        if (inline.find()) {
            return parseCode(inline.group(1));
        }
        return null;
    }

    private static Integer parseCode(String digits) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
