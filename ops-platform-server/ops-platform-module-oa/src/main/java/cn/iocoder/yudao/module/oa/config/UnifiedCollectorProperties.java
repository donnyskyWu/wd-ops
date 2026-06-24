package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * unify-collector-api 客户端配置（Channel-A · ADR-047）。
 *
 * <p>{@link #stub}：{@code true} 时不发起 HTTP（IT 默认）；真实 E2E 联调设 {@code false}，
 * 并配置 {@link #baseUrl} / {@link #apiToken}。示例见 {@code application-dev-local.yml.example}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "oa.unified-collector")
public class UnifiedCollectorProperties {

    /** unify-collector-api 基址 */
    private String baseUrl = "http://127.0.0.1:8000";

    /** Bearer API_TOKEN */
    private String apiToken = "test-key-2026";

    /** HTTP 超时毫秒 */
    private int timeoutMs = 30_000;

    /** {@code true} = 不调用真实 collector（stub import/health）；{@code false} = 真实 HTTP 联调 */
    private boolean stub = false;
}
