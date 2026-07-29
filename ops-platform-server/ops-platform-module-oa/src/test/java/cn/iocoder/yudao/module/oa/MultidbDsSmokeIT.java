package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * GATE-MDB-S1: post C-WP7 — OPS 进程仅 master(wd)；跨库经 Feign。
 */
@SpringBootTest(properties = "spring.flyway.enabled=false")
@ActiveProfiles({"dev", "dev-local-multidb"})
class MultidbDsSmokeIT {

    @Test
    void contextLoadsWithMasterOnlyDatasource() {
        // Profile dev-local-multidb 不再配置 system/member/mp/pay 数据源；上下文应正常启动。
    }
}
