package cn.iocoder.yudao.module.oa.db.migration;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * ADR-047 · M8-COL hard cut：INTERNAL 平台类凭证合并至 oa_account 并软删 oa_collect_config 行。
 * 使用 Java 迁移以复用 AES-256（明文 cookie → cookie_encrypted）。
 */
@Component
public class V113__migrate_internal_collect_to_m4 extends BaseJavaMigration {

    private final String aesKeyBase64;

    public V113__migrate_internal_collect_to_m4(@Value("${oa.crypto.aes-key}") String aesKeyBase64) {
        this.aesKeyBase64 = aesKeyBase64;
    }

    @Override
    public void migrate(Context context) throws Exception {
        AES aes = SecureUtil.aes(Base64.getDecoder().decode(aesKeyBase64));
        Connection conn = context.getConnection();

        String selectSql = """
                SELECT c.id, c.account_id, c.platform_type, c.sub_type, c.app_id, c.app_secret_encrypted,
                       c.auth_token_encrypted, c.cookie, c.field_mapping
                FROM oa_collect_config c
                WHERE c.scope = 'INTERNAL' AND c.deleted = 0
                  AND c.platform_type IN ('WECHAT_OFFICIAL','DOUYIN','KUAISHOU','WECHAT_VIDEO','SERVICE_ACCOUNT')
                  AND (c.sub_type = 'platform'
                       OR c.sub_type IN ('ACCOUNT_METRICS','CONTENT_METRICS','LIVE_METRICS')
                       OR c.sub_type IS NULL)
                """;

        try (PreparedStatement ps = conn.prepareStatement(selectSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long configId = rs.getLong("id");
                Long accountId = rs.getObject("account_id", Long.class);
                if (accountId != null) {
                    mergeToAccount(conn, aes, accountId, rs);
                }
                softDeleteConfig(conn, configId);
            }
        }
    }

    private void mergeToAccount(Connection conn, AES aes, long accountId, ResultSet collectRow) throws SQLException {
        String platformType = collectRow.getString("platform_type");
        String collectAppId = collectRow.getString("app_id");
        String collectAppSecret = collectRow.getString("app_secret_encrypted");
        String collectAuthToken = collectRow.getString("auth_token_encrypted");
        String collectCookie = collectRow.getString("cookie");
        String collectFieldMapping = collectRow.getString("field_mapping");

        AccountSnapshot account = loadAccount(conn, accountId);
        if (account == null) {
            return;
        }

        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (StrUtil.isBlank(account.appId) && StrUtil.isNotBlank(collectAppId)) {
            sets.add("app_id = ?");
            params.add(collectAppId);
        }
        if (StrUtil.isBlank(account.appSecretEncrypted) && StrUtil.isNotBlank(collectAppSecret)) {
            sets.add("app_secret_encrypted = ?");
            params.add(collectAppSecret);
        }
        if (StrUtil.isBlank(account.fieldMapping) && StrUtil.isNotBlank(collectFieldMapping)) {
            sets.add("field_mapping = ?");
            params.add(collectFieldMapping);
        }
        if (StrUtil.isNotBlank(collectAuthToken)) {
            if (isWechatMpPlatform(platformType) && StrUtil.isBlank(account.mpTokenEncrypted)) {
                sets.add("mp_token_encrypted = ?");
                params.add(collectAuthToken);
            } else if ("KUAISHOU".equals(platformType) && StrUtil.isBlank(account.authTokenEncrypted)) {
                sets.add("auth_token_encrypted = ?");
                params.add(collectAuthToken);
            }
        }
        if (StrUtil.isBlank(account.cookieEncrypted) && StrUtil.isNotBlank(collectCookie)) {
            sets.add("cookie_encrypted = ?");
            params.add(encryptCookieIfNeeded(aes, collectCookie));
        }

        if (sets.isEmpty()) {
            return;
        }

        sets.add("updater = 'v113-migrate'");
        sets.add("update_time = CURRENT_TIMESTAMP");

        String sql = "UPDATE oa_account SET " + String.join(", ", sets) + " WHERE id = ? AND deleted = 0";
        params.add(accountId);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
        }
    }

    private static boolean isWechatMpPlatform(String platformType) {
        return "WECHAT_OFFICIAL".equals(platformType) || "SERVICE_ACCOUNT".equals(platformType);
    }

    private static String encryptCookieIfNeeded(AES aes, String cookie) {
        if (StrUtil.isBlank(cookie)) {
            return null;
        }
        try {
            aes.decryptStr(cookie, StandardCharsets.UTF_8);
            return cookie;
        } catch (Exception ignored) {
            return aes.encryptBase64(cookie, StandardCharsets.UTF_8);
        }
    }

    private static AccountSnapshot loadAccount(Connection conn, long accountId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT app_id, app_secret_encrypted, mp_token_encrypted, auth_token_encrypted, cookie_encrypted, field_mapping "
                        + "FROM oa_account WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                AccountSnapshot snapshot = new AccountSnapshot();
                snapshot.appId = rs.getString("app_id");
                snapshot.appSecretEncrypted = rs.getString("app_secret_encrypted");
                snapshot.mpTokenEncrypted = rs.getString("mp_token_encrypted");
                snapshot.authTokenEncrypted = rs.getString("auth_token_encrypted");
                snapshot.cookieEncrypted = rs.getString("cookie_encrypted");
                snapshot.fieldMapping = rs.getString("field_mapping");
                return snapshot;
            }
        }
    }

    private static void softDeleteConfig(Connection conn, long configId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE oa_collect_config SET deleted = 1, status = 'DISABLED', updater = 'v113-migrate', "
                        + "update_time = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setLong(1, configId);
            ps.executeUpdate();
        }
    }

    private static final class AccountSnapshot {
        private String appId;
        private String appSecretEncrypted;
        private String mpTokenEncrypted;
        private String authTokenEncrypted;
        private String cookieEncrypted;
        private String fieldMapping;
    }
}
