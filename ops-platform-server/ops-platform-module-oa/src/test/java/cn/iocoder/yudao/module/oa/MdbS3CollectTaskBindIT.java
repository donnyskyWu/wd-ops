package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.service.collect.CollectTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * GATE-MDB-S3: collect task bind fields mpAccountId / oaAccountId.
 */
@SpringBootTest
@ActiveProfiles({"dev", "dev-local-multidb"})
class MdbS3CollectTaskBindIT {

    private static final Long TENANT = 1L;

    @Autowired(required = false)
    private CollectTaskService collectTaskService;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("S3-03: collect task page exposes bind id fields")
    void collectTaskPageHasBindFields() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(collectTaskService != null);

        var page = collectTaskService.page(null, null, null, null, null, 1, 10);
        assertNotNull(page.getList());
        page.getList().forEach(vo -> {
            if ("WECHAT_OFFICIAL".equals(vo.getPlatformType()) && vo.getAccountId() != null) {
                org.junit.jupiter.api.Assertions.assertEquals(vo.getAccountId(), vo.getMpAccountId());
            }
        });
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
