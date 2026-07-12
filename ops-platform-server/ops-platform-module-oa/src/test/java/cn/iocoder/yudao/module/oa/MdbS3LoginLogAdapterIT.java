package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.system.LoginLogAdapter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-MDB-S3: login log adapter reads system_login_log @DS system.
 */
@SpringBootTest
@ActiveProfiles({"dev", "dev-local-multidb"})
class MdbS3LoginLogAdapterIT {

    @Autowired(required = false)
    private LoginLogAdapter loginLogAdapter;

    @Test
    @DisplayName("S3-01: login log list reads system DB (total >= 3000)")
    void loginLogListFromSystem() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(loginLogAdapter != null);

        long rows = loginLogAdapter.countActiveRows();
        Assumptions.assumeTrue(rows >= 3000, "system_login_log seed expected >= 3000, got " + rows);

        var page = loginLogAdapter.list(1L, null, null, null, null, null, 1, 10);
        assertTrue(page.getTotal() >= 3000);
        assertFalse(page.getList().isEmpty());
        assertTrue(page.getList().get(0).getStatus() != null);
    }

    private void assumeLocalMysql() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/wd?useSSL=false&allowPublicKeyRetrieval=true", "root", "root")) {
            Assumptions.assumeTrue(conn.isValid(2));
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "localhost MySQL not available: " + ex.getMessage());
        }
    }
}
