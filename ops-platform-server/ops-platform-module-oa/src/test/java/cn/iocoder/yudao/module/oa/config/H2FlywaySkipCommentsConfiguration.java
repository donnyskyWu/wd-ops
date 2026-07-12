package cn.iocoder.yudao.module.oa.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * H2 test profile: skip MySQL-only migrations that fail under H2 (COMMENT syntax, PREPARE/EXECUTE, etc.).
 * MySQL dev/prod Flyway unchanged. H2 IT baseline stops at V124 then marks skipped versions applied.
 */
@Configuration
@Profile("test")
public class H2FlywaySkipCommentsConfiguration {

    /** MySQL-only migrations after V124 — metadata or cutover; not required for H2 unit/IT baseline. */
    private static final Set<String> H2_SKIP_VERSIONS = Set.of(
            "126", "127", "128", "129", "130", "131", "132", "133"
    );

    @Bean
    public FlywayMigrationStrategy h2FlywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            if (!isH2(dataSource)) {
                flyway.migrate();
                return;
            }
            Map<String, MigrationMeta> pending = resolveSkippedMigrations(flyway);
            Flyway.configure()
                    .configuration(flyway.getConfiguration())
                    .target("124")
                    .load()
                    .migrate();
            try {
                markSkippedMigrationsApplied(dataSource, pending);
                seedAuthorSsotForH2(dataSource);
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to mark H2-skipped Flyway migrations", ex);
            }
            Flyway.configure()
                    .configuration(flyway.getConfiguration())
                    .validateOnMigrate(false)
                    .load()
                    .migrate();
        };
    }

    private static Map<String, MigrationMeta> resolveSkippedMigrations(Flyway flyway) {
        Map<String, MigrationMeta> pending = new HashMap<>();
        for (MigrationInfo info : flyway.info().all()) {
            if (info.getVersion() == null) {
                continue;
            }
            String version = info.getVersion().getVersion();
            if (!H2_SKIP_VERSIONS.contains(version)) {
                continue;
            }
            pending.put(version, new MigrationMeta(
                    version,
                    info.getDescription(),
                    info.getScript(),
                    info.getChecksum()
            ));
        }
        return pending;
    }

    private static void markSkippedMigrationsApplied(DataSource dataSource, Map<String, MigrationMeta> migrations)
            throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            for (String version : H2_SKIP_VERSIONS) {
                MigrationMeta migration = migrations.get(version);
                if (migration == null || isVersionApplied(conn, version)) {
                    continue;
                }
                int rank = nextInstalledRank(conn);
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO flyway_schema_history
                        (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
                        VALUES (?, ?, ?, 'SQL', ?, ?, 'h2-skip', ?, 0, TRUE)
                        """)) {
                    ps.setInt(1, rank);
                    ps.setString(2, migration.version());
                    ps.setString(3, migration.description());
                    ps.setString(4, migration.script());
                    if (migration.checksum() == null) {
                        ps.setNull(5, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(5, migration.checksum());
                    }
                    ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                    ps.executeUpdate();
                }
            }
        }
    }

    private static int nextInstalledRank(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean isVersionApplied(Connection conn, String version) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = TRUE")) {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static void seedAuthorSsotForH2(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/h2-post124-author-ssot.sql"));
        }
    }

    private static boolean isH2(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductName().toUpperCase().contains("H2");
        } catch (SQLException ex) {
            return false;
        }
    }

    private record MigrationMeta(String version, String description, String script, Integer checksum) {
    }
}
