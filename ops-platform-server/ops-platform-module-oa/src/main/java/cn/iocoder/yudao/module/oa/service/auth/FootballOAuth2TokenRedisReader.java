package cn.iocoder.yudao.module.oa.service.auth;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.config.FootballOAuth2RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Reads Football OAuth2 access tokens from Redis ({@code oauth2_access_token:{token}}).
 * Matches football-module-system {@code OAuth2AccessTokenRedisDAO}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FootballOAuth2TokenRedisReader {

    private static final String REDIS_KEY_FORMAT = "oauth2_access_token:%s";
    private static final DateTimeFormatter EXPIRES_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FootballOAuth2RedisProperties properties;

    public Optional<FootballOAuth2TokenSnapshot> getToken(String accessToken) {
        if (!properties.isEnabled() || accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        try (Jedis jedis = openJedis()) {
            String json = jedis.get(String.format(REDIS_KEY_FORMAT, accessToken));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(parseSnapshot(json));
        } catch (Exception ex) {
            log.warn("Football OAuth2 Redis lookup failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Jedis openJedis() {
        Jedis jedis = new Jedis(properties.getHost(), properties.getPort(), properties.getTimeoutMs());
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            jedis.auth(properties.getPassword());
        }
        jedis.select(properties.getDatabase());
        return jedis;
    }

    private FootballOAuth2TokenSnapshot parseSnapshot(String json) {
        JSONObject obj = JSONUtil.parseObj(json);
        FootballOAuth2TokenSnapshot snapshot = new FootballOAuth2TokenSnapshot();
        snapshot.setUserId(obj.getLong("userId"));
        snapshot.setTenantId(obj.getLong("tenantId"));
        snapshot.setUserType(obj.getInt("userType"));
        snapshot.setExpiresTime(parseExpiresTime(obj.get("expiresTime")));
        return snapshot;
    }

    private LocalDateTime parseExpiresTime(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(number.longValue()), ZoneId.systemDefault());
        }
        if (raw instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        String text = raw.toString();
        if (text.isBlank()) {
            return null;
        }
        if (text.chars().allMatch(Character::isDigit)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(text)), ZoneId.systemDefault());
        }
        if (text.contains("T")) {
            return LocalDateTime.parse(text);
        }
        return LocalDateTime.parse(text, EXPIRES_FORMAT);
    }
}
