package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.mysql.account.MpAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.FootballSystemDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.smoke.PayDsSmokeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.smoke.SystemDsSmokeMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * GATE-MDB-S1 auxiliary: four Football DS connectivity smoke (member/mp/pay/system).
 */
@SpringBootTest
@ActiveProfiles({"dev", "dev-local-multidb"})
class MultidbDsSmokeIT {

    @Autowired(required = false)
    private AuthorUserMapper authorUserMapper;
    @Autowired(required = false)
    private MpAccountMapper mpAccountMapper;
    @Autowired(required = false)
    private PayDsSmokeMapper payDsSmokeMapper;
    @Autowired(required = false)
    private SystemDsSmokeMapper systemDsSmokeMapper;
    @Autowired(required = false)
    private FootballSystemDictDataMapper footballSystemDictDataMapper;

    @Test
    void allFourDatasourcesReachable() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(authorUserMapper != null && mpAccountMapper != null
                && payDsSmokeMapper != null && systemDsSmokeMapper != null
                && footballSystemDictDataMapper != null);

        authorUserMapper.selectCount(null);
        mpAccountMapper.selectCount(null);
        payDsSmokeMapper.selectOne();
        systemDsSmokeMapper.selectOne();
        footballSystemDictDataMapper.countActiveRows();
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
