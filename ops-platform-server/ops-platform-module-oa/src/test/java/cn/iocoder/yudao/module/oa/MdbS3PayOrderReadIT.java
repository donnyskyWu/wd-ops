package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.service.football.FootballOrderReadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * G-PAY-01 cutover: order list Feign-only (pay-server :48085); no @DS pay mapper.
 */
@SpringBootTest
@ActiveProfiles({"dev", "dev-nacos", "dev-nacos-local", "dev-local-multidb"})
class MdbS3PayOrderReadIT {

    private static final Long TENANT = 1L;

    @Autowired(required = false)
    private FootballOrderReadService footballOrderReadService;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(TENANT);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("S3-04: football order list via PayOrderApi Feign")
    void payOrderListViaFeign() throws Exception {
        assumePayServerUp();
        Assumptions.assumeTrue(footballOrderReadService != null);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        var page = footballOrderReadService.listPayAllOrders(start, end, null, null, 1, 10);
        assertNotNull(page);
    }

    private void assumePayServerUp() throws Exception {
        URL url = new URL("http://127.0.0.1:48085/rpc-api/pay/order/page");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        try {
            conn.getResponseCode();
            Assumptions.assumeTrue(true);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "pay-server :48085 not available: " + ex.getMessage());
        } finally {
            conn.disconnect();
        }
    }
}
