package cn.iocoder.yudao.module.oa.service.support;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * shenyu-system {@code system_users} reads on a dedicated bean.
 * Uses explicit JDBC against the {@code system} datasource to avoid master-transaction DS stickiness.
 */
@Component
@RequiredArgsConstructor
public class FootballSystemUserSystemReader {

    private static final String USER_BY_ID_SQL = """
            SELECT id, tenant_id, username, nickname, status
            FROM system_users
            WHERE id = ? AND deleted = 0
            LIMIT 1
            """;
    private static final String USER_BY_USERNAME_SQL = """
            SELECT id, tenant_id, username, nickname, status
            FROM system_users
            WHERE username = ? AND deleted = 0 AND status = 0
            LIMIT 1
            """;
    private static final String USERNAME_BY_ID_SQL = """
            SELECT username FROM system_users WHERE id = ? AND deleted = 0 LIMIT 1
            """;

    private final DynamicRoutingDataSource dynamicRoutingDataSource;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FootballSystemUserDO findById(Long userId) {
        if (userId == null) {
            return null;
        }
        return queryUser(USER_BY_ID_SQL, userId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String findUsernameById(Long userId) {
        if (userId == null) {
            return null;
        }
        JdbcTemplate jdbc = systemJdbc();
        if (jdbc == null) {
            return null;
        }
        return jdbc.query(USERNAME_BY_ID_SQL, rs -> rs.next() ? rs.getString(1) : null, userId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FootballSystemUserDO findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return queryUser(USER_BY_USERNAME_SQL, username);
    }

    private JdbcTemplate systemJdbc() {
        DataSource system = dynamicRoutingDataSource.getDataSource("system");
        if (system == null) {
            return null;
        }
        return new JdbcTemplate(system);
    }

    private FootballSystemUserDO queryUser(String sql, Object arg) {
        JdbcTemplate jdbc = systemJdbc();
        if (jdbc == null) {
            return null;
        }
        return jdbc.query(sql, rs -> rs.next() ? mapUser(rs) : null, arg);
    }

    private static FootballSystemUserDO mapUser(ResultSet rs) throws SQLException {
        FootballSystemUserDO user = new FootballSystemUserDO();
        user.setId(rs.getLong("id"));
        user.setTenantId(rs.getLong("tenant_id"));
        user.setUsername(rs.getString("username"));
        user.setNickname(rs.getString("nickname"));
        user.setStatus(rs.getObject("status") == null ? null : rs.getInt("status"));
        return user;
    }
}
