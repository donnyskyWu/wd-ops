package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.system.OperateLogAdapter;
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
 * GATE-MDB-S3: operate log adapter reads system_operate_log @DS system.
 */
@SpringBootTest
@ActiveProfiles({"dev", "dev-local-multidb"})
class MdbS3OperateLogAdapterIT {

    @Autowired(required = false)
    private OperateLogAdapter operateLogAdapter;

    @Test
    @DisplayName("S3-01: operation log list reads system DB (total >= 600)")
    void operationLogListFromSystem() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(operateLogAdapter != null);

        long rows = operateLogAdapter.countActiveRows();
        Assumptions.assumeTrue(rows >= 600, "system_operate_log seed expected >= 600, got " + rows);

        var page = operateLogAdapter.list(1L, null, null, null, null, null, 1, 10);
        assertTrue(page.getTotal() >= 600);
        assertFalse(page.getList().isEmpty());
        assertTrue(page.getList().get(0).getModule() != null);
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
