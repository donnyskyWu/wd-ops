package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可选 Live Smoke：探测真实 unify-collector-api {@code GET /livez}。
 *
 * <p>默认 CI 跳过。本地运行步骤：
 * <ol>
 *   <li>启动 unify-collector-api（默认 {@code http://127.0.0.1:8000}，Token {@code test-key-2026}）</li>
 *   <li>设置环境变量 {@code UNIFY_COLLECTOR_LIVE=true}</li>
 *   <li>可选：{@code OA_UNIFIED_COLLECTOR_BASE_URL}、{@code OA_UNIFIED_COLLECTOR_API_TOKEN}</li>
 *   <li>{@code mvn test -Dtest=M10ApiCollectorLiveSmokeIT}</li>
 * </ol>
 */
@EnabledIfEnvironmentVariable(named = "UNIFY_COLLECTOR_LIVE", matches = "true")
class M10ApiCollectorLiveSmokeIT extends OaITBase {

    @Autowired
    private UnifiedCollectorApiClient unifiedCollectorApiClient;

    @DynamicPropertySource
    static void liveCollectorProperties(DynamicPropertyRegistry registry) {
        registry.add("oa.unified-collector.stub", () -> "false");
        registry.add("oa.unified-collector.base-url",
                () -> System.getenv().getOrDefault("OA_UNIFIED_COLLECTOR_BASE_URL", "http://127.0.0.1:8000"));
        registry.add("oa.unified-collector.api-token",
                () -> System.getenv().getOrDefault("OA_UNIFIED_COLLECTOR_API_TOKEN", "test-key-2026"));
    }

    @Test
    @DisplayName("Live Smoke: unify-collector-api GET /livez")
    void collectorLivezReturnsOk() {
        assertTrue(unifiedCollectorApiClient.checkLive(), "collector /livez 应返回 2xx");
    }
}
