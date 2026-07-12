package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.mysql.football.FootballPayAllOrderReadMapper;
import cn.iocoder.yudao.module.oa.service.football.FootballOrderReadService;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-MDB-S3: pay_all_order read-only @DS pay.
 */
@SpringBootTest
@ActiveProfiles({"dev", "dev-local-multidb"})
class MdbS3PayOrderReadIT {

    private static final Long TENANT = 1L;

    @Autowired(required = false)
    private FootballOrderReadService footballOrderReadService;
    @Autowired(required = false)
    private FootballPayAllOrderReadMapper footballPayAllOrderReadMapper;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("S3-04: football order list reads pay DB")
    void payOrderListCrossDb() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(footballOrderReadService != null && footballPayAllOrderReadMapper != null);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        long total = footballPayAllOrderReadMapper.countPage(TENANT,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay(), null, null);
        Assumptions.assumeTrue(total > 0, "pay_all_order expected non-empty in shenyu-pay");

        var page = footballOrderReadService.listPayAllOrders(start, end, null, null, 1, 10);
        assertTrue(page.getTotal() > 0);
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
