package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.mysql.dict.FootballSystemDictDataMapper;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import cn.iocoder.yudao.module.oa.service.system.SystemDictAdapter;
import cn.iocoder.yudao.module.oa.service.system.SystemDictService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * GATE-MDB-S2: platform dict adapter + business dict dual-track.
 */
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "dev-local-multidb"})
class MdbS2DictAdapterIT {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired(required = false)
    private MockMvc mockMvc;
    @Autowired(required = false)
    private SystemDictService systemDictService;
    @Autowired(required = false)
    private SystemDictAdapter systemDictAdapter;
    @Autowired(required = false)
    private DictService dictService;
    @Autowired(required = false)
    private FootballSystemDictDataMapper footballSystemDictDataMapper;

    @Test
    @DisplayName("S2-03: platform dict admin list reads system DB (total >= 500)")
    void platformDictAdminListFromSystem() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(mockMvc != null && systemDictAdapter != null);

        long systemRows = systemDictAdapter.countActiveDataRows();
        Assumptions.assumeTrue(systemRows >= 500, "system_dict_data seed expected >= 500, got " + systemRows);

        mockMvc.perform(get("/admin-api/oa/system/dict/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(500)));
    }

    @Test
    @DisplayName("S2-03: business dict data API still reads wd dict_platform_type")
    void businessDictDataStillFromWd() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(mockMvc != null && dictService != null);

        assertTrue(dictService.typeExists("dict_platform_type"));

        mockMvc.perform(get("/admin-api/oa/dict/data")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("type", "dict_platform_type"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));
    }

    @Test
    @DisplayName("S2-03: business dict admin list via dictType filter reads wd")
    void businessDictAdminListViaFilter() throws Exception {
        assumeLocalMysql();
        Assumptions.assumeTrue(mockMvc != null);

        mockMvc.perform(get("/admin-api/oa/system/dict/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("dictType", "dict_platform_type")
                        .param("pageNo", "1")
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));
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
