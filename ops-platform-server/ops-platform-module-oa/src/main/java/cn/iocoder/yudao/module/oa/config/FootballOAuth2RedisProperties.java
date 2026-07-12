package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Football system-server OAuth2 token Redis (ADR-047 §5.3 · INTEGRATION-PROGRESS §23 #1).
 * Align with {@code scripts/integration-config/football-integration-overlay.yml}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "oa.auth.football-redis")
public class FootballOAuth2RedisProperties {

    /** When false, skip Redis lookup (standalone :8080 dev-token path). */
    private boolean enabled = false;

    private String host = "127.0.0.1";

    private int port = 6379;

    private int database = 0;

    private String password = "";

    private int timeoutMs = 2000;
}
